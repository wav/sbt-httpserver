package wav.devtools.sbt.httpserver

import org.http4s.server.HttpService

object BuildService {

  val DefaultConfig = new Config(route = "buildService")

  class Config(val route: String)

  private [httpserver] def resourceService(config: Config): HttpService =
    FileServer.builder(s"${config.route}/app") { (r,p) =>
      if (p == "config.js") {

        FileServer.serve(p,
          s"""var BuildServiceConfig = {
             |  commandService: "ws://${r.serverName}:${r.serverPort}/${config.route}/commands",
             |  eventService: "ws://${r.serverName}:${r.serverPort}/${config.route}/events"
             |};""".stripMargin.getBytes)
      } else {
        val path = if (p.endsWith("/")) (p + "index.html") else p
        FileServer.serveFrom[BuildService](s"/public/$path")
      }
    }

}

private [httpserver] class BuildService