resolvers += Resolver.url("scala-js-releases", url("http://repo.scala-js.org/repo/snapshots/"))(Resolver.ivyStylePatterns)

addSbtPlugin("org.scala-lang.modules.scalajs" % "scalajs-sbt-plugin" % "0.4-SNAPSHOT")

lazy val root = project.in(file(".")).dependsOn(file("../../scala-js-workbench"))

addSbtPlugin("com.typesafe.sbt" % "sbt-start-script" % "0.10.0")
