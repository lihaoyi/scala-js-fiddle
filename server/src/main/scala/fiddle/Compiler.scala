package fiddle

import scala.tools.nsc.Settings
import java.net.URLClassLoader
import scala.reflect.io.{VirtualFile, VirtualDirectory}
import scala.tools.nsc.util.ClassPath
import java.io.{File, PrintWriter, Writer}
import akka.util.ByteString
import scala.tools.nsc.reporters.ConsoleReporter
import scala.tools.nsc.plugins.Plugin
import scala.concurrent.Future

import scala.reflect.internal.util.{BatchSourceFile, OffsetPosition}
import scala.tools.nsc.interactive.Response

object Compiler{
  import concurrent.ExecutionContext.Implicits.global
  val blacklist = Seq(
    "<init>"
  )
  val prelude =
    """
      |import fiddle.{Output => output}
      |import fiddle.Output.println
      |import fiddle.Client.canvas
      |import fiddle.Client.renderer
      |import fiddle.Page.{red, green, blue}
    """.stripMargin

  def toFuture[T](func: Response[T] => Unit): Future[T] = {
    val r = new Response[T]
    Future { func(r) ; r.get.left.get }
  }
  import scala.async.Async.{async, await}

  def autocomplete(code: String, flag: String, pos: Int): Future[List[String]] = async {
    val vd = new VirtualDirectory("(memory)", None)
    // global can be reused, just create new runs for new compiler invocations
    val compiler = initGlobal(
      (settings, reporter) => new scala.tools.nsc.interactive.Global(settings, reporter),
      vd,
      s => ()
    )

    val file      = new BatchSourceFile(makeFile(prelude.getBytes ++ code.getBytes), prelude + code)
    val position  = new OffsetPosition(file, pos + prelude.length)


    await(toFuture[Unit](compiler.askReload(List(file), _)))
    val maybeMems = flag match{
      case "scope" => await(toFuture[List[compiler.Member]](compiler.askScopeCompletion(position, _)))
      case "member" => await(toFuture[List[compiler.Member]](compiler.askTypeCompletion(position, _)))
    }

    compiler.ask(() =>
      maybeMems.map(x => x.sym.decodedName)
               .filter(x => !blacklist.contains(x))
               .distinct
    )
  }

  def makeFile(src: Array[Byte]) = {
    val singleFile = new VirtualFile("Main.scala")
    val output = singleFile.output
    output.write(src)
    output.close()
    singleFile
  }
  def initGlobal[T](make: (Settings, ConsoleReporter) => T, vd: VirtualDirectory, logger: String => Unit) = {
    lazy val settings = new Settings
    val loader = getClass.getClassLoader.asInstanceOf[URLClassLoader]
    val entries = loader.getURLs.map(_.getPath) :+ "target/scala-2.10/classes/classes"

    settings.outputDirs.setSingleOutput(vd)
    settings.classpath.value            = ClassPath.join(entries: _*)

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
    make(settings, reporter)

  }
  def apply(src: Array[Byte], logger: String => Unit): Option[String] = {

    val singleFile = makeFile(prelude.getBytes ++ src)
    val vd = new VirtualDirectory("(memory)", None)
    val compiler = initGlobal(
      (settings, reporter) => new scala.tools.nsc.Global(settings, reporter){
        override lazy val plugins = List[Plugin](new scala.scalajs.compiler.ScalaJSPlugin(this))
      },
      vd,
      logger
    )
    val run = new compiler.Run()
    run.compileFiles(List(singleFile))

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
