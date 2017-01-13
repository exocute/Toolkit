package api

import com.zink.fly.FlyPrime
import exonode.clifton.Protocol._
import exonode.clifton.node.ExoEntry

import scala.collection.immutable.HashMap

/**
  * Created by #ScalaTeam on 04/01/2017.
  */
class GrpChecker(grp: GrpInfo, space: FlyPrime) extends Thread {

  private val tmplInit = new ExoEntry(TABLE_MARKER, makeUniformTable(grp.actsId))
  private val tmpl = new ExoEntry(TABLE_MARKER, null)

  override def run(): Unit = {

    space.write(new ExoEntry(TABLE_MARKER, makeUniformTable(grp.actsId)), INITIAL_TABLE_LEASE_TIME)

    while (true) {

      if (space.read(tmpl, GRP_CHECKER_TABLE_TIMEOUT) == null)
        space.write(new ExoEntry(TABLE_MARKER, makeUniformTable(grp.actsId)), INITIAL_TABLE_LEASE_TIME)

      Thread.sleep(GRP_CHECKER_SLEEP_TIME)
    }

  }

  def makeUniformTable(vec: Vector[String]): TableType = {
    HashMap(vec.map(v => v -> 0) :+ (ANALISER_ACT_ID -> 0): _*)
  }


}
