package fiddle

import spray.http._
import spray.http.HttpHeaders._
import spray.http.ChunkedResponseStart
import scala.Some
import spray.http.HttpResponse
import spray.routing.{RequestContext, SimpleRoutingApp}
import akka.actor.ActorSystem

object Main extends SimpleRoutingApp {
  implicit val system = ActorSystem()
  def main(args: Array[String]): Unit = {
    startServer("localhost", port = 8080) {
      get {
        pathSingleSlash {
          getFromResource("index.html")
        } ~
        pathPrefix("js") {
          getFromResourceDirectory("..")
        } ~
        getFromResourceDirectory("")
      } ~
      post {
        path("compile"){
          compileStuff
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

