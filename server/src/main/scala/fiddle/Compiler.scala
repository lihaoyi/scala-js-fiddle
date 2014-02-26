package fiddle

import scala.tools.nsc.{Global, Settings}
import java.net.URLClassLoader
import scala.reflect.io.{VirtualFile, VirtualDirectory}
import scala.tools.nsc.util.ClassPath
import java.io.{PrintWriter, Writer}
import akka.util.ByteString
import scala.tools.nsc.reporters.ConsoleReporter
import scala.tools.nsc.plugins.Plugin

object Compiler{
  def apply(src: Array[Byte], logger: String => Unit): Option[String] = {
    lazy val settings = new Settings

    val loader = getClass.getClassLoader.asInstanceOf[URLClassLoader]
    val entries = loader.getURLs map(_.getPath)
    val vd = new VirtualDirectory("(memory)", None)
    settings.outputDirs.setSingleOutput(vd)
    settings.classpath.value = ClassPath.join(entries: _*)

    val writer = new Writer{
      var inner = ByteString()
      def write(cbuf: Array[Char], off: Int, len: Int): Unit = {
        inner = inner ++ ByteString.fromArray(cbuf.map(_.toByte), off, len)
      }
      def flush(): Unit = {
        logger(inner.utf8String)
        inner = ByteString()
      }
      def close(): Unit = ()
    }
    val reporter = new ConsoleReporter(settings, scala.Console.in, new PrintWriter(writer))
    val compiler = new Global(settings, reporter){
      override lazy val plugins = List[Plugin](new scala.scalajs.compiler.ScalaJSPlugin(this))
    }

    val singleFile = new VirtualFile("Main.scala")

    val output = singleFile.output
    output.write(src)
    output.close()

    val run = new compiler.Run()
    run.compileFiles(List(singleFile))

    vd.iterator.filter(_.name.endsWith(".js")).foreach(println)

    if (vd.iterator.isEmpty) None
    else Some(
      vd.iterator
        .filter(_.name.endsWith(".js"))
        .map(_.input)
        .map(io.Source.fromInputStream)
        .map(_.mkString)
        .mkString("\n")
    )
  }
}