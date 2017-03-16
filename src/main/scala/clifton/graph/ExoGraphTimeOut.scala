package clifton.graph

import java.io.{File, Serializable}
import java.util.UUID

import api.{Collector, Injector}
import clifton.graph.exceptions.{InjectorTimeOutException, MissingActivitiesException}
import distributer.JarSpaceUpdater
import exonode.clifton.config.Protocol._
import exonode.clifton.node.Log.{INFO, ND}
import exonode.clifton.node.entries.{BackupEntry, BackupInfoEntry, DataEntry, ExoEntry}
import exonode.clifton.node.{Log, SpaceCache}
import exonode.clifton.signals.LoggingSignal
import toolkit.GraphRep

/**
  * Created by #ScalaTeam on 07-02-2017.
  */
class ExoGraphTimeOut(jars: List[File], val graph: GraphRep, graphId: String, graphTimeOut: Long) extends ExoGraph {

  private val REMOVE_DATA_LIMIT = 50

  private val jarUpdater = new JarSpaceUpdater()
  private val MIN_TIME_TO_RESET_TIMEOUT = math.min(60 * 1000, graphTimeOut)
  private val TIMEOUT = MIN_TIME_TO_RESET_TIMEOUT + graphTimeOut

  updateAllEntries()

  val (injector: Injector, collector: Collector) = {
    val injCollUUID = UUID.randomUUID().toString
    val cliftonInjector = new TimeOutInjector(new CliftonInjector(
      injCollUUID, graphId + ":" + INJECT_SIGNAL_MARKER, graphId + ":" + graph.getRoot.get.id))
    val cliftonCollector = new CliftonCollector(injCollUUID, graphId + ":" + COLLECT_SIGNAL_MARKER)

    (cliftonInjector, cliftonCollector)
  }

  {
    val startMsg = s"Graph <${graph.name}> is ready to receive injects"
    println(graphId + ";" + startMsg)
    Log.receiveLog(LoggingSignal(LOGCODE_STARTED_GRAPH, INFO, ND, graphId+":"+graph.name, ND, ND, ND, "Graph Started - " + graph.name, 0))
  }

  private var lastTime = System.currentTimeMillis()

  private def verifyJarsAndGraphActivities(): Unit = {
    val jarActivities =
      for {
        jar <- jars
        act <- jarUpdater.getAllClassEntries(jar)
      } yield act
    val graphActivities = graph.getAllActivitiesNames
    val diffActivities = graphActivities.toSet.diff(jarActivities.toSet)
    if (diffActivities.nonEmpty)
      throw new MissingActivitiesException(diffActivities)
  }

  private def updateAllEntries(): Unit = {
    verifyJarsAndGraphActivities()
    updateJars(jars)
    GraphCreator.injectGraph(graph, graphId, TIMEOUT)
    updateGraphInSpace(graph, graphId)
  }

  def closeGraph(): Unit = {
    removeJars(jars)
    GraphCreator.removeGraph(graph, graphId)
    removeGraphFromSpace(graph, graphId)
    removeData(graph, graphId)

    val endMsg = "Graph is finished"
    println(graphId + ";" + endMsg)
    //Log.info(graphId, endMsg)
  }

  private def tryResetTimeOut(): Boolean = {
    val currentTime = System.currentTimeMillis()
    if (currentTime - lastTime > MIN_TIME_TO_RESET_TIMEOUT) {
      if (currentTime - lastTime > TIMEOUT) {
        // Graph is invalid (has timed out)
        false
      } else {
        lastTime = currentTime
        updateAllEntries()
        true
      }
    } else
      true
  }

  private def updateJars(jars: List[File]): Unit = {
    jars.foreach(file => jarUpdater.update(file, TIMEOUT))
  }

  private def removeJars(jars: List[File]): Unit = {
    jars.foreach(file => jarUpdater.remove(file))
  }

  private def updateGraphInSpace(graph: GraphRep, graphId: String) = {
    val activities = graph.getActivities
    val exoEntry = ExoEntry(GRAPH_MARKER, (graphId, activities))
    val signalSpace = SpaceCache.getSignalSpace
    signalSpace.take(exoEntry, 0)
    signalSpace.write(exoEntry, TIMEOUT)
  }

  private def removeData(graph: GraphRep, graphId: String): Unit = {
    val signalSpace = SpaceCache.getSignalSpace
    for (actId <- graph.getActivities :+ COLLECT_SIGNAL_MARKER) {
      val fullId = graphId + ":" + actId
      while (signalSpace.takeMany(DataEntry(fullId, null, null, null, null), REMOVE_DATA_LIMIT).nonEmpty) {}
      while (signalSpace.takeMany(BackupEntry(fullId, null, null, null, null), REMOVE_DATA_LIMIT).nonEmpty) {}
      while (signalSpace.takeMany(BackupInfoEntry(fullId, null, null, null), REMOVE_DATA_LIMIT).nonEmpty) {}
    }
  }

  private def removeGraphFromSpace(graph: GraphRep, graphId: String) = {
    val activities = graph.getActivities
    val exoEntry = ExoEntry(GRAPH_MARKER, (graphId, activities))
    SpaceCache.getSignalSpace.take(exoEntry, 0)
  }

  private class TimeOutInjector(injector: Injector) extends Injector {
    override def inject(input: Serializable): Int = {
      if (tryResetTimeOut())
        injector.inject(input)
      else
        throw new InjectorTimeOutException()
    }

    override def inject(occurrences: Int, input: Serializable): Iterable[Int] = {
      if (tryResetTimeOut())
        injector.inject(occurrences, input)
      else
        throw new InjectorTimeOutException()
    }

    override def injectMany(inputs: Iterable[Serializable]): Vector[Int] = {
      if (tryResetTimeOut())
        injector.injectMany(inputs)
      else
        throw new InjectorTimeOutException()
    }
  }

}
