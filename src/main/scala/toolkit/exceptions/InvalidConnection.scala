package toolkit.exceptions

/**
  * Created by #GrowinScala
  */
class InvalidConnection(from: String, to: String, msg: String) extends
  Exception(s"Invalid connection ($from -> $to) $msg")