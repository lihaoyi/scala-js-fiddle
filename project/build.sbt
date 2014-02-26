resolvers += "spray repo" at "http://repo.spray.io"

resolvers += Resolver.url("scala-js-releases",
  url("http://dl.bintray.com/content/scala-js/scala-js-releases"))(
    Resolver.ivyStylePatterns)

resolvers += Resolver.url("scala-js-snapshots",
  url("http://repo.scala-js.org/repo/snapshots/"))(
    Resolver.ivyStylePatterns)

addSbtPlugin("org.scala-lang.modules.scalajs" % "scalajs-sbt-plugin" % "0.4-SNAPSHOT")

lazy val root = project.in(file(".")).dependsOn(uri("https://github.com/lihaoyi/workbench.git"))

addSbtPlugin("com.typesafe.sbt" % "sbt-start-script" % "0.10.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-proguard" % "0.2.2")