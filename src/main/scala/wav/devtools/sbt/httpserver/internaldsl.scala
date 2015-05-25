package wav.devtools.sbt.httpserver

import org.http4s._
import org.http4s.dsl._
import org.http4s.server._
import org.http4s.server.websocket._
import org.http4s.util.CaseInsensitiveString
import org.http4s.websocket.WebsocketBits.WebSocketFrame

import scala.util.{Success,Failure}
import scala.concurrent.{ExecutionContext, Promise}
import scalaz.concurrent.Task
import scalaz.\/.{right,left}

import scalaz.stream._

private [httpserver] object internaldsl {

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

  def exchange(endpoint: CaseInsensitiveString, in: Process[Task,WebSocketFrame], out: Sink[Task, WebSocketFrame]): HttpService =
    HttpService {
      case req@GET -> Root / mount if endpoint.equals(si(mount)) => WS(Exchange(in,out))
    }

  implicit class RichPromise[T](p: Promise[T])(implicit ec: ExecutionContext) {
    def task: Task[T] =
      Task.async { f =>
        p.future.onComplete {
          case Success(a) => f(right(a))
          case Failure(t) => f(left(t))
        }
      }
  }

}
