package app

import scalajs.js, js.Dynamic.{global => g}, js.JSConverters._
import wav.devtools.sbt.httpserver.buildservice.BuildService

object App extends js.JSApp {

  implicit def `f2->undefjs`[P1,P2,R](f: (P1,P2) => R) =
    Some(f: js.Function2[P1, P2, R]).orUndefined

  def onBuildEvent(project: String, event: String): Unit =
    g.console.debug("scalajs.onBuildEvent", project, event)

  def main(): Unit = {
    BuildService.configure { c =>
      c.onBuildEvent = onBuildEvent _
    }
    BuildService().foreach(_.start())
  }

}