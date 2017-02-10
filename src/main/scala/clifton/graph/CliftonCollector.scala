package clifton.graph

import java.io.Serializable

import api.Collector
import clifton.graph.exceptions.CollectException
import exonode.clifton.node.SpaceCache
import exonode.clifton.node.entries.DataEntry

import scala.collection.mutable.ListBuffer

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

  def collectMany(numObjects: Int, waitTime: Long): List[Serializable] = {
    var buffer: ListBuffer[Serializable] = ListBuffer()
    val start = System.currentTimeMillis()
    var remainingTime = waitTime
    var totalObjects = 0
    val MAX_COLLECT_EACH_CALL = 50

    while (totalObjects < numObjects && remainingTime > 0L) {
      val amount = math.min(MAX_COLLECT_EACH_CALL, numObjects - totalObjects)
      val results: Iterable[DataEntry] = dataSpace.takeMany(template, amount)
      if (results.nonEmpty) {
        buffer ++= results.map(_.data)
        totalObjects += results.size
      } else {
        try {
          Thread.sleep(1)
        } catch {
          case e: InterruptedException => e.printStackTrace()
        }
      }
      remainingTime = waitTime - (System.currentTimeMillis() - start)
    }
    buffer.toList
  }

}
