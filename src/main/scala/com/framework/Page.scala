package com
package framework

import akka.actor.{ReceiveTimeout, ActorRef, Actor}
import rx.{Obs, Sig}

import spray.http._
import scalatags.ScalaTags._
import spray.http.HttpResponse
import com.framework.Page.Flush
import spray.json._
import spray.io.IOBridge.Closed
import scala.concurrent.duration._
import spray.httpx.encoding.Gzip

object Page{
  case class Flush()
  case class Update(id: String, msg: JsValue)
  case class OpenDownlink(connection: ActorRef, lastMsgId: String)
  case class Hello(server: ActorRef)
}
case class CometMessage(id: String,
                        listener: String,
                        msg: JsValue)

trait Page extends Actor{

  context.setReceiveTimeout(10 seconds)
  val id: String
  var path: String
  val view: STag

  var downlink: List[ActorRef] = Nil
  var buffer: List[CometMessage] = Nil
  var lastMsgId = ""

  def pageHeader = Seq(
    script.src("/javascript/stateful.js")(""),
    script(s"ajax.runComet('/ajax/down', '$id')")

  )
  case class CallBack(uuid: String, jsString: String, callback: JsValue => Unit){
    override def toString() = jsString
  }

  def stash(text: String => String, callback: JsValue => Unit) = {
    val uuid = scala.util.Random.alphanumeric.take(6).mkString
    val finalText = text(uuid)
    Raw(CallBack(uuid, finalText, callback))
  }

  implicit def extractSig(s: Sig[HtmlTag]): STag = {
    val uuid = scala.util.Random.alphanumeric.take(6).mkString
    def withCls: STag = s.now().cls(""+uuid).attr("cow" -> updater)
    lazy val updater = Obs(s){
      println("SigUpdate")
      val msgId = scala.util.Random.alphanumeric.take(6).mkString
      val msgBody =
        JsObject(
          "fragId" -> JsString(uuid),
          "contents" -> JsString(withCls.toXML.toString)
        )

      buffer = buffer :+ CometMessage(
        msgId,
        "partialUpdate",
        msgBody
      )
      self ! Flush()
    }
    withCls
  }

  def receive = {
    case Page.OpenDownlink(connection, lastMsgId) =>
      context.setReceiveTimeout(Duration.Undefined)
      buffer = buffer.dropWhile(_.id != lastMsgId).drop(1)
      connection ! ChunkedResponseStart(HttpResponse(
        StatusCodes.OK,
        HttpBody(ContentType.`text/plain`, ""),
        headers = List(HttpHeaders.`Content-Encoding`(HttpEncodings.gzip))
      ))
      downlink = connection :: downlink
      self ! Flush()

    case Flush() =>
      (downlink, buffer) match {
        case (_, Nil) | (Nil, _) => ()
        case (first :: rest, stuff) =>

          val data = Gzip.newCompressor
                         .compress(buffer.toJson.toString.getBytes)
                         .finish()

          first ! MessageChunk(data)
          first ! ChunkedMessageEnd()
          downlink = rest
      }

    case Page.Update(id,  msg) =>
      def find[T](tag: STag)(filter: PartialFunction[STag, T]): Seq[T] =
        tag.children.flatMap(x => find(x)(filter)) ++ filter.lift(tag)

      val matchingCallback = find(view){
        case Raw(CallBack(uuid, jsString, callback)) if uuid == id => callback
      }.head

      matchingCallback(msg)

    case Page.Hello(server) =>
      val data = Gzip.newCompressor
                     .compress(("<!DOCTYPE html>" + view.toXML.toString).getBytes)
                     .finish()

      server ! HttpResponse(
        entity = HttpBody(MediaTypes.`text/html`, data),
        headers = List(HttpHeaders.`Content-Encoding`(HttpEncodings.gzip))
      )

    case Closed(conn, reason) =>
      downlink = downlink.filter(_ != sender)
      if (downlink == Nil) context.setReceiveTimeout(10 seconds)

    case ReceiveTimeout =>
      println("TIMEOUT")
      context.stop(self)

  }
}