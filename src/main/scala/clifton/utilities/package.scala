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

}