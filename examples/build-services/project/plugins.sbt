resolvers ++= Seq(
  Resolver.url("wav", url("https://dl.bintray.com/wav/maven"))(Resolver.ivyStylePatterns),
  "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases") // scalaz-stream

addSbtPlugin("wav.devtools" % "sbt-httpserver" % "0.3.0")
