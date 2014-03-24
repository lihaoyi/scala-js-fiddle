import com.typesafe.sbt.SbtStartScript

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-reflect" % "2.10.3",
  "org.scalamacros" % "quasiquotes_2.10.3" % "2.0.0-M3"
)

addCompilerPlugin("org.scalamacros" % "paradise_2.10.3" % "2.0.0-M3")

autoCompilerPlugins := true

(SbtStartScript.stage in Compile) := (Keys.`package` in Compile).value