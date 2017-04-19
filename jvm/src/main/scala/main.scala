package io.underscore.webrtc

import fs2._
import spinoco.fs2.http
import http._
import spinoco.protocol.http._
import spinoco.protocol.http.header.value._

import spinoco.fs2.http.routing._

import java.net.InetSocketAddress
import java.util.concurrent.Executors
import java.nio.channels.AsynchronousChannelGroup

import java.nio.file.Paths

object Main {

  val ES = Executors.newCachedThreadPool(Strategy.daemonThreadFactory("ACG"))
  implicit val ACG = AsynchronousChannelGroup.withThreadPool(ES) // http.server requires an ACG
  implicit val S = Strategy.fromExecutor(ES) // Async (Task) requires a strategy

  type Directory = Map[Uri.Path, HttpResponse[Task]]

  object Directory {
    import java.nio.file.Path
    import java.nio.file.Files
    import scala.collection.JavaConverters._
    import java.util.stream.Collectors
    val html = ContentType(MediaType.`text/html`, Some(HttpCharset.`UTF-8`), boundary = None)
    def files(from: Path): Stream[Task, Path] = {
      val getFiles: Task[Seq[Path]] = Task.delay { Files.list(from).collect(Collectors.toList()).asScala }
      Stream.eval(getFiles).flatMap(Stream.emits)
    }

    def slurp(from: Path): Unit = {
      val eff = files(from)
      println("Runnning...")
      println(eff.runLog.unsafeRun())
    }
  }

  def fileService(dir: Directory)(request: HttpRequestHeader, body: Stream[Task,Byte]): Stream[Task,HttpResponse[Task]] = {
    dir.get(request.path) match {
      case Some(response) => println("OK"); Stream.emit(response)
      case None           => println("404 "+request.path); Stream.empty//emit(HttpResponse(HttpStatusCode.NotFound))
    }
  }


  def main(args: Array[String]): Unit = {
    val staticRoot = Paths.get("src/main/web/")
    Directory.slurp(staticRoot)

    val socket = new InetSocketAddress("127.0.0.1", 9090)

    val dir = Map[Uri.Path, HttpResponse[Task]](
      Uri.Path / "index.html" -> {
        val p = Paths.get("src/main/web/index.html")
        val html = ContentType(MediaType.`text/html`, Some(HttpCharset.`UTF-8`), boundary = None)
        val len = p.toFile.length
        val ok = HttpResponse[Task](HttpStatusCode.Ok).withBodySize(len).withContentType(html)
        val read: Stream[Task, Byte] = io.file.readAll[Task](p, 1024*8)
        ok.copy(body=read)
      }
    )

    http.server(socket)(fileService(dir)).run.unsafeRun()
  }
}
