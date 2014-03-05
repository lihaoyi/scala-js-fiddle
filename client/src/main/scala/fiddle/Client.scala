package fiddle
import scala.scalajs.js
import js.Dynamic.{global, literal => lit}
import org.scalajs.dom
import collection.mutable
import scalatags.{Modifier, Tags2}
import scala.concurrent.{Promise, Future}
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.async.Async.{async, await}
import scalatags.all._

object Page{

  def body = Seq(
    pre(id:="editor"),
    pre(id:="logspam"),
    div(id:="sandbox")(
      canvas(id:="canvas", style:="position: absolute")
    )
  )

  val red = span(color:="#ff8888")
  val blue = span(color:="#8888ff")
  val green = span(color:="#88ff88")
}
import Page.{red, green, blue}
object Client{

  def sandbox = js.Dynamic.global.sandbox.asInstanceOf[dom.HTMLDivElement]
  def canvas = js.Dynamic.global.canvas.asInstanceOf[dom.HTMLCanvasElement]
  def logspam = js.Dynamic.global.logspam.asInstanceOf[dom.HTMLPreElement]
  dom.document.body.innerHTML = Page.body.mkString

  val cloned = sandbox.innerHTML
  lazy val editor: js.Dynamic = {
    val editor = global.ace.edit("editor")
    editor.setTheme("ace/theme/twilight")
    editor.getSession().setMode("ace/mode/scala")
    editor.renderer.setShowGutter(false)
    val bindings = Seq(
      ("Compile", "Enter", compile _),
      ("Save", "S", save _),
      ("Complete", "`", complete _)
    )
    for ((name, key, func) <- bindings){
      editor.commands.addCommand(lit(
        name = name,
        bindKey = lit(
          win = "Ctrl-" + key,
          mac = "Command-" + key,
          sender = "editor|cli"
        ),
        exec = func: js.Function0[_]
      ))
    }

    editor.getSession().setTabSize(2)
    editor
  }

  def clear() = {
    for(i <- 0 until 1000){
      dom.clearInterval(i)
      dom.clearTimeout(i)
    }
    sandbox.innerHTML = cloned
    canvas.height = sandbox.clientHeight
    canvas.width = sandbox.clientWidth
  }
  val fiddleUrl = "http://www.scala-js-fiddle.com"
  val saved = mutable.Map.empty[String, String]
  var logged = div(
    div("- ", blue("Cmd/Ctrl-Enter"), " to compile & execute, ", blue("Cmd/Ctrl-S"), " to save to a Gist"),
    div("- Go to ", a(href:=fiddleUrl, fiddleUrl), " to find out more.")
  )
  def logln(s: Modifier*): Unit = {
    log(div(s:_*))
  }
  def log(s: Modifier*): Unit = {
    logged = s.foldLeft(logged)((a, b) => b.transform(a))
    logspam.innerHTML = logged.toString()
    logspam.scrollTop = logspam.scrollHeight - logspam.clientHeight
  }
  val defaultGistId = "9350814"
  def main(args: Array[String]): Unit = {

    clear()
    if (dom.document.location.pathname == "/") load(defaultGistId)
    else load(dom.document.location.pathname.drop(6))
  }
  def load(gistId: String): Unit = async {

    val gistUrl = "https://gist.github.com/" + gistId

    logln("Loading code from gist ", a(href:=gistUrl)(gistUrl), "...")

    val res = await(Ajax.get("https://api.github.com/gists/" + gistId))
    val result = js.JSON.parse(res.responseText)

    val allFiles = result.files.asInstanceOf[js.Dictionary[js.Dynamic]]

    val mainFile = allFiles("Main.scala")

    val firstFile = allFiles(js.Object.keys(allFiles)(0).toString)

    val content = (if (!mainFile.isInstanceOf[js.Undefined]) mainFile else firstFile).selectDynamic("content").toString

    editor.getSession().setValue(content)
    saved(content) = gistId
    compile()
  }.onFailure{ case e =>
    logln(red(s"Loading failed with $e, Falling back to default example."))
    load(defaultGistId)
  }
  def compile(): Future[Unit] = async {
    val code = editor.getSession().getValue().asInstanceOf[String]
    log("Compiling... ")
    val res = await(Ajax.post("/compile", code))

    val result = js.JSON.parse(res.responseText)
    if (result.logspam.toString != ""){
      logln(result.logspam.toString)
    }
    if(result.success.asInstanceOf[js.Boolean]){
      clear()
      js.eval(""+result.code)
      log(green("Success"))
    }else{
      log(red("Failure"))
    }
    logln()
  }.transform(x => x, { case e =>
    e.printStackTrace()
    e
  })

  def complete() = async {

    val Seq(row, col) = Seq(
      editor.getCursorPosition().row,
      editor.getCursorPosition().column
    ).map(_.asInstanceOf[js.Number].toInt)

    val code = editor.getSession().getValue().asInstanceOf[String]
    val intOffset =code.split("\n").take(row).map(_.length + 1).sum + col
    logln("Completing...")
    val xhr = await(Ajax.post("/complete/" + intOffset, code))
    val result = js.JSON.parse(xhr.responseText)
    dom.console.log(result)

  }
  def save(): Unit = async{
    await(compile())
    val code = editor.getSession().getValue().asInstanceOf[String]
    val resultId = saved.lift(code) match{
      case Some(id) => id
      case None =>
        val res = await(Ajax.post("https://api.github.com/gists",
          data = js.JSON.stringify(
            lit(
              description = "Scala.jsFiddle gist",
              public = true,
              files = js.Dictionary(
                ("Main.scala": js.String) -> lit(
                  content = code
                )
              )
            )
          )
        ))
        val result = js.JSON.parse(res.responseText)
        saved(code) = result.id.toString
        result.id
    }

    val fiddleUrl = "http://" + dom.document.location.host + "/gist/" + resultId
    logln("Saved as ", a(href:=fiddleUrl)(fiddleUrl))
    dom.history.pushState(null, null, "/gist/" + resultId)
    val gistUrl = "https://gist.github.com/" + resultId
    logln("Or view on github at ", a(href:=gistUrl)(gistUrl))
  }
}
