package toolkit.exceptions

/**
  * Created by #GrowinScala
  *
  * Activity identifier is not defined
  */
class UnknownActivityId(id: String) extends Exception("Unknown activity identifier: " + id)
