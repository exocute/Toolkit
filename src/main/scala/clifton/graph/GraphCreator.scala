package clifton.graph

import com.zink.fly.FlyPrime
import exonode.clifton.Protocol
import exonode.clifton.node.{ExoEntry, SpaceCache}
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

  def injectGraph(graph: GraphRep): Unit = {

    val injectMarker = Protocol.INJECT_SIGNAL_MARKER
    val collectMarker = Protocol.COLLECT_SIGNAL_MARKER

    val seenActivities = mutable.HashSet[String]()

    def addSignal(act: ActivityRep): Unit = {
      val name = act.name
      val params = act.parameters

      val inActivities = graph.getReverseConnections(act)

      val inMarkers =
        if (inActivities.isEmpty) {
          Vector(injectMarker)
        } else {
          inActivities.foldLeft(Vector[String]())((vector, nextAct) => {
            val inMarker = nextAct.id
            vector :+ inMarker
          })
        }

      val outActivities = graph.getConnections(act)
      val outMarkers =
        if (outActivities.isEmpty) {
          Vector(collectMarker)
        } else {
          outActivities.foldLeft(Vector[String]())((vector, prevAct) => {
            val outMarker = prevAct.id
            vector :+ outMarker
          })
        }

      val signal = ActivitySignal(name, params, inMarkers, outMarkers)
      space.write(ExoEntry(act.id, signal), Protocol.ACT_SIGNAL_LEASE_TIME)

      seenActivities += act.id
      for (nextAct <- graph.getConnections(act)) {
        if (!seenActivities.contains(nextAct.id))
          addSignal(nextAct)
      }
    }

    addSignal(graph.getRoot.get)
  }

}
