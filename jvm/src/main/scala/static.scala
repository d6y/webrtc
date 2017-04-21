package io.underscore.webrtc

import fs2._

import spinoco.fs2.http._
import spinoco.fs2.http.routing._
import spinoco.protocol.http._
import spinoco.protocol.http.header.value._

import java.nio.file
import collection.JavaConverters._

object StaticContent {

  type Cache = Map[Uri.Path, HttpResponse[Task]]

  object ContentTypes {
    val html = ContentType(MediaType.`text/html`, Some(HttpCharset.`UTF-8`), boundary = None)

    val all = Map(
      ".html" -> html
    )

    def knownFileTypes(p: file.Path): Boolean = p.toString endsWith ".html"
  }

  def preCache(from: file.Path): Task[Cache] = {

    import ContentTypes._

    def filePathToUriPath(f: file.Path): Uri.Path = {
      val elements: Iterator[file.Path] = f.iterator().asScala
      elements.foldLeft(Uri.Path.Root) { _ / _.toString }
    }

    def cache(p: file.Path): (Uri.Path, HttpResponse[Task]) = {
      val len = p.toFile.length
      val ok = HttpResponse[Task](HttpStatusCode.Ok).withBodySize(len).withContentType(html)
      val read: Stream[Task, Byte] = io.file.readAll[Task](p, 1024*8)
      val uri = filePathToUriPath(from relativize p)
      uri -> ok.copy(body=read)
    }

    Directory.filesRecursive(from)
      .filter(knownFileTypes)
      .map(cache)
      .runLog
      .map(_.toMap)
  }
}
