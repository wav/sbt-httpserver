package wav.devtools.sbt.httpserver

import org.json4s.JField
import org.json4s.JsonAST.JValue

import collection.mutable
import concurrent.duration._
import concurrent.{Promise, promise, Await}
import scala.util.{Try, Success, Failure}

import org.http4s.Request
import org.http4s.websocket.WebsocketBits.{Text, WebSocketFrame}

import org.http4s.server.HttpService

import scalaz.concurrent.Task

import internaldsl._

import scalaz.stream._
import scalaz.stream.async._

object RequestResponse {
  def ClientLabel(id: String): String = "+clientId:" + id
}

case class RequestResponse(endpoint: String) {
  private var lastClientId = 0
  private val activeClients = mutable.Set[String]()
  private val activeRequests = mutable.Map[String, (String, Promise[JField])]()
  private val pendingRequests = mutable.Set[String]()
  private val in = sink.lift[Task, WebSocketFrame](m => Task {
    m match {
      case Text(Message((id, data: JValue)), _) =>
        activeRequests.get(id).foreach { t =>
          val (clientId, p) = t
          p.success((clientId, data))
        }
      case _ =>
    }
  })

  private val outq = unboundedQueue[(String, Set[String], WebSocketFrame)]

  private val t = topic[(String, Set[String], WebSocketFrame)]()

  Process.repeat(outq.dequeue.to(t.publish)).run.runAsync(_ => ())

  def ask[T](id: String, message: T, labels: Set[String] = Set.empty, atMost: Duration = 100.milliseconds)(implicit m: Manifest[T]): Try[JField] = {
    assert(!activeRequests.contains(id))
    val p = promise[JField]
    activeRequests(id) = (null,p)
    pendingRequests += id
    outq.enqueueOne((id, labels, Text(Message(id, message)))).run
    Try(Await.result(p.future, atMost)).transform(
      s => {
        activeRequests -= id
        Success(s)
      },
      t => {
        activeRequests -= id
        Failure(t)
      })
  }

  private def outIn(r: Request): (Process[Task,WebSocketFrame], Sink[Task, WebSocketFrame]) = {
    val clientId = {lastClientId += 1; lastClientId}.toString
    val subs = r.params.get("labels").map(_.split(",").filter(!_.startsWith("+")).toSet).getOrElse(Set.empty)
    val allSubs = Set(RequestResponse.ClientLabel(clientId)) ++ subs
    val out = t.subscribe.filter {
      case (id, labels, _) =>
        val yes = subs.isEmpty || (labels & subs).nonEmpty
        if (yes) {
          pendingRequests -= id
          val Some((_, p)) = activeRequests.remove(id)
          activeRequests(id) = (clientId.toString,p)
        }
        yes
    }
    (out.map(_._3).onComplete(Process.eval_(Task(activeClients -= clientId))),in)
  }

  lazy val service: HttpService = exchange(endpoint, outIn)
}