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

    def notFound: Stream[Task,HttpResponse[Task]] = {
      println(s"404 - ${request.path}")
      Stream.emit(HttpResponse(HttpStatusCode.NotFound))
    }

    def found(response: HttpResponse[Task]): Stream[Task,HttpResponse[Task]] = {
      println(s"OK - ${request.path}")
      Stream.emit(response)
    }

    dir.get(request.path).fold(notFound)(found)
  }

  def main(args: Array[String]): Unit = {
    val staticRoot = Paths.get("src/main/web/")
    val dir = StaticContent.preCache(staticRoot).unsafeRun()
    //dir.foreach(println)

    val socket = new InetSocketAddress("127.0.0.1", 9090)
    http.server(socket)(fileService(dir)).run.unsafeRun()
  }
}
