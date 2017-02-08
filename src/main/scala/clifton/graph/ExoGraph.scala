package clifton.graph

import java.io.{File, Serializable}

import api.{Collector, Injector}
import clifton.graph.exceptions.InjectorTimeOutException
import distributer.JarSpaceUpdater
import exonode.clifton.Protocol._
import exonode.clifton.node.entries.{BackupEntry, BackupInfoEntry, DataEntry, ExoEntry}
import exonode.clifton.node.{Log, SpaceCache}
import toolkit.GraphRep

/**
  * Created by #ScalaTeam on 07-02-2017.
  */
class ExoGraph(jars: List[File], graph: GraphRep, graphId: String, graphTimeOut: Long) {

  private val REMOVE_DATA_LIMIT = 50

  val injector: Injector = {
    val cliftonInjector = new CliftonInjector(graphId + ":" + INJECT_SIGNAL_MARKER, graphId + ":" + graph.getRoot.get.id)
    new TimeOutInjector(cliftonInjector)
  }

  val collector: Collector = new CliftonCollector(graphId + ":" + COLLECT_SIGNAL_MARKER)

  private val jarUpdater = new JarSpaceUpdater()
  private val MIN_TIME_TO_RESET_TIMEOUT = math.min(60 * 1000, graphTimeOut)
  private val TIMEOUT = MIN_TIME_TO_RESET_TIMEOUT + graphTimeOut

  {
    updateAllEntries()

    val startMsg = "Graph is ready to receive injects"
    println(graphId + ";" + startMsg)
    Log.info(graphId, startMsg)
  }

  private var lastTime = System.currentTimeMillis()

  def updateAllEntries(): Unit = {
    updateJars(jars)
    GraphCreator.injectGraph(graph, graphId, TIMEOUT)
    updateGraphInSpace(graph, graphId)
  }

  def close(): Unit = {
    removeJars(jars)
    GraphCreator.removeGraph(graph, graphId)
    removeGraphFromSpace(graph, graphId)
    removeData(graph, graphId)

    val endMsg = "Graph is finished"
    println(graphId + ";" + endMsg)
    Log.info(graphId, endMsg)
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
      while (signalSpace.takeMany(DataEntry(fullId, null, null, null), REMOVE_DATA_LIMIT).nonEmpty) {}
      while (signalSpace.takeMany(BackupEntry(fullId, null, null, null), REMOVE_DATA_LIMIT).nonEmpty) {}
      while (signalSpace.takeMany(BackupInfoEntry(fullId, null, null), REMOVE_DATA_LIMIT).nonEmpty) {}
    }
  }

  private def removeGraphFromSpace(graph: GraphRep, graphId: String) = {
    val activities = graph.getActivities
    val exoEntry = ExoEntry(GRAPH_MARKER, (graphId, activities))
    SpaceCache.getSignalSpace.take(exoEntry, 0)
  }

  private class TimeOutInjector(injector: Injector) extends Injector {
    override def inject(input: Serializable): String = {
      if (tryResetTimeOut())
        injector.inject(input)
      else
        throw new InjectorTimeOutException()
    }

    override def inject(occurrences: Int, input: Serializable): Iterable[String] = {
      if (tryResetTimeOut())
        injector.inject(occurrences, input)
      else
        throw new InjectorTimeOutException()
    }

    override def injectMany(inputs: Iterable[Serializable]): Iterable[String] = {
      if (tryResetTimeOut())
        injector.injectMany(inputs)
      else
        throw new InjectorTimeOutException()
    }
  }

}
