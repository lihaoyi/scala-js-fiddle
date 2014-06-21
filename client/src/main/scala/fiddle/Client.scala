package fiddle
import acyclic.file
import scala.scalajs.js
import scala.scalajs.js.Dynamic.{literal => lit, _}
import org.scalajs.dom
import scala.concurrent.{Promise, Future}
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import scala.async.Async.{async, await}
import scalatags.JsDom.all._
import scalatags.JsDom._
import rx._
import scala.scalajs.js.annotation.JSExport
import org.scalajs.dom.extensions.Ajax
import Page.fiddleUrl
import scala.Some
import JsVal.jsVal2jsAny
import Client.RedLogger
import scala.Some

@JSExport
object Checker{
  /**
   * Deadline by which the user code must complete execution.
   */
  private[this] var endTime = 0.0
  /**
   * Switch to flip to once you have run out of time to make
   * `check` fail every single time, ensuring you get thrown out
   * of the user code
   */
  private[this] var dead = false
  /**
   * Used to avoid doing an expensive `currentTimeMillis` check on every call,
   * and instead doing one every N calls.
   */
  private[this] var count = 0
  @JSExport
  def check(): Unit = {
    count += 1
    if (count % 1000 == 0 && js.Date.now() > endTime || dead) {
      dead = true
      Client.clearTimeouts()
      js.eval("""throw new Error("Time's Up! Your code took too long to run.")""")
    }
  }

  @JSExport
  def reset(max: Int) = {
    count = 0
    dead = false
    endTime = math.max(js.Date.now() + max, endTime)
  }

  def scheduleResets() = {
    dom.setInterval(() => Checker.reset(1000), 100)
  }
}

class Client(){
  import Page.{log, logln, red, blue, green}
  Client.scheduleResets()
  val command = Channel[(String, String)]()

  def exec(s: String) = {
    Client.clear()
    Client.scheduleResets()

    Checker.reset(1000)
    try{
      js.eval(s"""(function(ScalaJS){
        $s;
        ScalaJSExample().main();
      })""").asInstanceOf[js.Function1[js.Any, js.Any]](storedScalaJS)
    }catch{case e: Throwable =>
      Client.logError(e.toString())
    }
  }
  val instrument = "c"
  var compileEndpoint = s"/fastOpt"
  var extdeps = ""

  lazy val extdepsLoop = task*async{
    extdeps = await(Ajax.post("/fullOpt")).responseText
    compileEndpoint = s"/compile"
  }
  var storedScalaJS: js.Any = ""
  val compilationLoop = task*async{
    val (code, cmd) = await(command())
    await(compile(code, cmd)).foreach(exec)

    while(true){
      val (code, cmd) = await(command())
      val compiled = await(compile(code, cmd))
      if (extdeps != ""){
        Checker.reset(5000)
        storedScalaJS = js.eval(s"""(function(){
          $extdeps
          return ScalaJS
        }).call(window)""")
        extdeps = ""
      }
      compiled.foreach(exec)
      extdepsLoop
    }
  }

  val editor: Editor = new Editor(Seq(
    ("Compile", "Enter", () => command.update((editor.code, compileEndpoint))),
    ("FastOptimize", "Alt-Enter", () => command.update((editor.code, s"/fastOpt"))),
    ("FullOptimize", "Shift-Enter", () => command.update((editor.code, "/fullOpt"))),
    ("Save", "S", save),
    ("Complete", "Space", () => editor.complete()),
    ("Javascript", "J", () => viewJavascript(s"/compile")),
    ("FastOptimizeJavascript", "Alt-J", () => viewJavascript(s"/fastOpt")),
    ("FullOptimizedJavascript", "Shift-J", () => viewJavascript(s"/fullOpt")),
    ("Export", "E", export)
  ), complete, RedLogger)

  logln("- ", blue("Cmd/Ctrl-Enter"), " to compile & execute, ", blue("Cmd/Ctrl-Space"), " for autocomplete.")
  logln("- Go to ", a(href:=fiddleUrl, fiddleUrl), " to find out more.")

  def compile(code: String, endpoint: String): Future[Option[String]] = async {
    if (code == "") None
    else {
      log(s"Compiling with $endpoint... ")
      val res = await(Ajax.post(endpoint, code))
      val result = JsVal.parse(res.responseText)
      if (result("logspam").asString != ""){
        logln(result("logspam").asString)
      }
      if(result("success").asBoolean) log(green("Success"))
      else log(red("Failure"))
      logln()
      result.get("code").map(_.asString)
    }
  }

  def viewJavascript(endpoint: String) = task*async {
    await(compile(editor.code, endpoint)).foreach{ compiled  =>
      Client.clear()
      Page.output.innerHTML = Page.highlight(compiled , "ace/mode/javascript")
    }
  }

  def complete() = async {
    log("Completing... ")

    val code = editor.sess.getValue().asInstanceOf[String]

    val intOffset = editor.column + code.split("\n")
                                        .take(editor.row)
                                        .map(_.length + 1)
                                        .sum

    val flag = if(code.take(intOffset).endsWith(".")) "member" else "scope"


    val xhr = await(Ajax.post(s"/complete/$flag/$intOffset", code))
    log("Done")
    logln()
    js.JSON
      .parse(xhr.responseText)
      .asInstanceOf[js.Array[js.Array[String]]]
      .toSeq
      .map(_.toSeq)
  }

  def export(): Unit = task*async {
    logln("Exporting...")
    await(compile(editor.code, "/optimize")).foreach{ code =>
      Util.Form.post("/export",
        "source" -> editor.code,
        "compiled" -> code
      )
    }
  }

  def save(): Unit = task*async{
    await(compile(editor.code, "/optimize"))
    val data = JsVal.obj(
      "description" -> "Scala.jsFiddle gist",
      "public" -> true,
      "files" -> JsVal.obj(
        "Main.scala" -> JsVal.obj(
          "content" -> editor.code
        )
      )
    ).toString()

    val res = await(Ajax.post("https://api.github.com/gists", data = data))
    val result = JsVal.parse(res.responseText)
    Util.Form.get("/gist/" + result("id").asString)
  }
}

@JSExport
object Client{
  implicit val RedLogger = new Logger(logError)
  import Page.{canvas, sandbox, logln, red, blue, green}
  dom.onerror = ({(event: dom.Event, source: js.String, fileno: js.Number, columnNumber: js.Number) =>
    dom.console.log("dom.onerror")
    Client.logError(event.toString())
  }: js.Function4[dom.Event, js.String, js.Number, js.Number, Unit]).asInstanceOf[dom.ErrorEventHandler]


  @JSExport
  def logError(s: String): Unit = {
    logln(red(s))
  }
  @JSExport
  def clearTimeouts() = {
    for(i <- -100000 until 100000){
      dom.clearInterval(i)
      dom.clearTimeout(i)
    }
    Client.scheduleResets()
  }
  def clear() = {
    clearTimeouts()
    Page.clear()

    canvas.height = sandbox.clientHeight
    canvas.width = sandbox.clientWidth
  }

  @JSExport
  def gistMain(args: js.Array[String]): Unit = task*async{

    Editor.initEditor
    val (gistId, fileName) = args.toSeq match{
      case Nil => ("9759723", Some("LandingPage.scala"))
      case Seq(g) => (g, None)
      case Seq(g, f) => (g, Some(f))
    }
    val src = await(load(gistId, fileName))
    val client = new Client()
    client.editor.sess.setValue(src)
    client.command.update((src, "/optimize"))
  }

  @JSExport
  def importMain(): Unit = {
    clear()
    val client = new Client()
    client.command.update(("", "/compile"))
    js.eval(Page.compiled)
  }

  def load(gistId: String, file: Option[String]): Future[String] = async {
    val gistUrl = "https://gist.github.com/" + gistId
    logln(
      "Loading ",
      file.fold(span)(s => span(
        a(href:=gistUrl + "#file-" + s.toLowerCase.replace('.', '-'))(s),
        " from "
      )),
      a(href:=gistUrl)(gistUrl),
      "..."
    )

    val res = await(Ajax.get("https://api.github.com/gists/" + gistId))
    val result = JsVal.parse(res.responseText)
    val mainFile = result("files").get(file.getOrElse(""))
    val firstFile = result("files").values(0)
    mainFile.getOrElse(firstFile)("content").asString
  }
  def scheduleResets() = {
    dom.setInterval(() => Checker.reset(1000), 100)
  }
}