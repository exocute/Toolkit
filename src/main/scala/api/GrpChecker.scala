package api

import com.zink.fly.FlyPrime
import exonode.clifton.Protocol._
import exonode.clifton.node.ExoEntry

import scala.collection.immutable.HashMap

/**
  * Created by #ScalaTeam on 04/01/2017.
  *
  * Check if the representation of the graph is no longer available
  * and replaces it with the default representation.
  */
class GrpChecker(grpId: String, actsId: Vector[String], space: FlyPrime) extends Thread {

  setDaemon(true)

  private val templateInitial = new ExoEntry(TABLE_MARKER, makeUniformTable(actsId))
  private val tmpl = new ExoEntry(TABLE_MARKER, null)

  override def run(): Unit = {

    // writes the default representation in the space
    space.write(templateInitial, INITIAL_TABLE_LEASE_TIME)

    while (true) {
      if (space.read(tmpl, GRP_CHECKER_TABLE_TIMEOUT) == null)
        space.write(templateInitial, INITIAL_TABLE_LEASE_TIME)

      Thread.sleep(GRP_CHECKER_SLEEP_TIME)
    }

  }

  def makeUniformTable(vec: Vector[String]): TableType = {
    HashMap(vec.map(v => v -> 0) :+ (ANALYSER_ACT_ID -> 0): _*)
  }


}
