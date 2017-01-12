package api

import com.zink.fly.FlyPrime
import exonode.clifton.Protocol._
import exonode.clifton.node.ExoEntry

import scala.collection.immutable.HashMap

/**
  * Created by #ScalaTeam on 04/01/2017.
  */
class GrpChecker(grp: GrpInfo, space: FlyPrime) extends Thread {

  private val UPDATETIME = 2 * 60 * 1000
  private val tmplInit = new ExoEntry(TABLE_MARKER, makeUniformTable(grp.actsID))
  private val tmpl = new ExoEntry(TABLE_MARKER, null)
  private val INTERVALTIME = 60 * 1000

  override def run(): Unit = {

    space.write(new ExoEntry(TABLE_MARKER, makeUniformTable(grp.actsID)), UPDATETIME)

    while (true) {

      if (space.read(tmpl, UPDATETIME) == null)
        space.write(new ExoEntry(TABLE_MARKER, makeUniformTable(grp.actsID)), UPDATETIME)

      Thread.sleep(INTERVALTIME)
    }

  }

  def makeUniformTable(vec: Vector[String]): TableType = {
    HashMap(vec.map(v => v -> 0) :+ (ANALISER_ID -> 0): _*)
  }


}
