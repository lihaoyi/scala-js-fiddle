import scala.scalajs.sbtplugin.ScalaJSPlugin._
import ScalaJSKeys._
import com.typesafe.sbt.SbtStartScript


scalaJSSettings

name := "Example"

version := "0.1-SNAPSHOT"

scalaVersion := "2.10.3"

libraryDependencies ++= Seq(
  "org.scala-lang.modules.scalajs" %% "scalajs-dom" % "0.3",
  "com.scalatags" % "scalatags_2.10" % "0.2.4-JS",
  "com.scalarx" % "scalarx_2.10" % "0.2.3-JS",
  "org.scala-lang.modules.scalajs" %% "scalajs-jquery" % "0.3",
  "org.scala-lang.modules" %% "scala-async" % "0.9.0-M4" % "provided"
)

(SbtStartScript.stage in Compile) := (optimizeJS in Compile).value

relativeSourceMaps := true
//workbenchSettings

//bootSnippet := "ScalaJS.modules.fiddle_Client().main__AT__V();"

//retrieveManaged := true

//managedDirectory := file("server/target/scala-2.10/classes/classpath")

//updateBrowsers <<= updateBrowsers.triggeredBy(packageJS in Compile)
