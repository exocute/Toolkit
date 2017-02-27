package clifton.graph

import java.io.Serializable

import api.Collector
import clifton.graph.exceptions.CollectException
import exonode.clifton.config.Protocol.COLLECTED
import exonode.clifton.node.Log.{INFO, ND}
import exonode.clifton.node.entries.DataEntry
import exonode.clifton.node.{Log, SpaceCache}
import exonode.clifton.signals.LoggingSignal

import scala.collection.mutable.ListBuffer

/**
  * Created by #ScalaTeam on 21/12/2016.
  *
  * collects results from the space saved in dataEntries with a < marker
  */
class CliftonCollector(uuid: String, marker: String) extends Collector {

  private val template: DataEntry = DataEntry(marker, null, null, null)
  private val dataSpace = SpaceCache.getDataSpace

  def collectIndex(injectIndex: Int, waitTime: Long = 0): Option[Serializable] = {
    val injectId = s"$uuid:$injectIndex"
    try {
      val ent = dataSpace.take(template.setInjectId(injectId), waitTime)
      ent match {
        case Some(entry) =>
          sendLog()
          Some(entry.data)
        case None => None
      }
    } catch {
      case e: Exception => throw new CollectException("Collector Error")
    }
  }

  private def sendLog() = {
    Log.receiveLog(LoggingSignal(COLLECTED, INFO, ND, ND, ND, ND, ND, "Result Collected", 0))
  }

  def collect(): Option[Serializable] = {
    collect(0L)
  }

  def collect(waitTime: Long): Option[Serializable] = {
    try {
      val ent = dataSpace.take(template, waitTime)
      ent match {
        case Some(entry) =>
          sendLog()
          Some(entry.data)
        case None => None
      }
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
    buffer.foreach(x => sendLog())
    buffer.toList
  }

}
