package wav.devtools.sbt.httpserver.buildservice

import scalajs.js, js.Dynamic.{global => g}
import js.annotation.JSName

trait BuildServiceConfig extends js.Object {

  import BuildService._

  var onServiceEvent: js.UndefOr[js.Function3[String, String, js.UndefOr[js.Any], Unit]] = js.native

  var onBuildEvent: js.UndefOr[js.Function2[String, String, Unit]] = js.native

  var onBuildCommand: js.UndefOr[js.Function2[Send, js.UndefOr[js.Any], Unit]] = js.native

  var buildEventService: js.UndefOr[String] = js.native

  var commandService: js.UndefOr[String] = js.native

}

object BuildService {

  type Send = js.Function1[js.UndefOr[js.Any], Unit]

  private var config: Option[BuildServiceConfig] =
    g.BuildServiceConfig.asInstanceOf[js.UndefOr[BuildServiceConfig]].toOption

  private var buildService: Option[BuildService] = None

  def apply(): Option[BuildService] =
    buildService

  def configure(configure: BuildServiceConfig => Unit = identity): Unit = {
    buildService.foreach(_.stop)
    buildService = config.map { c =>
      configure(c)
      new BuildService(c)
    }
  }

}

@JSName("BuildService")
class BuildService private[buildservice] (config: BuildServiceConfig) extends js.Object {
  def start(): Unit = js.native
  def stop(): Unit = js.native
}