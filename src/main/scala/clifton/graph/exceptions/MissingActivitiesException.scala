package clifton.graph.exceptions

/**
  * Created by #GrowinScala
  */
class MissingActivitiesException(missingActs: Iterable[String]) extends
  Exception("Activities Missing: " + missingActs.mkString(", ")) {

}
