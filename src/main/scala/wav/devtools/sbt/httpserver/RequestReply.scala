package wav.devtools.sbt.httpserver

import org.http4s.server.HttpService
import org.http4s.util.CaseInsensitiveString

import scalaz.concurrent.Task

import internaldsl._

case class RequestReply(endpoint: CaseInsensitiveString) {
  def ask(id: String, m: String): Task[String] = ???
  lazy val service: HttpService = exchange(endpoint, ???, ???)
}