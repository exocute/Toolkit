package clifton.signals

/**
  * Created by #ScalaTeam on 20-12-2016.
  */
class DeconfigureSignal extends Serializable {

  private var graphInstance: String = _

  def getGraphInstance: String = graphInstance

  def setGraphInstance(graphInstance: String) = this.graphInstance = graphInstance

  override def toString: String = "Deconfigure " + graphInstance
}
