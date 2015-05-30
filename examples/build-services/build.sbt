import wav.devtools.sbt.httpserver.{SbtHttpServerPlugin,FileServer,RequestResponse}
import wav.devtools.sbt.httpserver.SbtHttpServerPlugin.autoImport._
import HttpServerKeys._

enablePlugins(SbtHttpServerPlugin)

lazy val commandService = settingKey[RequestResponse]("commandService")

lazy val sendCommand = inputKey[Unit]("Send a command to an active browser client")

commandService := RequestResponse("commands")

// Host the app at "http://localhost:8083/app/index.html"
setHttpServices(
  _ += FileServer.service("app",Seq((crossTarget in Compile).value / "classes")),
  _ += commandService.value.service,
  buildEventService((copyResources in Compile) -> "updated"))

sendCommand := {
  import concurrent.duration._
  import scala.util.{Success,Failure}
  val log = streams.value.log
  val args: Seq[String] = Def.spaceDelimited("<arg>").parsed
  commandService.value.ask(args(0), args.drop(1), 1.seconds) match {
    case Success(data) => log.info(data)
    case Failure(t) => log.error(t.toString)
  }
}

// sendCommand <<= sendCommand dependsOn(copyResources in Compile)