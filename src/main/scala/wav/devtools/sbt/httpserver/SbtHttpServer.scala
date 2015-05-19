package wav.devtools.sbt.httpserver

import org.http4s.server.HttpService
import org.http4s.util.CaseInsensitiveString
import sbt._
import Keys._
import org.http4s.server.blaze.BlazeBuilder

object Import {

  val si = CaseInsensitiveString.apply _

  object HttpServerKeys {

    lazy val httpServerService = settingKey[Seq[HttpService]]("The http service to be mounted onto the server")
    lazy val httpServerPort = settingKey[Int]("The port to host the http server")
    lazy val httpServerAddress = settingKey[String]("The http service address. (Calculated, not an input)")

  }

  import HttpServerKeys._

  type ServiceSettings = SettingKey[Seq[HttpService]] => Seq[Setting[_]]

  lazy val serveBuildFolderService: ServiceSettings =
    _ += FileServer.service(si("files"), Seq((baseDirectory in ThisBuild).value))

  def buildEventService[T](eventMapping: (TaskKey[T], String)*): ServiceSettings = k => {
    val (emitter, s) = EventStream.service(si("buildEvents"))
    def emit(t: TaskKey[T], event: String) =
      t <<= (name in t.scope, t) { (n, t) =>
        t.andFinally(emitter( s"""{"event":"$event","project":"$n"}"""))
      }
    Seq(k += s) ++ eventMapping.map(e => emit(e._1,e._2))
  }

  def addHttpServices(settings: ServiceSettings*): Seq[Setting[_]] =
    settings.map(_(httpServerService in Global)).flatten

  def setHttpServices(settings: ServiceSettings*): Seq[Setting[_]] =
    (httpServerService in Global := Seq.empty) +: addHttpServices(settings: _*)

  lazy val httpServerSettings: Seq[Setting[_]] = Seq(
    onLoad in Global := (onLoad in Global).value andThen (load(_, (httpServerService in Global).value, (httpServerPort in Global).value)),
    (onUnload in Global) := (onUnload in Global).value andThen (unload),
    httpServerPort in Global := 8083,
    httpServerAddress in Global := s"http://localhost:${(httpServerPort in Global).value}") ++
    addHttpServices(serveBuildFolderService)

  // ++

  private val serverAttrKey = AttributeKey[Server]("sbt-httpserver-instance")

  private def unload(state: State): State = {
    state.get(serverAttrKey) match {
      case Some(s) =>
        s.stop
        state.remove(serverAttrKey)
      case _ => state
    }
  }

  private def load(state: State, services: Seq[HttpService], port: Int): State = {
    if (services.isEmpty) state
    else {
      val server = new SimpleWebSocketServer(port, services)
      server.start
      val newState = state.put(serverAttrKey, server)
      newState.addExitHook(unload(newState))
    }
  }

}

object SbtHttpServerPlugin extends AutoPlugin {

  val autoImport = Import

  import autoImport._

  override def globalSettings: Seq[Setting[_]] =
    httpServerSettings

  override def projectSettings: Seq[Setting[_]] =
    addHttpServices(buildEventService((compile in Compile) -> "compiled"))

}