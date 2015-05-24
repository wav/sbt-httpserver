package wav.devtools.sbt.httpserver

import org.http4s.server.HttpService
import org.http4s.util.CaseInsensitiveString
import org.http4s.websocket.WebsocketBits._

import scalaz.concurrent.Task
import scalaz.{\/-, -\/}
import scalaz.stream._
import scalaz.stream.async.unboundedQueue

import internaldsl._

object MessageQueue {

  trait OUT {
    private val q = unboundedQueue[WebSocketFrame]

    protected val out = q.dequeue

    def enqueue(s: String): Unit = q.enqueueOne(Text(s)).run
  }

  trait IN {
    private val q = unboundedQueue[WebSocketFrame]

    protected val in = q.enqueue

    def take(n: Int): Task[Seq[String]] =
      q.dequeue.collect {
        case Text(m, _) => m
      }.take(n).runLog.attempt.map {
        case \/-(ss) => ss
        case -\/(t) => throw t
      }
  }

  case class O(endpoint: CaseInsensitiveString) extends OUT {
    lazy val service: HttpService = exchange(endpoint, out, Process.halt)
  }

  case class IO(endpoint: CaseInsensitiveString) extends IN with OUT {
    lazy val service: HttpService = exchange(endpoint, out, in)
  }

}