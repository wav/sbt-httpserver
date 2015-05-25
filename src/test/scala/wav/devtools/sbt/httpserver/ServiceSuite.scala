package wav.devtools.sbt.httpserver

import java.util.concurrent.Executors
import org.json4s.JsonAST.JValue
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.scalatest.FunSuite
import org.slf4j.LoggerFactory
import sbt._
import org.http4s.dsl._
import io.backchat.hookup._
import scala.concurrent.{Await, ExecutionContext, promise}
import concurrent.duration._
import scala.util.Try

import internaldsl._

class ServiceSuite extends FunSuite {

  val logger = LoggerFactory.getLogger(classOf[ServiceSuite])

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

  test("A client that listens to server events (over web sockets) should receive a `Connected` message") {
    val port = 8084
    val mount = "MessageQueue.O"
    val endpoint = s"ws://localhost:$port/$mount"
    val queue = MessageQueue.O(si(mount))
    val server = new SimpleWebSocketServer(port, Seq(queue.service))
    server.start
    val p = promise[String]
    val f = p.future
    val client = new TestClient(new URI(endpoint),_ => {
      case TextMessage(message) =>
        p.success(message)
    })
    try {
      Await.result(client.connected, 10.seconds)
      queue.enqueue("Connected")
      val r = if (f.isCompleted) f.value.get else Try(Await.result(f, 0.5.seconds))
      assert("Connected" == r.get)
    } finally {
      try { client.close }
    }
  }

  test("A server can send and receive messages (over web sockets) from connected clients") {
    val port = 8085
    val mount = "MessageQueue.IO"
    val endpoint = s"ws://localhost:$port/$mount"
    val queue = MessageQueue.IO(si(mount))
    val server = new SimpleWebSocketServer(port, Seq(queue.service))
    server.start
    val p = promise[String]
    val f = p.future
    val client = new TestClient(new URI(endpoint),sender => {
      case TextMessage(message) =>
        p.success(message)
        sender ! "Hello Server"
    })
    try {
      Await.result(client.connected, 5.seconds)
      queue.enqueue("Hello Client")
      val sent = if (f.isCompleted) f.value.get else Try(Await.result(f, 0.5.seconds))
      assert("Hello Client" == sent.get)
      val result = queue.take(1).runFor(5.seconds)
      assert(result == Seq("Hello Server"))
    } finally {
      try { client.close }
    }
  }

  test("A server can make requests to connected clients and receive replies (over web sockets)") {
    val port = 8086
    val mount = "RequestReply"
    val endpoint = s"ws://localhost:$port/$mount"
    val exchange = RequestResponse(si(mount))
    val server = new SimpleWebSocketServer(port, Seq(exchange.service))
    server.start
    val client = new TestClient(new URI(endpoint),sender => {
      case JsonMessage(RequestData(id, n)) =>
        sender ! ResponseData(id, (n.toInt+1).toString).toString
    })
    try {
      val sum = for {
        a <- exchange.ask("a", 1)
        b <- exchange.ask("b", 2)
      } yield (a.toInt + b.toInt)
      val result = sum.runFor(5.seconds)
      assert(result == 5)
    } finally {
      try { client.close }
    }
  }

}