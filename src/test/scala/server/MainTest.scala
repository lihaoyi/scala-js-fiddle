package server

import utest._
import akka.io.IO
import spray.can.Http
import spray.http.{HttpMethods, HttpResponse, HttpRequest, Uri}
import akka.pattern.ask
import akka.actor.ActorSystem
import scala.concurrent.Await
import scala.concurrent.duration._
object MainTest extends TestSuite{
  implicit val timeout = akka.util.Timeout(10000)
  implicit val system = ActorSystem()
  import system.dispatcher
  def tests = TestSuite{
    "test" - {
      service.Main.main(Array())

      val res = IO(Http) ? HttpRequest(
        HttpMethods.POST,
        Uri("http://localhost:8080"),
        entity=
          """
            object Main{
              def main(args: Array[String]): Unit = {
                val a = 2
                val b = 3
                println(a + b)
              }
            }
          """
      )
      println(Await.result(res.mapTo[HttpResponse], 10 seconds))
    }
  }
}
