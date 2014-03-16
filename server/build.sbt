import com.typesafe.sbt.SbtStartScript

resolvers += "spray repo" at "http://repo.spray.io"

resolvers += Resolver.url("scala-js-releases",
  url("http://dl.bintray.com/content/scala-js/scala-js-releases"))(
    Resolver.ivyStylePatterns)

resolvers += Resolver.url("scala-js-snapshots",
  url("http://repo.scala-js.org/repo/snapshots/"))(
    Resolver.ivyStylePatterns)


libraryDependencies ++= Seq(
  "com.lihaoyi" % "utest_2.10" % "0.1.2" % "test",
  "org.scala-lang" % "scala-compiler" % "2.10.3",
  "com.typesafe.akka" %% "akka-actor" % "2.3.0",
  "io.spray" % "spray-can" % "1.3.0",
  "io.spray" % "spray-caching" % "1.3.0",
  "io.spray" % "spray-httpx" % "1.3.0",
  "io.spray" % "spray-routing" % "1.3.0",
  "org.scala-lang.modules.scalajs" %% "scalajs-compiler" % "0.4.0",
  "org.scala-lang.modules.scalajs" %% "scalajs-library" % "0.4.0",
  "org.scala-lang.modules.scalajs" %% "scalajs-dom" % "0.3",
  "org.scala-lang.modules.scalajs" %% "scalajs-jquery" % "0.3",
  "com.scalatags" % "scalatags_2.10" % "0.2.4-JS",
  "com.scalarx" % "scalarx_2.10" % "0.2.3-JS",
  "io.spray" %%  "spray-json" % "1.2.5",
  "org.scala-lang.modules" %% "scala-async" % "0.9.0-M4",
  "org.scala-sbt" % "sbt" % "0.13.0"
)

libraryDependencies +=
  ("org.scala-lang.modules.scalajs" % "scalajs-sbt-plugin" % "0.4.0").extra(
    "sbtVersion" -> "0.13", "scalaVersion" -> "2.10"
  )

testFrameworks += new TestFramework("utest.runner.JvmFramework")

SbtStartScript.startScriptForClassesSettings

Revolver.settings

