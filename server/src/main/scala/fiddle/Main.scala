package fiddle

import spray.http._
import spray.http.HttpHeaders._
import spray.httpx.encoding.Gzip
import spray.routing.directives.CachingDirectives._
import scala.Some
import spray.http.HttpResponse
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

object Main extends SimpleRoutingApp {
  implicit val system = ActorSystem()
  implicit val executionContext = system.dispatcher

  /**
   * Only set this once
   */  
  lazy val setSecurityManager = System.setSecurityManager(SecurityManager)

  def main(args: Array[String]): Unit = {
    implicit val Default: CacheKeyer = CacheKeyer {
      case RequestContext(HttpRequest(_, uri, _, entity, _), _, _) => (uri, entity)
    }
    val simpleCache = routeCache(maxCapacity = 1000)

    startServer("localhost", port = 8080) {
      cache(simpleCache) {
        get {
          respondWithHeader(`Cache-Control`(`public`, `max-age`(60L*60L*24L))) {
            encodeResponse(Gzip) {
              pathSingleSlash {
                getFromResource("index.html")
              } ~
              path("gist" / Segment){ i =>
                getFromResource("index.html")
              } ~
              path("gist" / Segment / Segment){ (i, j) =>
                getFromResource("index.html")
              } ~
              pathPrefix("js") {
                getFromResourceDirectory("..")
              } ~
              getFromResourceDirectory("")
            }
          }
        } ~
        post {
          path("compile"){
            compileStuff
          } ~
          path("complete" / IntNumber){
            completeStuff
          }
        }
      }
    }

  }
  def completeStuff(offset: Int)(ctx: RequestContext): Unit = {
//    setSecurityManager
    Compiler.autocomplete(ctx.request.entity.asString, offset).foreach { res: List[String] =>
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
  def compileStuff(ctx: RequestContext): Unit = try{

    val output = mutable.Buffer.empty[String]
//    setSecurityManager

    val res = Compiler(
      ctx.request.entity.data.toByteArray,
      output.append(_)
    )

    val returned = res match {
      case None =>
        JsObject(
          "success" -> false.toJson,
          "logspam" -> output.mkString.toJson
        )

      case Some(code) =>
        JsObject(
          "success" -> true.toJson,
          "logspam" -> output.mkString.toJson,
          "code" -> (code + "ScalaJS.modules.ScalaJSExample().main__AT__V()").toJson
        )
    }

    ctx.responder ! HttpResponse(
      entity=returned.toString,
      headers=List(
        `Access-Control-Allow-Origin`(spray.http.AllOrigins)
      )
    )
  } catch{case e: AccessControlException =>
    e.printStackTrace()
    ctx.responder ! HttpResponse(
      status=StatusCodes.BadRequest,
      entity="",
      headers=List(
        `Access-Control-Allow-Origin`(spray.http.AllOrigins)
      )
    )
  }
}

/**
 * First approximation security manager that allows the good stuff while
 * blocking everything else. Doesn't block things like infinite-looping
 * or infinite-memory, and is probably full of other holes, but at least
 * obvious bad stuff doesn't work.
 */
object SecurityManager extends java.lang.SecurityManager{
  override def checkPermission(perm: Permission): Unit = {
    perm match{
      case p: FilePermission if p.getActions == "read" =>
        // Needed for the compiler to read class files
      case p: PropertyPermission =>
        // Needed for the filesystem operations to work properly
      case p: ReflectPermission if p.getName == "suppressAccessChecks" =>
        // Needed for scalac to load data from classpath
      case p: SocketPermission if p.getActions == "accept,resolve" =>
        // Needed to continue accepting incoming HTTP requests
      case p: RuntimePermission
        if p.getName == "setContextClassLoader"
        || p.getName == "getClassLoader"
        || p.getName == "getenv.*"
        || p.getName == "accessDeclaredMembers" // needed to start htreads, for some reason
        || p.getName == "getenv.SOURCEPATH" =>

      case _ =>
        throw new AccessControlException(perm.toString)
    }
  }
  override def checkAccess(g: Thread) = {
    if (g.getName != "system") super.checkAccess(g)
  }
  override def checkAccess(g: ThreadGroup) = {
    if (g.getName != "SIGTERM handler") super.checkAccess(g)
  }
}