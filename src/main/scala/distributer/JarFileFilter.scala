package distributer

import java.io.File

import scala.collection.mutable

/**
  * Created by #GrowinScala
  */
class JarFileFilter extends java.io.FilenameFilter {

  private val jarMap = mutable.HashMap[String, Long]()

  def accept(dir: File, name: String): Boolean = {
    if (name.endsWith(".jar")) {
      val file: File = new File(dir, name)
      val lastModified = file.lastModified
      jarMap.get(name) match {
        case None =>
          jarMap += name -> lastModified
          true
        case Some(date) if date != lastModified =>
          jarMap += name -> lastModified
          true
        case _ =>
          false
      }
    } else
      false
  }

}
