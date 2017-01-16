package clifton.utilities

import java.io.File

/**
  * Created by #ScalaTeam on 20-12-2016.
  */

object Utilities {

  def readFile(file: File): String = scala.io.Source.fromFile(file).mkString

  def clearCommnents(fileText: String): String = {
    fileText.split("\n").map(str => {
      str.indexOf("//") match {
        case -1 => str
        case index => str.substring(0, index)
      }
    }).map(str => str.filterNot(_ == '\r')).mkString("\n")
  }

}