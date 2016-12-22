package clifton.signals

/**
  * Created by #ScalaTeam on 20-12-2016.
  */
class ActivitySignal extends Serializable {

  private var activity: String = _
  private var inMarkers: Vector[String] = Vector[String]()
  private var outMarkers: Vector[String] = Vector[String]()

  def getActivityName: String = activity

  def setActivityName(activity: String): Unit = this.activity = activity

  def getInMarker: Vector[String] = inMarkers

  def addInMarker(inMarker: String): Unit =
    this.inMarkers = this.inMarkers :+ inMarker

  def getOutMarker: Vector[String] = outMarkers

  def addOutMarker(outMarker: String): Unit =
    this.outMarkers = this.outMarkers :+ outMarker

  override def toString: String = {
    val ret = new StringBuilder(64)
    ret.append("Activity :" + activity + "\n"
      + "In  " + inMarkers.mkString(", ") + "\n"
      + "Out " + outMarkers.mkString(", ") + "\n")
    ret.toString
  }
}
