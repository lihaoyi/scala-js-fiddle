package fiddle
import scala.scalajs.js
import js.Dynamic.{global, literal => lit}
import org.scalajs.dom
import collection.mutable
import scalatags.{HtmlTag, Modifier, Tags2}
import scala.concurrent.{Promise, Future}
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.async.Async.{async, await}
import scalatags.all._
import rx._
object Page{

  def body = Seq(
    pre(id:="editor"),
    pre(id:="logspam"),
    div(id:="sandbox")(
      canvas(id:="canvas", style:="position: absolute"),
      div(
        id:="output",
        color:="lightgrey",
        padding:="25px",
        boxSizing:="border-box"
      )
    )
  )

  val red = span(color:="#ffaaaa")
  val blue = span(color:="#aaaaff")
  val green = span(color:="#aaffaa")
}

import Page.{red, green, blue}

object Output{
  private[this] var outputted = div()
  def println(s: Any*) = {
    val modifier = div(s.map{
      case t: Modifier => t
      case x => x.toString: Modifier
    }:_*)
    outputted = modifier.transform(outputted)
    Client.output.innerHTML = outputted.toString()
    Client.output.scrollTop = Client.output.scrollHeight - Client.output.clientHeight
  }
  def clear(){
    outputted = div()
    Client.output.innerHTML = outputted.toString()
  }
  def scroll(px: Int){
    Client.output.scrollTop = Client.output.scrollTop + px
  }
}



object Client{

  lazy val sandbox = js.Dynamic.global.sandbox.asInstanceOf[dom.HTMLDivElement]
  lazy val canvas = js.Dynamic.global.canvas.asInstanceOf[dom.HTMLCanvasElement]
  lazy val renderer = canvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]
  
  lazy val output = js.Dynamic.global.output.asInstanceOf[dom.HTMLDivElement]

  def logspam = js.Dynamic.global.logspam.asInstanceOf[dom.HTMLPreElement]
  dom.document.body.innerHTML = Page.body.mkString

  val cloned = output.innerHTML

  var autocompleted: Option[Completer] = None

  def clear() = {
    for(i <- 0 until 1000){
      dom.clearInterval(i)
      dom.clearTimeout(i)
    }
    Output.clear()
    output.innerHTML = cloned
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
  val (defaultGistId, defaultFile) = ("9405209", "LandingPage.scala")

  def main(args: Array[String]): Unit = {
    clear()
    if (dom.document.location.pathname == "/") load(defaultGistId, Some(defaultFile))
    else {
      val path = dom.document.location.pathname.drop("/gist/".length)
      val parts = path.split("/")
      load(
        parts(0), parts.lift(1)
      )
    }
  }
  def load(gistId: String, file: Option[String]): Unit = async {

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
    val result = js.JSON.parse(res.responseText)

    val allFiles = result.files.asInstanceOf[js.Dictionary[js.Dynamic]]
    val mainFile = allFiles(file.getOrElse(""): String)

    val firstFile = allFiles(js.Object.keys(allFiles)(0).toString)
    val content = (if (!mainFile.isInstanceOf[js.Undefined]) mainFile else firstFile).selectDynamic("content").toString

    Editor.sess.setValue(content)
    saved(content) = gistId
    compile()
  }.recover{ case e =>
    logln(red(s"Loading failed with $e, Falling back to default example."))
    load(defaultGistId, Some(defaultFile))
  }

  def compile(): Future[Unit] = async {

    val code = Editor.sess.getValue().asInstanceOf[String]
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
  }.recover{ case e =>
    e.printStackTrace()
    e
  }

  def complete() = async {
    if (autocompleted == None){
      logln("Completing...")
      val (row, column) = Editor.rowCol()

      val line = Editor.aceDoc.getLine(row).asInstanceOf[js.String]
      val code = Editor.sess.getValue().asInstanceOf[String]
      val intOffset = code.split("\n").take(row).map(_.length + 1).sum + column

      val flag = if(code.take(intOffset).endsWith(".")) "member" else "scope"
      val xhr = await(Ajax.post(
        s"/complete/$flag/$intOffset",
        code
      ))
      val result = js.JSON.parse(xhr.responseText).asInstanceOf[js.Array[String]]
      val identifier = line.substring(
        Completer.startColumn(line, column),
        Completer.endColumn(line, column)
      )

      Editor.aceDoc.removeInLine(row, column, Completer.endColumn(line, column))
      val newAutocompleted = new Completer(
        Var(result.toList.find(_.toLowerCase().startsWith(identifier.toLowerCase())).getOrElse(result(0))),
        row,
        Completer.startColumn(line, column),
        result.toList,
        () => autocompleted = None
      )
      autocompleted = Some(newAutocompleted)
      newAutocompleted.renderAll()
    }
  }
  def save(): Unit = async{
    await(compile())
    val code = Editor.sess.getValue().asInstanceOf[String]
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

    val fiddleUrl = dom.document.location.origin + "/gist/" + resultId
    logln("Saved as ", a(href:=fiddleUrl)(fiddleUrl))
    dom.history.pushState(null, null, "/gist/" + resultId)
    val gistUrl = "https://gist.github.com/" + resultId
    logln("Or view on github at ", a(href:=gistUrl)(gistUrl))
  }
}
