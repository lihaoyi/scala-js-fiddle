package com
package framework

import akka.actor._
import spray.http._
import spray.http.HttpMethods._

import java.io.{IOException, FileInputStream}
import concurrent.{ExecutionContext, Future}
import ExecutionContext.Implicits.global
import spray.http.ChunkedMessageEnd
import spray.http.HttpResponse
import spray.http.ChunkedResponseStart
import util.Random
import spray.json._


abstract class RootRouter extends Actor{
  def makePage(id: String, path: String): Page

  def receive = {
    case HttpRequest(POST, path, headers, reqBody, _)
      if path startsWith "/ajax/up" =>

      val JsArray(List(JsString(pageId), JsString(fragId), content)) = reqBody.asString.asJson

      context.actorFor(pageId) ! com.framework.Page.Update(fragId, content)
      sender ! HttpResponse(StatusCodes.OK, HttpBody(ContentType.`text/plain`, ""))

    case HttpRequest(POST, "/ajax/down", headers, reqBody, _) =>
      val Seq(pageId, lastMsgId) = reqBody.asString.asJson.convertTo[Seq[String]]
      context.actorFor(pageId) ! com.framework.Page.OpenDownlink(sender, lastMsgId)
  }

  def receiveDefault: PartialFunction[Any, Unit] = {
    case HttpRequest(GET, path, headers, reqBody, _) =>
      val newId = Random.alphanumeric.take(6).mkString
      val server = sender
      println("new Actor ID " + newId)
      context.actorOf(Props(makePage(newId, path)), newId)
      context.actorFor(newId) ! com.framework.Page.Hello(server)
  }

  def serveFile(root: String, path: String, contentType: ContentType, sender: ActorRef) = Future{
    val inputStream = Util.loadResource(root + path)
    val total = inputStream.available
    var current = 0
    val gz = spray.httpx.encoding.Gzip.newCompressor

    sender ! ChunkedResponseStart(HttpResponse(
      StatusCodes.OK,
      HttpBody(contentType, ""),
      headers = List(
        HttpHeaders.`Content-Length`(total),
        HttpHeaders.`Content-Encoding`(HttpEncodings.gzip)
      )

    ))

    while(current < total){
      val chunkSize = Math.min(65536, total - current)
      val bytes = Array.ofDim[Byte](chunkSize)
      inputStream.read(bytes)
      gz.compress(bytes)
      val result = gz.flush()
      sender ! MessageChunk(result)
      current = current + chunkSize
    }

    inputStream.close()
    sender ! ChunkedMessageEnd()

  }.recover{ case e: IOException =>
    sender ! HttpResponse(StatusCodes.NotFound)
  }
}
