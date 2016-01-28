import com.heroku.sbt.HerokuPlugin
import com.typesafe.sbt.SbtNativePackager
import sbt._
import Keys._
import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import com.typesafe.sbt.SbtNativePackager._
import NativePackagerKeys._
import com.heroku.sbt.HerokuPlugin.autoImport._
import spray.revolver.RevolverPlugin._

object Build extends sbt.Build{

  lazy val root = project.in(file("."))
    .aggregate(client, page, server, runtime)
    .settings(
      (resources in (server, Compile)) ++= {
        (managedClasspath in (runtime, Compile)).value.map(_.data) ++ Seq(
          (packageBin in (runtime, Compile)).value,
          (packageBin in (page, Compile)).value,
          (packageBin in (shared, Compile)).value,
          (fullOptJS in (client, Compile)).value.data
        )
      },
      scalaVersion := "2.11.7"
    )
  lazy val shared = project.in(file("shared")).enablePlugins(ScalaJSPlugin)
                           .settings(scalaVersion := "2.11.7")

  lazy val client = project
    .dependsOn(page, shared)
    .enablePlugins(ScalaJSPlugin)
    .settings(
      libraryDependencies ++= Seq(
        "org.scala-js" %%% "scalajs-dom" % "0.8.2",
        "com.lihaoyi" %%% "scalatags" % "0.4.5",
        "com.lihaoyi" %%% "scalarx" % "0.2.7",
        "com.lihaoyi" %%% "upickle" % "0.2.6",
        "com.lihaoyi" %%% "autowire" % "0.2.4",
        "org.scala-lang.modules" %% "scala-async" % "0.9.1" % "provided",
        "com.lihaoyi" %% "acyclic" % "0.1.2" % "provided"
      ),
      relativeSourceMaps := true,
      addCompilerPlugin("com.lihaoyi" %% "acyclic" % "0.1.2"),
      autoCompilerPlugins := true,
      scalaVersion := "2.11.7"
    )
  lazy val page = project
    .dependsOn(shared)
    .enablePlugins(ScalaJSPlugin)
    .settings(
      libraryDependencies ++= Seq(
        "org.scala-js" %%% "scalajs-dom" % "0.8.2",
        "com.lihaoyi" %%% "scalatags" % "0.4.5"
      ),
      scalaVersion := "2.11.7"
    )
  lazy val runtime = project
    .dependsOn(page)
    .enablePlugins(ScalaJSPlugin)
    .settings(
      resolvers += Resolver.sonatypeRepo("snapshots"),
      libraryDependencies ++= Seq(
        "org.scala-lang" % "scala-reflect" % scalaVersion.value,
        "org.scala-js" %%% "scalajs-dom" % "0.8.2",
        "com.lihaoyi" %%% "scalatags" % "0.4.5",
        "org.scala-lang.modules" %% "scala-async" % "0.9.1",
        "com.lihaoyi" %%% "scalarx" % "0.2.7",
        "com.nativelibs4java" %% "scalaxy-loops" % "0.1.1"
      ),
      autoCompilerPlugins := true,
      scalaVersion := "2.11.7"
    )

  lazy val server = project
    .dependsOn(shared)
    .enablePlugins(HerokuPlugin)
    .settings(packageArchetype.java_server:_*)
    .settings(Revolver.settings:_*)
    .settings(
      herokuAppName in Compile := "scala-js-fiddle",
      libraryDependencies ++= Seq(
        "org.scala-lang" % "scala-compiler" % scalaVersion.value,
        "com.typesafe.akka" %% "akka-actor" % "2.3.2",
        "io.spray" %% "spray-can" % "1.3.1",
        "io.spray" %% "spray-client" % "1.3.1",
        "io.spray" %% "spray-caching" % "1.3.1",
        "io.spray" %% "spray-httpx" % "1.3.1",
        "io.spray" %% "spray-routing" % "1.3.1",
        "org.scala-js" % s"scalajs-compiler" % "0.6.6" cross CrossVersion.full,
        "org.scala-js" %% "scalajs-tools" % "0.6.6",
        "org.scala-lang.modules" %% "scala-async" % "0.9.1" % "provided",
        "com.lihaoyi" %% "scalatags" % "0.4.5",
        "com.lihaoyi" %% "acyclic" % "0.1.2" % "provided",
        "org.webjars" % "ace" % "01.08.2014",
        "org.webjars" % "jquery" % "2.1.0-2",
        "org.webjars" % "normalize.css" % "2.1.3",
        "com.lihaoyi" %% "upickle" % "0.2.6",
        "com.lihaoyi" %% "autowire" % "0.2.4",
        "com.lihaoyi" %% "utest" % "0.3.0" % "test",
        "io.apigee" % "rhino" % "1.7R5pre4" % "test"
      ),

      resolvers += "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/",
      testFrameworks += new TestFramework("utest.runner.Framework"),
      javaOptions in Revolver.reStart += "-Xmx2g",
      addCompilerPlugin("com.lihaoyi" %% "acyclic" % "0.1.2"),
      autoCompilerPlugins := true,
      scalaVersion := "2.11.7"
    )
}
