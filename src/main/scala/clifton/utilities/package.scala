/**
  * Created by #ScalaTeam on 20-12-2016.
  */

package object clifton {

  sealed trait LogLevel

  case object Info extends LogLevel {
    override def toString: String = "INFO"
  }

  case object Error extends LogLevel {
    override def toString: String = "ERROR"
  }

  val inoSignal = ">"
  val exoSignal = "<"

  def readFile(path: String) = scala.io.Source.fromFile(path).mkString

  def clearCommnents(file: String): String = {
    file.split("\n").map(str => {
      val index = str.indexOf("//")
      if (index == -1) str
      else str.substring(0, index)
    }).map(str => str.filterNot(_ == '\r')).mkString("\n")
  }



}