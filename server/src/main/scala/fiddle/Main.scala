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
object Main extends SimpleRoutingApp {
  implicit val system = ActorSystem()
  def main(args: Array[String]): Unit = {
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
    def send(msg: String) = {
      ctx.responder ! MessageChunk(msg)
    }

    ctx.responder ! ChunkedResponseStart(HttpResponse(
      entity="Compiling...\n" + ctx.request.entity.data.asString + "\n",
      headers=List(
        `Access-Control-Allow-Origin`(spray.http.AllOrigins)
      )
    ))
    val res = Compiler(
      ctx.request.entity.data.toByteArray,
      msg => send(msg)
    )
    res match {
      case None => ctx.responder ! ChunkedMessageEnd()
      case Some(code) =>
        send("\n\n\n\n\n" + code + "\n\n\n\n\nCompilation Complete")
        ctx.responder ! ChunkedMessageEnd()
    }
  }
}

