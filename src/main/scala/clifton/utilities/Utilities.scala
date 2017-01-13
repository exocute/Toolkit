package clifton.utilities

/**
  * Created by #ScalaTeam on 20-12-2016.
  */

object Utilities {

  def readFile(path: String): String = scala.io.Source.fromFile(path).mkString

  def clearCommnents(fileText: String): String = {
    fileText.split("\n").map(str => {
      str.indexOf("//") match {
        case -1 => str
        case index => str.substring(0, index)
      }
    }).map(str => str.filterNot(_ == '\r')).mkString("\n")
  }

}