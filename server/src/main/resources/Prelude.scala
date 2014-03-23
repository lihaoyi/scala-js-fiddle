import scala.scalajs.js.annotation.{JSName, JSExport}
@JSName("Output2")
object Output extends scalajs.js.Object{
  def printlnImpl(s: String*): Unit = ???
  def renderlnImpl(s: String*): Unit = ???
  def clear(): Unit = ???
  def scroll(px: Int): Unit = ???
  def output: this.type = ???
  def renderer: org.scalajs.dom.CanvasRenderingContext2D = ???
  def canvas: org.scalajs.dom.HTMLCanvasElement = ???
}
import Output._
import fiddle.Macros.{println, renderln}
object Stuff{
  import scalatags.all._
  def red = span(color:="#ffaaaa")
  def blue = span(color:="#aaaaff")
  def green = span(color:="#aaffaa")
}
import Stuff._
