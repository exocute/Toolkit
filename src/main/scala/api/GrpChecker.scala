package api

import clifton.nodes.ExoEntry
import com.zink.fly.FlyPrime

import scala.collection.immutable.HashMap

/**
  * Created by #ScalaTeam on 04/01/2017.
  */
class GrpChecker(grp: GrpInfo, space: FlyPrime) extends Thread {

  private val UPDATETIME = 5 * 60 * 1000
  private val tmplInit = new ExoEntry("TABLE", makeUniformTable(grp.actsID))
  private val tmpl = new ExoEntry("TABLE", null)
  private val INTERVALTIME = 60 * 1000

  override def run(): Unit = {

    space.write(new ExoEntry("TABLE", makeUniformTable(grp.actsID)), UPDATETIME)

    while (true) {

      if (space.read(tmpl, UPDATETIME) == null)
        space.write(new ExoEntry("TABLE", makeUniformTable(grp.actsID)), UPDATETIME)

      Thread.sleep(INTERVALTIME)
    }

  }

  def makeUniformTable(vec: Vector[String]): HashMap[String, (Double,Double)] = {
    val med = 1.0 / vec.size
    HashMap(vec.map(v => v -> (med,0.0)) :+ ("@" -> (0.0,0.0)): _*)
  }



}
