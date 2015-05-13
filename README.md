## SBT httpserver

Hosts an http4s `HttpService` sbt, suitable for client-side development if the filesystem doesn't suffice.

For sbt 0.13.8.

## Getting started

1. Clone this repository and run `sbt publish-local`

2. Add the following dependency to your `plugins.sbt`

```scala
addSbtPlugin("wav.devtools" % "sbt-httpserver" % version)
```

3. Add the following to your `build.sbt` to start using it.

```scala
import wav.devtools.sbt.httpserver.SbtHttpServerPlugin
import wav.devtools.sbt.httpserver.SbtHttpServerPlugin.autoImport._

enablePlugins(SbtHttpServerPlugin)
```

## Settings

The `httpServerService in Global` is where you define an Http4s `HttpService`. This will be started when sbt loads and hosted on the port defined in the setting `httpServerPort in Global` (default 8083).

The default `httpServerService in Global` is a file server that serves your build folder.

For more settings, see [SbtHttpServer.scala](src/main/scala/wav/devtools/sbt/httpserver/SbtHttpServer.scala)