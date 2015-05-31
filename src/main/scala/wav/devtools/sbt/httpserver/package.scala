package wav.devtools.sbt

import org.http4s.server._
import sbt._

package object httpserver {

  type ApplyServiceSettings = SettingKey[Seq[HttpService]] => Seq[Setting[_]]

}
