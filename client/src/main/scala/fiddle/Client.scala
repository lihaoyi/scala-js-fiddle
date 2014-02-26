package fiddle
import scala.scalajs.js
import js.Dynamic.{global, literal => lit}
import org.scalajs.dom
import collection.mutable
import scalatags.Tags2

object Page{
  import scalatags.all._

  def body = Seq(
    pre(id:="editor")(starting),
    pre(id:="logspam")(
      """
        |- Enter your code on the left pane
        |- Shift-Enter to compile and execute your program
        |- Draw pictures on the right pane and see println()s in the browser console
      """.stripMargin
    ),
    div(id:="sandbox")(
      canvas(id:="canvas")
    )
  )
  val starting =
    """
      |import org.scalajs.dom
      |
      |case class Pt(x: Double, y: Double)
      |object Main{
      |  println("Hello!!")
      |  val sandbox = dom.document
      |    .getElementById("canvas")
      |    .asInstanceOf[dom.HTMLCanvasElement]
      |
      |  val renderer = sandbox.getContext("2d")
      |    .asInstanceOf[dom.CanvasRenderingContext2D]
      |
      |  val corners = Seq(
      |    Pt(sandbox.width/2, 0),
      |    Pt(0, sandbox.height),
      |    Pt(sandbox.width, sandbox.height)
      |  )
      |  var p = corners(0)
      |  val (w, h) = (sandbox.height.toDouble, sandbox.height.toDouble)
      |  def main(args: Array[String]): Unit = {
      |    dom.setInterval(() => for(_ <- 0 until 10){
      |      val c = corners(util.Random.nextInt(3))
      |      p = Pt((p.x + c.x) / 2, (p.y + c.y) / 2)
      |      val m = (p.y / h)
      |      val r = 255 - (p.x / w * m * 255).toInt
      |      val g = 255 - ((w-p.x) / w * m * 255).toInt
      |      val b = 255 - ((h - p.y) / h * 255).toInt
      |      renderer.fillStyle = s"rgb($r, $g, $b)"
      |      renderer.fillRect(p.x, p.y, 1, 1)
      |    }, 10)
      |  }
      |}
    """.stripMargin

}
object Client{

  var requestInFlight = false
  lazy val sandbox = js.Dynamic.global.sandbox.asInstanceOf[dom.HTMLDivElement]
  lazy val canvas = js.Dynamic.global.canvas.asInstanceOf[dom.HTMLCanvasElement]
  lazy val logspam = js.Dynamic.global.logspam.asInstanceOf[dom.HTMLPreElement]
  lazy val cache = mutable.Map.empty[String, (String, Option[String])]

  def clear() = {
    canvas.height = sandbox.clientHeight
    canvas.width = sandbox.clientWidth
    for(i <- 0 until 1000){
      dom.clearInterval(i)
      dom.clearTimeout(i)
    }
    sandbox.innerHTML = sandbox.innerHTML
  }

  def main(args: Array[String]): Unit = {
    dom.document.body.innerHTML = Page.body.mkString
    clear()

    val editor = global.ace.edit("editor")
    editor.setTheme("ace/theme/twilight")
    editor.getSession().setMode("ace/mode/scala")
    editor.renderer.setShowGutter(false)

    val callback = { () =>
      println("callback2")
      val code = editor.getSession().getValue().asInstanceOf[String]


      if (!requestInFlight){
        val req = new dom.XMLHttpRequest()
        requestInFlight = true

        req.onreadystatechange = { (e: dom.Event) =>
          logspam.textContent = req.responseText
          logspam.scrollTop = logspam.scrollHeight - logspam.clientHeight

          if (req.readyState.toInt == 4) {
            requestInFlight = false
            val parts = req.responseText.split("\n\n\n\n\n")
            if (parts.length.toInt == 3){
              val res = parts(1)
              clear()
              js.eval(res)
              js.eval("ScalaJS.modules.Main().main()")
              cache(code) = (req.responseText, Some(res))
            }else{
              cache(code) = (req.responseText, None)
            }
          }
        }
        req.open("POST", "/")
        req.send(code)
        logspam.textContent = "Compiling..."
      }
    }


    editor.commands.addCommand(lit(
      name = "saveFile",
      bindKey = lit(
        win = "Ctrl-Enter",
        mac = "Command-Enter",
        sender = "editor|cli"
      ),
      exec = callback: js.Function0[_]
    ))
    callback()
  }
}
