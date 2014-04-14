import com.typesafe.sbt.SbtStartScript
import ScalaJSKeys._

libraryDependencies ++= Seq(
  "org.scala-lang.modules.scalajs" %% "scalajs-dom" % "0.3",
  "com.scalatags" % "scalatags_2.10" % "0.2.4-JS"
)

(SbtStartScript.stage in Compile) := {
  (Keys.packageBin in Compile).value
}

scalaJSSettings

