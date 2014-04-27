import sbt._
import Keys._
import scala.scalajs.sbtplugin.ScalaJSPlugin._
import ScalaJSKeys._
import com.typesafe.sbt.SbtStartScript
import sbtassembly.Plugin._
import AssemblyKeys._
import spray.revolver.RevolverPlugin._
object Build extends sbt.Build{

  lazy val root = project.in(file("."))
    .aggregate(client, page, server, runtime)
    .settings(assemblySettings:_*)
    .settings(net.virtualvoid.sbt.graph.Plugin.graphSettings: _*)
    .settings(
      SbtStartScript.stage in Compile := Unit,
      (assembly in Compile) := {
        (SbtStartScript.stage in Compile).value
        (assembly in (server, Compile)).value
      },
      (crossTarget in (client, Compile, optimizeJS)) := (classDirectory in (server, Compile)).value,
      (crossTarget in (client, Compile, preoptimizeJS)) := (classDirectory in (server, Compile)).value,
      (crossTarget in (page, Compile, packageBin)) := (classDirectory in (server, Compile)).value,
      (crossTarget in (runtime, Compile, packageBin)) := (classDirectory in (server, Compile)).value,
      scalaVersion := "2.10.3"
    )
  lazy val client = project
    .dependsOn(page)
    .settings(scalaJSSettings:_*)
    .settings(
      libraryDependencies ++= Seq(
        "org.scala-lang.modules.scalajs" %% "scalajs-dom" % "0.3",
        "com.scalatags" % "scalatags_2.10" % "0.2.4-JS",
        "com.scalarx" % "scalarx_2.10" % "0.2.3-JS",
        "org.scala-lang.modules.scalajs" %% "scalajs-jquery" % "0.3",
        "org.scala-lang.modules" %% "scala-async" % "0.9.0" % "provided",
        "com.lihaoyi" %% "acyclic" % "0.1.1" % "provided"
      ),
      (SbtStartScript.stage in Compile) := (optimizeJS in Compile).value,
      relativeSourceMaps := true,
      addCompilerPlugin("com.lihaoyi" %% "acyclic" % "0.1.1"),
      autoCompilerPlugins := true
    )
  lazy val page = project
    .settings(scalaJSSettings:_*)
    .settings(
      libraryDependencies ++= Seq(
        "org.scala-lang" % "scala-reflect" % "2.10.3",
        "org.scalamacros" % "quasiquotes_2.10.3" % "2.0.0-M3",
        "org.scala-lang.modules.scalajs" %% "scalajs-dom" % "0.3",
        "com.scalatags" % "scalatags_2.10" % "0.2.4-JS"
      ),
      addCompilerPlugin("org.scalamacros" % "paradise_2.10.3" % "2.0.0-M3"),
      autoCompilerPlugins := true,
      (SbtStartScript.stage in Compile) := {
        (packageBin in Compile).value
        (optimizeJS in Compile).value
      }
  )
  lazy val runtime = project
    .settings(scalaJSSettings:_*)
    .settings(
      libraryDependencies ++= Seq(
        "org.scala-lang.modules.scalajs" %% "scalajs-dom" % "0.3",
        "com.scalatags" % "scalatags_2.10" % "0.2.4-JS"
      ),
      (SbtStartScript.stage in Compile) := {
        (Keys.packageBin in Compile).value
      }
    )

  lazy val server = project
    .settings(net.virtualvoid.sbt.graph.Plugin.graphSettings: _*)
    .settings(assemblySettings ++ Revolver.settings ++ SbtStartScript.startScriptForClassesSettings:_*)
    .settings(
      resolvers += Resolver.url("scala-js-releases",
        url("http://dl.bintray.com/content/scala-js/scala-js-releases"))(
          Resolver.ivyStylePatterns),

      resolvers += Resolver.url("scala-js-snapshots",
        url("http://repo.scala-js.org/repo/snapshots/"))(
        Resolver.ivyStylePatterns),


      libraryDependencies ++= Seq(
        "com.lihaoyi" % "utest_2.10" % "0.1.2" % "test",
        "org.scala-lang" % "scala-compiler" % "2.10.3",
        "com.typesafe.akka" %% "akka-actor" % "2.3.0",
        "io.spray" % "spray-can" % "1.3.1",
        "io.spray" % "spray-client" % "1.3.1",
        "io.spray" % "spray-caching" % "1.3.1",
        "io.spray" % "spray-httpx" % "1.3.1",
        "io.spray" % "spray-routing" % "1.3.1",
        "org.scala-lang.modules.scalajs" %% "scalajs-compiler" % "0.4.2",
        "com.typesafe.play" %% "play-json" % "2.2.2",
        "org.scala-lang.modules.scalajs" %% "scalajs-tools" % "0.4.2",
        "org.scala-lang.modules" %% "scala-async" % "0.9.0" % "provided",
        "com.scalatags" % "scalatags_2.10" % "0.2.4",
        "com.lihaoyi" %% "acyclic" % "0.1.1" % "provided",
        "org.webjars" % "ace" % "07.31.2013",
        "org.webjars" % "jquery" % "2.1.0-2",
        "org.webjars" % "normalize.css" % "2.1.3",
        "com.lihaoyi" %% "utest" % "0.1.3"
      ),

      resolvers += "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/",
      testFrameworks += new TestFramework("utest.runner.JvmFramework"),
      javaOptions += "-XX:MaxPermSize=2g",
      javaOptions in Revolver.reStart += "-Xmx2g",
      addCompilerPlugin("com.lihaoyi" %% "acyclic" % "0.1.1"),
      autoCompilerPlugins := true
    )
}