import com.typesafe.sbt.SbtStartScript
import scala.scalajs.sbtplugin.ScalaJSPlugin.ScalaJSKeys._

val client = project.in(file("client"))

val macros = project.in(file("macros"))

val server = project.in(file("server")).dependsOn(macros)

SbtStartScript.stage in Compile := Unit

(crossTarget in (client, Compile, packageInternalDepsJS)) := (classDirectory in (server, Compile)).value

(crossTarget in (client, Compile, packageExportedProductsJS)) := (classDirectory in (server, Compile)).value

(crossTarget in (client, Compile, packageExternalDepsJS)) := (classDirectory in (server, Compile)).value

(crossTarget in (client, Compile)) := (classDirectory in (server, Compile)).value

(crossTarget in (macros, Compile)) := (classDirectory in (server, Compile)).value