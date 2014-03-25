package fiddle

import scalatags.all._
import scalatags.Styles.color
import scala.scalajs.js
import org.scalajs.dom
import scala.scalajs.js.annotation.JSExport
import scalatags.Modifier

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

  @JSExport
  def printlnImpl(s: String) = {
    val elem = dom.document.createElement("div")
    elem.textContent = s
    output.appendChild(elem)
    output.scrollTop = output.scrollHeight - output.clientHeight
  }
  @JSExport
  def renderlnImpl(s: String) = {
    val elem = dom.document.createElement("div")
    elem.innerHTML = s
    output.appendChild(elem)
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
