package wav.devtools.sbt.httpserver

import org.http4s.server.HttpService

object BuildService {

  val DefaultConfig = new Config(route = "buildService")

  class Config(val route: String) {
    private [httpserver] var port: Int = _
  }

  private [httpserver] def resourceService(config: Config): HttpService =
    FileServer.builder(s"${config.route}/app") { p =>
      if (p == "config.js") {
        FileServer.serve(p,
          s"""var BuildServicesConfig = {
             |  buildCommandService: "ws://localhost:${config.port}/${config.route}/commands",
             |  buildEventService: "ws://localhost:${config.port}/${config.route}/events"
             |};""".stripMargin.getBytes)
      } else {
        val path = if (p.endsWith("/")) (p + "index.html") else p
        FileServer.serveFrom[BuildService](s"/public/$path")
      }
    }

}

private [httpserver] class BuildService