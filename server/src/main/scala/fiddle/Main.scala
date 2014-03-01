package fiddle

import spray.http._
import spray.http.HttpHeaders._
import spray.http.ChunkedResponseStart
import spray.httpx.encoding.Gzip
import spray.routing.directives.CachingDirectives._
import scala.Some
import spray.http.HttpResponse
import spray.routing.{RequestContext, SimpleRoutingApp}
import akka.actor.ActorSystem
import concurrent.duration._
import spray.routing.directives.CacheKeyer
import scala.collection.mutable
import play.api.libs.json._

object Main extends SimpleRoutingApp {
  implicit val system = ActorSystem()
  def main(args: Array[String]): Unit = {
    implicit val Default: CacheKeyer = CacheKeyer {
      case RequestContext(HttpRequest(_, uri, _, entity, _), _, _) => (uri, entity)
    }
    val simpleCache = routeCache(maxCapacity = 1000)

    startServer("localhost", port = 8080) {
      cache(simpleCache) {
        get {
          encodeResponse(Gzip) {
            pathSingleSlash {
                getFromResource("index.html")
            } ~
            pathPrefix("js") {
              getFromResourceDirectory("..")
            } ~
            getFromResourceDirectory("")
          }
        } ~
        post {
          path("compile"){
            compileStuff
          }
        }
      }
    }
  }
  def compileStuff(ctx: RequestContext): Unit = {
    val output = mutable.Buffer.empty[String]

    val res = Compiler(
      ctx.request.entity.data.toByteArray,
      output.append(_)
    )

    val returned = res match {
      case None =>
        Json.obj(
          "success" -> false,
          "logspam" -> output.mkString
        )
      case Some(code) =>
        Json.obj(
          "success" -> true,
          "logspam" -> output.mkString,
          "code" -> (code + "ScalaJS.modules.ScalaJSExample().main__AT__V()")
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

