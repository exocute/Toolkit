package toolkit

import scala.collection.mutable.HashMap

/**
  * Created by #ScalaTeam on 12/12/2016.
  *
  * Graph is defined has a HashMap of Activities to LinkedList of AcitivityRep
  */
class GraphImplementation {


  /**
    * key: id of the activity
    */
  val activities = new HashMap[String, ActivityRep]


  val adj = new HashMap[ActivityRep, List[ActivityRep]]

  val adjInverse = new HashMap[ActivityRep,List[ActivityRep]]

  /**
    * Adds a new activity Rep
    * If the activityRep is already present in the graph false is returned, true otherwise
    *
    * @param activityRep
    * @return false if activity already present, true otherwise
    */
  def addNode(activityRep: ActivityRep): Boolean = {
    if (!adj.contains(activityRep)) {
      activities.put(activityRep.id, activityRep)
      adj.put(activityRep, Nil)
      adjInverse.put(activityRep,Nil)
      true
    }
    else false
  }

  /**
    * Adds a new edge to Graph
    *
    * @param activityRepFrom
    * @param activityRepTo
    * @return true if all activities are presents in graph and are as excepted, false otherwise
    */
  def addEdge(activityRepFrom: ActivityRep, activityRepTo: ActivityRep): Boolean = {
    if (activityRepFrom != activityRepTo && adj.contains(activityRepFrom) &&
      adj.contains(activityRepTo) && !adj(activityRepFrom).contains(activityRepTo)) {
      adj.update(activityRepFrom, adj(activityRepFrom):+ activityRepTo)
      adjInverse.update(activityRepTo, adjInverse(activityRepTo):+ activityRepFrom)
      true
    }
    else false
  }

  /**
    * Removes a single edge
    *
    * @param activityRepFrom
    * @param activityRepTo
    */
  def removeEdge(activityRepFrom: ActivityRep, activityRepTo: ActivityRep): Unit = {
    ???
  }

  /**
    * Removes all edges of a single node
    *
    * @param activityRepFrom
    * @return
    */
  def removeAllEdge(activityRepFrom: ActivityRep): Boolean = {
    ???
  }

  /**
    * returns an activityRep if exists
    *
    * @param actId
    * @return activityRep
    */
  def getActById(actId: String): Option[ActivityRep] = {
    activities.get(actId)
  }

  /**
    * verifies if an activityRep with idName is defined in the Graph
    *
    * @param idName
    * @return true if defined, false otherwise
    */
  def hasActivity(idName: String): Boolean = {
    getActById(idName).isDefined
  }

  /**
    * gets the adj of a single activity
    * @param activity
    * @return
    */
  def getAdj(activity: ActivityRep): List[ActivityRep] = {
    adj(activity)
  }


  /**
    * returns the nodes number of the graph
    * @return
    */
  def numberNodes : Int = {
    adj.size
  }

  /**
    * verifies if a graph has cycles and subgraphs in a DFS style implementation
    * @return true if has cycless, false otherwise
    */
  def hasCyclesAndSubGraphs : Boolean = {
    var marked = Set[ActivityRep]()
    var onStack = List[ActivityRep]()
    var cycleFound = false
    findCycle(getRoot.get)

    //DFS running
    def findCycle(init : ActivityRep) : Unit = {
      marked = marked + init
      onStack = init :: onStack
      for(
        x <- adj(init)
      ) if(!marked.contains(x)) findCycle(x)
      else if(onStack.contains(x)){
        cycleFound = true
        return
      }
      onStack = onStack.filter(_!=init)
    }

    //has subgraphs if not all nodes were visited
    cycleFound && marked.size != adj.size
  }

  /**
    * verifies if a graph has subgraphs not connected, connections not used and lost nodes
    * also verifies if the graph has a absolute Root and Sink
    * @return
    */
  def nodesWithoutConnections() : Boolean = {
   hasRoot && hasSink && !hasCyclesAndSubGraphs
  }

  /**
    * if exists returns the root node
    * @return rootNode
    */
  def getRoot : Option[ActivityRep] = {
    if(hasRoot)
      Some(adjInverse.filter{case(act,list) => list.isEmpty}.head._1)
    else
      None
  }

  /**
    * verifies if a graph has a Root node
    * @return true if it has a root, false otherwise
    */
  def hasRoot : Boolean = {
    adjInverse.count{case (act,list) => list.isEmpty}==1
  }

  /**
    * verifies if a graph has a Sink node
    * @return true if it has a sink, false otherwise
    */
  def hasSink : Boolean = {
    adj.count{case (act,list) => list.isEmpty}==1
  }

  /**
    * gets a Sink node
    * @return
    */
  def getSink : Option[ActivityRep] = {
    if(hasRoot)
      Some(adj.filter{case(act,list) => list.isEmpty}.head._1)
    else
      None
  }

  /**
    * returns the of nodes that has a connection to that node
    * @param activityRep
    * @return
    */
  def referencedByNodes(activityRep: ActivityRep) : Int = {
    adjInverse(activityRep).size
  }

  /**
    * new toString method to GraphImplemetation
    * @return
    */
  override def toString: String = activities.values.mkString("\n") + "\n" +
    adj.flatMap { case (act, list) => {
      if (list.isEmpty) Nil
      else List(act.id + ":" + list.map(_.id).mkString(","))
    }
    }.mkString("\n")
}
