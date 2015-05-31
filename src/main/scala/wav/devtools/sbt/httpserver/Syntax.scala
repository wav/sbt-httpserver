package wav.devtools.sbt.httpserver

import java.io.File

import com.google.common.io.Resources
import java.net.URL

trait Syntax {

  implicit def manifestAsURLLookup[T](m: Manifest[T]): String => URL =
    Resources.getResource(m.runtimeClass, _)

  implicit def fileAsURLLookup(f: File): String => URL =
    p => new File(f, p).toURI.toURL

  implicit def filesAsURLLookups(fs: Seq[File]): Seq[String => URL] =
    fs.map(fileAsURLLookup)

}