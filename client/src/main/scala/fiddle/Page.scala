package fiddle
import acyclic.file
import scalatags.all._
import scalatags.Styles.color
import scala.scalajs.js
import org.scalajs.dom
import scala.scalajs.js.annotation.JSExport
import scalatags.Modifier

/**
 * API for things that belong to the page, and are useful to both the fiddle
 * client, user code as well as exported read-only pages.
 */
@JSExport
object Page{
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
  import scalatags.raw
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
}
