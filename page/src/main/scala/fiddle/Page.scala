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

  def sandbox = Util.getElem[dom.HTMLDivElement]("sandbox")
  @JSExport
  def canvas = Util.getElem[dom.HTMLCanvasElement]("canvas")
  @JSExport
  def renderer = canvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]
  def output = Util.getElem[dom.HTMLDivElement]("output")
  def logspam = Util.getElem[dom.HTMLPreElement]("logspam")
  def source = Util.getElem[dom.HTMLDivElement]("source")

  @JSExport
  def println(ss: String*) = {
    renderln(div(ss).toString)
  }

  @JSExport
  def print(ss: String*) = {
    for (s <- ss) output.appendChild(dom.document.createTextNode(s))
    output.scrollTop = output.scrollHeight - output.clientHeight
  }

  @JSExport
  def renderln(ss: String*) = {
    render(div(ss.map(raw)).toString)
  }
  @JSExport
  def render(ss: String*) = {
    for (s <- ss) output.appendChild(Util.createDom(s))
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
    editor.getSession().setValue(source.textContent)
    editor.renderer.setShowGutter(false)
    editor
  }
}
