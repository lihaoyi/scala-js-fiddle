resolvers += "spray repo" at "http://repo.spray.io"

resolvers += Resolver.url("scala-js-releases",
  url("http://dl.bintray.com/content/scala-js/scala-js-releases"))(
    Resolver.ivyStylePatterns)

resolvers += Resolver.url("scala-js-snapshots",
  url("http://repo.scala-js.org/repo/snapshots/"))(
    Resolver.ivyStylePatterns)

// for com.typesafe.play % play-json_2.10 % 2.2.0-RC1 (needed for workbench)
resolvers += "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/"

addSbtPlugin("org.scala-lang.modules.scalajs" % "scalajs-sbt-plugin" % "0.4.0")

addSbtPlugin("com.lihaoyi" % "workbench" % "0.1.1")

addSbtPlugin("com.typesafe.sbt" % "sbt-start-script" % "0.10.0")

addSbtPlugin("io.spray" % "sbt-revolver" % "0.7.1")
