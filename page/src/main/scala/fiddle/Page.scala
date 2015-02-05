package fiddle

import scala.scalajs.js
import js.annotation.JSExport
import org.scalajs.dom
import dom.html
import scalatags.JsDom.all._

/**
 * API for things that belong to the page, and are useful to both the fiddle
 * client, user code as well as exported read-only pages.
 */
@JSExport
object Page{
  val fiddlePrelude = Shared.prelude
  val fiddleUrl = Shared.url
  val fiddleGistId = Shared.gistId

  def red = span(color:="#E95065")
  def blue = span(color:="#46BDDF")
  def green = span(color:="#52D273")


  def sandbox = Util.getElem[html.Div]("sandbox")
  def canvas = Util.getElem[html.Canvas]("canvas")
  def renderer = canvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]
  def output = Util.getElem[html.Div]("output")
  def logspam = Util.getElem[html.Pre]("logspam")
  def source = Util.getElem[html.Div]("source")

  def println(ss: Modifier*) = {
    print(div(ss: _*))
  }

  def print(ss: Modifier*) = {
    ss.foreach(_.applyTo(output))
    output.scrollTop = output.scrollHeight - output.clientHeight
  }

  def clear() = {
    output.innerHTML = ""
    canvas.height = sandbox.clientHeight
    canvas.width = sandbox.clientWidth
    val tmp = renderer.fillStyle
    renderer.fillStyle = "rgb(20, 20, 20)"
    renderer.clearRect(0, 0, 10000, 10000)
    renderer.fillStyle = tmp
  }

  def scroll(px: Int) = {
    dom.console.log("Scrolling", px)
    output.scrollTop = output.scrollTop + px
  }

  def logln(s: Modifier*): Unit = {
    log(div(s:_*))
  }

  def log(s: Modifier*): Unit = {
    s.foreach(_.applyTo(logspam))
    logspam.scrollTop = 1000000000
  }

  val compiled = Util.getElem[html.Div]("compiled").textContent

  @JSExport
  def exportMain(): Unit = {
    dom.console.log("exportMain")
    clear()
    val editor = Util.getElem[html.Div]("editor")
    js.Dynamic.global.require("ace")
    editor.innerHTML = highlight(source.textContent, "ace/mode/scala")
    if(logspam.textContent == "") {
      logln("- Code snippet exported from ", a(href := fiddle.Shared.url, fiddle.Shared.url))
      logln("- ", blue("Ctrl/Cmd-S"), " and select ", blue("Web Page, Complete"), " to save for offline use")
      logln("- Click ", a(id := "editLink", href := "javascript:", "here"), " to edit a copy online")
    }
    dom.document
       .getElementById("editLink")
       .asInstanceOf[html.Anchor]
       .onclick = { (e: dom.MouseEvent) =>
      Util.Form.post(fiddle.Shared.url + "/import",
        "source" -> source.textContent,
        "compiled" -> compiled
      )
    }
  }

  /**
   * Uses the Ace editor to generate a syntax-highlighted
   * HTML from the given blob of code.
   */
  def highlight(source: String, m: String): String = {
    val highlighter = js.Dynamic.global.require("ace/ext/static_highlight")
    val mode = js.Dynamic.global.require(m).Mode
    val theme = js.Dynamic.global.require("ace/theme/twilight")
    val dom =  js.Dynamic.global.require("ace/lib/dom")
    val highlighted = highlighter.render(
      source,
      js.Dynamic.newInstance(mode)(),
      theme,
      1,
      true
    )
    dom.importCssString(highlighted.css, "ace_highlight")
    highlighted.html.toString

  }

}
