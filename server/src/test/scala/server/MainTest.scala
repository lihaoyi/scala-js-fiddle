package server

import utest._
import akka.io.IO
import spray.can.Http
import spray.http._
import akka.pattern.ask
import akka.actor.ActorSystem
import scala.concurrent.Await
import scala.concurrent.duration._
import spray.http.HttpRequest
import spray.http.HttpResponse

object MainTest extends TestSuite{
  implicit val timeout = akka.util.Timeout(10000)
  implicit val system = ActorSystem()
  import system.dispatcher

  def makeRequest(body: String) = {
    val res = IO(Http) ? HttpRequest(
      HttpMethods.POST,
      Uri("http://localhost:8080"),
      entity=body
    )
    Await.result(res.mapTo[HttpResponse], 10 seconds)
  }
  def tests = TestSuite{
    "test" - {
      fiddle.Main.main(Array())
      val res = makeRequest("""

      """)
      val code = res.entity.asString
      assert(
        res.status == StatusCodes.OK,
        code.contains("var a = 2;"),
        code.contains("var b = 3;"),
        code.contains("a + b")
      )
    }
    "compileError" - {
      val res = makeRequest("""
        object Main{
          def main(args: Array[String]): Unit = {
            val a = 2
            val b = 3
            println(a + c)
          }
        }
      """)
      val errors = res.entity.asString
      assert(
        res.status == StatusCodes.BadRequest,
        errors.contains("not found: value c"),
        errors.contains("println(a + c")
      )
      println(res)
    }
  }
}
