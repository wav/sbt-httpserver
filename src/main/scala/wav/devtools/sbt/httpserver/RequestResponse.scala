package wav.devtools.sbt.httpserver

import collection.mutable
import concurrent.duration._
import concurrent.{Promise, promise, Await}
import scala.util.{Try, Success, Failure}

import org.http4s.Request
import org.http4s.websocket.WebsocketBits.{Text, WebSocketFrame}
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.json4s.jackson.Serialization.write

import org.http4s.server.HttpService

import scalaz.concurrent.Task

import internaldsl._

import scalaz.stream._
import scalaz.stream.async._

object ResponseData {
  def unapply(s: String): Option[ResponseData] =
    Try {
      val (id, data) = parse(s).extract[(String, JValue)]
      ResponseData(id, data)
    }.toOption
}

case class ResponseData(id: String, data: JValue) {
  override def toString: String = write((id, data))
}

case class RequestData[T](id: String, data: T)(implicit m: Manifest[T]) {
  override def toString: String = write((id, data))
}

object RequestData {
  def unapply(v: JValue): Option[(String, String)] =
    Try(v.extract[(String, String)]).toOption
}

object RequestResponse {
  def ClientId(id: Int): String = "+clientId:" + id
}

case class RequestResponse(endpoint: String) {
  private var lastClientId = 0
  private val activeClients = mutable.Set[Int]()
  private val activeRequests = mutable.Map[String, (Int, Promise[(Int, JValue)])]()
  private val pendingRequests = mutable.Set[String]()
  private val in = sink.lift[Task, WebSocketFrame](m => Task {
    m match {
      case Text(ResponseData(rd), _) =>
        activeRequests.get(rd.id).foreach { t =>
          val (clientId, p) = t
          p.success((clientId, rd.data))
        }
      case _ =>
    }
  })

  private val outq = unboundedQueue[(String, Set[String], WebSocketFrame)]

  private val t = topic[(String, Set[String], WebSocketFrame)]()

  Process.repeat(outq.dequeue.to(t.publish)).run.runAsync(_ => ())

  def ask[T](id: String, message: T, labels: Set[String] = Set.empty, atMost: Duration = 100.milliseconds)(implicit m: Manifest[T]): Try[(Int,JValue)] = {
    assert(!activeRequests.contains(id))
    val p = promise[(Int, JValue)]
    activeRequests(id) = (0,p)
    pendingRequests += id
    outq.enqueueOne((id, labels, Text(RequestData(id, message).toString))).run
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
    val clientId = {lastClientId += 1; lastClientId}
    val subs = r.params.get("labels").map(_.split(",").filter(!_.startsWith("+")).toSet).getOrElse(Set.empty)
    val allSubs = Set(RequestResponse.ClientId(clientId)) ++ subs
    val out = t.subscribe.filter {
      case (id, labels, _) =>
        val yes = subs.isEmpty || (labels & subs).nonEmpty
        if (yes) {
          pendingRequests -= id
          val Some((_, p)) = activeRequests.remove(id)
          activeRequests(id) = (clientId,p)
        }
        yes
    }
    (out.map(_._3).onComplete(Process.eval_(Task(activeClients -= clientId))),in)
  }

  lazy val service: HttpService = exchange(endpoint, outIn)
}