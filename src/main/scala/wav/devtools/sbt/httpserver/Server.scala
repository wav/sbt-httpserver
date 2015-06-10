package wav.devtools.sbt.httpserver

import java.net.InetSocketAddress
import java.nio.ByteBuffer

import org.http4s.blaze.channel.SocketConnection
import org.http4s.blaze.channel.nio1.NIO1SocketServerChannelFactory
import org.http4s.blaze.pipeline.LeafBuilder
import org.http4s.server.HttpService
import org.http4s.server.blaze.{WebSocketSupport, Http1ServerStage}
import org.http4s.server.middleware.URITranslation

abstract class Server {
  def start: Unit
  def stop: Unit
}

class SimpleWebSocketServer(port: Int, services: Seq[HttpService]) extends Server {
  private def pipebuilder(conn: SocketConnection): LeafBuilder[ByteBuffer] =
    new Http1ServerStage(URITranslation.translateRoot("/")(services.reduce(_ orElse _)), Some(conn)) with WebSocketSupport
  private val server = NIO1SocketServerChannelFactory(pipebuilder, 12, 8*1024)
    .bind(new InetSocketAddress(port))
  lazy val start: Unit = server.runAsync
  lazy val stop: Unit = server.close
}