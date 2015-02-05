package fiddle
import acyclic.file
import scala.tools.nsc.Settings
import scala.reflect.io
import scala.tools.nsc.util._
import java.io._
import akka.util.ByteString
import scala.tools.nsc.reporters.ConsoleReporter
import scala.tools.nsc.plugins.Plugin
import scala.concurrent.Future
import scala.async.Async.{async, await}
import concurrent.ExecutionContext.Implicits.global
import scala.reflect.internal.util.{BatchSourceFile, OffsetPosition}
import scala.tools.nsc.interactive.{InteractiveAnalyzer, Response}
import scala.tools.nsc
import scala.io.Source
import org.scalajs.core.tools.optimizer.{ScalaJSClosureOptimizer, ScalaJSOptimizer}
import org.scalajs.core.tools.io._
import org.scalajs.core.tools.logging.Level

import scala.tools.nsc.backend.JavaPlatform
import scala.tools.nsc.util.ClassPath.JavaContext
import scala.collection.mutable
import scala.tools.nsc.typechecker.Analyzer
import org.scalajs.core.tools.classpath.{CompleteClasspath, LinkedClasspath, IRClasspath, PartialClasspath}
import scala.Some

/**
 * Handles the interaction between scala-js-fiddle and
 * scalac/scalajs-tools to compile and optimize code submitted by users.
 */
object Compiler{
  val blacklist = Seq("<init>")

  val semantics = org.scalajs.core.tools.sem.Semantics.Defaults

  /**
   * Converts Scalac's weird Future type
   * into a standard scala.concurrent.Future
   */
  def toFuture[T](func: Response[T] => Unit): Future[T] = {
    val r = new Response[T]
    Future { func(r) ; r.get.left.get }
  }

  /**
   * Converts a bunch of bytes into Scalac's weird VirtualFile class
   */
  def makeFile(src: Array[Byte]) = {
    val singleFile = new io.VirtualFile("Main.scala")
    val output = singleFile.output
    output.write(src)
    output.close()
    singleFile
  }

  def inMemClassloader = {
    new ClassLoader(this.getClass.getClassLoader){
      val classCache = mutable.Map.empty[String, Option[Class[_]]]
      override def findClass(name: String): Class[_] = {
        val fileName = name.replace('.', '/') + ".class"
        val res = classCache.getOrElseUpdate(
          name,
          Classpath.scalac
            .map(_.lookupPathUnchecked(fileName, false))
            .find(_ != null).map{f =>
            val data = f.toByteArray
            this.defineClass(name, data, 0, data.length)
          }
        )
        res match{
          case None => throw new ClassNotFoundException()
          case Some(cls) => cls
        }
      }
    }
  }
  /**
   * Mixed in to make a Scala compiler run entirely in-memory,
   * loading its classpath and running macros from pre-loaded
   * in-memory files
   */
  trait InMemoryGlobal { g: scala.tools.nsc.Global =>
    def ctx: JavaContext
    def dirs: Vector[DirectoryClassPath]
    override lazy val plugins = List[Plugin](new org.scalajs.core.compiler.ScalaJSPlugin(this))
    override lazy val platform: ThisPlatform = new JavaPlatform{
      val global: g.type = g
      override def classPath = new JavaClassPath(dirs, ctx)
    }

  }

  /**
   * Code to initialize random bits and pieces that are needed
   * for the Scala compiler to function, common between the
   * normal and presentation compiler
   */
  def initGlobalBits(logger: String => Unit)= {
    val vd = new io.VirtualDirectory("(memory)", None)
    val jCtx = new JavaContext()
    val jDirs = Classpath.scalac.map(new DirectoryClassPath(_, jCtx)).toVector
    lazy val settings = new Settings

    settings.outputDirs.setSingleOutput(vd)
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
    (settings, reporter, vd, jCtx, jDirs)

  }

  def autocomplete(code: String, flag: String, pos: Int): Future[List[(String, String)]] = async {
    // global can be reused, just create new runs for new compiler invocations
    val (settings, reporter, vd, jCtx, jDirs) = initGlobalBits(_ => ())
    val compiler = new nsc.interactive.Global(settings, reporter) with InMemoryGlobal { g =>
      def ctx = jCtx
      def dirs = jDirs
      override lazy val analyzer = new {
        val global: g.type = g
      } with InteractiveAnalyzer {
        override def findMacroClassLoader() = inMemClassloader
      }
    }

    val file      = new BatchSourceFile(makeFile(Shared.prelude.getBytes ++ code.getBytes), Shared.prelude + code)
    val position  = new OffsetPosition(file, pos + Shared.prelude.length)

    await(toFuture[Unit](compiler.askReload(List(file), _)))

    val maybeMems = await(toFuture[List[compiler.Member]](flag match{
      case "scope" => compiler.askScopeCompletion(position, _: compiler.Response[List[compiler.Member]])
      case "member" => compiler.askTypeCompletion(position, _: compiler.Response[List[compiler.Member]])
    }))

    val res = compiler.ask{() =>
      def sig(x: compiler.Member) = {
        Seq(
          x.sym.signatureString,
          s" (${x.sym.kindString})"
        ).find(_ != "").getOrElse("--Unknown--")
      }
      maybeMems.map((x: compiler.Member) => sig(x) -> x.sym.decodedName)
        .filter(!blacklist.contains(_))
        .distinct
    }
    compiler.askShutdown()
    res
  }

  def compile(src: Array[Byte], logger: String => Unit = _ => ()): Option[PartialClasspath] = {

    val singleFile = makeFile(Shared.prelude.getBytes ++ src)

    val (settings, reporter, vd, jCtx, jDirs) = initGlobalBits(logger)
    val compiler = new nsc.Global(settings, reporter) with InMemoryGlobal{ g =>
      def ctx = jCtx
      def dirs = jDirs
      override lazy val analyzer = new {
        val global: g.type = g
      } with Analyzer{
        override def findMacroClassLoader() = inMemClassloader
      }
    }

    val run = new compiler.Run()
    run.compileFiles(List(singleFile))

    if (vd.iterator.isEmpty) None
    else {
      val things = for{
        x <- vd.iterator.to[collection.immutable.Traversable]
        if x.name.endsWith(".sjsir")
      } yield {
        val f =  new MemVirtualSerializedScalaJSIRFile(x.path)
        f.content = x.toByteArray
        f: VirtualScalaJSIRFile
      }
      Some(
        new org.scalajs.core.tools.classpath.PartialClasspath(
          Nil,
          Map.empty,
          things,
          None
        )
      )

    }
  }

  def export(p: CompleteClasspath) = {
    p.allCode.map(_.content).mkString("\n\n")
  }

  def fastOpt(userFiles: PartialClasspath): LinkedClasspath = {
    new ScalaJSOptimizer(semantics).optimizeCP(
      Classpath.scalajs.merge(userFiles).resolve(),
      ScalaJSOptimizer.Config(WritableMemVirtualJSFile("output.js")),
      Logger
    )
  }

  def fullOpt(userFiles: PartialClasspath): LinkedClasspath = {
    val sems = semantics.optimized
    new ScalaJSClosureOptimizer(sems).optimizeCP(
      new ScalaJSOptimizer(sems),
      Classpath.scalajs.merge(userFiles).resolve(),
      ScalaJSClosureOptimizer.Config(WritableMemVirtualJSFile("output.js")),
      Logger
    )
  }

  object Logger extends org.scalajs.core.tools.logging.Logger {
    def log(level: Level, message: => String): Unit = println(message)
    def success(message: => String): Unit = println(message)
    def trace(t: => Throwable): Unit = t.printStackTrace()
  }

  object IgnoreLogger extends org.scalajs.core.tools.logging.Logger {
    def log(level: Level, message: => String): Unit = ()
    def success(message: => String): Unit = ()
    def trace(t: => Throwable): Unit = ()
  }
}
