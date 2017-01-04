package api

import clifton.nodes.ExoEntry
import com.zink.fly.FlyPrime

import scala.collection.immutable.HashMap

/**
  * Created by #ScalaTeam on 04/01/2017.
  */
class grpChecker(grp: grpInfo, space: FlyPrime) extends Thread {

  val UPDATETIME = 5 * 60 * 1000
  val tmplInit = new ExoEntry("COUNT", makeTableExp(grp.getActID))
  val tmpl = new ExoEntry("COUNT", null)
  val INTERVALTIME = 60 * 1000

  override def run() = {

    space.write(tmplInit, UPDATETIME)

    while (true) {

      if (space.read(tmpl, UPDATETIME) == null)
        space.write(new ExoEntry("COUNT",makeUniformTable(grp.getActID)), UPDATETIME)

      Thread.sleep(INTERVALTIME)
    }

  }

  def makeUniformTable(vec: Vector[String]): HashMap[String, Double] = {
    val med = 1.0 / vec.size
    HashMap(vec.map(v => v -> med) :+ ("@" -> 0.0): _*)
  }

  def makeTableExp(vec: Vector[String]): HashMap[String, Double] = {
    if (vec.size <= 3)
      makeUniformTable(vec)
    else {
      val (vec3, vectOthers) = vec.splitAt(3)
      val med = 0.25 / vectOthers.size
      HashMap(vec3.map(v => v -> 0.25) ++ vectOthers.map(v => v -> med): _*)
    }
  }

}
