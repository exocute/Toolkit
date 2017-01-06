package clifton.graph

import java.util.UUID

import clifton.nodes.{ExoEntry, SignalOutChannel, SpaceCache}
import clifton.signals.ActivitySignal
import com.zink.fly.FlyPrime
import toolkit.{ActivityRep, GraphRep}

import scala.collection.mutable

/**
  * Created by #ScalaTeam on 20/12/2016.
  */
class GraphCreator {

  private var injectMarker: String = _
  private var collectMarker: String = _
  private var space: FlyPrime = _

  def putObject(signal: ExoEntry) = {
    space.write(signal, 60 * 60 * 1000)
  }

  def injectGraph(graph: GraphRep): Unit = {
    val outChannel = new SignalOutChannel(clifton.inoSignal)

    val graphName = graph.name
    val graphInstance: String = graphName + ":" + generateUUID + ":"
    injectMarker = clifton.inoSignal
    collectMarker = clifton.exoSignal
    space = SpaceCache.getSignalSpace

    var seenActivities = mutable.HashSet[String]()

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

      println(signal)

      //outChannel.putObject(signal)
      putObject(new ExoEntry(act.id,signal))

      //println(s"Sending signal: $signal")

      seenActivities += act.id
      graph.getConnections(act).foreach { nextAct =>
        if (!seenActivities.contains(nextAct.id))
          addSignal(nextAct)
      }
    }

    addSignal(graph.getRoot.get)
  }

  def getInjectMarker: String = injectMarker

  def getCollectMarker: String = collectMarker

  private def generateUUID: String = UUID.randomUUID().toString

}
