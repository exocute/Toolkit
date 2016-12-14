/**
  * Created by Eduardo Rodrigues on 12/12/2016.
  */

class GraphRep(name: String, var importName: String, var exportName: String, activities: GraphImplementation) {

  var lastACT: ActivityRep = _

  /**
    * This construct allows users to create graphs just using the name
    *
    * @param name
    */
  def this(name: String) {
    this(name, "", "", new GraphImplementation)
  }

  /**
    * Add a single activity to graph
    * If the activity doesn't follow the correct format exceptions will be thrown
    * Activities are added to the graph in sequential order
    *
    * @param activityRep
    */
  def addActivity(activityRep: ActivityRep) = {
    if (activities.addNode(activityRep)) {
      if (lastACT == null) {
        lastACT = activityRep
      }
      else {
        if (activities.addEdge(lastACT, activityRep))
          lastACT = activityRep
        else
          throw new ActivityWithWrongParameters
      }
    }
    else throw new ActivitiesWithoutUniqueID
  }

  /**
    * adds a new activity to graph
    * check if the new activity already exists in the graph
    *
    * @param activityRep
    */
  def addSingleActivity(activityRep: ActivityRep) = {
    if (!activities.addNode(activityRep))
      throw new ActivitiesWithoutUniqueID
  }

  /**
    * Changes the current graph to allow forks and joins
    * Makes a Fork with Activities to be executed in parallel
    * Removes all edges from activityRep in activityRepFrom
    * Adds new Edges from activityRepTo to every activityRepFrom
    *
    * @param activityRepTo   - Root
    * @param activityRepFrom - All Directions from Root
    */
  def addDownStream(activityRepTo: String, activityRepFrom: List[String]) = {

    val actTo = activityById(activityRepTo)

    //check valid activities
    for {
      x <- activityRepTo :: activityRepFrom
      if !activities.hasActivity(x)
    } throw new NoSuchIDToActivity(x)

    //remove all edges
    for {
      x <- activityRepFrom
    } if (!activities.removeAllEdge(activityById(x))) throw new ActivityWithWrongParameters

    //add new edges to make join
    for {
      x <- activityRepFrom
    } if (!activities.addEdge(activityById(x), actTo)) throw new ActivityWithWrongParameters
  }

  /**
    * Changes the current graph to allow forks and joins    *
    * Makes a Join with activities to merge the result of more than one activity
    * Removes all edges of the activityRepFrom
    * Adds new edges from every activityRepTo to activityRepFrom
    *
    * @param activityRepTo
    * @param activityRepFrom
    */
  def addUpStream(activityRepTo: List[String], activityRepFrom: String) = {

    val actFrom = activityById(activityRepFrom)

    //check valid activities
    for {
      x <- activityRepFrom :: activityRepTo
      if !activities.hasActivity(x)
    } throw new NoSuchIDToActivity(x)

    //get an activity by name
    if (!activities.removeAllEdge(actFrom))
      throw new ActivityWithWrongParameters

    //add new edges to make fork
    for {
      x <- activityRepTo
    } if (!activities.addEdge(actFrom, activityById(x))) throw new ActivityWithWrongParameters
  }


  /**
    * adds a single connection between activities
    *
    * @param activityFrom
    * @param activityTo
    */
  def addConnection(activityFrom: String, activityTo: String) = {
    if (!activities.addEdge(activityById(activityFrom), activityById(activityTo)))
      throw new ActivityWithWrongParameters
  }

  /**
    * Forks activities
    * adds multiple connection from an activity to others
    * checks if the activities exists and are correct
    *
    * @param activityFrom
    * @param activityTo
    */
  def addConnection(activityFrom: String, activityTo: List[String]) = {
    val from = activityById(activityFrom)
    for {
      to <- activityTo
    } if (!activities.addEdge(from, activityById(to))) throw new ActivityWithWrongParameters
  }

  /**
    * @param id - ID of Activity
    * @return - ActivityRep with ID
    */
  def activityById(id: String): ActivityRep = {
    val act = activities.getActById(id)
    act match {
      case None => throw new NoSuchIDToActivity(id)
      case Some(x) => x
    }
  }

  override def toString: String = s"$activities \n ImportName: $importName ExportName: $exportName Name $name"
}

object test {
  def main(args: Array[String]): Unit = {
//    val l = new PipelineRep("pipeline")
//    val a = ActivityRep("a", "b", "c", "d", "e")
//    val b = ActivityRep("b", "b", "c", "d", "e")
//    val c = ActivityRep("c", "b", "c", "d", "e")
//    val d = ActivityRep("d", "a")
//    val e = ActivityRep("e", "", "c", "d", "e")

//    l.addSingleActivity(a)
//    l.addSingleActivity(b)
//    l.addSingleActivity(c)
//    l.addSingleActivity(d)
//    l.addSingleActivity(e)
//    l.addConnection("a", "e")
//    l.addConnection("a", "c")
//    l.addConnection("b", List("a", "c"))
//    println(l)
  }
}

/**
  * When activities doesn't have unique id's
  *
  * Every Activity should have a unique ID given by the user
  */
class ActivitiesWithoutUniqueID extends Exception("Activities With Wrong ID")

/**
  * Some parameters in the creation of the activity aren't correct
  */
class ActivityWithWrongParameters extends Exception("Wrong Parameters in Activity")

/**
  * Some ID are not defined yet
  *
  * downstream and upstream keys should be placed in the end of the pln file
  */
class NoSuchIDToActivity(id: String) extends Exception("ID doesn't exist: " + id)
