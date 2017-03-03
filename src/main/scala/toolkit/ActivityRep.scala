package toolkit

import exonode.clifton.signals.ActivityType

/**
  * Created by #ScalaTeam on 12/12/2016.
  *
  * Representation of an activity
  */
case class ActivityRep(id: String, name: String, actType: ActivityType,
                       parameters: Vector[String], importName: Vector[String], exportName: String) {

  def addParameter(newParameter: String): ActivityRep = {
    ActivityRep(id, name, actType, parameters :+ newParameter, importName, exportName)
  }

  def addImport(newImport: String): ActivityRep = {
    ActivityRep(id, name, actType, parameters, importName :+ newImport, exportName)
  }

  def setExport(newExport: String): ActivityRep = {
    ActivityRep(id, name, actType, parameters, importName, newExport)
  }

  /**
    * two activities are equals when their id are the same
    *
    * @param that
    * @return true if equals, false otherwise
    */
  override def equals(that: Any): Boolean = that match {
    case ActivityRep(idVal, _, _, _, _, _) => this.id == idVal
    case _ => false
  }

  override def hashCode(): Int = id.hashCode

  private def getListOfActivityParameters = List("Id" -> id, "Name" -> name, "Type" -> actType.toString, "Import" -> importName.mkString(","), "Export" -> exportName, "Parameters" -> parameters.mkString(","))

  private def showValidParameters(list: List[(String, String)]) = list.filter(_._2.nonEmpty).map { case (key, value) => key + ": " + value }

  override def toString: String = showValidParameters(getListOfActivityParameters).mkString(", ")

}

object ActivityRep {
  def apply(id: String, name: String, actType: ActivityType): ActivityRep = {
    ActivityRep(id, name, actType, Vector[String](), Vector[String](), "")
  }
}
