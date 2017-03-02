package toolkit

import scala.collection.mutable

/**
  * Created by #ScalaTeam on 12/12/2016.
  *
  * Graph is defined has a HashMap of Activities to List of ActivityRep
  */
class GraphImplementation {

  /**
    * key: id of the activity
    */
  val activities = new mutable.HashMap[String, ActivityRep]

  val adj = new mutable.HashMap[ActivityRep, List[ActivityRep]]

  val adjInverse = new mutable.HashMap[ActivityRep, List[ActivityRep]]

  /**
    * Adds a new activity
    * Returns true if the activity was not already present, false otherwise
    *
    * @param activityRep the activity to add
    * @return false if activity already present, true otherwise
    */
  def addNode(activityRep: ActivityRep): Boolean = {
    if (!adj.contains(activityRep)) {
      activities.put(activityRep.id, activityRep)
      adj.put(activityRep, Nil)
      adjInverse.put(activityRep, Nil)
      true
    }
    else false
  }

  /**
    * Adds a new connection to the graph
    *
    * @param activityFrom
    * @param activityTo
    * @return true if all activities are presents in graph and are as excepted, false otherwise
    */
  def addEdge(activityFrom: ActivityRep, activityTo: ActivityRep): Boolean = {
    if (activityFrom != activityTo && adj.contains(activityFrom) &&
      adj.contains(activityTo) && !adj(activityFrom).contains(activityTo)) {
      adj.update(activityFrom, adj(activityFrom) :+ activityTo)
      adjInverse.update(activityTo, adjInverse(activityTo) :+ activityFrom)
      true
    }
    else false
  }

  /**
    * Returns an activityRep if it exists
    *
    * @param actId
    * @return activityRep
    */
  def getActivityById(actId: String): Option[ActivityRep] = {
    activities.get(actId)
  }

  /**
    * Checks if an activity with actId is defined in the Graph
    *
    * @param actId
    * @return true if defined, false otherwise
    */
  def hasActivity(actId: String): Boolean = {
    activities.get(actId).isDefined
  }

  /**
    * gets the adj of an activity
    *
    * @param activity
    * @return
    */
  def getAdj(activity: ActivityRep): List[ActivityRep] = {
    adj(activity)
  }

  /**
    * gets the reverse adj of an activity
    *
    * @param activityRep
    * @return
    */
  def getInverseAdj(activityRep: ActivityRep): List[ActivityRep] = {
    adjInverse(activityRep)
  }

  /**
    * returns the number of activities in the graph
    *
    * @return
    */
  def numberNodes: Int = activities.size

  /**
    * verifies if a graph has cycles and subgraphs in a DFS style implementation
    *
    * @return true if has cycless, false otherwise
    */
  def hasCyclesAndSubGraphs: Boolean = {
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

    //has subgraphs if not all nodes were visited
    cycleFound && marked.size != numberNodes
  }

  /**
    * Checks if a graph has subgraphs not connected, connections not used and lost nodes.
    * Also checks if the graph has a root and a sink
    *
    * @return
    */
  def nodesWithoutConnections: Boolean = {
    hasRoot && hasSink && !hasCyclesAndSubGraphs
  }


  /**
    * Checks if the graph has a root node
    *
    * @return true if it has a root, false otherwise
    */
  def hasRoot: Boolean = {
    adjInverse.count { case (_, list) => list.isEmpty } == 1
  }

  /**
    * Returns the root activity if it exists
    *
    * @return rootNode
    */
  def getRoot: Option[ActivityRep] = {
    if (hasRoot)
      Some(adjInverse.filter { case (_, list) => list.isEmpty }.head._1)
    else
      None
  }

  /**
    * Checks if the graph has a sink node
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
    * Gets a list of all activities loaded in the graph
    *
    * @return
    */
  def getAllActivities() : List[ActivityRep] = {
    adj.keySet.toList
  }

  /**
    * new toString method to GraphImplemetation
    *
    * @return
    */
  override def toString: String = {
    val adjStr: String = adj.toList.sortBy(_._1.id).flatMap { case (act, list) =>
      if (list.isEmpty) Nil
      else List(act.id + ":" + list.map(_.id).mkString(","))
    }.mkString("\n")

    activities.values.toList.sortBy(_.id).mkString("\n") +
      (if (adjStr.isEmpty) "" else "\n" + adjStr)
  }
}
