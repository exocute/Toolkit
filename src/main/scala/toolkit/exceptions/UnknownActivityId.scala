package toolkit.exceptions

/**
  * Created by #ScalaTeam on 15/12/2016.
  *
  * Activity identifier is not defined
  */
class UnknownActivityId(id: String) extends Exception("Unknown activity identifier: " + id)
