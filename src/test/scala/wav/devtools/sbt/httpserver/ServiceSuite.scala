package wav.devtools.sbt.httpserver

import java.util.concurrent.Executors
import org.json4s.{JField, JInt}
import org.scalatest.FunSuite
import org.slf4j.LoggerFactory
import sbt._
import org.http4s.dsl._
import io.backchat.hookup._
import scala.concurrent.{Await, ExecutionContext, promise}
import concurrent.duration._
import scala.util.{Try, Success}

import internaldsl._

class ServiceSuite extends FunSuite {

  private val logger = LoggerFactory.getLogger(classOf[ServiceSuite])
  implicit val ES = Executors.newScheduledThreadPool(1)
  implicit val EC = ExecutionContext.fromExecutorService(ES)

  test("A file server returns a file") {
    val testDir = file("target/test.dir")

    val d1 = testDir / "dir1"
    if (!d1.exists) d1.mkdirs
    val fa = d1 / "a.txt"
    IO.write(fa, "a")

    val d2 = testDir / "dir2"
    if (!d2.exists) d2.mkdirs
    val fb = d2 / "b.txt"
    IO.write(fb, "b")

    val service = endpoint(FileServer.service("files", Seq(d1, d2))) _

    val result1 = service(req(GET, "files/a.txt")).map(r => readEntityBody(r.body))
    assert("a" == result1.get)

    val result2 = service(req(GET, "files/b.txt")).map(r => readEntityBody(r.body))
    assert("b" == result2.get)
  }

  test("A client that listens to server events (over web sockets) should receive a `Connected` message") {
    val port = 8084
    val mount = "MessageQueue.O"
    val endpoint = s"ws://localhost:$port/$mount"
    val queue = MessageQueue.O(mount)
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
    val queue = MessageQueue.IO(mount)
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
      Await.result(client.connected, 1.seconds)
      queue.enqueue("Hello Client")
      val sent = if (f.isCompleted) f.value.get else Try(Await.result(f, 1.seconds))
      assert("Hello Client" == sent.get)
      val result = queue.take(1).runFor(1.seconds)
      assert(result == Seq("Hello Server"))
    } finally {
      try { client.close }
    }
  }

  test("A server can make requests to connected clients and receive replies (over web sockets)") {
    val port = 8086
    val mount = "RequestReply"
    val endpoint = s"ws://localhost:$port/$mount?labels=test"
    val exchange = RequestResponse(mount)
    val server = new SimpleWebSocketServer(port, Seq(exchange.service))
    server.start
    val client = new TestClient(new URI(endpoint),sender => {
      case JsonMessage(Message((id, JInt(n)))) =>
        sender ! Message(id, n+1)
    })
    try {
      val Success(sum) = for {
        (_, JInt(a)) <- exchange.ask("a", 1, Set("test"))
        (_, JInt(b)) <- exchange.ask("b", 2, Set("test"))
      } yield a + b
      assert(sum == 5)
    } finally {
      try { client.close }
    }
  }

}