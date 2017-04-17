package toolkit

import exonode.clifton.signals.ActivityType

/**
  * Created by #GrowinScala
  *
  * Representation of an activity
  */
case class ActivityRep(id: String, className: String, actType: ActivityType,
                       parameters: Vector[String], importNames: Vector[String], exportName: String) {

  def addParameter(newParameter: String): ActivityRep = {
    ActivityRep(id, className, actType, parameters :+ newParameter, importNames, exportName)
  }

  def addImport(newImport: String): ActivityRep = {
    ActivityRep(id, className, actType, parameters, importNames :+ newImport, exportName)
  }

  def setExport(newExport: String): ActivityRep = {
    ActivityRep(id, className, actType, parameters, importNames, newExport)
  }

  /**
    * Checks if two activities are equal using their id
    *
    * @param that object to check
    * @return true if the activities are equal, false otherwise
    */
  override def equals(that: Any): Boolean = that match {
    case ActivityRep(idVal, _, _, _, _, _) => this.id == idVal
    case _ => false
  }

  override def hashCode: Int = id.hashCode

  override def toString: String = {
    s"$actType $id $className${showIf(parameters.nonEmpty, ":" + parameters.mkString(":"))}" +
      s"${showIf(importNames.nonEmpty, s"\nImport ${importNames.mkString(", ")}")}" +
      s"${showIf(exportName, s"\nExport $exportName")}"
  }

  private def showIf(test: Boolean, exp: => Any): String =
    if (test) exp.toString else ""

  private def showIf(test: String, exp: => Any): String =
    if (test.nonEmpty) exp.toString else ""

}

object ActivityRep {
  def apply(id: String, name: String, actType: ActivityType): ActivityRep = {
    ActivityRep(id, name, actType, Vector(), Vector(), "")
  }
}
