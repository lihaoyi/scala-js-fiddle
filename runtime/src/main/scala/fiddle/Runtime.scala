import scala.scalajs.js.annotation.{JSName, JSExport}
@JSName("Page2")
object Page extends scalajs.js.Object{
  def println(s: String*): Unit = ???
  def renderln(s: String*): Unit = ???
  def print(s: String*): Unit = ???
  def render(s: String*): Unit = ???
  def clear(): Unit = ???
  def scroll(px: Int): Unit = ???
  def output: this.type = ???
  def renderer: org.scalajs.dom.CanvasRenderingContext2D = ???
  def canvas: org.scalajs.dom.HTMLCanvasElement = ???
}


object Stuff{
  import scalatags.all._
  def red = span(color:="#ffaaaa")
  def blue = span(color:="#aaaaff")
  def green = span(color:="#aaffaa")
}
