package wav.devtools.sbt.httpserver

import org.http4s.websocket.WebsocketBits.{Text, WebSocketFrame}
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.json4s.jackson.Serialization.write
import org.slf4j.LoggerFactory

import collection.mutable
import org.http4s.server.HttpService
import org.http4s.util.CaseInsensitiveString

import scala.util.Try
import scalaz.concurrent.Task
import scala.concurrent.{ExecutionContext, Promise, promise}

import internaldsl._

import scalaz.stream._
import scalaz.stream.async.unboundedQueue

object ResponseData {
  def unapply(s: String): Option[ResponseData] =
    Try {
      val (id, data) = parse(s).extract[(String,String)]
      ResponseData(id, data)
    }.toOption
}

case class ResponseData(id: String, data: String) {
  override def toString: String = write((id, data))
}

case class RequestData[T](id: String, data: T)(implicit m: Manifest[T]) {
  override def toString: String = write((id, data))
}

object RequestData {
  def unapply(v: JValue): Option[(String, String)] =
    Try(v.extract[(String,String)]).toOption
}

case class RequestResponse(endpoint: CaseInsensitiveString) {
  import RequestResponse._

  private val activeRequests = mutable.Map[String, Promise[String]]()
  private val outq = unboundedQueue[WebSocketFrame]
  private val in = sink.lift[Task, ResponseData](m => Task {
    activeRequests.get(m.id).foreach(_.success(m.data))
  }).contramap[WebSocketFrame] { case Text(ResponseData(rd),_) => rd }

  def ask[T](id: String, message: T)(implicit ex: ExecutionContext, m: Manifest[T]): Task[String] = {
    assert(!activeRequests.contains(id))
    val p = promise[String]
    activeRequests(id) = p
    outq.enqueueOne(Text(RequestData(id, message).toString)).run
    p.task.onFinish(err => Task(activeRequests -= id))
  }

  lazy val service: HttpService = exchange(endpoint, outq.dequeue, in)
}