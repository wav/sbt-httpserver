organization := "wav.devtools"

name := "sbt-httpserver"

description := "Host an http4s service in SBT"

licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html"))

// ++

sbtPlugin := true

scalaVersion in ThisBuild := "2.10.5"

crossScalaVersions in ThisBuild := Seq()

resolvers += "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases" // scalaz-stream

val http4sv = "0.7.0"

libraryDependencies ++= Seq(
	"org.http4s" %% "http4s-dsl"         % http4sv,
	"org.http4s" %% "http4s-blazeserver" % http4sv,
	"net.databinder.dispatch" %% "dispatch-core" % "0.11.2")

scalacOptions in ThisBuild ++= Seq(
	"-deprecation", "-unchecked", "-feature", 
	"-language:implicitConversions",
	"-language:higherKinds", 
	"-language:existentials")

javacOptions in ThisBuild ++= Seq(
	"-source",    "1.8",
	"-target",    "1.8",
	"-encoding",  "UTF8",
	"-Xlint:deprecation",
	"-Xlint:unchecked")

scriptedSettings

scriptedLaunchOpts ++= Seq(
	"-Dproject.version=" + version.value,
	"-Dscripted.testport=8083")

fork in scripted := true // this may be required for log output

publishMavenStyle := false

bintrayReleaseOnPublish in ThisBuild := false

publishArtifact in Test := false

publishArtifact in (Compile, packageSrc) := false

publishArtifact in (Compile, packageDoc) := false

bintrayRepository := "maven"