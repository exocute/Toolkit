package toolkit.exceptions

/**
  * Created by #GrowinScala
  *
  * Each activity should have a unique identifier
  */
class ActivitiesWithDuplicateId(id: String) extends Exception("Activities with duplicate identifier: " + id)
