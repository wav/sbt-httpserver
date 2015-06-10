enablePlugins(ScalaJSPlugin)

scalaVersion := "2.11.6"

libraryDependencies += "wav.devtools" %%% "sbt-httpserver-buildservice" % "0.3.1"

fastOptJS in Compile <<= (classDirectory in Compile, fastOptJS in Compile) map { (cd, js) =>
  val n = js.data.getName
  val n_map = n + ".map"
  IO.copyFile(js.data, cd / n)
  IO.copyFile(js.data.getParentFile / n_map, cd / n_map)
  js
}

scalacOptions in ThisBuild ++= Seq("-language:implicitConversions")

emitBuildEvent((fastOptJS in Compile), "compiled")