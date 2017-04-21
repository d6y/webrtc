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

  type FileToContentType = file.Path => Option[ContentType]

  /**
   * Pre-caching a directory means recursively walking from a path
   * and caching all the recognized files into responses.
   *
   * - "Recognized" means the file has a known extension such as `.html` in `ContentTypes`.
   * - The directory is assumed to mapped relative to the root URI (/).
   *
   * For example, pre-caching src/resources/web which contains index.html
   * and foo/baz.html will provide /index.html as the content of the index.html file,
   * and /foo/baz.html (returning the relevant content).
   */
  def preCache(from: file.Path)(implicit ct: FileToContentType): Task[Cache] =
    Directory.filesRecursive(from)
      .map(cache(from))
      .runLog
      .map(_.toMap)

  def cache
    (from: file.Path)
    (p: file.Path)
    (implicit ctLookup: FileToContentType): (Uri.Path, HttpResponse[Task]) = {

    val uri = filePathToUriPath(from relativize p)

    val resp: HttpResponse[Task] = ctLookup(p) match {
      case None =>
        HttpResponse(HttpStatusCode.UnsupportedMediaType)
      case Some(ct) =>
        val len = p.toFile.length
        val ok = HttpResponse[Task](HttpStatusCode.Ok).withBodySize(len).withContentType(ct)
        val read: Stream[Task, Byte] = io.file.readAll[Task](p, 1024*8)
        ok.copy(body=read)
    }
    uri -> resp
  }

  def filePathToUriPath(f: file.Path): Uri.Path = {
    val elements: Iterator[file.Path] = f.iterator().asScala
    elements.foldLeft(Uri.Path.Root) { _ / _.toString }
  }

  object ContentTypes {

    object Implicits {
      implicit val defaultContentTypeLookup: FileToContentType = ContentTypes.fromExtension _
    }

    def fromExtension(p: file.Path): Option[ContentType] =
      extension(p).collect {
        case "html" => ContentType(MediaType.`text/html`, Some(HttpCharset.`UTF-8`), boundary = None)
        case "css"  => ContentType(MediaType.`text/css`, Some(HttpCharset.`UTF-8`), boundary = None)
        case "js"   => ContentType(MediaType.`application/javascript`, Some(HttpCharset.`UTF-8`), boundary = None)
        case "png"  => ContentType(MediaType.`image/png`, None, boundary = None)
        case "jpg"  => ContentType(MediaType.`image/jpeg`, None, boundary = None)
      }

    private[this] def extension(p: file.Path): Option[String] = {
      val parts: Array[String] = p.getFileName().toString.split('.')
      if (parts.length <= 1) None // Length 1 if no '.' in the filename
      else Some(parts.last)
    }
  }
}
