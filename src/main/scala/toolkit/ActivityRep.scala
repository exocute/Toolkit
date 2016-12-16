package toolkit

import java.io.Serializable

/**
  * Created by #ScalaTeam on 12/12/2016.
  */
case class ActivityRep(id: String, name: String, var parameters: List[String], var importName: List[String], var exportName: String) {


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


  def getListOfActivityParameters = List(("Id", id), ("Name", name), ("Import", importName.mkString(",")), ("Export", exportName), ("Parameters", parameters.mkString(",")))

  def showValidParameters(list: List[(String, String)]) = list.filter(_._2.nonEmpty).map { case (name, value) => name + ": " + value }

  override def toString: String = showValidParameters(getListOfActivityParameters).mkString(", ")

}

object ActivityRep {
  def apply(id: String, name: String): ActivityRep = {
    ActivityRep(id, name, Nil, Nil, "")
  }
}