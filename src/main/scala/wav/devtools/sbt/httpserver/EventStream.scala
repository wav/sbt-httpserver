package wav.devtools.sbt.httpserver

import java.util.concurrent.Executors

import org.http4s.{Header, Status}
import org.http4s.dsl._
import org.http4s.server.HttpService
import org.http4s.server.websocket.WS
import org.http4s.util.CaseInsensitiveString
import org.http4s.websocket.WebsocketBits._
import org.http4s.websocket.{WebsocketBits, WebsocketHandshake}

import scalaz.stream._
import scalaz.stream.async.unboundedQueue

/**
 * For sending messages to clients.
 */
object EventStream {

  private val si = CaseInsensitiveString.apply _

  def service(endpoint: CaseInsensitiveString): (String => Unit, HttpService) = {
    implicit val scheduledEC = Executors.newScheduledThreadPool(1)
    val q = unboundedQueue[WebSocketFrame]
    val service = HttpService {
      case req @ GET -> Root / mount if endpoint.equals(si(mount)) =>
        WS(Exchange(q.dequeue, Process.halt)) 
    }
    (s => q.enqueueOne(Text(s)).run, service)
  }

}