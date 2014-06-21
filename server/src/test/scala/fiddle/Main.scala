package fiddle

import utest._

import scala.concurrent.Await
import scala.concurrent.duration._

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
    "complete" - {
      def complete(src: String, mode: String, index: Int): List[(String, String)] = {
        Await.result(Compiler.autocomplete(src, mode, index), 60 seconds)
      }
      "basic" - {
        val a = complete("object Main{ def zzzzx = 123}", "member", 12)
        val b = complete("object Main{ def zzzzx = 123}", "member", 0)
        val c = complete("object Main{ def zzzzx = 123}", "scope", 12)
        val d = complete("object Main{ def zzzzx = 123}", "scope", 0)

        assert(
          a.contains((": Int", "zzzzx")),
          !b.contains((": Int", "zzzzx")),
          c.contains((": Int", "zzzzx")),
          !d.contains((": Int", "zzzzx"))
        )
      }
      "positional" - {
        "scopes" - {

          val snippet = """

            object Lul{
              def lol = "omg"
            }
            object Main{
              def zzzzx = 123;
            }

          """.replaceAll("\n *", "\n")

          def check(end: Int, token: String, lul: Boolean, main: Boolean) = {
            val newEnd = snippet.indexOf(token, end)
            for (i <- end until newEnd) {
              val e = complete(snippet, "scope", i)
              assert(
                lul == e.contains((": String", "lol")),
                main == e.contains((": Int", "zzzzx"))
              )
            }
            newEnd
          }
          val end0 = 0
          val end1 = check(end0, "{", false, false)
          val end2 = check(end1, "object", true, false)
          val end3 = check(end2, "{", false, false)
          val end4 = check(end3, "grargh" /*This isn't found and goes to EOL*/, false, true)
        }
      }
    }
  }
}
