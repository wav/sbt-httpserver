package wav.devtools.sbt.httpserver

import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.util.concurrent.Executors

import org.http4s.blaze.channel.SocketConnection
import org.http4s.blaze.channel.nio1.NIO1SocketServerChannelFactory
import org.http4s.blaze.pipeline.LeafBuilder
import org.http4s.server.blaze.{Http1ServerStage, WebSocketSupport, BlazeBuilder}
import org.http4s.server.middleware.URITranslation
import org.scalatest.FunSuite
import sbt._
import org.http4s.dsl._
import io.backchat.hookup._
import scala.concurrent.{Await, ExecutionContext, promise}
import concurrent.duration._
import scala.util.Try

import internaldsl._

class ServiceSuite extends FunSuite {

  implicit val ES = Executors.newScheduledThreadPool(1)
  implicit val EC = ExecutionContext.fromExecutorService(ES)

  val serverName = "localhost"
  test("A file server returns a file") {
    val content = "this is a test file"
    val wd = file(".")
    val resource = wd / "test.txt"
    IO.write(resource, content)
    val service = endpoint(FileServer.service(si("files"), Seq(wd))) _
    val result = service(req(GET, "files/test.txt")).map(r => readEntityBody(r.body))
    assert(content == result.get)
  }

  test("A client that subscribes to an EventStream should receive a `Connected` message") {
    val port = 8084
    val mount = "eventstream"
    val endpoint = s"ws://localhost:$port/$mount"
    val (send, service) = EventStream.service(si(mount))
    val server = new SimpleWebSocketServer(port, Seq(service))
    server.start
    val p = promise[String]
    val f = p.future
    var client = new SingleUseClient(new URI(endpoint),{
      case TextMessage(message) =>
        p.success(message)
      case JsonMessage(message) =>
        p.success(message.toString)
    })
    try {
      Await.result(client.connected, 10.seconds)
      send("Connected")
      val r = if (f.isCompleted) f.value.get else Try(Await.result(f, 0.5.seconds))
      assert("Connected" == r.get)
    } finally {
      try { client.close }
      // try { server.stop } // not required, it has a scalaz Catchable context
    }
  }

  test("A server can RPC a client and receive a response") {
    // start

    // request/response (server->client->server)
    // val result = Await(ask("do something"), 5.seconds)

    // kill
    ???
  }

}