package io.underscore.webrtc

import java.nio.file.{Files, Path}
import scala.collection.JavaConverters._
import java.util.stream.Collectors.toList

import fs2._
import fs2.util.syntax._

object Directory {

  def filesRecursive(from: Path): Stream[Task, Path] = filesRecursive(from :: Nil)

  def filesRecursive(paths: Seq[Path]): Stream[Task, Path] =
    if (paths.isEmpty) Stream.empty
    else {
      val (dirs, files) = paths.partition(_.toFile.isDirectory)
      Stream.emits(files) ++ Stream.eval(getFiles(dirs)).flatMap(filesRecursive)
    }
  
  private[this] def getFiles(dir: Path): Task[Seq[Path]] = Task.delay {
    Files.list(dir).collect(toList()).asScala
  }

  private[this] def getFiles(dirs: Seq[Path]): Task[Seq[Path]] =
    dirs.map(getFiles).sequence.map(_.flatten)
}
