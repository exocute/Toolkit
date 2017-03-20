package toolkit

import toolkit.exceptions._

import scala.language.implicitConversions
import scala.util.Try

/**
  * Created by #GrowinScala
  *
  * Representation of the graph
  */
class GraphRep(val name: String, importName: String, exportName: String) extends Serializable {
  self =>

  /**
    * This construct allows users to create graphs just using the graph name
    */
  def this(name: String) {
    this(name, "", "")
  }

  def setImport(newImportName: String): GraphRep = {
    new GraphRep(name, newImportName, exportName)
  }

  def setExport(newExportName: String): GraphRep = {
    new GraphRep(name, importName, newExportName)
  }

  /**
    * key: id of the activity
    */
  protected val activities: Map[String, ActivityRep] = Map[String, ActivityRep]()

  protected val adj: Map[ActivityRep, Vector[ActivityRep]] = Map[ActivityRep, Vector[ActivityRep]]()

  protected val adjInverse: Map[ActivityRep, Vector[ActivityRep]] = Map[ActivityRep, Vector[ActivityRep]]()

  /**
    * Adds a new activity to graph
    * checks if the new activity already exists in the graph
    *
    * @param activityRep the activity to add
    */
  def addActivity(activityRep: ActivityRep): GraphRep = {
    if (adj.contains(activityRep)) {
      throw new ActivitiesWithDuplicateId(activityRep.id)
    } else {
      new GraphRep(name, importName, exportName) {
        override val activities: Map[String, ActivityRep] =
          self.activities.updated(activityRep.id, activityRep)

        override val adj: Map[ActivityRep, Vector[ActivityRep]] =
          self.adj.updated(activityRep, Vector.empty)

        override val adjInverse: Map[ActivityRep, Vector[ActivityRep]] =
          self.adjInverse.updated(activityRep, Vector.empty)
      }
    }
  }

  def connectionExists(activityFromId: String, activityToId: String): Boolean = {
    adj(getActivity(activityFromId)).contains(getActivity(activityToId))
  }

  def connectionExists(activityFrom: ActivityRep, activityTo: ActivityRep): Boolean = {
    adj.contains(activityFrom) && adj.contains(activityTo) &&
      adj(activityFrom).contains(activityTo)
  }

  /**
    * Adds a single connection between two activities
    *
    * @param activityFrom
    * @param activityTo
    */
  def addConnection(activityFrom: String, activityTo: String): GraphRep = {
    addConnection(getActivity(activityFrom), getActivity(activityTo))
  }

  /**
    * Adds a single connection between two activities
    *
    * @param activityFrom
    * @param activityTo
    * @return true if all activities are presents in graph and are as excepted, false otherwise
    */
  def addConnection(activityFrom: ActivityRep, activityTo: ActivityRep): GraphRep = {
    def connectionHasValidTypes(activityFrom: ActivityRep, activityTo: ActivityRep): Boolean = {
      val from = activityFrom.exportNames
      val to = activityTo.importNames

      from.isEmpty || to.isEmpty || {
        if (from.size <= adj(activityFrom).size)
          throw new InvalidConnection(activityFrom.id, activityTo.id, s"${activityFrom.id} doesn't have the expected type")
        if (to.size <= adjInverse(activityTo).size)
          throw new InvalidConnection(activityFrom.id, activityTo.id, s"${activityTo.id} doesn't have the expected type")
        val result = from(adj(activityFrom).size) == to(adjInverse(activityTo).size)
        if (!result)
          throw new InvalidConnectionType(activityFrom.id, from(adj(activityFrom).size),
            activityTo.id, to(adjInverse(activityTo).size))
        result
      }
    }

    if (activityFrom != activityTo && adj.contains(activityFrom) &&
      adj.contains(activityTo) && !adj(activityFrom).contains(activityTo) &&
      connectionHasValidTypes(activityFrom, activityTo)) {
      new GraphRep(name, importName, exportName) {
        override val activities: Map[String, ActivityRep] =
          self.activities

        override val adj: Map[ActivityRep, Vector[ActivityRep]] =
          self.adj.updated(activityFrom, self.adj(activityFrom) :+ activityTo)

        override val adjInverse: Map[ActivityRep, Vector[ActivityRep]] =
          self.adjInverse.updated(activityTo, self.adjInverse(activityTo) :+ activityFrom)
      }
    } else {
      throw new InvalidConnection(activityFrom.id, activityTo.id, "")
    }
  }

  /**
    * Checks if the graph has a root activity
    *
    * @return true if it has a root, false otherwise
    */
  def hasRoot: Boolean = {
    adjInverse.count { case (_, list) => list.isEmpty } == 1
  }

  /**
    * Returns the root activity if it exists
    *
    * @return
    */
  def getRoot: Option[ActivityRep] =
    if (hasRoot)
      Some(adjInverse.filter { case (_, list) => list.isEmpty }.head._1)
    else
      None

  /**
    * Checks if the graph has a sink activity
    *
    * @return true if it has a sink, false otherwise
    */
  def hasSink: Boolean = {
    adj.count { case (_, list) => list.isEmpty } == 1
  }

  /**
    * Returns the sink activity if it exists
    *
    * @return
    */
  def getSink: Option[ActivityRep] = {
    if (hasSink)
      Some(adj.filter { case (_, list) => list.isEmpty }.head._1)
    else
      None
  }

  /**
    * Checks if the parameter's value to export it's the same has the export parameter of the Sink
    *
    * @param activity
    * @return
    */
  private def validCollector(activity: ActivityRep): Boolean =
    exportName.isEmpty || activity.exportNames.isEmpty ||
      activity.exportNames.size == 1 && activity.exportNames.head == exportName

  /**
    * Checks if the parameter's value to import it's the same has the import parameter of the Root
    *
    * @param activity
    * @return
    */
  private def validInjector(activity: ActivityRep): Boolean =
    importName.isEmpty || activity.importNames.isEmpty ||
      activity.importNames.size == 1 && activity.importNames.head == importName

  /**
    * Checks if a graph has a sink, a root and do not have cycles or subgraphs
    *
    * @return the graph if it passes all checks
    */
  def checkValidGraph(): Try[GraphRep] = {
    Try {
      if (numberOfNodes == 0)
        throw new GraphHasNoActivities
      if (!hasRoot)
        throw new GraphHasNoRoot
      if (!hasSink)
        throw new GraphHasNoSink
      if (hasCyclesOrSubGraphs)
        throw new GraphHasCycles
      if (!validInjector(getRoot.get))
        throw new GraphRootHasInvalidTypes
      if (!validCollector(getSink.get))
        throw new GraphSinkHasInvalidTypes
      this
    }
  }

  /**
    * returns the activity with a specific id
    *
    * @param id the id the of the activity
    * @return the ActivityRep with id
    */
  def getActivity(id: String): ActivityRep = {
    activities.get(id) match {
      case None => throw new UnknownActivityId(id)
      case Some(act) => act
    }
  }

  /**
    * checks if a graph has cycles or subgraphs in a DFS style implementation
    *
    * @return true if has cycless, false otherwise
    */
  private def hasCyclesOrSubGraphs: Boolean = {
    var marked = Set[ActivityRep]()
    var onStack = List[ActivityRep]()
    var cycleFound = false
    findCycle(getRoot.get)

    //DFS running
    def findCycle(init: ActivityRep): Unit = {
      marked = marked + init
      onStack = init :: onStack
      for (act <- adj(init)) {
        if (!marked.contains(act))
          findCycle(act)
        else if (onStack.contains(act)) {
          cycleFound = true
          return
        }
      }
      onStack = onStack.filter(_ != init)
    }

    if (marked.size != numberOfNodes)
      throw new GraphIsNotConnected

    //has subgraphs if not all nodes were visited
    cycleFound
  }

  def numberOfNodes: Int = activities.size

  /**
    * gets the next activities from a single activity
    *
    * @param activity
    * @return next activities
    */
  def getConnections(activity: ActivityRep): Vector[ActivityRep] = adj(activity)

  /**
    * gets the previous activities from a single activity
    *
    * @param activity
    * @return next activities
    */
  def getReverseConnections(activity: ActivityRep): Vector[ActivityRep] = adjInverse(activity)

  def getActivities: Iterable[ActivityRep] = adj.keys

  override def toString: String = {
    s"Graph $name\n" +
      s"${showIf(importName, s"Import $importName\n")}" +
      s"${showIf(exportName, s"Export $exportName\n")}" +
      graphToString
  }

  private def showIf(test: String, exp: => Any): String =
    if (test.nonEmpty) exp.toString else ""

  private def graphToString: String = {
    val actsStr = activities.values.toList.sortBy(_.id).mkString("\n")

    val adjStr: String = adj.toList.sortBy(_._1.id).flatMap { case (act, connections) =>
      if (connections.isEmpty) Nil
      else List(s"Connection ${act.id} -> ${connections.map(_.id).mkString(", ")}")
    }.mkString("\n")

    actsStr + showIf(adjStr, "\n" + adjStr)
  }
}
