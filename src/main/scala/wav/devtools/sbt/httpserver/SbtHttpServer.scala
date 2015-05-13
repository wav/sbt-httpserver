package wav.devtools.sbt.httpserver

import sbt._
import Keys._
import org.http4s.dsl._
import org.http4s.{MediaType, Response, Request}
import org.http4s.headers.{`Content-Type`}
import org.http4s.server.{Server, HttpService}
import org.http4s.server.blaze.BlazeBuilder
import java.io.File

object FileServer {

  private def assemblePath(baseDir: File, name: String): String = {
    val realname = if (name.startsWith("/")) name.substring(1) else name
    s"${baseDir.getCanonicalPath}/$realname"
  }

  def service(baseDir: sbt.RichFile) = HttpService {
    case GET -> Root / path =>
      val fullPath = assemblePath(baseDir.asFile, path)
      if (!file(fullPath).exists) NotFound(s"404 Not Found: '$fullPath'")
      else {
        val mime = {
          val parts = path.split('.')
          if (parts.length > 0) MediaType.forExtension(parts.last)
            .getOrElse(MediaType.`application/octet-stream`)
          else MediaType.`application/octet-stream`
        }
        val bytes = io.Source.fromFile(fullPath).map(_.toByte).toArray
        Ok(bytes).putHeaders(`Content-Type`(mime))
      }
  }

}

object Import {

  object HttpServerKeys {

    lazy val httpServerService = settingKey[HttpService]("The http service to be mounted onto the server")
    lazy val httpServerPort = settingKey[Int]("The port to host the http server")
    lazy val httpServerAddress = settingKey[String]("The http service address. (Calculated, not an input)")

  }

  import HttpServerKeys._

  lazy val serveBuildFolder = httpServerService in Global := FileServer.service((baseDirectory in ThisBuild).value)

  lazy val httpServerSettings: Seq[Setting[_]] = Seq(
    onLoad in Global := (onLoad in Global).value andThen (load(_, (httpServerService in Global).value, (httpServerPort in Global).value)),
    (onUnload in Global) := (onUnload in Global).value andThen (unload),
    httpServerPort in Global := 8083,
    serveBuildFolder,
    httpServerAddress in Global := s"http://localhost:${(httpServerPort in Global).value}")

  // ++

  private val serverAttrKey = AttributeKey[Server]("sbt-httpserver-instance")

  private def unload(state: State): State = {
    state.get(serverAttrKey).foreach(_.shutdownNow)
    state.remove(serverAttrKey)
  }

  private def load(state: State, service: org.http4s.server.HttpService, port: Int): State = {
    val server = BlazeBuilder.bindHttp(port)
      .mountService(service, "/")
      .run
    val newState = state.put(serverAttrKey, server)
    newState.addExitHook(unload(newState))
  }

}

object SbtHttpServerPlugin extends AutoPlugin {

  val autoImport = Import

  import autoImport._

  override def globalSettings: Seq[Setting[_]] =
    httpServerSettings

}