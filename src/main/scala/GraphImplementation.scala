import java.util

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
  var activities = new HashMap[String, ActivityRep]

  //  var adj = new HashMap[ActivityRep, util.LinkedList[ActivityRep]]

  /**
    * Adds a new activity Rep
    * If the activityRep is already present in the graph false is returned, true otherwise
    *
    * @param activityRep
    * @return false if activity already present, true otherwise
    */
  def addNode(activityRep: ActivityRep): Boolean = {
    activities.put(activityRep.id, activityRep).isEmpty
  }

  /**
    * Adds a new edge to Graph
    *
    * @param activityRepFrom
    * @param activityRepTo
    * @return true if all activities are presents in graph and are as excepted, false otherwise
    */
  def addEdge(activityRepFrom: ActivityRep, activityRepTo: ActivityRep): Boolean = {
    if (activityRepFrom.id != activityRepTo.id && activities.contains(activityRepFrom.id)
      && activities.contains(activityRepTo.id)
      && !activities(activityRepFrom.id).connections.contains(activityRepTo)) {
        val act = activities(activityRepFrom.id)
        act.connections = activityRepTo :: act.connections
        true
    } else
      false
  }

  /**
    * Removes a single edge
    *
    * @param activityRepFrom
    * @param activityRepTo
    */
  def removeEdge(activityRepFrom: ActivityRep, activityRepTo: ActivityRep): Unit = {
    val act = activities(activityRepFrom.id)
    act.connections = act.connections.filterNot(_ == activityRepTo)
  }

  /**
    * Removes all edges of a single node
    *
    * @param activityRepFrom
    * @return
    */
  def removeAllEdge(activityRepFrom: ActivityRep): Boolean = {
    //    adj(activityRepFrom).remove()
    //    addNode(activityRepFrom)
    if (activities.contains(activityRepFrom.id)) {
      activities(activityRepFrom.id).connections = Nil
      true
    } else
      false
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

  override def toString: String = activities.toString()


}
