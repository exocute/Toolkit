package clifton.graph

import java.io.Serializable

import api.Collector
import clifton.graph.exceptions.CollectException
import exonode.clifton.node.SpaceCache
import exonode.clifton.node.entries.DataEntry

/**
  * Created by #ScalaTeam on 21/12/2016.
  *
  * collects results from the space saved in dataEntries with a < marker
  */
class CliftonCollector(marker: String) extends Collector {

  private val template: DataEntry = DataEntry(marker, null, null, null)
  private val dataSpace = SpaceCache.getDataSpace

  def collect(injectId: String, waitTime: Long = 0): Option[Serializable] = {
    try {
      val ent = dataSpace.take(template.setInjectId(injectId), waitTime)
      ent.map(_.data)
    } catch {
      case e: Exception => throw new CollectException("Collector Error")
    }
  }

  def collect(): Option[Serializable] = {
    collect(0L)
  }

  def collect(waitTime: Long): Option[Serializable] = {
    try {
      val ent = dataSpace.take(template, waitTime)
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
