import com.typesafe.sbt.SbtStartScript

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-reflect" % "2.10.3",
  "org.scalamacros" % "quasiquotes_2.10.3" % "2.0.0-M3",
  "org.scala-lang.modules.scalajs" %% "scalajs-dom" % "0.3",
  "com.scalatags" % "scalatags_2.10" % "0.2.4-JS"
)

addCompilerPlugin("org.scalamacros" % "paradise_2.10.3" % "2.0.0-M3")

autoCompilerPlugins := true

(SbtStartScript.stage in Compile) := (Keys.`package` in Compile).value

scalaJSSettings

