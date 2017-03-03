package clifton.graph.exceptions

/**
  * Created by #ScalaTeam on 02/03/2017.
  */
class MissingActivitiesException(missingActs: List[String]) extends
  Exception("Activities Missing: " + missingActs.mkString(", ")) {

}
