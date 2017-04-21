package io.underscore.webrtc

import fs2._
import spinoco.fs2.http
import http._
import spinoco.protocol.http._

import java.net.InetSocketAddress
import java.util.concurrent.Executors
import java.nio.channels.AsynchronousChannelGroup

import java.nio.file.Paths

object Main {

  val ES = Executors.newCachedThreadPool(Strategy.daemonThreadFactory("ACG"))
  implicit val ACG = AsynchronousChannelGroup.withThreadPool(ES) // http.server requires an ACG
  implicit val S = Strategy.fromExecutor(ES) // Async (Task) requires a strategy

  def fileService
    (dir: StaticContent.Cache)
    (request: HttpRequestHeader, body: Stream[Task,Byte]): Stream[Task,HttpResponse[Task]] = {

    dir.get(request.path) match {
      case None =>
        println(s"404 - ${request.path}")
        Stream.emit(HttpResponse(HttpStatusCode.NotFound))
      case Some(response) => 
        println(s"OK - ${request.path}")
        Stream.emit(response)
    }
  }

  // Add aliases into the cache, such as / -> index.html
  def aliasing(cache: StaticContent.Cache): StaticContent.Cache =
    cache.get(Uri.Path.Root / "index.html") match {
      case Some(response) => cache + (Uri.Path.Root -> response)
      case _ => cache
    }

  def main(args: Array[String]): Unit = {
    val staticRoot = Paths.get("src/main/web/")
    val dir = StaticContent.preCache(staticRoot).unsafeRun()
    val aliased = aliasing(dir)

    val socket = new InetSocketAddress("127.0.0.1", 9090)
    http.server(socket)(fileService(aliased)).run.unsafeRun()
  }
}
