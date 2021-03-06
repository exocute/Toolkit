package clifton.graph

import java.io.{File, Serializable}
import java.util.UUID

import api.{Collector, Injector}
import clifton.graph.exceptions.{InjectorTimeOutException, MissingActivitiesException}
import distributer.JarSpaceUpdater
import exonode.clifton.config.ProtocolConfig
import exonode.clifton.node.SpaceCache
import exonode.clifton.node.entries._
import exonode.clifton.signals.Log.{Log, LogStartedGraph}
import toolkit.ValidGraphRep

/**
  * Created by #GrowinScala
  */
class ExoGraphTimeOut(jars: List[File], val graph: ValidGraphRep, graphId: String, graphTimeOut: Long,
                      config: ProtocolConfig = ProtocolConfig.Default) extends ExoGraph {

  private val RemoveDataLimit = 50

  private val jarUpdater = new JarSpaceUpdater()
  private val MinTimeToResetTimeout = math.min(60 * 1000, graphTimeOut)
  private val Timeout = MinTimeToResetTimeout + graphTimeOut

  updateAllEntries()

  val (injector: Injector, collector: Collector) = {
    val injCollUUID = UUID.randomUUID().toString
    val rootActivities = graph.roots.map(rootActivity => graphId + ":" + rootActivity.id)
    val cliftonInjector = new TimeOutInjector(new CliftonInjector(
      injCollUUID, graphId + ":" + ProtocolConfig.InjectSignalMarker, rootActivities, () => graphReady, config))
    val cliftonCollector = new CliftonCollector(injCollUUID, graphId + ":" + ProtocolConfig.CollectSignalMarker,
      graph.depthOfFlatMaps, () => graphReady)

    (cliftonInjector, cliftonCollector)
  }

  {
    val startMsg = s"Graph <${graph.name}> is ready to receive injects"
    println(graphId + ";" + startMsg)
    Log.writeLog(LogStartedGraph(graphId, graph.name))
  }

  private var lastTime = System.currentTimeMillis()
  private var graphReady = true

  private def verifyJarsAndGraphActivities(): Unit = {
    val jarActivities =
      for {
        jar <- jars
        act <- jarUpdater.getAllClassEntries(jar)
      } yield act
    val activities = graph.getActivities.map(_.className)
    val missingActivities = activities.toSet.diff(jarActivities.toSet)
    if (missingActivities.nonEmpty)
      throw new MissingActivitiesException(missingActivities)
  }

  private def updateAllEntries(): Unit = {
    verifyJarsAndGraphActivities()
    updateJars(jars)
    GraphCreator.injectGraph(graph, graphId, Timeout)
    updateGraphInSpace(graph, graphId)
  }

  def closeGraph(): Unit = {
    removeJars(jars)
    removeGraphFromSpace(graph, graphId)
    GraphCreator.removeGraph(graph, graphId)
    removeData(graph, graphId)

    val endMsg = "Graph is finished"
    println(graphId + ";" + endMsg)

    graphReady = false
  }

  private def tryResetTimeOut(): Boolean = {
    val currentTime = System.currentTimeMillis()
    if (currentTime - lastTime > MinTimeToResetTimeout) {
      if (currentTime - lastTime > Timeout) {
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
    jars.foreach(file => jarUpdater.update(file, Timeout))
  }

  private def removeJars(jars: List[File]): Unit = {
    jars.foreach(file => jarUpdater.remove(file))
  }

  private def updateGraphInSpace(graph: ValidGraphRep, graphId: String) = {
    val activities = graph.getActivities.map(_.id).toVector
    val graphEntry = GraphEntry(graphId, activities)
    val signalSpace = SpaceCache.getSignalSpace
    signalSpace.take(graphEntry, 0)
    signalSpace.write(graphEntry, Timeout)
  }

  private def removeData(graph: ValidGraphRep, graphId: String): Unit = {
    val signalSpace = SpaceCache.getSignalSpace

    def remove(actId: String) = {
      val fullId = graphId + ":" + actId
      while (signalSpace.takeMany(DataEntry(fullId, null, null, null, null), RemoveDataLimit).nonEmpty) {}
      while (signalSpace.takeMany(BackupEntry(fullId, null, null, null, null), RemoveDataLimit).nonEmpty) {}
      while (signalSpace.takeMany(BackupInfoEntry(fullId, null, null, null), RemoveDataLimit).nonEmpty) {}
      while (signalSpace.takeMany(FlatMapEntry(graphId, null, null), RemoveDataLimit).nonEmpty) {}
    }

    remove(ProtocolConfig.CollectSignalMarker)
    for (actId <- graph.getActivities.map(_.id))
      remove(actId)
  }

  private def removeGraphFromSpace(graph: ValidGraphRep, graphId: String): Unit = {
    val graphEntry = GraphEntry(graphId, null)
    SpaceCache.getSignalSpace.take(graphEntry, 0)
  }

  private class TimeOutInjector(injector: Injector) extends Injector {
    override def canInject: () => Boolean = { () =>
      injector.canInject() && {
        val currentTime = System.currentTimeMillis()
        currentTime - lastTime > Timeout
      }
    }

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
