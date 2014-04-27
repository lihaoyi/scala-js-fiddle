package fiddle

import utest._
import fiddle.Compiler.IgnoreLogger

object Main extends TestSuite{
  val tests = TestSuite{
    println("Hello World")
    Compiler.compile(
      "".getBytes,
      _ => ()
    )
  }

}
