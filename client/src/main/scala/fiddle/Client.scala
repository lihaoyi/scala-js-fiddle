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
import scalatags._
import rx._
import scala.scalajs.js.annotation.JSExport
import org.scalajs.dom.extensions.Ajax


@JSExport
object Output{
  @JSExport
  def printlnImpl(s: String) = {
    val elem = dom.document.createElement("div")
    elem.textContent = s
    Client.output.appendChild(elem)
    Client.output.scrollTop = Client.output.scrollHeight - Client.output.clientHeight
  }
  @JSExport
  def renderlnImpl(s: String) = {
    val elem = dom.document.createElement("div")
    elem.innerHTML = s
    Client.output.appendChild(elem)
    Client.output.scrollTop = Client.output.scrollHeight - Client.output.clientHeight
  }
  @JSExport
  def clear() = {
    Client.output.innerHTML = ""
  }
  @JSExport
  def scroll(px: Int) = {
    Client.output.scrollTop = Client.output.scrollTop + px
  }
  @JSExport
  def renderer = Client.renderer
  @JSExport
  def canvas = Client.canvas
  @JSExport
  def output = this

  def red = span(color:="#ffaaaa")
  def blue = span(color:="#aaaaff")
  def green = span(color:="#aaffaa")
}
import Output.{red, green, blue}

@JSExport
object Client{

  def sandbox = js.Dynamic.global.sandbox.asInstanceOf[dom.HTMLDivElement]
  def canvas = js.Dynamic.global.canvas.asInstanceOf[dom.HTMLCanvasElement]
  def renderer = canvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]

  def output = js.Dynamic.global.output.asInstanceOf[dom.HTMLDivElement]

  def logspam = js.Dynamic.global.logspam.asInstanceOf[dom.HTMLPreElement]

  var autocompleted: Option[Completer] = None

  def clear() = {
    for(i <- 0 until 1000){
      dom.clearInterval(i)
      dom.clearTimeout(i)
    }
    Output.clear()
    canvas.height = sandbox.clientHeight
    canvas.width = sandbox.clientWidth
  }
  val fiddleUrl = "http://www.scala-js-fiddle.com"
  val saved = mutable.Map.empty[String, String]
  val compilations = mutable.Map.empty[String, String]
  var logged = div()
  def logln(s: Modifier*): Unit = {
    log(div(s:_*))
  }

  def log(s: Modifier*): Unit = {
    logged = s.foldLeft(logged)((a, b) => b.transform(a))
    logspam.innerHTML = logged.toString()
    logspam.scrollTop = logspam.scrollHeight - logspam.clientHeight
  }
  var exportAction = ""
  @JSExport
  def main(args: js.Any): Unit = {
    args match {
      case args: js.Array[String] =>
        val (gistId, fileName) = args.toSeq match{
          case Nil => ("9689412", Some("LandingPage.scala"))
          case Seq(g) => (g, None)
          case Seq(g, f) => (g, Some(f))
        }
        exportAction = "/export"
        load(gistId, fileName)

        logln("- ", blue("Cmd/Ctrl-Enter"), " to compile & execute, ", blue("Cmd/Ctrl-`"), " for autocomplete.")
        logln("- Go to ", a(href:=fiddleUrl, fiddleUrl), " to find out more.")
      case raw: js.Any =>
        clear()
        val data = raw.asInstanceOf[js.Dictionary[String]]
        Editor.sess.setValue(dom.atob(data("source")))
        compilations(dom.atob(data("source"))) = dom.atob(data("compiled"))
        js.eval(dom.atob(data("compiled")))
        if (""+data("export") == "true"){
          dom.console.log("A")
          exportAction = "http://localhost:8080/import"
          Editor.editor.setReadOnly(true)
          logln("- Code snippet exported from ", a(href:=fiddleUrl, fiddleUrl))
          logln("- ", blue("Ctrl/Cmd-S"), " and select ", blue("Web Page, Complete"), " to save for offline use")
          logln("- Click ", a(id:="editLink", href:="javascript:", "here"), " to edit a copy online")
          dom.document
             .getElementById("editLink")
             .asInstanceOf[dom.HTMLAnchorElement]
             .onclick = (e: dom.MouseEvent) => export()
        }else{
          dom.console.log("B")
          exportAction = "/export"
          logln("- ", blue("Cmd/Ctrl-Enter"), " to compile & execute, ", blue("Cmd/Ctrl-`"), " for autocomplete.")
          logln("- Go to ", a(href:=fiddleUrl, fiddleUrl), " to find out more.")
        }
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
    val compiled = await(compile("/optimize"))
    clear()
    js.eval(compiled)
  }.recover{ case e =>
    logln(red(s"Loading failed with $e,"))
    e.printStackTrace()
    Editor.sess.setValue("")
  }

  def compile(endpoint: String): Future[String] = async {
    val code = Editor.sess.getValue().asInstanceOf[String]
    log("Compiling... ")
    val res = await(Ajax.post(endpoint, code))

    val result = js.JSON.parse(res.responseText)
    if (result.logspam.toString != ""){
      logln(result.logspam.toString)
    }
    if(result.success.asInstanceOf[js.Boolean]){
      log(green("Success"))
      logln()
      ""+result.code
    }else{
      log(red("Failure"))
      throw new Exception("Compilation Failed")
    }
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

  def export() = async {
    logln("Exporting...")
    val source = Editor.sess.getValue().asInstanceOf[String]
    val compiled = compilations.get(source) match{
      case Some(x) => x
      case None => await(compile("/optimize"))
    }

    global.exportCompiled.value = dom.btoa(compiled)
    global.exportSource.value = dom.btoa(source)
    global.exportForm.action = exportAction
    global.exportForm.submit()
  }.recover{ case e =>
    logln(red(s"Exporting failed with $e,"))
    e.printStackTrace()
    Editor.sess.setValue("")
  }

  def save(): Unit = async{
    await(compile("/preoptimize"))
    val code = Editor.sess.getValue().asInstanceOf[String]
    val resultId = saved.lift(code) match {
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