import sbt.Keys._

organization in ThisBuild := "wav.devtools"

name := "sbt-httpserver"

version in ThisBuild := "0.3.1"

description := "Host an http4s service in SBT"

licenses in ThisBuild +=("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html"))

// ++

sbtPlugin := true

scalaVersion := "2.10.5"

resolvers += "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases" // scalaz-stream

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "2.2.4" % "test",
  "io.backchat.hookup" %% "hookup" % "0.4.0" % "test",
  "org.json4s" %% "json4s-jackson" % "3.2.10",
  "ch.qos.logback" % "logback-classic" % "1.1.2", //implements slf4j
  "com.google.guava" % "guava" % "18.0",
  "org.http4s" %% "http4s-dsl" % "0.7.0",
  "org.http4s" %% "http4s-blazeserver" % "0.7.0",
  "net.databinder.dispatch" %% "dispatch-core" % "0.11.2")

scalacOptions in ThisBuild ++= Seq(
  "-deprecation", "-unchecked", "-feature",
  "-language:implicitConversions",
  "-language:higherKinds")

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

publishMavenStyle in ThisBuild := false

bintrayReleaseOnPublish in ThisBuild := false

publishArtifact in Test in ThisBuild := false

publishArtifact in(Compile, packageDoc) in ThisBuild := false

bintrayRepository := "maven"

updateOptions := updateOptions.value.withCachedResolution(true)

// logLevel in Test := Level.Debug

fork in Test := true // Hookup client jvm gets shutdown.

resourceGenerators in Compile <+= Def.task {
  val inDir = (sourceDirectory in Compile).value / "javascript"
  val outDir = (resourceManaged in Compile).value / "public"
  val target = outDir / "index.js"
  val sources = (inDir ** "*.js").filter(_.isFile).get
  val mappings = sources pair rebase(Seq(inDir), outDir)
  // `npm install babel -g`
  // `npm install babel-es6-polyfill --save`, then copy to resources
  s"""babel $inDir --out-dir $outDir""" !;
  mappings.map(_._2)
}

lazy val `sbt-httpserver-buildservice` = project.in(file("buildservice-sjs"))
  .enablePlugins(ScalaJSPlugin)
  .settings(scalaVersion := "2.11.6")