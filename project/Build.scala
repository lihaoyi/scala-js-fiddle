import sbt._
import Keys._



object Build extends sbt.Build{

  lazy val proj = Project(
    "sprayTest",
    file("."),
    settings =
      Defaults.defaultSettings ++
      sbtassembly.Plugin.assemblySettings ++
      spray.revolver.RevolverPlugin.Revolver.settings ++
      net.virtualvoid.sbt.graph.Plugin.graphSettings ++ Seq(
      organization  := "com.example",
      version       := "0.1",
      scalaVersion  := "2.10.0",

      addCompilerPlugin("org.scala-lang.plugins" % "continuations" % "2.10.0"),

      scalacOptions += "-P:continuations:enable",

      resolvers ++= Seq(
        "sonatype releases"  at "https://oss.sonatype.org/content/repositories/releases/",
        "sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
        "typesafe repo"      at "http://repo.typesafe.com/typesafe/releases/",
        "spray repo"         at "http://repo.spray.io/"
      ),
      libraryDependencies ++= Seq(
        "io.spray"                %   "spray-can"     % "1.1-M7",
        "com.typesafe.akka"       %%  "akka-actor"    % "2.1.0",
        "org.pegdown"             %  "pegdown"        % "1.2.0",
        "net.java.dev.textile-j"  % "textile-j"       % "2.2.864",
        "io.spray"                %%  "spray-json"    % "1.2.3",
        "io.spray"                %  "spray-httpx"    % "1.1-M7"
      )
    ),
    dependencies = Seq(
      RootProject(file("../scalatags")),
      RootProject(file("../scala.rx"))
    )
  )
}

