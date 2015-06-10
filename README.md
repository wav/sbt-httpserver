## SBT httpserver

Host an http4s service in SBT

For sbt 0.13.8.

## Getting started

Add the following dependency to your `plugins.sbt`

```scala
resolvers ++= Seq(
    Resolver.url("wav", url("https://dl.bintray.com/wav/maven"))(Resolver.ivyStylePatterns),
    "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases") // scalaz-stream

addSbtPlugin("wav.devtools" % "sbt-httpserver" % "0.3.1")
```

Add the following to your `build.sbt` to start using it.

```scala
import wav.devtools.sbt.httpserver.SbtHttpServerPlugin
import SbtHttpServerPlugin.autoImport._

enablePlugins(SbtHttpServerPlugin)
```

An http service will be started on `localhost:8083`. The preferred port is defined in the setting `HttpServerKeys.httpServerPort in Global`. If you would like a random port assigned use:
  
  `httpServerPort in Global := 0` or set the system property `sbthttpserver.port`

See the settings here: [Plugin.scala](src/main/scala/wav/devtools/sbt/httpserver/Plugin.scala)

## Tips

If you don't want the http service to run when sbt starts (like when you're using an IDE), add the property `sbthttpserver.dontStart` to your `SBT_OPTS`.

## Included http services

There are some http services included in this build.

- A file server where you can specify directories to be served  
- A build service that allows build events to be sent to connected websocket clients and make requests and receive replies.

See the [examples](examples).