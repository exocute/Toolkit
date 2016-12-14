package toolkit

import scala.collection.mutable.HashMap

/**
  * Created by Eduardo Rodrigues on 12/12/2016.
  *
  * Graph is defined has a HashMap of Activities to LinkedList of AcitivityRep
  */
class GraphImplementation {

  /**
    * key: id of the activity
    */
  val activities = new HashMap[String, ActivityRep]

  val adj = new HashMap[ActivityRep, List[ActivityRep]]

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
      adj.update(activityRepFrom, activityRepTo :: adj(activityRepFrom))
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

  override def toString: String = activities.values.mkString("\n") + "\n" +
    adj.flatMap { case (act, list) => {
      if (list.isEmpty) Nil
      else List(act.id + ":" + list.map(_.id).mkString(","))
    }}.mkString("\n")

}
