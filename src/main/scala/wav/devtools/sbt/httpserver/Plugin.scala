package wav.devtools.sbt.httpserver

import org.http4s.server.HttpService
import sbt._
import Keys._

import scala.util.Try

object Import extends Syntax {

  object HttpServerKeys {

    object ENV {
      val dontStart = "sbthttpserver.dontStart"
      val alternativePort = "sbthttpserver.port"
    }

    lazy val buildServiceConfig = settingKey[Option[BuildService.Config]]("build service config")
    lazy val buildEventService = settingKey[Option[MessageQueue.O]]("build event service")
    lazy val buildCommandService = settingKey[Option[RequestResponse]]("build command service")
    lazy val httpServices = settingKey[Seq[HttpService]]("The http service to be mounted onto the server")
    lazy val httpServerPort = settingKey[Int]("The *preferred* port to host the http server on")
    lazy val httpServerAddress = taskKey[String]("The http service address. [WARN] fails when debugging.")

  }

  import HttpServerKeys._

  def addHttpServices(settings: ApplyServiceSettings*): Seq[Setting[_]] =
    settings.map(_(httpServices in Global)).flatten

  def setHttpServices(settings: ApplyServiceSettings*): Seq[Setting[_]] =
    (httpServices in Global := Seq.empty) +: addHttpServices(settings: _*)

  lazy val httpServerSettings: Seq[Setting[_]] = Seq(
    onLoad := (onLoad in Global).value andThen (load(_, (httpServices in Global).value, (httpServerPort in Global).value)),
    onUnload := (onUnload in Global).value andThen (unload),
    httpServerPort := 8083,
    getHttpServerAddress,
    httpServices := Seq.empty,
    buildServiceConfig := Some(BuildService.DefaultConfig),
    buildCommandService := buildServiceConfig.value.map(c => RequestResponse(s"${c.route}/commands")),
    buildEventService := buildServiceConfig.value.map(c => MessageQueue.O(s"${c.route}/events")))

  lazy val httpServerBuildServices: ApplyServiceSettings =
    _ ++= {
      val services = for {
        c <- (buildServiceConfig in Global).value
        bes <- (buildEventService in Global).value
        bcs <- (buildCommandService in Global).value
      } yield Seq(
          bes.service,
          bcs.service,
          BuildService.resourceService(c))
      services.getOrElse(Seq.empty)
    }

  // This setting must be specified as a part of each project's settings.
  def emitBuildEvent[T](t: TaskKey[T], event: String) =
    t <<= (name in t.scope, t, buildEventService in Global) { (n, t, q) =>
      t.andFinally(q.get.enqueue( s"""["$n", "$event"]"""))
    }

  val getHttpServerAddress = httpServerAddress := {
    val s = (state in Global).value
    s.get(serverAttrKey) match {
      case Some((token, port, server)) =>
        val selectedPort: Int =
          if (port > 0) port
          else {
            val result = Util.findHost(
              route = "sbthttpserver",
              token = token,
              debug = m => streams.value.log.info(m))
            if (result.isEmpty) sys.error("Cannot find port, supported platforms are: " + Util.findPorts.keys.mkString(","))
            val Some((_, port)) = result
            port
          }
        s"http://localhost:$selectedPort"
      case _ => sys.error("Server not running.")
    }
  }

  private val serverAttrKey = AttributeKey[(String, Int, Server)]("sbt-httpserver-instance")

  private def unload(state: State): State = {
    state.get(serverAttrKey) match {
      case Some((token, port, server)) =>
        server.stop
        state.remove(serverAttrKey)
      case _ => state
    }
  }

  private def load(state: State, services: Seq[HttpService], port: Int): State = {
    if (sys.props.contains(ENV.dontStart) || services.isEmpty) state
    else {
      val (token, baseService) = basicService
      val actualPort = sys.props.get(ENV.alternativePort).collect {
        case port: String if Try(port.toInt).isSuccess => port.toInt
      }.getOrElse(port)
      val server = new SimpleWebSocketServer(actualPort, baseService +: services)
      server.start
      val newState = state.put(serverAttrKey, (token, actualPort, server))
      newState.addExitHook(unload(newState))
    }
  }

  private lazy val basicService: (String, HttpService) = {
    val token = scala.util.Random.alphanumeric.take(8).mkString
    val basic = {
      import org.http4s.dsl._
      HttpService { case GET -> Root / "sbthttpserver" => Ok(token) }
    }
    (token, basic)
  }

}

object SbtHttpServerPlugin extends AutoPlugin {

  val autoImport = Import

  import autoImport._

  override def globalSettings: Seq[Setting[_]] =
    httpServerSettings

}