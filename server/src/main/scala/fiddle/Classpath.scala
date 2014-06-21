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
    val jarFiles = for {
      name <- Seq(
        "/scala-library-2.10.4.jar",
        "/scala-reflect-2.10.4.jar",
        "/scalajs-library_2.10-0.5.0.jar",
        "/scalajs-dom_sjs0.5_2.10-0.6.jar",
        "/scalatags_sjs0.5_2.10-0.3.0.jar",
        "/scalarx_sjs0.5_2.10-0.2.5.jar",
        "/scala-async_2.10-0.9.0.jar",
//        "/scalaxy-loops_2.10-0.3-SNAPSHOT.jar",
        "/runtime_sjs0.5_2.10-0.1-SNAPSHOT.jar",
        "/page_sjs0.5_2.10-0.1-SNAPSHOT.jar"
      )
    } yield {
      val stream = getClass.getResourceAsStream(name)
      println("Loading file" + name + ": " + stream)
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

    jarFiles ++ bootFiles
  }
  /**
   * The loaded files shaped for Scalac to use
   */
  lazy val scalac = for((name, bytes) <- loadedFiles) yield {
    println(s"Loading $name")
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
    def builder = new AbstractJarLibClasspathBuilder{
      def listFiles(d: File): Traversable[File] = Nil
      def toJSFile(f: File): VirtualJSFile = {
        val file = new MemVirtualJSFile(f._1)
        file.content = new String(f._2)
        file
      }

      def toIRFile(f: File): VirtualScalaJSIRFile = {
        val file = new MemVirtualSerializedScalaJSIRFile(f._1)
        file.content = f._2
        file
      }
      def isDirectory(f: File): Boolean = false
      type File = (String, Array[Byte])
      def toInputStream(f: File): InputStream = new ByteArrayInputStream(f._2)
      def isFile(f: File): Boolean = true
      def getName(f: File): String = f._1

      def isIRFile(f: File): Boolean = true

      def getVersion(f: File): String = Random.nextInt().toString

      def getAbsolutePath(f: File): String = f._1
      def isJSFile(f: File): Boolean = false
      def toReader(f: File): Reader = new InputStreamReader(new ByteArrayInputStream(f._2))

      def isJARFile(f: File): Boolean = true
    }

    loadedFiles.map{x => println(x._1); builder.build(x)}
               .reduce(_ merge _)
  }
}
