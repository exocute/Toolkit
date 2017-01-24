package clifton.graph

import java.io.Serializable

import api.Collector
import clifton.graph.exceptions.CollectException
import com.zink.fly.FlyPrime
import exonode.clifton.node.SpaceCache
import exonode.clifton.node.entries.DataEntry

/**
  * Created by #ScalaTeam on 21/12/2016.
  *
  * collects results from the space saved in dataEntries with a < marker
  */
class CliftonCollector(val marker: String) extends Collector {

  private val template: DataEntry = DataEntry(marker, null, null, null)

  def collect(): Option[Serializable] = {
    collect(0L)
  }

  def collect(waitTime: Long): Option[Serializable] = {
    val space = SpaceCache.getDataSpace
    try {
      val ent = space.take(template, waitTime)
      ent.map(_.data)
    } catch {
      case e: Exception => throw new CollectException("Collector Error")
    }
  }

  def collect(numObjects: Int, waitTime: Long): List[Serializable] = {
    var serializable: List[Serializable] = Nil
    val start = System.currentTimeMillis()
    var remainingTime = waitTime
    var totalObjects = 0

    while (totalObjects < numObjects && remainingTime > 1L) {
      val ser: Option[Serializable] = collect()
      if (ser.isDefined) {
        serializable = ser.get :: serializable
        totalObjects += 1
      } else {
        try {
          Thread.sleep(1)
        } catch {
          case e: InterruptedException => e.printStackTrace()
        }
      }
      remainingTime = waitTime - (System.currentTimeMillis() - start)
    }
    serializable.reverse
  }

}
