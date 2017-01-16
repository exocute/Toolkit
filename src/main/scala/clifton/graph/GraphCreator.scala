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

  private var space: FlyPrime = SpaceCache.getSignalSpace

  def injectGraph(graph: GraphRep): Unit = {

    val injectMarker = Protocol.INJECT_SIGNAL_MARKER
    val collectMarker = Protocol.COLLECT_SIGNAL_MARKER

    val seenActivities = mutable.HashSet[String]()

    def addSignal(act: ActivityRep): Unit = {
      val signal = new ActivitySignal()

      signal.name = act.name
      signal.params = act.parameters

      val in = graph.getReverseConnections(act)
      if (in.isEmpty)
        signal.inMarkers = signal.inMarkers :+ injectMarker
      else
        in.foreach(nextAct => {
          val inMarker = nextAct.id
          signal.inMarkers = signal.inMarkers :+ inMarker
        })

      val out = graph.getConnections(act)
      if (out.isEmpty)
        signal.outMarkers = signal.outMarkers :+ collectMarker
      else
        out.foreach(prevAct => {
          val outMarker = prevAct.id
          signal.outMarkers = signal.outMarkers :+ outMarker
        })

      space.write(new ExoEntry(act.id, signal), Protocol.ACT_SIGNAL_LEASE_TIME)

      seenActivities += act.id
      for (nextAct <- graph.getConnections(act)) {
        if (!seenActivities.contains(nextAct.id))
          addSignal(nextAct)
      }
    }

    addSignal(graph.getRoot.get)
  }

}
