package fiddle

import scala.scalajs.js
import js.annotation.JSExport
import org.scalajs.dom
import scalatags._
import scalatags.all._
import scala.scalajs.js.Dynamic._

/**
 * API for things that belong to the page, and are useful to both the fiddle
 * client, user code as well as exported read-only pages.
 */
@JSExport
object Page{
  val fiddleUrl = "http://www.scala-js-fiddle.com"
  def red = span(color:="#ffaaaa")
  def blue = span(color:="#aaaaff")
  def green = span(color:="#aaffaa")

  def sandbox = js.Dynamic.global.sandbox.asInstanceOf[dom.HTMLDivElement]
  @JSExport
  def canvas = js.Dynamic.global.canvas.asInstanceOf[dom.HTMLCanvasElement]
  @JSExport
  def renderer = canvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]

  def output = js.Dynamic.global.output.asInstanceOf[dom.HTMLDivElement]

  def logspam = js.Dynamic.global.logspam.asInstanceOf[dom.HTMLPreElement]
  @JSExport
  def println(s: String) = {
    renderln(div(s).toString)
  }

  @JSExport
  def print(s: String) = {
    output.appendChild(dom.document.createTextNode(s))
    output.scrollTop = output.scrollHeight - output.clientHeight
  }

  @JSExport
  def renderln(s: String) = {
    render(div(raw(s)).toString)
  }
  @JSExport
  def render(s: String) = {
    output.appendChild(Util.createDom(s))
    output.scrollTop = output.scrollHeight - output.clientHeight
  }
  @JSExport
  def clear() = {
    output.innerHTML = ""
    canvas.height = sandbox.clientHeight
    canvas.width = sandbox.clientWidth
  }
  @JSExport
  def scroll(px: Int) = {
    output.scrollTop = output.scrollTop + px
  }
  
  def page = this

  var logged = div()
  def logln(s: Modifier*): Unit = {
    log(div(s:_*))
  }

  def log(s: Modifier*): Unit = {
    logged = s.foldLeft(logged)((a, b) => b.transform(a))
    logspam.innerHTML = logged.toString()
    logspam.scrollTop = logspam.scrollHeight - logspam.clientHeight
  }

  @JSExport
  def exportMain(): Unit = {
    dom.console.log("exportMain")
    clear()
    val editor = initEditor

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

  lazy val initEditor: js.Dynamic = {
    val editor = global.ace.edit("editor")
    editor.setTheme("ace/theme/twilight")
    editor.getSession().setMode("ace/mode/scala")
    editor.renderer.setShowGutter(false)
    editor
  }
}
