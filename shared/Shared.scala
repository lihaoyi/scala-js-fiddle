package fiddle
object Shared{
  val prelude =
    """
      |import fiddle.Page._
      |import scalatags.JsDom._
      |import org.scalajs.dom
      |import scalajs.js
      |
    """.stripMargin

  val default = """
    |import scalajs.js
    |object ScalaJSExample extends js.JSApp{
    |  def main() = {
    |    println("Looks like there was an error loading the default Gist!")
    |    println("Loading an empty application so you can get started")
    |  }
    |}
  """.stripMargin

  val gistId = "9443f8e0ecc68d1058ad"

  val url = "http://localhost:8080"
}
