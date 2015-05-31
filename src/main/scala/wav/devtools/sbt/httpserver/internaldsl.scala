package wav.devtools.sbt.httpserver

import org.http4s._
import org.http4s.dsl._
import org.http4s.server._
import org.http4s.server.websocket._
import org.http4s.websocket.WebsocketBits.WebSocketFrame
import org.json4s.DefaultFormats
import scalaz.concurrent.Task

import scalaz.stream._

private [httpserver] object internaldsl extends Syntax {

  implicit val formats = DefaultFormats

  implicit def headersAsStrings(headers: Headers): Traversable[(String, String)] =
    headers.map(e => (e.name.toString -> e.value.toString))

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

  def exchange(endpoint: String, outIn: Request => (Process[Task,WebSocketFrame], Sink[Task, WebSocketFrame])): HttpService = {
    val endpointPath = Path(endpoint.split("/").toList)
    HttpService {
      case req @ GET -> path if path.equals(endpointPath) =>
        val (out, in) = outIn(req)
        WS(Exchange(out, in))
    }
  }

}
