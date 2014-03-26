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
import Client.fiddleUrl
import scala.Some

class Client(){
  import Page._
  val exportAction = "/export"
  var autocompleted: Option[Completer] = None
  val editor = new Editor(this)

  logln("- ", blue("Cmd/Ctrl-Enter"), " to compile & execute, ", blue("Cmd/Ctrl-`"), " for autocomplete.")
  logln("- Go to ", a(href:=fiddleUrl, fiddleUrl), " to find out more.")

  def compile(endpoint: String): Future[String] = async {
    val code = editor.sess.getValue().asInstanceOf[String]
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
      val (row, column) = editor.rowCol()

      val line = editor.aceDoc.getLine(row).asInstanceOf[js.String]
      val code = editor.sess.getValue().asInstanceOf[String]
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

      editor.aceDoc.removeInLine(row, column, Completer.endColumn(line, column))
      val newAutocompleted = new Completer(
        this,
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
    val source = editor.sess.getValue().asInstanceOf[String]
    val compiled = await(compile("/optimize"))
    Util.Form.post("/export",
      "source" -> source,
      "compiled" -> compiled
    )
  }

  def save(): Unit = async{
    await(compile("/optimize"))
    val code = editor.sess.getValue().asInstanceOf[String]
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
    val resultId = result.id
    Util.Form.get("/gist/" + resultId)
  }
}

@JSExport
object Client{
  val fiddleUrl = "http://www.scala-js-fiddle.com"
  import Page._

  def clear() = {
    for(i <- 0 until 1000){
      dom.clearInterval(i)
      dom.clearTimeout(i)
    }
    Page.clear()
    canvas.height = sandbox.clientHeight
    canvas.width = sandbox.clientWidth
  }

  @JSExport
  def gistMain(args: js.Array[String]): Unit = async{
    Editor.init
    dom.console.log("gistMain")
    val (gistId, fileName) = args.toSeq match{
      case Nil => ("9759723", Some("LandingPage.scala"))
      case Seq(g) => (g, None)
      case Seq(g, f) => (g, Some(f))
    }
    async{
      val client = new Client()
      val src = await(load(gistId, fileName))
      client.editor.sess.setValue(src)
      val compiled = await(client.compile("/optimize"))
      clear()
      js.eval(compiled)
    }.recover{ case e =>
      println("DIED "+ e)
      e.printStackTrace()
    }
  }

  @JSExport
  def importMain() = {
    clear()
    new Client()
  }

  @JSExport
  def exportMain() = {
    clear()
    val editor = Editor.init
    logln("- Code snippet exported from ", a(href:=fiddleUrl, fiddleUrl))
    logln("- ", blue("Ctrl/Cmd-S"), " and select ", blue("Web Page, Complete"), " to save for offline use")
    logln("- Click ", a(id:="editLink", href:="javascript:", "here"), " to edit a copy online")
    dom.document
      .getElementById("editLink")
      .asInstanceOf[dom.HTMLAnchorElement]
      .onclick = { (e: dom.MouseEvent) =>
      Util.Form.post("http://localhost:8080/import",
        "source" -> editor.getSession().getValue().toString,
        "compiled" -> dom.document.getElementById("compiled").innerHTML
      )
    }
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
    val result = js.JSON.parse(res.responseText)
    val allFiles = result.files.asInstanceOf[js.Dictionary[js.Dynamic]]
    val mainFile = allFiles(file.getOrElse(""): String)
    val firstFile = allFiles(js.Object.keys(allFiles)(0).toString)
    (if (!mainFile.isInstanceOf[js.Undefined]) mainFile else firstFile).selectDynamic("content").toString
  }.recover{ case e =>
    logln(red(s"Loading failed with $e,"))
    e.printStackTrace()
    ""
  }
}