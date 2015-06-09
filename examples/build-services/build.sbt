import wav.devtools.sbt.httpserver.{SbtHttpServerPlugin,FileServer}
import SbtHttpServerPlugin.autoImport._

enablePlugins(SbtHttpServerPlugin)

name := "app"

// Host the app at "http://localhost:8083/app/index.html"
addHttpServices(
  _ += FileServer.service("app",Seq((classDirectory in Compile).value))
)

addHttpServices(httpServerBuildServices)

// Remember to run the `copyResources` task before opening the browser for the first time.
emitBuildEvent((copyResources in Compile), "updated")

lazy val sendCommand = inputKey[Unit]("Send a command to an active browser client, e.g. `sbt> sendCommand echo hello`")

sendCommand := {
  import concurrent.duration._
  import scala.util.{Success,Failure}
  val log = streams.value.log
  val args: Seq[String] = Def.spaceDelimited("<arg>").parsed
  val svc = HttpServerKeys.buildCommandService.value.get
  svc.ask(args(0), args.drop(1)) match {
    case Success(data) => log.info(data.toString)
    case Failure(t) => log.error(t.toString)
  }
}