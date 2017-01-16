package clifton.graph

import java.io.Serializable

import clifton.graph.exceptions.CollectException
import com.zink.fly.FlyPrime
import exonode.clifton.node.{DataEntry, SpaceCache}

/**
  * Created by #ScalaTeam on 21/12/2016.
  *
  * collects results from the space saved in dataEntries with a < marker
  */
class CliftonCollector(val marker: String) {

  private val tpl: DataEntry = new DataEntry().setTo(marker)

  def collect(): Option[Serializable] = {
    collect(0L)
  }

  def collect(waitTime: Long): Option[Serializable] = {
    val space: FlyPrime = SpaceCache.getDataSpace
    try {
      val ent = space.take(tpl, waitTime)
      if (ent != null)
        Some(ent.data)
      else
        None
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
