package toolkit

import toolkit.exceptions.{ActivitiesWithDuplicateId, InvalidConnection, UnknownActivityId}

/**
  * Created by #GrowinScala
  *
  * Representation of the graph
  */
class GraphRep(val name: String, importName: String, exportName: String, activitiesGraph: GraphImplementation) extends Serializable {

  /**
    * This construct allows users to create graphs just using the name
    *
    * @param name
    */
  def this(name: String) {
    this(name, "", "", new GraphImplementation)
  }

  def setImport(newImportName: String): GraphRep = {
    new GraphRep(name, newImportName, exportName, activitiesGraph)
  }

  def setExport(newExportName: String): GraphRep = {
    new GraphRep(name, importName, newExportName, activitiesGraph)
  }

  /**
    * adds a new activity to graph
    * check if the new activity already exists in the graph
    *
    * @param activityRep
    */
  def addActivity(activityRep: ActivityRep): Unit = {
    if (!activitiesGraph.addNode(activityRep))
      throw new ActivitiesWithDuplicateId(activityRep.id)
  }

  /**
    * adds a single connection between activities
    *
    * @param activityFrom
    * @param activityTo
    */
  def addConnection(activityFrom: String, activityTo: String): Unit = {
    if (!activitiesGraph.addEdge(activityById(activityFrom), activityById(activityTo))
      && !validConnection(activityFrom, activityTo))
      throw new InvalidConnection(activityFrom, activityTo)
  }

  /**
    * Forks activities
    * adds multiple connection from an activity to others
    * checks if the activities exists and are correct
    *
    * @param activityFrom
    * @param activityTo
    */
  def addConnection(activityFrom: String, activityTo: Iterable[String]): Unit = {
    val from = activityById(activityFrom)
    for (to <- activityTo)
      addConnection(activityFrom, to)
  }

  /**
    * get Root activity
    *
    * @return
    */
  def getRoot: Option[ActivityRep] = activitiesGraph.getRoot

  /**
    * verifies is the graph has a Root
    *
    * @return
    */
  def hasRoot: Boolean = activitiesGraph.hasRoot


  /**
    * get Sink activity
    *
    * @return
    */
  def getSink: Option[ActivityRep] = activitiesGraph.getSink

  /**
    * verifies is the Graph has a Sink
    *
    * @return
    */
  def hasSink: Boolean = activitiesGraph.hasSink

  /**
    * checks the import and export parameters of a connection
    *
    * @param activityFrom
    * @param activityTo
    * @return true if its valid, false otherwise
    */
  def validConnection(activityFrom: String, activityTo: String): Boolean = {
    val from = activityById(activityFrom).exportName
    val to = activityById(activityTo).importName
    if (to.isEmpty)
      false
    else
      to.contains(from)
  }

  /**
    * checks the import and export parameters of a connection
    *
    * @param activityFrom
    * @param activityTo
    * @return true if its valid, false otherwise
    */
  def validConnection(activityFrom: String, activityTo: List[String]): Boolean = {
    for (to <- activityTo)
      if (!validConnection(activityFrom, to))
        return false
    true
  }

  /**
    * verifies if the parameter's value to export it's the same has the export parameter of the Sink
    *
    * @param value
    * @param exportName
    * @return
    */
  def validCollector(value: ActivityRep, exportName: String): Boolean =
    if (exportName.isEmpty || value.exportName.isEmpty) true else value.exportName == exportName

  /**
    * verifies if the parameter's value to import it's the same has the import parameter of the Root
    *
    * @param value
    * @param importName
    * @return
    */
  def validInjector(value: ActivityRep, importName: String): Boolean =
    if (importName.isEmpty || value.importName.isEmpty) true else value.importName.head == importName

  /**
    * checks if a graph has a Sink, Root and do not has cycles or subgraphs
    *
    * @return true if it's a valid graph, false otherwise
    */
  def checkValidGraph(): Boolean = {
    if (!activitiesGraph.hasSink || !activitiesGraph.hasRoot || activitiesGraph.hasCyclesAndSubGraphs
      || !validInjector(activitiesGraph.getRoot.get, importName) || !validCollector(activitiesGraph.getSink.get, exportName))
      false
    else true
  }

  /**
    * @param id - ID of Activity
    * @return - ActivityRep with ID
    */
  def activityById(id: String): ActivityRep = {
    val act = activitiesGraph.getActivityById(id)
    act match {
      case None => throw new UnknownActivityId(id)
      case Some(x) => x
    }
  }

  /**
    * gets the next activities from a single activity
    *
    * @param activity
    * @return next activities
    */
  def getConnections(activity: ActivityRep): List[ActivityRep] = activitiesGraph.getAdj(activity)

  /**
    * gets the previous activities from a single activity
    *
    * @param activity
    * @return next activities
    */
  def getReverseConnections(activity: ActivityRep): List[ActivityRep] = activitiesGraph.getInverseAdj(activity)

  def getActivities: Vector[String] = activitiesGraph.activities.keys.toVector

  def getAllActivitiesNames: List[String] = activitiesGraph.getAllActivities().map(act => act.name)

  def getAllActivityReps: List[ActivityRep] = activitiesGraph.getAllActivities()

  /**
    * returns the string of a value if the value its defined
    *
    * @param s
    * @param value
    * @return
    */
  private def showIfNotEmpty(s: String, value: String): String = if (!value.isEmpty) s + ": " + value + "\n" else ""

  override def toString: String = s"Name: $name\n" +
    showIfNotEmpty("ImportName", importName) + showIfNotEmpty("ExportName", exportName) + s"$activitiesGraph"
}
