package fiddle

import acyclic.file
import scala.reflect.io.{VirtualDirectory, Streamable}
import scala.io.Source
import java.util.zip.ZipInputStream
import java.io.ByteArrayInputStream
import scala.scalajs.tools.classpath.ScalaJSClasspathEntries

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
        "/scalajs-library_2.10-0.4.3.jar",
        "/scalajs-dom_2.10-0.3.jar",
        "/scalatags_2.10-0.2.4-JS.jar",
        "/scalarx_2.10-0.2.3-JS.jar",
        "/scala-async_2.10-0.9.0.jar",
        "/scalaxy-loops_2.10-0.3-SNAPSHOT.jar",
        "/runtime_2.10-0.1-SNAPSHOT.jar"
      )
    } yield {
      name -> Streamable.bytes(getClass.getResourceAsStream(name))
    }

    val bootFiles = for {
      prop <- Seq(/*"java.class.path", */"sun.boot.class.path")
      path <- System.getProperty(prop).split(":")
      val vfile = scala.reflect.io.File(path)
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
    val builder = new ScalaJSClasspathEntries.Builder
    for((name, bytes) <- loadedFiles){
      ScalaJSClasspathEntries.readEntriesInJar(
        builder,
        new ByteArrayInputStream(bytes)
      )
    }
    builder.result
  }
}
