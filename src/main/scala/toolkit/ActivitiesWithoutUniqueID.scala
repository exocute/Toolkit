package toolkit

/**
  * When activities doesn't have unique id's
  *
  * Every Activity should have a unique ID given by the user
  */
class ActivitiesWithoutUniqueID extends Exception("Activities With Wrong ID")
