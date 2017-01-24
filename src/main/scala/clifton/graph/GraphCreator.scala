package clifton.graph

import exonode.clifton.Protocol._
import exonode.clifton.node.SpaceCache
import exonode.clifton.node.entries.ExoEntry
import exonode.clifton.signals.ActivitySignal
import toolkit.{ActivityRep, GraphRep}

import scala.collection.mutable

/**
  * Created by #ScalaTeam on 20/12/2016.
  *
  * Receives a graph rep and adds to space the representation
  */
class GraphCreator {

  private val space = SpaceCache.getSignalSpace

  def injectGraph(graph: GraphRep, graphId: String): Unit = {
    val seenActivities = mutable.HashSet[String]()

    def addSignal(act: ActivityRep): Unit = {
      val name = act.name
      val params = act.parameters

      val inActivities = graph.getReverseConnections(act)

      val inMarkers =
        if (inActivities.isEmpty) {
          Vector(graphId + ":" + INJECT_SIGNAL_MARKER)
        } else {
          inActivities.foldLeft(Vector[String]())((vector, nextAct) => {
            val inMarker = graphId + ":" + nextAct.id
            vector :+ inMarker
          })
        }

      val outActivities = graph.getConnections(act)
      val outMarkers =
        if (outActivities.isEmpty) {
          Vector(graphId + ":" + COLLECT_SIGNAL_MARKER)
        } else {
          outActivities.foldLeft(Vector[String]())((vector, prevAct) => {
            val outMarker = graphId + ":" + prevAct.id
            vector :+ outMarker
          })
        }

      val signal = ActivitySignal(name, params, inMarkers, outMarkers)
      space.write(ExoEntry(graphId + ":" + act.id, signal), ACT_SIGNAL_LEASE_TIME)

      seenActivities += act.id
      for (nextAct <- graph.getConnections(act)) {
        if (!seenActivities.contains(nextAct.id))
          addSignal(nextAct)
      }
    }

    addSignal(graph.getRoot.get)
  }

}
