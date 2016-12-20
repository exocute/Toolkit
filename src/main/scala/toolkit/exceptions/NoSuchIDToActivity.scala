package toolkit.exceptions

/**
  * Created by #ScalaTeam on 15/12/2016.
  *
  * Some ID are not defined yet
  *
  * downstream and upstream keys should be placed in the end of the pln file
  */
class NoSuchIDToActivity(id: String) extends Exception("ID doesn't exist: " + id)
