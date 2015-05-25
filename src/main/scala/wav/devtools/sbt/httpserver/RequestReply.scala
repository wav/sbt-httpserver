package wav.devtools.sbt.httpserver

import org.http4s.websocket.WebsocketBits.{Text, WebSocketFrame}
import org.json4s._
import org.json4s.jackson.JsonMethods._
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

object RequestData {
  def unapply(s: String): Option[RequestData] =
    Try {
      implicit val formats = DefaultFormats
      val List(id, data) = parse(s).extract[List[String]]
      RequestData(id, data)
    }.toOption
  def unapply(j: JValue): Option[RequestData] = j match {
    case JArray(List(JString(id), JString(data))) =>
      Some(RequestData(id, data))
    case _ => None
  }
}

case class RequestData(id: String, data: String) {
  override def toString: String =
    compact(render(toJson))
  lazy val toJson: JValue =
    JArray(List(JString(id), JString(data)))
}

case class RequestReply(endpoint: CaseInsensitiveString) {
  import RequestReply._

  private val logger = LoggerFactory.getLogger(classOf[RequestReply])
  private val activeRequests = mutable.Map[String, Promise[String]]()
  private val outq = unboundedQueue[WebSocketFrame]
  private val in = sink.lift[Task, RequestData](m => Task {
    activeRequests.get(m.id).foreach(_.success(m.data))
  }).contramap[WebSocketFrame] { case Text(RequestData(rd),_) => rd }

  def ask(id: String, m: String)(implicit ex: ExecutionContext): Task[String] = {
    assert(!activeRequests.contains(id))
    val p = promise[String]
    activeRequests(id) = p
    outq.enqueueOne(Text(RequestData(id, m).toString)).run
    p.task.onFinish(err => Task(activeRequests -= id))
  }

  lazy val service: HttpService = exchange(endpoint, outq.dequeue, in)
}