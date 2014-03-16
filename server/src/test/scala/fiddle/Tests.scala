package fiddle

import utest._
import java.net.URLClassLoader

object Tests extends TestSuite{
  def tests = TestSuite{
    "HelloWorld" - {
      println("Hello")
      println(Compiler("object Cow".getBytes, _ => ()))
      val loader = getClass.getClassLoader.asInstanceOf[URLClassLoader]
      val entries = loader.getURLs.map(_.getPath)
      entries.foreach(println)
    }
  }
}
