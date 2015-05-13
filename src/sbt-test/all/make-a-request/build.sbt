import wav.devtools.sbt.httpserver.SbtHttpServerPlugin
import wav.devtools.sbt.httpserver.SbtHttpServerPlugin.autoImport._

enablePlugins(SbtHttpServerPlugin)

lazy val testService = taskKey[Unit]("Check if the HttpService is running")

testService := {
	val fileToServe = (baseDirectory in ThisBuild).value / "test.txt"
	val expectedResponse = io.Source.fromFile(fileToServe.getCanonicalPath).mkString
	import dispatch._, Defaults._
	val svc = url((HttpServerKeys.httpServerAddress in Global).value + "/test.txt")
	val request = Http(svc OK as.String)
	assert(expectedResponse == request())
}