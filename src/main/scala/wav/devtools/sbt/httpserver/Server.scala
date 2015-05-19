package wav.devtools.sbt.httpserver

import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.util.concurrent.RejectedExecutionException

import org.http4s.blaze.channel.{ServerChannel, SocketConnection}
import org.http4s.blaze.channel.nio1.NIO1SocketServerChannelFactory
import org.http4s.blaze.pipeline.LeafBuilder
import org.http4s.server.HttpService
import org.http4s.server.blaze.{BlazeBuilder, WebSocketSupport, Http1ServerStage}
import org.http4s.server.middleware.URITranslation

abstract class Server {
  def start: Unit
  def stop: Unit
}

class SimpleServer(port: Int, services: Seq[HttpService]) extends Server {
  private lazy val server = BlazeBuilder.bindHttp(port)
    .mountService(services.reduce(_ orElse _), "/")
    .run
  lazy val start: Unit = server
  lazy val stop: Unit = server.shutdownNow
}

class SimpleWebSocketServer(port: Int, services: Seq[HttpService]) extends Server {
  private def pipebuilder(conn: SocketConnection): LeafBuilder[ByteBuffer] =
    new Http1ServerStage(URITranslation.translateRoot("/")(services.reduce(_ orElse _)), Some(conn)) with WebSocketSupport
  private val server = NIO1SocketServerChannelFactory(pipebuilder, 12, 8*1024)
    .bind(new InetSocketAddress(port))
  lazy val start: Unit = server.runAsync
  lazy val stop: Unit = try {
    server.close
  } catch {
    case ex :RejectedExecutionException if ex.getMessage == "This SelectorLoop is closed." =>
  }
}
