val releaseRepo = Resolver.url("wav", url("https://dl.bintray.com/wav/maven"))(Resolver.ivyStylePatterns)

val ivyLocal = Resolver.file("local", file(Path.userHome.absolutePath + "/.ivy2/local"))(Resolver.ivyStylePatterns)

val V = sys.props.get("project.version").getOrElse("NOVERSION")

val isReleaseTest = sys.props.get("isReleaseTest").getOrElse("no") == "yes"

resolvers ++= {
	if (isReleaseTest) Seq(
		releaseRepo,
	    "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases") // scalaz-stream
	else Seq.empty
}

externalResolvers := (if(isReleaseTest) Seq.empty else Seq(ivyLocal))

addSbtPlugin("wav.devtools" % "sbt-httpserver" % V)