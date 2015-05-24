package wav.devtools.sbt.httpserver

import java.net.URI
import io.backchat.hookup._
import org.json4s.JValue
import org.slf4j.LoggerFactory

import concurrent.{ExecutionContext, promise, Future}



object SingleUseClient {

  class Sender(
    string: String => Unit,
    json: JValue => Unit) {
    def ! (m: String): Unit = string(m)
    def ! (m: JValue): Unit = json(m)
  }

}

class SingleUseClient(
  val endpoint: URI,
  val handler: SingleUseClient.Sender => HookupClient.Receive)(implicit ec: ExecutionContext)
  extends DefaultHookupClient(HookupClientConfig(endpoint)) {

  private val logger = LoggerFactory.getLogger(classOf[SingleUseClient])

  private val pconnected = promise[Unit]

  val connected = pconnected.future

  private lazy val connectionHandler: HookupClient.Receive = {
    case Connected =>
      pconnected.success()
    case _: Disconnected =>
      if (!pconnected.isCompleted)
        pconnected.failure(new IllegalArgumentException("Client didn't receive a message"))
  }

  private lazy val errorHandler: HookupClient.Receive = {
    case Error(x) =>
      if (!pconnected.isCompleted)
        pconnected.failure(new IllegalArgumentException("bad message: " + x.toString))
    case x =>
      logger.debug("Discarded message: " + x.getClass)
  }

  private lazy val sender = new SingleUseClient.Sender(s => send(s), j => send(j))

  def receive() = connectionHandler orElse handler(sender) orElse errorHandler

  connect() onSuccess {
    case Success =>
    case x =>
      if (!pconnected.isCompleted)
        pconnected.failure(new IllegalArgumentException("unknown connection message: " + x.toString))
  }

}