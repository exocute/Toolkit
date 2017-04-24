package clifton.graph

import java.io.Serializable

import api.Collector
import clifton.graph.CliftonCollector.MAX_NEXT_LEAF_CALLS
import clifton.graph.exceptions.CollectException
import exonode.clifton.config.ProtocolConfig.LOGCODE_COLLECTED
import exonode.clifton.node.Log.{INFO, ND}
import exonode.clifton.node.entries.{DataEntry, FlatMapEntry}
import exonode.clifton.node.{Log, SpaceCache}
import exonode.clifton.signals.LoggingSignal

import scala.annotation.tailrec
import scala.collection.mutable.ListBuffer

/**
  * Created by #GrowinScala
  *
  * Collects results from the space saved in dataEntries with a < marker <br>
  * Supports graphs with flatMap transformations
  */
class CliftonCollector(uuid: String, marker: String, nFlatMaps: Int, val canCollect: () => Boolean) extends Collector {

  require(nFlatMaps >= 0)

  private val template: DataEntry = DataEntry(marker, null, null, null, null)
  private val dataSpace = SpaceCache.getDataSpace
  private var indexTree: Tree = OrderTree(uuid, 0, "", 0, Int.MaxValue, 0, None)

  private def sendLog() = {
    Log.writeLog(LoggingSignal(LOGCODE_COLLECTED, INFO, ND, ND, ND, ND, ND, "Result Collected", 0))
  }

  def collect(): Option[Option[Serializable]] = {
    collect(0L)
  }

  def collect(waitTime: Long): Option[Option[Serializable]] = {
    if (!canCollect())
      throw new CollectException("Collector Error")

    try {
      dataSpace.take(template, waitTime).map {
        dataEntry =>
          sendLog()
          dataEntry.data
      }
    } catch {
      case _: Exception => throw new CollectException("Collector Error")
    }
  }

  def collectMany(numObjects: Int, waitTime: Long): List[Serializable] = {
    if (!canCollect())
      throw new CollectException("Collector Error")

    var buffer: ListBuffer[Serializable] = ListBuffer()
    val start = System.currentTimeMillis()
    var remainingTime = waitTime
    var totalObjects = 0
    val MAX_COLLECT_EACH_CALL = 50

    while (totalObjects < numObjects && remainingTime > 0L) {
      val amount = math.min(MAX_COLLECT_EACH_CALL, numObjects - totalObjects)
      val results: Iterable[DataEntry] = dataSpace.takeMany(template, amount)
      if (results.nonEmpty) {
        val newResults = results.map(_.data).filter(_.isDefined).map(_.get)
        buffer ++= newResults
        totalObjects += newResults.size
      } else {
        try {
          Thread.sleep(1)
        } catch {
          case e: InterruptedException => e.printStackTrace()
        }
      }
      remainingTime = waitTime - (System.currentTimeMillis() - start)
    }
    buffer.foreach(_ => sendLog())
    buffer.toList
  }

  private def collectNextOrdered(tree: Tree): (Option[Tree], Option[Option[Serializable]]) = {
    def aux(injectId: String, orderId: String): Option[Option[Serializable]] = {
      try {
        dataSpace.take(template.setInjectId(injectId).setOrderId(orderId), 0).map {
          dataEntry =>
            sendLog()
            dataEntry.data
        }
      } catch {
        case _: Exception => throw new CollectException("Collector Error")
      }
    }

    tree.nextLeaf(0) match {
      case None =>
        (None, None)
      case Some(newTree: OrderTree) =>
        (Some(newTree), None)
      case Some(leaf@OrderLeaf(injectId, orderId, parent)) =>
        val result = aux(injectId, orderId)
        if (result.isDefined) {
          (parent, result)
        } else {
          (Some(leaf), None)
        }
    }
  }

  def collectManyOrdered(numObjects: Int, waitTime: Long): List[Serializable] = {
    require(waitTime >= 0)

    if (!canCollect())
      throw new CollectException("Collector Error")

    var buffer: ListBuffer[Serializable] = ListBuffer()
    val start = System.currentTimeMillis()
    var remainingTime = Math.max(1, waitTime)
    var totalObjects = 0

    while (totalObjects < numObjects && remainingTime > 0L) {
      collectNextOrdered(indexTree) match {
        case (updatedTree, None) =>
          indexTree = updatedTree.get
          try {
            Thread.sleep(1)
          } catch {
            case e: InterruptedException => e.printStackTrace()
          }
        case (updatedTree, Some(data)) =>
          if (data.isDefined) {
            buffer += data.get
            totalObjects += 1
          }
          indexTree = updatedTree.get
      }
      remainingTime = waitTime - (System.currentTimeMillis() - start)
    }
    buffer.foreach(_ => sendLog())
    buffer.toList
  }

  def collectAllByIndex(injectIndex: Int): List[Serializable] = {
    if (!canCollect())
      throw new CollectException("Collector Error")

    var buffer: ListBuffer[Serializable] = ListBuffer()

    @tailrec def collectNext(tree: Tree): Unit = {
      collectNextOrdered(tree) match {
        case (None, None) =>
        case (Some(updatedTree), None) =>
          try {
            Thread.sleep(1)
          } catch {
            case e: InterruptedException => e.printStackTrace()
          }
          collectNext(updatedTree)
        case (updatedTree, Some(data)) =>
          if (data.isDefined)
            buffer += data.get
          if (updatedTree.isDefined)
            collectNext(updatedTree.get)
      }
    }

    val tree: Tree = OrderTree(uuid, injectIndex, "", injectIndex, injectIndex, 0, None)
    collectNext(tree)
    buffer.foreach(_ => sendLog())
    buffer.toList
  }

  private sealed trait Tree {
    def nextLeaf(calls: Int): Option[Tree]
  }

  private case class OrderTree(uuid: String, injectId: Int, prefixOrderId: String, orderId: Int,
                               size: Int, depth: Int, parent: Option[OrderTree]) extends Tree {
    private def joinOrderId: String = if (prefixOrderId.isEmpty) orderId.toString else s"$prefixOrderId:$orderId"

    def nextLeaf(calls: Int): Option[Tree] = {
      //prevents infinite loops and stack overflows
      if (calls > MAX_NEXT_LEAF_CALLS)
        Some(this)
      else {
        val someTree: Option[Option[OrderTree]] =
          if (depth < nFlatMaps) {
            val fullOrderId = joinOrderId
            dataSpace.take(FlatMapEntry(uuid, fullOrderId, null), 0).map {
              case FlatMapEntry(_, _, newLevelSize) =>
                if (newLevelSize == 0)
                  nextOrderId
                else
                  Some(OrderTree(uuid, injectId, fullOrderId, 0, newLevelSize, depth + 1, Some(this)))
            }
          } else {
            Some(Some(this))
          }
        someTree match {
          case None =>
            Some(this)
          case Some(None) =>
            None
          case Some(Some(tree)) =>
            if (tree.depth < nFlatMaps)
              tree.nextLeaf(calls + 1)
            else
              Some(OrderLeaf(s"$uuid:$injectId", tree.joinOrderId, tree.nextOrderId))
        }
      }
    }

    def nextOrderId: Option[OrderTree] = {
      if (orderId < size - 1) {
        if (depth == 0)
          Some(OrderTree(uuid, injectId + 1, prefixOrderId, orderId + 1, size, depth, parent))
        else
          Some(OrderTree(uuid, injectId, prefixOrderId, orderId + 1, size, depth, parent))
      } else
        parent match {
          case Some(tree) => tree.nextOrderId
          case None => None
        }
    }
  }

  private case class OrderLeaf(injectId: String, orderId: String, parent: Option[OrderTree]) extends Tree {
    def nextLeaf(calls: Int): Option[Tree] = Some(this)
  }

}

object CliftonCollector {

  val MAX_NEXT_LEAF_CALLS = 100

}
