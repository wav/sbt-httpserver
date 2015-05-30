## SBT httpserver

Host an http4s service in SBT

For sbt 0.13.8.

## Getting started

Add the following dependency to your `plugins.sbt`

```scala
resolvers ++= Seq(
    Resolver.url("wav", url("https://dl.bintray.com/wav/maven"))(Resolver.ivyStylePatterns),
    "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases") // scalaz-stream

addSbtPlugin("wav.devtools" % "sbt-httpserver" % "0.2.0")
```

Add the following to your `build.sbt` to start using it.

```scala
import wav.devtools.sbt.httpserver.SbtHttpServerPlugin
import wav.devtools.sbt.httpserver.SbtHttpServerPlugin.autoImport._

enablePlugins(SbtHttpServerPlugin)
```

See a full example here: [build-services](examples/build-services/build.sbt)

## Settings

The `httpServerService in Global` is where you define an Http4s `HttpService`. This will be started when sbt loads and hosted on the port defined in the setting `httpServerPort in Global` (default 8083).

The default `httpServerService in Global` is a file server that serves your build folder.

For more settings, see [SbtHttpServer.scala](src/main/scala/wav/devtools/sbt/httpserver/SbtHttpServer.scala)