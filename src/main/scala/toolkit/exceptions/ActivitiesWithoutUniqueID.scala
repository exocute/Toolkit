package toolkit.exceptions

/**
  * Created by #ScalaTeam on 15/12/2016.
  *
  * When activities doesn't have unique id's
  *
  * Every Activity should have a unique ID given by the user
  */
class ActivitiesWithoutUniqueID(id: String) extends Exception("Activities With Wrong ID: " + id)
