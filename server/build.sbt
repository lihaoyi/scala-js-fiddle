import com.typesafe.sbt.SbtStartScript

resolvers += "spray repo" at "http://repo.spray.io"

resolvers += Resolver.url("scala-js-releases",
  url("http://dl.bintray.com/content/scala-js/scala-js-releases"))(
    Resolver.ivyStylePatterns)

resolvers += Resolver.url("scala-js-snapshots",
  url("http://repo.scala-js.org/repo/snapshots/"))(
    Resolver.ivyStylePatterns)

libraryDependencies ++= Seq(
  "com.lihaoyi.utest" % "utest_2.10" % "0.1.1" % "test",
  "org.scala-lang" % "scala-compiler" % "2.10.3" % "provided",
  "com.typesafe.akka" %% "akka-actor" % "2.2.3",
  "io.spray" % "spray-can" % "1.2.0",
  "io.spray" % "spray-caching" % "1.2.0",
  "io.spray" % "spray-httpx" % "1.2.0",
  "io.spray" % "spray-routing" % "1.2.0",
  "org.scala-lang.modules.scalajs" %% "scalajs-compiler" % "0.4-SNAPSHOT",
  "org.scala-lang.modules.scalajs" %% "scalajs-library" % "0.4-SNAPSHOT",
  "org.scala-lang.modules.scalajs" %% "scalajs-dom" % "0.3-SNAPSHOT",
  "org.scala-lang.modules.scalajs" %% "scalajs-jquery" % "0.1-SNAPSHOT",
  "com.scalatags" % "scalatags_2.10" % "0.2.3-JS",
  "com.scalarx" % "scalarx_2.10" % "0.2.2-JS"
)

testFrameworks += new TestFramework("utest.runner.JvmFramework")

SbtStartScript.startScriptForClassesSettings

Revolver.settings

(products in Compile) := (products in Compile).value :+ generateClient.value

buildSettingsX

bootSnippet := "ScalaJS.modules.fiddle_Client().main();"

