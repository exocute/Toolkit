import java.util

/**
  * Created by Eduardo Rodrigues on 12/12/2016.
  */
case class ActivityRep(id: String, name: String, var parameters: List[String], var importName: List[String], var exportName: String) {

  var connections: List[ActivityRep] = Nil

  /**
    * prints the id of the activity
    *
    * @return id
    */
  override def toString: String = s"$id, $name, $parameters, $importName, $exportName"

  /**
    * two activities are equals when their id are the same
    *
    * @param that
    * @return true if equals, false otherwise
    */
  override def equals(that: scala.Any): Boolean = that match {
    case ActivityRep(idVal, _, _, _, _) => this.id == idVal
    case _ => false
  }

  override def hashCode(): Int = id.hashCode
}

object ActivityRep {
  def apply(id: String, name: String): ActivityRep = {
    ActivityRep(id, name, Nil, Nil, "")
  }
}