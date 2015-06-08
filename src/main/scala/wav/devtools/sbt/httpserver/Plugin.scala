package wav.devtools.sbt.httpserver

import org.http4s.server.HttpService
import sbt._
import Keys._

object Import extends Syntax {

  object HttpServerKeys {

    lazy val buildServiceConfig = settingKey[Option[BuildService.Config]]("build service config")
    lazy val buildEventService = settingKey[Option[MessageQueue.O]]("build event service")
    lazy val buildCommandService = settingKey[Option[RequestResponse]]("build command service")
    lazy val services = settingKey[Seq[HttpService]]("The http service to be mounted onto the server")
    lazy val port = settingKey[Int]("The port to host the http server")
    lazy val address = settingKey[String]("The http service address. (Calculated, not an input)")

  }

  import HttpServerKeys._

  def addHttpServices(settings: ApplyServiceSettings*): Seq[Setting[_]] =
    settings.map(_(services in Global)).flatten

  def setHttpServices(settings: ApplyServiceSettings*): Seq[Setting[_]] =
    (services in Global := Seq.empty) +: addHttpServices(settings: _*)

  lazy val httpServerSettings: Seq[Setting[_]] = Seq(
    onLoad in Global := (onLoad in Global).value andThen (load(_, (services in Global).value, (port in Global).value)),
    (onUnload in Global) := (onUnload in Global).value andThen (unload),
    port in Global := 8083,
    services in Global := Seq.empty,
    address in Global := s"http://localhost:${(port in Global).value}",
    buildServiceConfig := Some(BuildService.DefaultConfig),
    buildCommandService := buildServiceConfig.value.map(c => RequestResponse(s"${c.route}/commands")),
    buildEventService := buildServiceConfig.value.map(c => MessageQueue.O(s"${c.route}/events")))

  lazy val httpServerBuildServices: ApplyServiceSettings =
    _ ++= {
        val services = for {
          c <- buildServiceConfig.value
          bes <- buildEventService.value
          bcs <- buildCommandService.value
        } yield Seq(
            bes.service,
            bcs.service,
            BuildService.resourceService({c.port = (port in Global).value; c}))
        services.getOrElse(Seq.empty)
    }

  def emitBuildEvent[T](t: TaskKey[T], event: String) =
    t <<= (name in t.scope, t, buildEventService) { (n, t, q) =>
      if (q.isDefined) t.andFinally(q.get.enqueue( s"""["$n", "$event"]""")) else t
    }

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

}