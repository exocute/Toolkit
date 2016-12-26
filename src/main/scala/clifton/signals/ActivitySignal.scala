package clifton.signals

/**
  * Created by #ScalaTeam on 20-12-2016.
  */
class ActivitySignal(var name: String, var params: Vector[String], var inMarkers: Vector[String], var outMarkers: Vector[String]) extends Serializable {

  def this() = this("", Vector[String](), Vector[String](), Vector[String]())

  //  private  = _
  //  private var inMarkers: Vector[String] = Vector[String]()
  //  private var outMarkers: Vector[String] = Vector[String]()

  //  def getActivityName: String = activity
  //
  //  def setActivityName(activity: String): Unit = this.activity = activity
  //
  //  def getInMarker: Vector[String] = inMarkers
  //
  //  def addInMarker(inMarker: String): Unit =
  //    this.inMarkers = this.inMarkers :+ inMarker
  //
  //  def getOutMarker: Vector[String] = outMarkers
  //
  //  def addOutMarker(outMarker: String): Unit =
  //    this.outMarkers = this.outMarkers :+ outMarker

  override def toString: String = {
    val ret = new StringBuilder(64)
    ret.append("Activity :" + name + "\n"
      + "Params " + params.mkString(", ") + "\n"
      + "In  " + inMarkers.mkString(", ") + "\n"
      + "Out " + outMarkers.mkString(", ") + "\n")
    ret.toString
  }
}
