package io.underscore.webrtc

import fs2._

import spinoco.fs2.http._
import spinoco.fs2.http.routing._
import spinoco.protocol.http._
import spinoco.protocol.http.header.value._

import java.nio.file
import java.nio.file.Paths
import collection.JavaConverters._

object StaticContent {

  type Cache = Map[Uri.Path, HttpResponse[Task]]

  val html = ContentType(MediaType.`text/html`, Some(HttpCharset.`UTF-8`), boundary = None)

  def preCache(from: file.Path): Cache = {

    val eff = Directory.filesRecursive(from)
    println("Runnning...")
    val ps = eff.runLog.unsafeRun()

    def filePathToUriPath(f: file.Path): Uri.Path = {
      val elements: Iterator[file.Path] = f.iterator().asScala
      elements.foldLeft(Uri.Path.Root) { _ / _.toString }
    }

    println(ps.map(p => from.relativize(p)).map(filePathToUriPath))

    val dir = Map[Uri.Path, HttpResponse[Task]](
      Uri.Path / "index.html" -> {
        val p = Paths.get("src/main/web/index.html")
        val len = p.toFile.length
        val ok = HttpResponse[Task](HttpStatusCode.Ok).withBodySize(len).withContentType(html)
        val read: Stream[Task, Byte] = io.file.readAll[Task](p, 1024*8)
        ok.copy(body=read)
      }
    )

    //def knownType(p: file.Path): Boolean = ???

    Directory.filesRecursive(from).filter(_.toString.endsWith(".html")).map{p =>
        println("Considering: "+p)
        p
    }.runLog.unsafeRun()

    dir

  }
}
