organization := "wav.devtools"

name := "sbt-httpserver"

description := "Host an http4s service in SBT"

licenses +=("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html"))

// ++

sbtPlugin := true

scalaVersion in ThisBuild := "2.10.5"

crossScalaVersions in ThisBuild := Seq()

resolvers += "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases" // scalaz-stream

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "2.2.4" % "test",
  "io.backchat.hookup" %% "hookup" % "0.4.0" % "test",
  "org.json4s" %% "json4s-jackson" % "3.2.11",
  "ch.qos.logback" % "logback-classic" % "1.1.2", //implements slf4j
  "com.google.guava" % "guava" % "18.0",
  "org.http4s" %% "http4s-dsl" % "0.7.0",
  "org.http4s" %% "http4s-blazeserver" % "0.7.0",
  "net.databinder.dispatch" %% "dispatch-core" % "0.11.2")

scalacOptions in ThisBuild ++= Seq(
  "-deprecation", "-unchecked", "-feature",
  "-language:implicitConversions",
  "-language:higherKinds",
  "-language:existentials")

javacOptions in ThisBuild ++= Seq(
  "-source", "1.8",
  "-target", "1.8",
  "-encoding", "UTF8",
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

publishArtifact in(Compile, packageSrc) := false

publishArtifact in(Compile, packageDoc) := false

bintrayRepository := "maven"

updateOptions := updateOptions.value.withCachedResolution(true)

// logLevel in Test := Level.Debug

fork in Test := true // Hookup client jvm gets shutdown.