resolvers += Resolver.url("scala-js-releases",
  url("http://dl.bintray.com/content/scala-js/scala-js-releases"))(
    Resolver.ivyStylePatterns)

resolvers += Resolver.url("scala-js-snapshots",
  url("http://repo.scala-js.org/repo/snapshots/"))(
    Resolver.ivyStylePatterns)

resolvers += "typesafe" at "http://repo.typesafe.com/typesafe/releases/"

addSbtPlugin("org.scala-lang.modules.scalajs" % "scalajs-sbt-plugin" % "0.4.3")

addSbtPlugin("com.typesafe.sbt" % "sbt-start-script" % "0.10.0")

addSbtPlugin("io.spray" % "sbt-revolver" % "0.7.1")

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.11.2")

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.7.4")