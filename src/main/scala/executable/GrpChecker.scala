package executable

import com.zink.scala.fly.ScalaFly
import exonode.clifton.Protocol._
import exonode.clifton.node.{ExoEntry, SpaceCache}

import scala.collection.immutable.HashMap

/**
  * Created by #ScalaTeam on 04/01/2017.
  *
  * Check if the representation of the graph is no longer available
  * and replaces it with the default representation.
  */
class GrpChecker() extends Thread {

  setDaemon(true)

  private val space: ScalaFly = SpaceCache.getSignalSpace

  private val initialTableTemplate = ExoEntry(TABLE_MARKER, HashMap[String, Int]())
  private val anyTableTemplate = ExoEntry(TABLE_MARKER, null)

  override def run(): Unit = {

    // writes the default representation in the space if there is none
    if (space.read(anyTableTemplate, 0).isEmpty)
      space.write(initialTableTemplate, INITIAL_TABLE_LEASE_TIME)

    while (true) {
      if (space.read(anyTableTemplate, GRP_CHECKER_TABLE_TIMEOUT).isEmpty)
        space.write(initialTableTemplate, INITIAL_TABLE_LEASE_TIME)

      Thread.sleep(GRP_CHECKER_SLEEP_TIME)
    }

  }
}
