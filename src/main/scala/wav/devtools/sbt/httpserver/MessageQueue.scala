package wav.devtools.sbt.httpserver

import org.http4s.server.HttpService
import org.http4s.websocket.WebsocketBits._

import scalaz.concurrent.Task
import scalaz.{\/-, -\/}
import scalaz.stream._
import scalaz.stream.async.{unboundedQueue, topic}

import internaldsl._

object MessageQueue {

  trait OUT {
    private val q = unboundedQueue[WebSocketFrame]

    private val t = topic[WebSocketFrame]()

    Process.repeat(q.dequeue.to(t.publish)).run.runAsync(_ => ())

    protected val out = t.subscribe

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

  case class O(endpoint: String) extends OUT {
    lazy val service: HttpService = exchange(endpoint, _ => (out, Process.halt))
  }

  case class IO(endpoint: String) extends IN with OUT {
    lazy val service: HttpService = exchange(endpoint, _ => (out, in))
  }

}