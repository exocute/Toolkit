package toolkit.exceptions

/**
  * Created by #GrowinScala
  */
class InvalidType(typeFound: String, typeExpected: String) extends
  Exception(s"Invalid type ($typeFound doesn't match expected type $typeExpected)")