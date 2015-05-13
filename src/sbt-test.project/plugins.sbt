val ivyLocal = Resolver.file("local", file(Path.userHome.absolutePath + "/.ivy2/local"))(Resolver.ivyStylePatterns)

externalResolvers := Seq(ivyLocal)

addSbtPlugin("wav.devtools" % "sbt-httpserver" % sys.props("project.version"))