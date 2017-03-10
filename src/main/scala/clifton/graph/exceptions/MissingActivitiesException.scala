package clifton.graph.exceptions

/**
  * Created by #ScalaTeam on 02/03/2017.
  */
class MissingActivitiesException(missingActs: Iterable[String]) extends
  Exception("Activities Missing: " + missingActs.mkString(", ")) {

}
