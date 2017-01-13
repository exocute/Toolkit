package clifton.graph

import java.util.UUID

import clifton.nodes.SignalOutChannel
import com.zink.fly.FlyPrime
import exonode.clifton.Protocol
import exonode.clifton.node.{ExoEntry, SpaceCache}
import exonode.clifton.signals.ActivitySignal
import toolkit.{ActivityRep, GraphRep}

import scala.collection.mutable

/**
  * Created by #ScalaTeam on 20/12/2016.
  */
class GraphCreator {

  private var space: FlyPrime = SpaceCache.getSignalSpace

  def injectGraph(graph: GraphRep): Unit = {
    //val graphName = graph.names
    //val graphInstance: String = graphName + ":" + generateUUID + ":"

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

      //      println(signal)
      //outChannel.putObject(signal)
      space.write(new ExoEntry(act.id, signal), 60 * 60 * 1000) //FIXME: use Protocol.ACT_SIGNAL_LEASE_TIME

      //println(s"Sending signal: $signal")

      seenActivities += act.id
      for (nextAct <- graph.getConnections(act)) {
        if (!seenActivities.contains(nextAct.id))
          addSignal(nextAct)
      }
    }

    addSignal(graph.getRoot.get)
  }

}
