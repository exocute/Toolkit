package toolkit.exceptions

/**
  * Created by #ScalaTeam on 15/12/2016.
  *
  * Each activity should have a unique identifier
  */
class ActivitiesWithDuplicateId(id: String) extends Exception("Activities with duplicate identifier: " + id)
