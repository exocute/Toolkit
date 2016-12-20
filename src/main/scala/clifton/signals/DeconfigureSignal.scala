package clifton.signals

/**
  * Created by #ScalaTeam on 20-12-2016.
  */
class DeconfigureSignal extends Serializable {

  private var graphInstance: String = _

  def getPipelineInstance: String = graphInstance

  def setPipelineInstance(graphInstance: String) = this.graphInstance = graphInstance

  override def toString: String = "Deconfigure " + graphInstance
}
