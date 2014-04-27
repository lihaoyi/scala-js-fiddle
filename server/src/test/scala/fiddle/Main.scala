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
    Compiler.compile(s.getBytes, println)
      .toSeq
      .flatten
      .map(f => f.name -> f.toCharArray.mkString)
      .toMap
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

      assert(
        res("Main$.js").contains("Hello World"),
        res("Main$.js").contains("Main"),
        res("Main$.js").contains("main")
      )
    }
    "macro" - {
      val res = compile(
        """
          object Main{
            def main() = {
              renderln("Hello World")
            }
          }
        """
      )
      assert(
        res("Main$.js").contains("Hello World"),
        res("Main$.js").contains("Main"),
        res("Main$.js").contains("main")
      )
    }
  }
}
