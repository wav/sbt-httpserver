import wav.devtools.sbt.httpserver.{SbtHttpServerPlugin,FileServer}
import wav.devtools.sbt.httpserver.SbtHttpServerPlugin.autoImport._
import HttpServerKeys._

enablePlugins(SbtHttpServerPlugin)

lazy val testService = taskKey[Unit]("Check if the HttpService is running")

setHttpServices(_ += FileServer.service("resources", (resourceDirectories in Compile).value))

testService := {
	val fileToServe = (resourceDirectory in Compile).value / "test.txt"
	val expectedResponse = io.Source.fromFile(fileToServe.getCanonicalPath).mkString
	import dispatch._, Defaults._
	val svc = url((httpServerAddress in Global).value + "/resources/test.txt")
	val request = Http(svc OK as.String)
	assert(expectedResponse == request())
}