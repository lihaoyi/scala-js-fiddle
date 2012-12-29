package com.example

import spray.can.server.{HttpServer, SprayCanHttpServerApp}
import com.site.RootRouter
import akka.actor.Props
import spray.io._
import spray.io.PerConnectionHandler

object Boot extends App with SprayCanHttpServerApp{

  def service = system.actorOf(Props(new RootRouter))

  val httpServer = system.actorOf(
    Props(new HttpServer(
      ioBridge = IOExtension(system).ioBridge(),
      messageHandler = SingletonHandler(service)
    ))
  )

  httpServer ! Bind(interface = "0.0.0.0", port = 80)
}