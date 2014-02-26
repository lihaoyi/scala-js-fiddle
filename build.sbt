import com.typesafe.sbt.SbtStartScript

val client = project.in(file("client"))

val server = project.in(file("server"))

val root = project.in(file(".")).aggregate(client, server)

SbtStartScript.stage in Compile := Unit