package fiddle

import utest._

import scala.concurrent.Await
import scala.concurrent.duration._

/**
 * Internal API tests to make sure I'm using the Scala compiler and
 * ScalaJS tools correctly, since they're completely separate from the
 * web server. They're also complicated enough to be easy to screw up,
 * and it's easier to fix them from the command line without all the
 * HTTP server rubbish getting in the way.
 */
object Main extends TestSuite{
  def compile(s: String) = {
    Compiler.compile(s.getBytes, println).get
  }
  val tests = TestSuite {
    "compile" - {
      "simple" - {
        val res = compile("""
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
        val res = compile("""
          object Main{
            def main() = {
              println("Hello World")
            }
          }
        """) |> Compiler.export
        for(s <- Seq("Hello World", "Main", "main")){
          assert(res.contains(s))
        }
      }
      "async" - {
        val res = compile("""
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

    "optimize" - {
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
      "crasher" - {
        compile(
          """
            |@JSExport
            |object ScalaJSExample{
            |  @JSExport
            |  def main(): Unit = {
            |    val xs = Seq(1)
            |    val ys = Seq(2)
            |    xs.flatMap(x => ys.map(y => y))
            |  }
            |}
          """.stripMargin) |> Compiler.fastOpt
      }
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
        def check(snippet: String, key: String)(end: Int)(token: String, lul: Boolean, main: Boolean) = {
          val newEnd = Option(token).fold(snippet.length - 1)(snippet.indexOf(_, end))
          println(end + " -> " + newEnd)
          for (i <- end until newEnd) {
            val e = complete(snippet, key, i)
            assert(
              lul == e.contains((": String", "lol")),
              main == e.contains((": Int", "zzzzx"))
            )
          }
          newEnd
        }
        "scopes" - {

          val snippet = """
            object Lul{
              def lol = "omg"
            }
            object Main{
              def zzzzx = 123;
            }
          """.replaceAll("\n *", "\n")


          val c = check(snippet, "scope") _
          // There are things in scope after every open curly, but they
          // stop being in scope when the `object` keyword appears
          Seq(
            ("{", false, false),
            ("object", true, false),
            ("{", false, false),
            (null, false, true)
          ).foldLeft(0)(c(_).tupled(_))

        }
        "members" - {
          val snippet = """
            object Lul{
              def lol = "omg"
              Main. ;
            }
            object Main{
              def zzzzx = 123;
            }
          """.replaceAll("\n *", "\n")

          val c = check(snippet, "member") _
          Seq(
            ("{", false, false),
            ("def", true, false),
            ("Main", false, false),
            (";", false, true),
            ("object", true, false),
            ("{", false, false),
            ("def", false, true),
            ("\n", false, false),
            (null, false, true)
          ).foldLeft(0)(c(_).tupled( _))
        }
      }
    }
  }
}

