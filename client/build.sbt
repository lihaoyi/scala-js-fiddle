import scala.scalajs.sbtplugin.ScalaJSPlugin._
import ScalaJSKeys._
import com.typesafe.sbt.SbtStartScript

scalaJSSettings

buildSettingsX

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

ScalaJSKeys.packageJS in Compile := {
  (ScalaJSKeys.packageJS in Compile).value :+ generateClient.value
}

bootSnippet := "ScalaJS.modules.fiddle_Client().main();"

updateBrowsers <<= updateBrowsers.triggeredBy(ScalaJSKeys.packageJS in Compile)

(managedSources in packageExportedProductsJS in Compile) := {
  (managedSources in packageExportedProductsJS in Compile).value.filter(f =>
    new java.io.File(f.getAbsolutePath.dropRight(3) + ".sjsinfo").exists()
  )
}

(SbtStartScript.stage in Compile) := (packageJS in Compile).value