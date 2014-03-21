package fiddle

import utest._
import java.net.URLClassLoader
import scala.scalajs.tools.optimizer.{ScalaJSClosureOptimizer, ScalaJSOptimizer}
import scala.scalajs.tools.logging.Level
import java.util.zip.{ZipInputStream, ZipEntry, ZipFile}
import java.io.File
import scala.scalajs.tools.io.{VirtualFile, VirtualScalaJSClassfile, VirtualJSFile}
import scala.collection.JavaConversions._
import scala.io.Source
import java.util.jar.JarFile

object Tests extends TestSuite{
  val splash =
    """
      |import scalajs.js.annotation.JSExport
      |@JSExport
      |object Cow{
      |  @JSExport
      |  def main() = {
      |    println("hello world")
      |  }
      |}
    """.stripMargin

  def tests = TestSuite{
    def run(process: Seq[(String, String)] => String) = {
      val start = System.currentTimeMillis()
      for(i <- 0 until 3){
        val res = Compiler.compile(
          splash.getBytes,
          Compiler.validJars,
          println
        ).get.map(f => f.name -> new String(f.toByteArray))

        println(
          process(res).contains("Cow")
        )
      }
      val end = System.currentTimeMillis()
      println((end - start) / 3)
    }
    "HelloWorld" - {
      run(Compiler.deadCodeElimination)
      run(Compiler.optimize)

    }
  }
}
