package toolkit.exceptions

/**
  * Created by #ScalaTeam on 15/12/2016.
  */
class InvalidConnection(from: String, to: String) extends Exception(s"Invalid connection ($from -> $to)")