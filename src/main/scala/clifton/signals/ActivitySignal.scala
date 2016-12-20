package clifton.signals

/**
  * Created by #ScalaTeam on 20-12-2016.
  */
class ActivitySignal extends Serializable {

  private var activity: String = _
  private var inMarker: String = _
  private var outMarker: String = _

  def getActivityName: String = activity

  def setActivityName(activity: String) = this.activity = activity

  def getInMarker: String = inMarker

  def setInMarker(inMarker: String) = this.inMarker = inMarker

  def getOutMarker: String = outMarker

  def setOutMarker(outMarker: String) = this.outMarker = outMarker

  override def toString: String = {
    val ret = new StringBuilder(64)
    ret.append("Activity :" + activity + "\n"
      + "In  " + inMarker + "\n"
      + "Out " + outMarker + "\n")
    ret.toString
  }
}
