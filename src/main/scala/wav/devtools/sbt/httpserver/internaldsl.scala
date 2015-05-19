package wav.devtools.sbt.httpserver

import org.http4s._
import org.http4s.server._
import org.http4s.util.CaseInsensitiveString

object internaldsl {

  implicit def headersAsStrings(headers: Headers): Traversable[(String, String)] =
    headers.map(e => (e.name.toString -> e.value.toString))

  val si = CaseInsensitiveString.apply _

  def makeEntityBody(body: String): EntityBody = {
    import scodec.bits.ByteVector
    import scalaz.stream.Process.emit
    import java.nio.charset.StandardCharsets
    emit(ByteVector.view(body.getBytes(StandardCharsets.UTF_8)))
  }

  def readEntityBody(body: EntityBody): String =
    new String(body.runLog.run.reduce(_ ++ _).toArray)

  def uri(uri: String): Option[Uri] =
    Uri.fromString(uri).toOption

  case class req(method: Method, route: String, headers: Map[HeaderKey, String] = Map.empty, body: Option[String] = None)

  def endpoint(service: HttpService)(r: req): Option[Response] =
    service(Request(
      r.method,
      uri(s"http://localhost/${r.route}").get,
      body = r.body.map(makeEntityBody).getOrElse(null))
      .putHeaders(r.headers.toSeq.map(h => Header(h._1.name.toString, h._2)): _*))
      .run

}
