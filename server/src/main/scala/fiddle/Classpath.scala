package fiddle

import acyclic.file
import scala.reflect.io.{VirtualDirectory, Streamable}
import scala.io.Source
import java.util.zip.ZipInputStream
import java.io.{InputStream, Reader, ByteArrayInputStream}

import scala.scalajs.tools.classpath.builder.{AbstractJarLibClasspathBuilder, PartialClasspathBuilder, JarTraverser}
import scala.scalajs.tools.io.{VirtualJSFile, VirtualScalaJSIRFile}
import scala.collection.immutable.Traversable
import scala.util.Random

/**
 * Loads the jars that make up the classpath of the scala-js-fiddle
 * compiler and re-shapes it into the correct structure to satisfy
 * scala-compile and scalajs-tools
 */
object Classpath {
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
        "/scalaxy-loops_2.10-0.3-SNAPSHOT.jar",
        "/runtime_sjs0.5_2.10-0.1-SNAPSHOT.jar"
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

  lazy val scalajs = {
    println("Loading scalaJSClassPath")
    val builder = new AbstractJarLibClasspathBuilder{
      type File = (String, Array[Byte])
      def toInputStream(f: File): InputStream = new ByteArrayInputStream(f._2)
      def isFile(f: File): Boolean = true
      def getName(f: File): String = f._1
      def listFiles(d: File): Traversable[File] = ???
      def isIRFile(f: File): Boolean = ???
      def toJSFile(f: File): VirtualJSFile = ???
      def getVersion(f: File): String = Random.nextInt().toString
      def isDirectory(f: File): Boolean = ???
      def getAbsolutePath(f: File): String = ???
      def isJSFile(f: File): Boolean = ???
      def toReader(f: File): Reader = ???
      def toIRFile(f: File): VirtualScalaJSIRFile = ???
      def isJARFile(f: File): Boolean = true
    }

    loadedFiles.map(builder.build)
               .reduce(_ merge _)
  }
}
