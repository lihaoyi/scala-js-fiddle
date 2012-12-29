package com
package site

import spray.http._
import HttpMethods._

import spray.io.PipelineContext


class RootRouter extends com.framework.RootRouter{
  override def makePage(id: String, path: String) = new Page(id, path)
  override def receive = super.receive.orElse[Any, Unit]{
    case HttpRequest(GET, path, headers, _, _)
      if path startsWith "/javascript/" =>
      val Array(_, _, rest) = path.split("/", 3)
      println("Serving " + rest)
      serveFile("javascript/", rest, MediaTypes.`application/javascript`, sender)

    case HttpRequest(GET, path, headers, _, _)
      if path startsWith "/css/" =>
      val Array(_, _, rest) = path.split("/", 3)
      println("Serving " + rest)
      serveFile("css/", rest, MediaTypes.`text/css`, sender)

  }.orElse(super.receiveDefault)
}

