package fiddle
import acyclic.file
import scala.scalajs.js
import scala.scalajs.js.Dynamic.{literal => lit, _}
import org.scalajs.dom
import scala.concurrent.{Promise, Future}
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import scala.async.Async.{async, await}
import scalatags.all._
import scalatags._
import rx._
import scala.scalajs.js.annotation.JSExport
import org.scalajs.dom.extensions.Ajax
import Page.fiddleUrl
import scala.Some
import JsVal.jsVal2jsAny

import scala.Some

class Client(){
  import Page.{log, logln, red, blue, green}

  val command = Channel[String]()

  def exec(s: String) = {
    Client.clear()
    js.eval(s)
  }

  var compileEndpoint = "/preoptimize"
  var extdeps = ""

  lazy val extdepsLoop = task*async{
    extdeps = await(Ajax.post("/extdeps")).responseText
    compileEndpoint = "/compile"
  }

  val compilationLoop = task*async{
    val code = await(command())

    await(compile(code, "/optimize")).foreach(exec)
    while(true){
      val code = await(command())
      val compiled = await(compile(code, compileEndpoint))
      js.eval(extdeps)
      extdeps = ""
      compiled.foreach(exec)
      extdepsLoop
    }
  }

  val editor: Editor = new Editor(Seq(
    ("Compile", "Enter", () => command.update(editor.code)),
    ("Save", "S", save),
    ("Complete", "`", () => editor.complete()),
    ("Javascript", "J", () => viewJavascript("/compile")),
    ("PreOptimizedJavascript", "K", () => viewJavascript("/preoptimize")),
    ("OptimizedJavascript", "L", () => viewJavascript("/optimize")),
    ("Export", "E", export)
  ), complete)

  logln("- ", blue("Cmd/Ctrl-Enter"), " to compile & execute, ", blue("Cmd/Ctrl-`"), " for autocomplete.")
  logln("- Go to ", a(href:=fiddleUrl, fiddleUrl), " to find out more.")

  def compile(code: String, endpoint: String): Future[Option[String]] = async {
    if (code == "") None
    else {
      log("Compiling... ")
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

  import Page.{canvas, sandbox, logln, red, blue, green}

  def clear() = {
    for(i <- 0 until 10000){
      dom.clearInterval(i)
      dom.clearTimeout(i)
    }
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
    client.command.update(src)
  }

  @JSExport
  def importMain(): Unit = {
    clear()
    val client = new Client()
    client.command.update("")
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
}