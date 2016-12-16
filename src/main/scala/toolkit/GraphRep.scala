package toolkit

import exceptions.{ActivitiesWithoutUniqueID, ActivityWithWrongParameters, ImportAndExportParametersInConsecutiveActivitiesNotMatch, NoSuchIDToActivity}

/**
  * Created by Eduardo Rodrigues on 12/12/2016.
  */

class GraphRep(name: String, var importName: String, var exportName: String, activitiesGraph: GraphImplementation) {

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
    if (activitiesGraph.addNode(activityRep)) {
      if (lastACT == null) {
        lastACT = activityRep
      }
      else {
        if (activitiesGraph.addEdge(lastACT, activityRep))
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
    if (!activitiesGraph.addNode(activityRep))
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
      if !activitiesGraph.hasActivity(x)
    } throw new NoSuchIDToActivity(x)

    //remove all edges
    for {
      x <- activityRepFrom
    } if (!activitiesGraph.removeAllEdge(activityById(x))) throw new ActivityWithWrongParameters

    //add new edges to make join
    for {
      x <- activityRepFrom
    } if (!activitiesGraph.addEdge(activityById(x), actTo)) throw new ActivityWithWrongParameters
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
      if !activitiesGraph.hasActivity(x)
    } throw new NoSuchIDToActivity(x)

    //get an activity by name
    if (!activitiesGraph.removeAllEdge(actFrom))
      throw new ActivityWithWrongParameters

    //add new edges to make fork
    for {
      x <- activityRepTo
    } if (!activitiesGraph.addEdge(actFrom, activityById(x))) throw new ActivityWithWrongParameters
  }


  /**
    * adds a single connection between activities
    *
    * @param activityFrom
    * @param activityTo
    */
  def addConnection(activityFrom: String, activityTo: String) = {
    if (!activitiesGraph.addEdge(activityById(activityFrom), activityById(activityTo)) && !validConnection(activityFrom,activityTo))
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
    } if (!activitiesGraph.addEdge(from, activityById(to)) || !validConnection(activityFrom,activityTo))
          throw new ActivityWithWrongParameters
  }

  /**
    * get Root activity
    * @return
    */
  def getRoot = activitiesGraph.getRoot

  /**
    * verifies is the graph has a Root
    * @return
    */
  def hasRoot = activitiesGraph.hasRoot


  /**
    * get Sink activity
    * @return
    */
  def getSink = activitiesGraph.getSink

  /**
    * verifies is the Graph has a Sink
    * @return
    */
  def hasSink = activitiesGraph.hasSink

  /**
    * checks the import and export parameters of a connection
    * @param activityFrom
    * @param activityTo
    * @return true if its valid, false otherwise
    */
  def validConnection(activityFrom: String, activityTo: String) : Boolean = {
    val from = activityById(activityFrom).exportName
    val to = activityById(activityTo).importName
    if(to.size==1)
      from == to.head
    else if(to.size == 2)
      from == to.head || from == to.tail.head
    else
      true
  }

  /**
    * checks the import and export parameters of a connection
    * @param activityFrom
    * @param activityTo
    * @return true if its valid, false otherwise
    */
  def validConnection(activityFrom: String, activityTo: List[String]) : Boolean = {
    if(!validConnection(activityFrom,activityTo.head) && validConnection(activityFrom,activityTo.tail))
      throw new ImportAndExportParametersInConsecutiveActivitiesNotMatch
    else true
  }

  /**
    * verifies if the parameter's value to export it's the same has the export parameter of the Sink
    * @param value
    * @param exportName
    * @return
    */
  def validCollector(value: ActivityRep, exportName: String) : Boolean = if(exportName.isEmpty || value.exportName.isEmpty) true  else  value.exportName == exportName

  /**
    * verifies if the parameter's value to import it's the same has the import parameter of the Root
    * @param value
    * @param importName
    * @return
    */
  def validInjector(value: ActivityRep, importName: String) : Boolean = if(importName.isEmpty || value.importName.isEmpty) true  else value.importName.head == importName

  /**
    * checks if a graph has a Sink, Root and do not has cycles or subgraphs
    * @return true if it's a valid graph, false otherwise
    */
  def checkValidGraph() : Boolean = {
    if(activitiesGraph.numberNodes==1 && (!activitiesGraph.hasSink || !activitiesGraph.hasRoot || !validConnection(activitiesGraph.getRoot.get.id, activitiesGraph.getSink.get.id)))
      false
    else
      if(!activitiesGraph.hasSink || !activitiesGraph.hasRoot || activitiesGraph.hasCyclesAndSubGraphs
        || !validInjector(activitiesGraph.getRoot.get,importName) || !validCollector(activitiesGraph.getSink.get,exportName))
        false
    else true
  }

  /**
    * @param id - ID of Activity
    * @return - ActivityRep with ID
    */
  def activityById(id: String): ActivityRep = {
    val act = activitiesGraph.getActById(id)
    act match {
      case None => throw new NoSuchIDToActivity(id)
      case Some(x) => x
    }
  }

  /**
    * gets the next activities from a single activity
    * @param activity
    * @return next activities
    */
  def getConnections(activity: ActivityRep) = activitiesGraph.getAdj(activity)

  /**
    * compares the number of nodes that references a single activity
    * @param activityRep
    * @return if two activities references that activity it's true because we have a join, false otherwise
    */
  def isJoin(activityRep: ActivityRep) : Boolean = activitiesGraph.referencedByNodes(activityRep)==2

  /**
    * returns the string of a value if the value its defined
    * @param s
    * @param value
    * @return
    */
  def showIfNotEmpty(s: String, value: String) = if (!value.isEmpty) s + ": " + value + "\n" else ""

  override def toString: String = s"Name: $name\n" +
    showIfNotEmpty("ImportName", importName) + showIfNotEmpty("ExportName", exportName) + s"$activitiesGraph"
}
