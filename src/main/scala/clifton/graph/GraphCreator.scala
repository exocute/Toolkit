package clifton.graph

import java.util.UUID

import clifton.nodes.SignalOutChannel
import clifton.signals.ActivitySignal
import toolkit.{ActivityRep, GraphRep}

/**
  * Created by #ScalaTeam on 20/12/2016.
  */
class GraphCreator {

  private var injectMarker: String = _
  private var collectMarker: String = _


  def injectGraph(graph: GraphRep): Unit = {
    val outChannel = new SignalOutChannel(clifton.inoSignal)

    val graphName = graph.name
    val graphInstance: String = graphName + ":" + generateUUID + ":"
    injectMarker = graphInstance + clifton.inoSignal
    collectMarker = graphInstance + clifton.exoSignal

    def addSignal(act: ActivityRep): Unit = {
      val signal = new ActivitySignal

      signal.setActivityName(act.name + ":" + act.parameters.mkString(":"))

      val in = graph.getConnections(act)
      if (in.isEmpty)
        signal.addInMarker(injectMarker)
      else
        in.foreach(nextAct => {
          val inMarker = graphInstance + nextAct.id
          signal.addInMarker(inMarker)
        })

      val out = graph.getReverseConnections(act)
      if (out.isEmpty)
        signal.addOutMarker(collectMarker)
      else
        out.foreach(prevAct => {
          val outMarker = graphInstance + prevAct.id
          signal.addOutMarker(outMarker)
        })

      outChannel.putObject(signal)
      println(s"Sending signal: $signal")

      graph.getConnections(act).foreach(addSignal)
    }

    addSignal(graph.getRoot.get)
  }

  def getInjectMarker: String = injectMarker

  def getCollectMarker: String = collectMarker

  private def generateUUID: String = UUID.randomUUID().toString

}
