import com.typesafe.sbt.SbtStartScript
import scala.scalajs.sbtplugin.ScalaJSPlugin.ScalaJSKeys._

val client = project.in(file("client"))

val server = project.in(file("server"))

SbtStartScript.stage in Compile := Unit

(crossTarget in (client, Compile, packageInternalDepsJS)) := (classDirectory in (server, Compile)).value

(crossTarget in (client, Compile, packageExportedProductsJS)) := (classDirectory in (server, Compile)).value

(crossTarget in (client, Compile, packageExternalDepsJS)) := (classDirectory in (server, Compile)).value

(crossTarget in (server, Compile, generateClient)) := (classDirectory in (server, Compile)).value

