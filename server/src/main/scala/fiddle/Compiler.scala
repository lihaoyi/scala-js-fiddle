package fiddle
import acyclic.file
import scala.tools.nsc.{Global, Settings}
import java.net.{URL, URLClassLoader}
import scala.reflect.io
import scala.tools.nsc.util._
import java.io._
import akka.util.ByteString
import scala.tools.nsc.reporters.{Reporter, ConsoleReporter}
import scala.tools.nsc.plugins.Plugin
import scala.concurrent.Future
import scala.async.Async.{async, await}
import concurrent.ExecutionContext.Implicits.global
import scala.reflect.internal.util.{BatchSourceFile, OffsetPosition}
import scala.tools.nsc.interactive.Response

import scala.scalajs.tools.packager.ScalaJSPackager
import scala.io.Source
import scala.scalajs.tools.optimizer.{ScalaJSClosureOptimizer, ScalaJSOptimizer}
import scala.scalajs.tools.io._
import scala.scalajs.tools.logging.Level

import scala.tools.nsc.backend.JavaPlatform
import scala.tools.util.PathResolver
import scala.reflect.io._
import scala.tools.nsc.util.ClassPath.JavaContext
import scala.Some
import scala.collection.mutable
import java.util.zip.ZipInputStream
import scala.annotation.tailrec
import scala.Some
import scala.tools.nsc.typechecker.Analyzer
import scala.scalajs.tools.classpath.builder.PartialClasspathBuilder
import scala.scalajs.tools.classpath.{CompleteNCClasspath, CompleteCIClasspath, PartialIRClasspath, PartialClasspath}
import scala.scalajs.ir.Serializers
import scala.Some

/**
 * Handles the interaction between scala-js-fiddle and
 * scalac/scalajs-tools to compile and optimize code submitted by users.
 */
object Compiler{
  val prelude =
    Source.fromInputStream(getClass.getResourceAsStream("/Prelude.scala"))
          .mkString
  object Nontermination{
    val functionLiteral = "function\\([^\"\\n]*?\\) \\{(?=\\n)"
    val whileTrue = "\\n[^\"\\n]*while \\(.*(?=\\n)"
    val catchBlock = "\\n\\s*\\} catch.*(?=\\n)"
    val finallyBlock = "\\n\\s*\\} finally \\{(?=\\n)"
    def instrument(s: String, hook: String) = {
      if (hook == "") s
      else{
        s"($functionLiteral|$whileTrue|$catchBlock|$finallyBlock)".r.replaceAllIn(
          s, s"$$1\n\\$$$hook()\n"
        )
      }
    }
  }

  val blacklist = Seq("<init>")

  def toFuture[T](func: Response[T] => Unit): Future[T] = {
    val r = new Response[T]
    Future { func(r) ; r.get.left.get }
  }

  def autocomplete(code: String, flag: String, pos: Int): Future[List[(String, String)]] = async {
    val vd = new io.VirtualDirectory("(memory)", None)
    // global can be reused, just create new runs for new compiler invocations
    val compiler = initGlobal(
      (settings, reporter) => new scala.tools.nsc.interactive.Global(settings, reporter),
      vd,
      s => ()
    )

    val file      = new BatchSourceFile(makeFile(prelude.getBytes ++ code.getBytes), prelude + code)
    val position  = new OffsetPosition(file, pos + prelude.length)

    await(toFuture[Unit](compiler.askReload(List(file), _)))

    val maybeMems = await(toFuture[List[compiler.Member]](flag match{
      case "scope" => compiler.askScopeCompletion(position, _)
      case "member" => compiler.askTypeCompletion(position, _)
    }))

    compiler.ask{() =>
      def sig(x: compiler.Member) = {
        Seq(
          x.sym.signatureString,
          s" (${x.sym.kindString})"
        ).find(_ != "").getOrElse("--Unknown--")
      }
      maybeMems.map(x => sig(x) -> x.sym.decodedName)
               .filter(!blacklist.contains(_))
               .distinct
    }
  }

  def makeFile(src: Array[Byte]) = {
    val singleFile = new io.VirtualFile("Main.scala")
    val output = singleFile.output
    output.write(src)
    output.close()
    singleFile
  }

  def initGlobal[T](make: (Settings, ConsoleReporter) => T,
                    vd: io.VirtualDirectory,
                    logger: String => Unit) = {
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
    make(settings, reporter)

  }

  def compile(src: Array[Byte], logger: String => Unit = _ => ()): Option[PartialIRClasspath] = {

    val ctx = new JavaContext()
    val dirs = Classpath.scalac.map(new DirectoryClassPath(_, ctx)).toVector
    val singleFile = makeFile(prelude.getBytes ++ src)
    val vd = new io.VirtualDirectory("(memory)", None)

    val compiler = initGlobal(
      (settings, reporter) => new scala.tools.nsc.Global(settings, reporter){ g =>
        override lazy val plugins = List[Plugin](new scala.scalajs.compiler.ScalaJSPlugin(this))
        override lazy val platform: ThisPlatform = new JavaPlatform{
          val global: g.type = g
          override def classPath: ClassPath[BinaryRepr] = new JavaClassPath(dirs, ctx)
        }
        override lazy val analyzer = new {
          val global: g.type = g
        } with Analyzer{
          override lazy val macroClassloader = new ClassLoader(this.getClass.getClassLoader){
            val classCache = mutable.Map.empty[String, Option[Class[_]]]
            override def findClass(name: String): Class[_] = {
              val fileName = name.replace('.', '/') + ".class"
              val res = classCache.getOrElseUpdate(
                name,
                Classpath.scalac
                  .map(_.lookupPath(fileName, false))
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
      },
      vd,
      logger
    )

    val run = new compiler.Run()
    run.compileFiles(List(singleFile))

    if (vd.iterator.isEmpty) None
    else {
      val things = for{
        x <- vd.iterator.to[collection.immutable.Traversable]
        if x.name.endsWith(".sjsir")
      } yield {
        val f =  new MemVirtualSerializedScalaJSIRFile("x.name")
        f.content = x.toByteArray
        f: VirtualScalaJSIRFile
      }
      Some(
        new scala.scalajs.tools.classpath.PartialIRClasspath(
          Nil,
          Map.empty,
          things,
          None
        )
      )

    }
  }
  def export(p: PartialIRClasspath) = {
    (new ScalaJSPackager).packageCP(
      p,
      ScalaJSPackager.OutputConfig(WritableMemVirtualJSFile("")),
      Compiler.Logger
    ).scalaJSCode.map(_.content).mkString
  }
  def export(p: CompleteCIClasspath) = {
    p.allCode.map(_.content).mkString
  }
  def export(p: CompleteNCClasspath) = {
    p.allCode.map(_.content).mkString
  }

  def fastOpt(userFiles: PartialIRClasspath) = {
    new ScalaJSOptimizer().optimizeCP(
      ScalaJSOptimizer.Inputs(Classpath.scalajs.merge(userFiles).resolve()),
      ScalaJSOptimizer.OutputConfig(WritableMemVirtualJSFile("output.js")),
      Logger
    )
  }

  def fullOpt(userFiles: CompleteCIClasspath) = {
    new ScalaJSClosureOptimizer().optimizeCP(
      ScalaJSClosureOptimizer.Inputs(userFiles),
      ScalaJSClosureOptimizer.OutputConfig(WritableMemVirtualJSFile("output.js")),
      Logger
    )
  }

  object Logger extends scala.scalajs.tools.logging.Logger {
    def log(level: Level, message: => String): Unit = println(message)
    def success(message: => String): Unit = println(message)
    def trace(t: => Throwable): Unit = t.printStackTrace()
  }

  object IgnoreLogger extends scala.scalajs.tools.logging.Logger {
    def log(level: Level, message: => String): Unit = ()
    def success(message: => String): Unit = ()
    def trace(t: => Throwable): Unit = ()
  }
}
