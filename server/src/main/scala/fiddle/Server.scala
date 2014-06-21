package fiddle
import acyclic.file
import spray.http._
import spray.http.HttpHeaders._
import spray.httpx.encoding.Gzip
import spray.routing.directives.CachingDirectives._
import akka.actor.ActorSystem
import spray.routing.directives.CacheKeyer
import scala.collection.mutable
import spray.client.pipelining._

import spray.http.HttpRequest
import scala.Some
import spray.http.HttpResponse
import spray.routing._
import upickle._
import scala.scalajs.tools.classpath.PartialIRClasspath

object Server extends SimpleRoutingApp {
  implicit val system = ActorSystem()
  import system.dispatcher

  def main(args: Array[String]): Unit = {
    implicit val Default: CacheKeyer = CacheKeyer {
      case RequestContext(HttpRequest(_, uri, _, entity, _), _, _) => (uri, entity)
    }

    val clientFiles = Seq("/client-fastopt.js")
    val simpleCache = routeCache(maxCapacity = 1000)
    println("Power On Self Test")
    val res = Compiler.compile("""
      @JSExport
      object Main{
        @JSExport
        def main() = "omgggg"
      }
    """.getBytes, println)
    val optimized = res.get |> Compiler.fastOpt |> Compiler.fullOpt |> Compiler.export

    println("Power On Self Test complete: " + optimized.length + " bytes")
    assert(optimized.contains("omgggg"))

    startServer("0.0.0.0", port = 8080) {
      cache(simpleCache) {
        encodeResponse(Gzip) {
          get {
            pathSingleSlash {
              complete{
                HttpEntity(
                  MediaTypes.`text/html`,
                  Static.page(
                    s"Client().gistMain([])",
                    clientFiles,
                    "Loading gist..."
                  )
                )
              }
            } ~
            path("gist" / Segments){ i =>
              complete{
                HttpEntity(
                  MediaTypes.`text/html`,
                  Static.page(
                    s"Client().gistMain(${write(i)})",
                    clientFiles,
                    "Loading gist..."
                  )
                )
              }
            } ~
            getFromResourceDirectory("")
          } ~
          post {
            path("compile"){
              compileStuff(_, _ |> Compiler.export)
            } ~
            path("fastOpt"){
              compileStuff(_, _ |> Compiler.fastOpt |> Compiler.export)
            } ~
            path("fullOpt"){
              compileStuff(_, _ |> Compiler.fastOpt |> Compiler.fullOpt |> Compiler.export)
            } ~
            path("export"){
              formFields("compiled", "source"){
                renderCode(_, Nil, _, "Page().exportMain(); ScalaJSExample().main();", analytics = false)
              }
            } ~
            path("import"){
              formFields("compiled", "source"){
                renderCode(_, clientFiles, _, "Client().importMain(); ScalaJSExample().main();", analytics = true)
              }
            } ~
            path("complete" / Segment / IntNumber){
              completeStuff
            }
          }
        }
      }
    }
  }
  def renderCode(compiled: String, srcFiles: Seq[String], source: String, bootFunc: String, analytics: Boolean) = {

    complete{
      HttpEntity(
        MediaTypes.`text/html`,
        Static.page(bootFunc, srcFiles, source, compiled, analytics)
      )
    }
  }

  def completeStuff(flag: String, offset: Int)(ctx: RequestContext): Unit = {
//    setSecurityManager
    for(res <- Compiler.autocomplete(ctx.request.entity.asString, flag, offset)){
      val response = write(res)
      println(s"got autocomplete: sending $response")
      ctx.responder ! HttpResponse(
        entity=response.toString(),
        headers=List(
          `Access-Control-Allow-Origin`(spray.http.AllOrigins)
        )
      )
    }
  }

  def compileStuff(ctx: RequestContext, processor: PartialIRClasspath => String): Unit = {

    val output = mutable.Buffer.empty[String]

    val res = Compiler.compile(
      ctx.request.entity.data.toByteArray,
      output.append(_)
    )

    val returned = res match {
      case None =>
        Json.write(Js.Object(Seq(
          "success" -> writeJs(false),
          "logspam" -> writeJs(output.mkString)
        )))

      case Some(files) =>
        val code = processor(files)

        Json.write(Js.Object(Seq(
          "success" -> writeJs(true),
          "logspam" -> writeJs(output.mkString),
          "code" -> writeJs(code)
        )))
    }

    ctx.responder ! HttpResponse(
      entity=returned.toString,
      headers=List(
        `Access-Control-Allow-Origin`(spray.http.AllOrigins)
      )
    )
  }
}
