package wav.devtools.sbt.httpserver

import java.io.File
import org.http4s.dsl._
import org.http4s.Response
import org.http4s.MediaType
import org.http4s.headers.{`Content-Type`}
import org.http4s.server.HttpService
import org.http4s.util.CaseInsensitiveString
import sbt.RichFile
import scalaz.concurrent.Task

import internaldsl.si

object FileServer {

  def service(endpoint: CaseInsensitiveString, mapping: Map[CaseInsensitiveString, (Seq[File], String => Boolean)]) = HttpService {
    case GET -> Root / mount / group / path if (
      endpoint.equals(si(mount)) &&
        mapping.contains(si(group))) =>
      val (dirs, pathFilter) = mapping(si(group))
      serveFrom(dirs, path, pathFilter)
  }

  def service(endpoint: CaseInsensitiveString, dirs: Seq[File], pathFilter: String => Boolean = _ => true) = HttpService {
    case GET -> Root / mount / path if endpoint.equals(si(mount)) =>
      serveFrom(dirs, path, pathFilter)
  }

  def serveFrom(dirs: Seq[File], path: String, pathFilter: String => Boolean): Task[Response] = {
    if (!pathFilter(path)) notFound(path)
    else dirs.map(new RichFile(_) / path).find(_.exists) match {
      case Some(f) => serve(f)
      case _ => notFound(path)
    }
  }

  def serve(f: File): Task[Response] = {
    val fullPath = f.getCanonicalPath
    val name = f.getName
    val mime = {
      val parts = name.split('.')
      if (parts.length > 0) MediaType.forExtension(parts.last)
        .getOrElse(MediaType.`application/octet-stream`)
      else MediaType.`application/octet-stream`
    }
    val bytes = io.Source.fromFile(f.getCanonicalPath).map(_.toByte).toArray
    Ok(bytes).putHeaders(`Content-Type`(mime))
  }

  private def notFound(path: String): Task[Response] =
    NotFound(s"404 Not Found: '$path'")

}
