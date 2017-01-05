package api

import clifton.nodes.ExoEntry
import com.zink.fly.FlyPrime

import scala.collection.immutable.HashMap

/**
  * Created by #ScalaTeam on 04/01/2017.
  */
class GrpChecker(grp: GrpInfo, space: FlyPrime) extends Thread {

  private val UPDATETIME = 5 * 60 * 1000
  private val tmplInit = new ExoEntry("COUNT", makeTableExp(grp.actsID))
  private val tmpl = new ExoEntry("COUNT", null)
  private val INTERVALTIME = 60 * 1000

  override def run(): Unit = {

    space.write(tmplInit, UPDATETIME)

    while (true) {

      if (space.read(tmpl, UPDATETIME) == null)
        space.write(new ExoEntry("COUNT", makeUniformTable(grp.actsID)), UPDATETIME)

      Thread.sleep(INTERVALTIME)
    }

  }

  def makeUniformTable(vec: Vector[String]): HashMap[String, Double] = {
    val med = 1.0 / vec.size
    HashMap(vec.map(v => v -> med) :+ ("@" -> 0.0): _*)
  }

  def makeTableExp(vec: Vector[String]): HashMap[String, Double] = {
    if (vec.size <= 4)
      makeUniformTable(vec)
    else {
      val priorityActivities = 3
      val (vecPri, vectOthers) = vec.splitAt(priorityActivities)
      val medPri = 0.75 / vecPri.size
      val medOthers = 0.25 / vectOthers.size
      HashMap(vecPri.map(v => v -> medPri) ++ vectOthers.map(v => v -> medOthers): _*)
    }
  }

}
