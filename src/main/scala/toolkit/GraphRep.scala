package toolkit

import exonode.clifton.signals.ActivityFlatMapType
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
    * Adds a single directed connection between two activities: A -> B
    *
    * @param activityFrom activity A
    * @param activityTo   activity B
    * @return true if all activities are presents in graph and are as excepted, false otherwise
    */
  def addConnection(activityFrom: String, activityTo: String): GraphRep = {
    addConnection(getActivity(activityFrom), getActivity(activityTo))
  }

  /**
    * Adds a single directed connection between two activities: A -> B
    *
    * @param activityFrom activity A
    * @param activityTo   activity B
    * @return true if all activities are presents in graph and are as excepted, false otherwise
    */
  def addConnection(activityFrom: ActivityRep, activityTo: ActivityRep): GraphRep = {
    def connectionHasValidTypes(activityFrom: ActivityRep, activityTo: ActivityRep): Boolean = {
      val from = activityFrom.exportName
      val to = activityTo.importNames

      from.isEmpty || to.isEmpty || {
        if (to.size <= adjInverse(activityTo).size)
          throw new InvalidConnection(activityFrom.id, activityTo.id, s"${activityTo.id} doesn't have the expected type")
        val result = from == to(adjInverse(activityTo).size)
        if (!result)
          throw new InvalidConnectionType(activityFrom.id, from,
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
  def hasRoots: Boolean = {
    adjInverse.exists { case (_, list) => list.isEmpty }
  }

  /**
    * Returns a list of all root activities
    *
    * @return
    */
  def getRoots: List[ActivityRep] =
    adjInverse.filter { case (_, list) => list.isEmpty }.keys.toList

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
    */
  private def validCollector(activity: ActivityRep): Boolean =
    exportName.isEmpty || activity.exportName.isEmpty || activity.exportName == exportName

  /**
    * Checks if the parameter's value to import it's the same has the import parameter of the Root
    */
  private def validInjector(activities: List[ActivityRep]): Boolean =
    importName.isEmpty || activities.forall(activity =>
      activity.importNames.isEmpty ||
        activity.importNames.size == 1 && activity.importNames.head == importName)

  /**
    * Checks if a graph has a sink, a root and do not have cycles or subgraphs
    *
    * @return a valid graph if it passes all checks
    */
  def checkValidGraph(): Try[ValidGraphRep] = {
    Try {
      if (numberOfNodes == 0)
        throw new GraphHasNoActivities
      if (!hasRoots)
        throw new GraphHasNoRoot
      if (!hasSink)
        throw new GraphHasNoSink
      if (hasCycles)
        throw new GraphHasCycles
      if (!validInjector(getRoots))
        throw new GraphRootHasInvalidTypes
      if (!validCollector(getSink.get))
        throw new GraphSinkHasInvalidTypes
      new ValidGraphRep(name, activities, adj, adjInverse)
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
    * checks if a graph has cycles in a DFS style implementation
    *
    * @return true if there are no cycles, false otherwise
    */
  private def hasCycles: Boolean = {
    var marked = Set[ActivityRep]()
    var onStack = List[ActivityRep]()

    //DFS running
    def findCycle(initActivities: List[ActivityRep]): Boolean = {
      initActivities match {
        case Nil => false
        case init :: others =>
          marked = marked + init
          onStack = init :: onStack

          val vec = adj(init)

          def testActivity(index: Int): Boolean = {
            if (index >= vec.length)
              false
            else {
              val act = vec(index)
              if (!marked.contains(act))
                findCycle(List(act)) || testActivity(index + 1)
              else if (onStack.contains(act)) {
                true
              } else {
                testActivity(index + 1)
              }
            }
          }

          val result = testActivity(0)
          onStack = onStack.filter(_ != init)
          result || findCycle(others)
      }
    }

    findCycle(getRoots)
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

class ValidGraphRep private[toolkit](val name: String,
                                     activities: Map[String, ActivityRep],
                                     adj: Map[ActivityRep, Vector[ActivityRep]],
                                     adjInverse: Map[ActivityRep, Vector[ActivityRep]]) extends Serializable {

  def connectionExists(activityFromId: String, activityToId: String): Boolean = {
    adj(getActivity(activityFromId)).contains(getActivity(activityToId))
  }

  def connectionExists(activityFrom: ActivityRep, activityTo: ActivityRep): Boolean = {
    adj.contains(activityFrom) && adj.contains(activityTo) &&
      adj(activityFrom).contains(activityTo)
  }

  val roots: List[ActivityRep] = adjInverse.filter { case (_, list) => list.isEmpty }.keys.toList
  val sink: ActivityRep = adj.filter { case (_, list) => list.isEmpty }.head._1

  val depthOfFlatMaps: Int = {
    activities.count(_._2.actType == ActivityFlatMapType)
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

}