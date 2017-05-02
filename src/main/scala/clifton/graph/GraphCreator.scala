package clifton.graph

import exonode.clifton.config.ProtocolConfig
import exonode.clifton.node.SpaceCache
import exonode.clifton.node.entries.ExoEntry
import exonode.clifton.signals.ActivitySignal
import toolkit.{ActivityRep, ValidGraphRep}

import scala.collection.mutable

/**
  * Created by #GrowinScala
  *
  * Receives a graph representation and writes the ActivitySignals into the space
  */
object GraphCreator {

  def injectGraph(graph: ValidGraphRep, graphId: String, leaseTime: Long): Unit = {
    val seenActivities = mutable.HashSet[String]()
    val signalSpace = SpaceCache.getSignalSpace

    def addSignal(act: ActivityRep): Unit = {
      val name = act.className
      val params = act.parameters

      val inActivities = graph.getReverseConnections(act)

      val inMarkers =
        if (inActivities.isEmpty) {
          Vector(graphId + ":" + ProtocolConfig.INJECT_SIGNAL_MARKER)
        } else {
          inActivities.foldLeft(Vector[String]())((vector, nextAct) => {
            val inMarker = graphId + ":" + nextAct.id
            vector :+ inMarker
          })
        }

      val outActivities = graph.getConnections(act)
      val outMarkers =
        if (outActivities.isEmpty) {
          Vector(graphId + ":" + ProtocolConfig.COLLECT_SIGNAL_MARKER)
        } else {
          outActivities.foldLeft(Vector[String]())((vector, prevAct) => {
            val outMarker = graphId + ":" + prevAct.id
            vector :+ outMarker
          })
        }

      val signal = ActivitySignal(name, act.actType, params, inMarkers, outMarkers)
      val fullId = graphId + ":" + act.id
      signalSpace.take(ExoEntry(fullId, signal), 0)
      signalSpace.write(ExoEntry(fullId, signal), leaseTime)

      seenActivities += act.id
      for (nextAct <- graph.getConnections(act)) {
        if (!seenActivities.contains(nextAct.id))
          addSignal(nextAct)
      }
    }

    graph.roots.foreach(addSignal)
  }

  def removeGraph(graph: ValidGraphRep, graphId: String): Unit = {
    val signalSpace = SpaceCache.getSignalSpace
    for (activity <- graph.getActivities) {
      signalSpace.take(ExoEntry(graphId + ":" + activity.id, null), 0)
    }
  }

}
