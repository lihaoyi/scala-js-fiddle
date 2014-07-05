package fiddle

import acyclic.file
import scala.reflect.io.{VirtualDirectory, Streamable}
import java.util.zip.ZipInputStream
import java.io._

import scala.scalajs.tools.classpath.builder.{AbstractJarLibClasspathBuilder, PartialClasspathBuilder, JarTraverser}
import scala.scalajs.tools.io._
import scala.collection.immutable.Traversable
import scala.util.Random

/**
 * Loads the jars that make up the classpath of the scala-js-fiddle
 * compiler and re-shapes it into the correct structure to satisfy
 * scala-compile and scalajs-tools
 */
object Classpath {
  /**
   * In memory cache of all the jars used in the compiler. This takes up some
   * memory but is better than reaching all over the filesystem every time we
   * want to do something.
   */
  lazy val loadedFiles = {
    println("Loading files...")
    val jarFiles = for {
      name <- Seq(
        "/scala-library-2.11.1.jar",
        "/scala-reflect-2.11.1.jar",
        "/scalajs-library_2.11-0.5.0.jar",
        "/scalajs-dom_sjs0.5_2.11-0.6.jar",
        "/scalatags_sjs0.5_2.11-0.3.8.jar",
        "/scalarx_sjs0.5_2.11-0.2.5.jar",
        "/scala-async_2.11-0.9.1.jar",
        "/scalaxy-loops_2.11-0.1.1.jar",
        "/runtime_sjs0.5_2.11-0.1-SNAPSHOT.jar",
        "/page_sjs0.5_2.11-0.1-SNAPSHOT.jar",
        "/shared_sjs0.5_2.11-0.1-SNAPSHOT.jar"
      )
    } yield {
      val stream = getClass.getResourceAsStream(name)
      println("Loading file" + name + ": " + stream)
      if (stream == null) {
        throw new Exception(s"Classpath loading failed, jar $name not found")
      }
      name -> Streamable.bytes(stream)
    }

    val bootFiles = for {
      prop <- Seq(/*"java.class.path", */"sun.boot.class.path")
      path <- System.getProperty(prop).split(":")
      vfile = scala.reflect.io.File(path)
      if vfile.exists && !vfile.isDirectory
    } yield {
      path.split("/").last -> vfile.toByteArray()
    }
    println("Files loaded...")
    jarFiles ++ bootFiles
  }
  /**
   * The loaded files shaped for Scalac to use
   */
  lazy val scalac = for((name, bytes) <- loadedFiles) yield {
    println(s"Loading $name for Scalac")
    val in = new ZipInputStream(new ByteArrayInputStream(bytes))
    val entries = Iterator
      .continually(in.getNextEntry)
      .takeWhile(_ != null)
      .map((_, Streamable.bytes(in)))

    val dir = new VirtualDirectory(name, None)
    for{
      (e, data) <- entries
      if !e.isDirectory
    } {
      val tokens = e.getName.split("/")
      var d = dir
      for(t <- tokens.dropRight(1)){
        d = d.subdirectoryNamed(t).asInstanceOf[VirtualDirectory]
      }
      val f = d.fileNamed(tokens.last)
      val o = f.bufferedOutput
      o.write(data)
      o.close()
    }
    println(dir.size)
    dir
  }
  /**
   * The loaded files shaped for Scala-Js-Tools to use
   */
  lazy val scalajs = {
    println("Loading scalaJSClassPath")
    class Builder extends AbstractJarLibClasspathBuilder{
      def listFiles(d: File) = Nil
      def toJSFile(f: File) = {
        val file = new MemVirtualJSFile(f._1)
        file.content = new String(f._2)
        file
      }
      def toIRFile(f: File) = {
        val file = new MemVirtualSerializedScalaJSIRFile(f._1)
        file.content = f._2
        file
      }
      def isDirectory(f: File) = false
      type File = (String, Array[Byte])
      def toInputStream(f: File) = new ByteArrayInputStream(f._2)
      def isFile(f: File) = true
      def isJSFile(f: File) = f._1.endsWith(".js")
      def isJARFile(f: File) = f._1.endsWith(".jar")
      def getName(f: File) = f._1
      def isIRFile(f: File) = f._1.endsWith(".sjsir")
      def getVersion(f: File) = Random.nextInt().toString
      def getAbsolutePath(f: File) = f._1
      def toReader(f: File) = new InputStreamReader(toInputStream(f))
    }

    val res = loadedFiles.map(new Builder().build(_))
                         .reduceLeft(_ merge _)
    println("Loaded scalaJSClassPath")
    res
  }
}
