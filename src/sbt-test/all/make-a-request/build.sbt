import wav.devtools.sbt.httpserver.{SbtHttpServerPlugin,FileServer}
import SbtHttpServerPlugin.autoImport._
import HttpServerKeys._

enablePlugins(SbtHttpServerPlugin)

addHttpServices(_ += FileServer.service("resources", (resourceDirectories in Compile).value))

addHttpServices(httpServerBuildServices)

lazy val testService = taskKey[Unit]("Check if the service is running")

testService := {
	val fileToServe = (resourceDirectory in Compile).value / "test.txt"
	val expectedResponse = io.Source.fromFile(fileToServe.getCanonicalPath).mkString
	import dispatch._, Defaults._
	val svc = url(address.value + "/resources/test.txt")
	val request = Http(svc OK as.String)
	assert(expectedResponse == request())
}

lazy val testBuildService = taskKey[Unit]("Check if the build service is running")

testBuildService := {
  import dispatch._, Defaults._
  val svc = url(address.value + "/buildService/app/index.js")
  val request = Http(svc OK as.String)
  assert(request().contains("buildService"))
}