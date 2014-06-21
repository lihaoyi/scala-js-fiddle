package fiddle

import utest._
import fiddle.Compiler.IgnoreLogger
import org.mozilla.javascript.Context

object Main extends TestSuite{
  def jsEval(s: String): String = {
    val cx = Context.enter()
    val scope = cx.initStandardObjects()
    cx.evaluateString(scope, "Main", "Main.js", 0, null).toString
  }
  def compile(s: String) = {
    Compiler.compile(s.getBytes, println).get
  }
  val tests = TestSuite{
    "simple" - {
      val res = compile("""
        object Main{
          def main() = {
            Predef.println("Hello World")
            "omg"
          }
        }
      """)
      println("----------------RES----------------")
      println(res.resolve().cijsCode.map(_.content).mkString)

    }
    "macro" - {
      val res = compile("""
        object Main{
          def main() = {
            renderln("Hello World")
          }
        }
      """)
//      for(s <- Seq("Hello World", "Main", "main")){
//        assert(res("Main$.js").contains(s))
//      }
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
      """)
//      assert(
//        // it won't be in the main class, but it'll
//        // be in one of the async generated classes
//        res.values.mkString.contains("Hello World"),
//        res("Main$.js").contains("Main"),
//        res("Main$.js").contains("main")
//      )
    }
  }
}
