package wav.devtools.sbt.httpserver

import java.io.File
import org.http4s.dsl._
import org.http4s.{Response, Request}
import org.http4s.MediaType
import org.http4s.headers.`Content-Type`
import org.http4s.server.HttpService
import org.slf4j.LoggerFactory
import scalaz.concurrent.Task
import com.google.common.io.Resources
import java.net.URL
import scala.util.Try

object FileServer {

  private val logger = LoggerFactory.getLogger(classOf[FileServer])

  def service(endpoint: String, lookups: Seq[String => URL]) =
    builder(endpoint)((_, path) => serveFrom(lookups, path))

  def builder(endpoint: String)(handle: (Request, String) => Task[Response]) = {
    val endpointPath = Path(endpoint.split("/").toList)
    HttpService {
      case req @ GET -> path if path.startsWith(endpointPath) =>
        handle(req, path.toList.drop(endpointPath.toList.size).mkString("/"))
    }
  }

  def serveFrom(locations: Seq[String => URL], path: String): Task[Response] = {
    val matches = locations.map(_(path)).collect {
      case url if url.getProtocol.equalsIgnoreCase("file") =>
        val f = new File(url.toURI)
         if (f.exists) Some(serve(f)) else None
      case url =>
        Try(serve(path, Resources.toByteArray(url))).toOption
    }.flatten
    matches.headOption.getOrElse(NotFound(s"404 Not Found: '$path'"))
  }

  def serveFrom[T](path: String)(implicit m: Manifest[T]): Task[Response] = {
    val url = Resources.getResource(m.runtimeClass, path)
    serve(path, Resources.toByteArray(url))
  }

  def serve(name: String, bytes: Array[Byte]): Task[Response] =
    Ok(bytes).putHeaders(`Content-Type`(mime(name)))

  def serve(f: File): Task[Response] = {
    val fullPath = f.getCanonicalPath
    val bytes = io.Source.fromFile(f.getCanonicalPath).map(_.toByte).toArray
    serve(f.getName, bytes)
  }

  private def mime(name: String): MediaType = {
    val parts = name.split('.')
    if (parts.length > 0) MediaType.forExtension(parts.last)
      .getOrElse(MediaType.`application/octet-stream`)
    else MediaType.`application/octet-stream`
  }

}

private[httpserver] class FileServer
