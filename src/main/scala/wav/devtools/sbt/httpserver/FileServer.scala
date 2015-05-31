package wav.devtools.sbt.httpserver

import java.io.File
import org.http4s.dsl._
import org.http4s.Response
import org.http4s.MediaType
import org.http4s.headers.`Content-Type`
import org.http4s.server.HttpService
import org.slf4j.LoggerFactory
import scalaz.concurrent.Task
import com.google.common.io.Resources
import java.net.URL

object FileServer {

  private val logger = LoggerFactory.getLogger(classOf[FileServer])

  def service(endpoint: String, lookups: Seq[String => URL]) =
    builder(endpoint)(serveFrom(lookups, _))

  def builder(endpoint: String)(handle: String => Task[Response]) = {
    val endpointPath = Path(endpoint.split("/").toList)
    HttpService {
      case GET -> path if path.startsWith(endpointPath) =>
        handle(path.toList.drop(endpointPath.toList.size).mkString("/"))
    }
  }

  def serveFrom(locations: Seq[String => URL], path: String): Task[Response] =
    locations.map(_(path)).collectFirst {
      case url if url.getProtocol.equalsIgnoreCase("file") =>
        val f = new File(url.toURI)
        if (f.exists) serve(f)
        else notFound(url, path)
      case url =>
        serve(path, Resources.toByteArray(url))
    }.get

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

  private def notFound(url: URL, path: String): Task[Response] = {
    logger.debug(s"404 Not Found: '$path -> $url'")
    NotFound(s"404 Not Found: '$path'")
  }


}

private [httpserver] class FileServer
