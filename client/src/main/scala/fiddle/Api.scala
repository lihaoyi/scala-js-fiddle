package fiddle

import scala.scalajs.js.annotation.JSExport
import org.scalajs.dom

@JSExport
object Api {
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
}
