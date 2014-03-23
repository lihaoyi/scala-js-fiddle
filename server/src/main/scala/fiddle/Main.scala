package fiddle

import spray.http._
import spray.http.HttpHeaders._
import spray.httpx.encoding.Gzip
import spray.routing.directives.CachingDirectives._
import scala.Some
import spray.routing.{RequestContext, SimpleRoutingApp}
import akka.actor.ActorSystem
import spray.routing.directives.CacheKeyer
import scala.collection.mutable
import java.security.{AccessControlException, Permission}
import java.io.FilePermission
import java.util.PropertyPermission
import java.lang.reflect.ReflectPermission
import java.net.SocketPermission

import spray.json._
import DefaultJsonProtocol._
import scala.concurrent.Future
import scala.tools.nsc.interpreter.Completion
import scala.reflect.internal.util.OffsetPosition
import spray.http.HttpHeaders.`Cache-Control`
import spray.http.CacheDirectives.{`max-age`, `public`}
import spray.http.HttpRequest
import spray.routing.RequestContext
import scala.Some
import spray.http.HttpResponse
import spray.http.CacheDirectives.`max-age`

object Static{
  import scalatags._
  import scalatags.all._
  def page =
    html(
      head(
        meta(charset:="utf-8"),
        Tags2.title("Scala-Js-Fiddle"),
        script(src:="/ace/ace.js", `type`:="text/javascript", charset:="utf-8"),
        link(rel:="stylesheet", href:="/styles.css"),
        link(rel:="stylesheet", href:="/pure-base-min.css"),
        script(raw(
          """
            (function(i,s,o,g,r,a,m){i['GoogleAnalyticsObject']=r;i[r]=i[r]||function(){
                (i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),
              m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)
              })(window,document,'script','//www.google-analytics.com/analytics.js','ga');

              ga('create', 'UA-27464920-3', 'scala-js-fiddle.com');
              ga('send', 'pageview');
          """
        ))
      ),
      body(
        div(id:="spinner-holder")(
          div(display:="table-cell", verticalAlign:="middle", height:="100%")(
            div(style:="text-align: center")(
              h1("Loading Scala-Js-Fiddle"),
              div(
                img(src:="/Shield.svg", height:="200px")
              ),
              br,
              div(
                img(src:="/spinner.gif")
              ),
              p("This takes a while the first time. Please be patient =)")
            )
          )
        ),
        script(`type`:="text/javascript", src:="/example-extdeps.js"),
        script(`type`:="text/javascript", src:="/example-intdeps.js"),
        script(`type`:="text/javascript", src:="/example.js"),
        script("Client().main(); Output2=Output();")
      )
    )
}
object Main extends SimpleRoutingApp {
  implicit val system = ActorSystem()
  implicit val executionContext = system.dispatcher

  def main(args: Array[String]): Unit = {
    implicit val Default: CacheKeyer = CacheKeyer {
      case RequestContext(HttpRequest(_, uri, _, entity, _), _, _) => (uri, entity)
    }
    val simpleCache = routeCache(maxCapacity = 1000)
    startServer("localhost", port = 8080) {
      cache(simpleCache) {
        encodeResponse(Gzip) {
          get {
            respondWithHeaders(`Cache-Control`(`public`, `max-age`(60L*60L*24L))) {
              pathSingleSlash {
                complete{
                  HttpEntity(MediaTypes.`text/html`,"<!DOCTYPE html>" + Static.page.toString())
                }
              } ~
              path("gist" / Segments){ i =>
                complete{
                  HttpEntity(MediaTypes.`text/html`, "<!DOCTYPE html>" + Static.page.toString())
                }
              } ~
              pathPrefix("js") {
                getFromResourceDirectory("..")
              } ~
              pathPrefix("favicon.ico") {
                getFromResource("favicon.ico")
              } ~
              getFromResourceDirectory("")
            }
          } ~
          post {
            path("compile"){
              compileStuff(_, _.filter(_._1.endsWith(".js")).map(_._2).mkString("\n"))
            } ~
            path("optimize"){
              compileStuff(_, Compiler.optimize)
            } ~
            path("preoptimize"){
              compileStuff(_, Compiler.deadCodeElimination)
            } ~
            path("complete" / Segment / IntNumber){
              completeStuff
            }
          }
        }
      }
    }
  }
  def completeStuff(flag: String, offset: Int)(ctx: RequestContext): Unit = {
//    setSecurityManager
    Compiler.autocomplete(ctx.request.entity.asString, flag, offset, Compiler.validJars).foreach { res: List[String] =>
      val response = res.toJson.toString
      println(s"got autocomplete: sending $response")
      ctx.responder ! HttpResponse(
        entity=response,
        headers=List(
          `Access-Control-Allow-Origin`(spray.http.AllOrigins)
        )
      )
    }
  }
  def compileStuff(ctx: RequestContext, processor: Seq[(String, String)] => String): Unit = {

    val output = mutable.Buffer.empty[String]

    val res = Compiler.compile(
      Compiler.prelude.getBytes ++ ctx.request.entity.data.toByteArray,
      Compiler.validJars,
      output.append(_)
    )

    val returned = res match {
      case None =>
        JsObject(
          "success" -> false.toJson,
          "logspam" -> output.mkString.toJson
        )

      case Some(files) =>
        val code = processor(
          files.map(f => f.name -> new String(f.toByteArray))
        )

        JsObject(
          "success" -> true.toJson,
          "logspam" -> output.mkString.toJson,
          "code" -> s"(function(){ $code; ScalaJSExample().main()}).call(window)".toJson
        )
    }

    ctx.responder ! HttpResponse(
      entity=returned.toString,
      headers=List(
        `Access-Control-Allow-Origin`(spray.http.AllOrigins)
      )
    )
  }
}
