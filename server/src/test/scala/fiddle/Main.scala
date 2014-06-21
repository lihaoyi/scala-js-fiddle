package fiddle

import utest._
import fiddle.Compiler.IgnoreLogger
import org.mozilla.javascript.{Scriptable, Context}
import scala.scalajs.tools.packager.ScalaJSPackager
import scala.scalajs.tools.io.WritableMemVirtualJSFile

object Main extends TestSuite{
  def compile(s: String) = {
    Compiler.compile(s.getBytes, println).get
  }
  val tests = TestSuite{
    "compile" - {
      "simple" - {
        val res = compile(
          """
          object Main{
            def main() = {
              Predef.println("Hello World")
              "omg"
            }
          }
        """) |> Compiler.export
        for(s <- Seq("Main$", "Hello World")){
          assert(res.contains(s))
        }
      }

      "macro" - {
        val res = compile( """
          object Main{
            def main() = {
              renderln("Hello World")
            }
          }
        """) |> Compiler.export
        for(s <- Seq("Hello World", "Main", "main")){
          assert(res.contains(s))
        }
      }
      "async" - {
        val res = compile( """
          import async.Async._
          import concurrent.Future
          import scalajs.concurrent.JSExecutionContext.Implicits.queue
          object Main{
            def main() = async{
              await(Future("Hello World"))
            }
          }
        """) |> Compiler.export
        assert(
          res.contains("Hello World"),
          res.contains("Main"),
          res.contains("main")
        )
      }
    }
    "fastOpt" - {
      val res = compile("""
      @JSExport
      object Main{
        def iAmDead() = "lolzz"
        def iAmLive() = "wtfff"
        @JSExport
        def main() = {
          Predef.println("Hello World")
          iAmLive()
          "omg"
        }
      }
      """) |> Compiler.fastOpt  |> Compiler.export
      for(s <- Seq("Main", "Hello World", "iAmLive", "wtfff", "main")){
        assert(res.contains(s))
      }
      for(s <- Seq("iAmDead", "lolzz")){
        assert(!res.contains(s))
      }
      // fastOpt with such a small program should be less than 2mb
      assert(res.length < 2 * 1024 * 1024)
    }
    "fullOpt" - {
      val res = compile("""
      @JSExport
      object Main{
        def iAmDead() = "lolzz"
        def iAmLive() = "wtfff"
        @JSExport
        def main() = {
          Predef.println("Hello World")
          iAmLive()
          "omg"
        }
      }
      """) |> Compiler.fastOpt |> Compiler.fullOpt |> Compiler.export
      for(s <- Seq("Main", "Hello World", "main")){
        assert(res.contains(s))
      }
      for(s <- Seq("iAmDead", "lolzz", "iAmLive", "wtfff")){
        assert(!res.contains(s))
      }
      // fullOpt with such a small program should be less than 200kb
      assert(res.length < 200 * 1024)
    }
  }
}
