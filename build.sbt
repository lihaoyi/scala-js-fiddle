import com.typesafe.sbt.SbtStartScript

val client = project.in(file("client"))

val server = project.in(file("server"))

SbtStartScript.stage in Compile := Unit