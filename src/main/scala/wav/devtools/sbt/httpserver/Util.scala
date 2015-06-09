package wav.devtools.sbt.httpserver

import java.lang.management.ManagementFactory

import scala.concurrent.{ExecutionContext, Await, duration}
import duration._

import sbt.stringToProcess

import scala.util.Try

object Util {

  private def hostName: (Int, String) = {
    val Array(thisPid, thisHost) = ManagementFactory.getRuntimeMXBean().getName().split("@")
    (thisPid.toInt, thisHost)
  }

  def findHost(route: String, token: String, debug: String => Unit): Option[(String, Int)] = {
    implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
    val (thisPid, thisHost) = hostName
    val thisHosts = Set("*", thisHost, "localhost")
    for {
      finder <- findPorts.get(System.getProperty("os.name"))
      (_, _, port) <- finder(debug).find { r =>
        val (pid, host, port) = r
        if (thisHosts.map(_.toLowerCase).contains(host.toLowerCase) && pid == thisPid) {
          import dispatch.{Http, url, as}
          val h = if (host != "*") host else "localhost"
          val svc = url(s"http://$h:$port/${route.stripPrefix("/")}")
          val req = Http(svc OK as.String) // FIX: fails in debug mode
          val result = Try(Await.result(req, 5.seconds)).toOption
          result.filter(_.equals(token)).isDefined
        } else false
      }
    } yield ("localhost", port)
  }

  private[httpserver] val findPorts: Map[String, (String => Unit) => Seq[(Int, String, Int)]] = Map(
    "Mac OS X" -> findPortsMacOsx _
  )

  private def findPortsMacOsx(debug: String => Unit): Seq[(Int, String, Int)] =
    try {
      val user = System.getProperty("user.name")
      val PidPattern = "p([0-9]+)".r
      val PortPattern = "n(.*):([0-9]+)".r
      s"lsof -c java -i TCP -u $user -P -a -Fn".lines_!.sliding(2).collect {
        case Seq(PidPattern(pid), PortPattern(host, port)) =>
          (pid.toInt, host, port.toInt)
      }.toSeq.reverse
    } catch {
      case _: Throwable => Seq.empty
    }

}
