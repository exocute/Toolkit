package clifton.signals

/**
  * Created by #ScalaTeam on 20-12-2016.
  */
class KillSignal extends Serializable {

  private var killNow: Boolean = _

  def setKillNow() = killNow = true

  def setKillGracefull() = killNow = false

  def isKillNow: Boolean = killNow

  def isKillGracefull: Boolean = !killNow
}
