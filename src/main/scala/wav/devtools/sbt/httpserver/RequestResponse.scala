package wav.devtools.sbt.httpserver

import collection.mutable
import scala.concurrent.duration.Duration
import scala.concurrent.{Promise, promise, Await}
import scala.util.{Try, Success, Failure}

import org.http4s.websocket.WebsocketBits.{Text, WebSocketFrame}
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.json4s.jackson.Serialization.write

import org.http4s.server.HttpService
import org.http4s.util.CaseInsensitiveString

import scalaz.concurrent.Task

import internaldsl._

import scalaz.stream._
import scalaz.stream.async.unboundedQueue

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

case class RequestResponse(endpoint: CaseInsensitiveString) extends MessageQueue.OUT {
  private val activeRequests = mutable.Map[String, Promise[JValue]]()
  private val in = sink.lift[Task, WebSocketFrame](m => Task {
    m match {
      case Text(ResponseData(rd), _) =>
        activeRequests.get(rd.id).foreach(_.success(rd.data))
      case _ =>
    }
  })

  def ask[T](id: String, message: T, atMost: Duration)(implicit m: Manifest[T]): Try[JValue] = {
    assert(!activeRequests.contains(id))
    val p = promise[JValue]
    activeRequests(id) = p
    enqueue(RequestData(id, message).toString)
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

  lazy val service: HttpService = exchange(endpoint, out, in)
}