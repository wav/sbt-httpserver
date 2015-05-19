package wav.devtools.sbt.httpserver

import java.net.URI
import io.backchat.hookup._
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, promise}

class SingleUseClient(val endpoint: URI, val handler: HookupClient.Receive)(implicit ec: ExecutionContext)
  extends DefaultHookupClient(HookupClientConfig(endpoint)) {

  val logger = LoggerFactory.getLogger(classOf[SingleUseClient])

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

  def receive() = connectionHandler orElse handler orElse errorHandler

  connect() onSuccess {
    case Success =>
    case x =>
      if (!pconnected.isCompleted)
        pconnected.failure(new IllegalArgumentException("unknown connection message: " + x.toString))
  }



}