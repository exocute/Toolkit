package toolkit.exceptions

/**
  * Created by #GrowinScala
  */
class InvalidConnectionType(from: String, fromType: String, to: String, toType: String) extends
  Exception(s"Invalid connection ($from -> $to) ($fromType doesn't match $toType)")