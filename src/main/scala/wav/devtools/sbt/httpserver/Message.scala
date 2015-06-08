package wav.devtools.sbt.httpserver

import org.json4s.JsonAST.JString
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.json4s.jackson.Serialization.write


// Messages are deserialised as [String, JValue] or JField
//          are serialised as [String, JValue]
object Message {

  protected implicit val formats = org.json4s.DefaultFormats

  def unapply(s: String): Option[JField] =
    parseOpt(s).flatMap(unapply)

  def unapply(v: JValue): Option[JField] =
    v.extractOpt[JField] orElse v.extractOpt[List[JValue]].collect {
      case List(JString(id), data) => (id, data)
    }

  def extractOpt[T](s: String)(implicit m: Manifest[T]): Option[(String, T)] =
    unapply(s).flatMap { v =>
      v._2.extractOpt[T].map((v._1, _))
    }

  def extractOpt[T](v: JValue)(implicit m: Manifest[T]): Option[(String, T)] =
    unapply(v).flatMap { v =>
      v._2.extractOpt[T].map((v._1, _))
    }

  def apply[T](id: String, data: T)(implicit m: Manifest[T]): String =
    write(List(JString(id), data))

  def apply[T](f: JField): String =
    write(List(JString(f._1), f._2))

}