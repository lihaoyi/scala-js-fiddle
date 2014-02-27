import scala.scalajs.sbtplugin.ScalaJSPlugin._
import ScalaJSKeys._
import com.typesafe.sbt.SbtStartScript

scalaJSSettings

name := "Example"

version := "0.1-SNAPSHOT"

scalaVersion := "2.10.3"

libraryDependencies ++= Seq(
  "org.scala-lang.modules.scalajs" %% "scalajs-dom" % "0.2",
  "com.scalatags" % "scalatags_2.10" % "0.2.3-JS",
  "com.scalarx" % "scalarx_2.10" % "0.2.2-JS",
  "org.scala-lang.modules.scalajs" %% "scalajs-jquery" % "0.1-SNAPSHOT"
)

// Specify additional .js file to be passed to package-js and optimize-js
unmanagedSources in (Compile, ScalaJSKeys.packageJS) +=
  baseDirectory.value / "js" / "startup.js"

(SbtStartScript.stage in Compile) := (packageJS in Compile).value