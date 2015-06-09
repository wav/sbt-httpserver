## SBT httpserver

Host an http4s service in SBT

For sbt 0.13.8.

## Getting started

Add the following dependency to your `plugins.sbt`

```scala
resolvers ++= Seq(
    Resolver.url("wav", url("https://dl.bintray.com/wav/maven"))(Resolver.ivyStylePatterns),
    "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases") // scalaz-stream

addSbtPlugin("wav.devtools" % "sbt-httpserver" % "0.3.0")
```

Add the following to your `build.sbt` to start using it.

```scala
import wav.devtools.sbt.httpserver.SbtHttpServerPlugin
import SbtHttpServerPlugin.autoImport._

enablePlugins(SbtHttpServerPlugin)
```

An http service will be started on `localhost:8083`. The port is defined in the setting `HttpServerKeys.httpServerPort in Global`. 

See a full example here: [build-services](examples/build-services/build.sbt)

See settings here: [Plugin.scala](src/main/scala/wav/devtools/sbt/httpserver/Plugin.scala)