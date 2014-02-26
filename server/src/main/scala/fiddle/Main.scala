package fiddle

import akka.actor.{ActorSystem, Actor, Props}
import java.nio.file.{Files, Paths}
import spray.http._
import scala.concurrent.{Future, Await}
import java.io._

import akka.io.IO
import spray.can.Http
import akka.pattern.ask
import concurrent.duration._
import spray.http.HttpHeaders.`Access-Control-Allow-Origin`
import spray.http.HttpRequest
import spray.http.ChunkedResponseStart
import scala.Some
import spray.http.HttpResponse
import akka.pattern.pipe

object Main{
  implicit val system = ActorSystem()
  def main(args: Array[String]): Unit = {
    implicit val timeout = akka.util.Timeout(1000)

    val actor = system.actorOf(Props[Server])
    val res = IO(Http) ? Http.Bind(
      actor,
      interface = "0.0.0.0",
      port = util.Properties.envOrElse("PORT", "8080").toInt
    )

    Await.result(res, 1 second)
  }
}

class Server extends Actor {
  import Main.system.dispatcher
  val cache = new spray.caching.SimpleLruCache[HttpResponse](256, 256)
  def receive = {
    case Http.Connected(_, _) => sender ! Http.Register(self)
    case HttpRequest(HttpMethods.GET, uri, headers, entity, protocol) =>
      cache(uri){Future{
        try{
          val data = Files.readAllBytes(
            Paths.get(uri.path.toString.drop(1))
          )
          val mimeType: ContentType = uri.path.toString.split('.').lastOption match {
            case Some("css") => MediaTypes.`text/css`
            case Some("html") => MediaTypes.`text/html`
            case Some("js") => MediaTypes.`application/javascript`
            case _ => ContentTypes.`text/plain`
          }
          HttpResponse(
            StatusCodes.OK,
            entity=HttpEntity.apply(mimeType, data),
            headers=List(
              `Access-Control-Allow-Origin`(spray.http.AllOrigins)
            )
          )
        }catch{case _: IOException =>
          HttpResponse(StatusCodes.NotFound)
        }
      }}.pipeTo(sender)
    case HttpRequest(HttpMethods.POST, uri, headers, entity, protocol) =>
      val s = sender
      var alreadyExists = cache.get(uri).isDefined
      val res = cache(uri){
        var total = new StringBuffer()
        def send(msg: String) = {
          s ! MessageChunk(msg)
          total.append(msg)
        }
        Future{
          s ! ChunkedResponseStart(HttpResponse(
            entity="Compiling...\n" + entity.data.asString + "\n",
            headers=List(
              `Access-Control-Allow-Origin`(spray.http.AllOrigins)
            )
          ))
          val res = Compiler(
            entity.data.toByteArray,
            msg => send(msg)
          )
          res match {
            case None => sender ! ChunkedMessageEnd()
            case Some(code) =>
              send("\n\n\n\n\n" + code + "\n\n\n\n\nCompilation Complete")
              s ! ChunkedMessageEnd()
          }

          HttpResponse(
            entity=HttpEntity(ContentTypes.`text/plain`, total.toString),
            headers=List(
              `Access-Control-Allow-Origin`(spray.http.AllOrigins)
            )
          )
        }
      }
      if (alreadyExists) res.pipeTo(sender)
    case x =>
  }
}
