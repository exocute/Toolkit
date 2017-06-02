package executable

import java.text.{DateFormat, SimpleDateFormat}
import java.util.Date
import java.util.concurrent.LinkedBlockingDeque

import exonode.clifton.signals.Log._

import scala.annotation.tailrec
import scala.collection.immutable.HashMap

/**
  * Created by #GrowinScala
  */
case class InfoNode(graphID: String, actIDTo: String, actIDFrom: String, injID: String, processing: Boolean) {
  def setMessage(value: Boolean) = InfoNode(graphID, actIDTo, actIDFrom, injID, value)

  def setChangeAct(valueFrom: String, valueTo: String) = InfoNode(graphID, valueFrom, valueTo, injID, processing)
}

case class SignificantCount(info: Double, error: Double, warn: Double, recovered: Double) {
  def incInfo: SignificantCount = SignificantCount(info + 1, error, warn, recovered)

  def incError: SignificantCount = SignificantCount(info, error + 1, warn, recovered)

  def incWarn: SignificantCount = SignificantCount(info, error, warn + 1, recovered)

  def incRecovered: SignificantCount = SignificantCount(info, error, warn, recovered + 1)

  def getAll = List(info, error, warn, recovered)
}

case class ProcessedAndInjected(processing: Double, injected: Double, processed: Double, collected: Double) {

  def incProcessing: ProcessedAndInjected = ProcessedAndInjected(processing + 1, injected, processed, collected)

  def decProcessingFinal: ProcessedAndInjected = ProcessedAndInjected(processing - 1, injected, processed + 1, collected)

  def decProcessing: ProcessedAndInjected = ProcessedAndInjected(processing - 1, injected, processed, collected)

  def incInjected: ProcessedAndInjected = ProcessedAndInjected(processing, injected + 1, processed, collected)

  def incCollected: ProcessedAndInjected = ProcessedAndInjected(processing, injected, processed, collected + 1)

  def getAll = List(processing, injected, processed, collected)
}

case class ActDB(nodeCount: Int, processedCount: Int, avgTime: Double, sumProcessed: Double) {
  def setNodeCount(value: Int) = ActDB(value, processedCount, avgTime, sumProcessed)

  def incNodeCount = ActDB(nodeCount + 1, processedCount, avgTime, sumProcessed)

  def decNodeCount = ActDB(nodeCount - 1, processedCount, avgTime, sumProcessed)

  def updateProcessedAndAverage(value: Long) =
    ActDB(nodeCount, processedCount + 1, (sumProcessed + value) / (processedCount + 1), sumProcessed + value)
}

case class DataBase(nodesDB: HashMap[String, InfoNode],
                    actDB: HashMap[String, ActDB],
                    countLevels: SignificantCount,
                    otherCounters: ProcessedAndInjected,
                    graphUID: List[(String, String)]) {
  def setNodeID(nDB: HashMap[String, InfoNode]) = DataBase(nDB, actDB, countLevels, otherCounters, graphUID)

  def setActID(aDB: HashMap[String, ActDB]) = DataBase(nodesDB, aDB, countLevels, otherCounters, graphUID)

  def setCountLev(cLevel: SignificantCount) = DataBase(nodesDB, actDB, cLevel, otherCounters, graphUID)

  def setOtherCounters(oCount: ProcessedAndInjected) = DataBase(nodesDB, actDB, countLevels, oCount, graphUID)

  def setNodesAndAct(nDB: HashMap[String, InfoNode],
                     aDB: HashMap[String, ActDB]) = DataBase(nDB, aDB, countLevels, otherCounters, graphUID)

  def setNodesActandCount(nDB: HashMap[String, InfoNode],
                          aDB: HashMap[String, ActDB],
                          cLevel: SignificantCount) = DataBase(nDB, aDB, cLevel, otherCounters, graphUID)

  def setNodesCountandOthers(nDB: HashMap[String, InfoNode],
                             cLevel: SignificantCount,
                             oCount: ProcessedAndInjected) = DataBase(nDB, actDB, cLevel, oCount, graphUID)

  def setNodesAndCount(nDB: HashMap[String, InfoNode],
                       cLevel: SignificantCount) = DataBase(nDB, actDB, cLevel, otherCounters, graphUID)

  def setCounters(cLevel: SignificantCount,
                  oCount: ProcessedAndInjected) = DataBase(nodesDB, actDB, cLevel, oCount, graphUID)

  def addUID(uid: (String, String)) = DataBase(nodesDB, actDB, countLevels, otherCounters, uid :: graphUID)
}

case class Graphics(top5: List[String], actData: List[String]) {
  def setTop(top: List[String], data: List[String]) = Graphics(top, data)
}

class SystemAnalyser(updateTime: Int = 2, logs: LinkedBlockingDeque[LogType]) extends Thread {

  private val SleepTime = updateTime * 1000

  val SIGNIFICANT = Array("Error", "Warn", "Recovered")
  val COUNTERS = Array("Injected", "Processed", "Collected")
  private val dateFormat: DateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss")

  override def run(): Unit = {

    val graphics = Graphics(Nil, Nil)
    startGraphics(graphics)
    processingLog(DataBase(new HashMap[String, InfoNode](), new HashMap[String, ActDB](), SignificantCount(0, 0, 0, 0), ProcessedAndInjected(0, 0, 0, 0), Nil), graphics)
  }

  @tailrec
  private def processingLog(db: DataBase, graphics: Graphics): DataBase = {
    val start: Long = System.currentTimeMillis()
    val database: DataBase = {
      processLogs(db, graphics)
    }
    val spentTime: Long = System.currentTimeMillis() - start
    if (spentTime > 0)
      Thread.sleep(SleepTime - spentTime)
    val (top5, actRank) = updateGraphics(database, graphics)
    processingLog(database, graphics.setTop(top5, actRank))
  }

  @tailrec
  private def processLogs(data: DataBase, graphics: Graphics): DataBase = {
    if (!logs.isEmpty) {
      processLogs(processLog(data, logs.pollLast(), graphics), graphics)
    } else
      data
  }

  def processLog(data: DataBase, log: LogType, graphics: Graphics): DataBase = {
    GraphicInterfaceScala.addLogLine(dateFormat.format(new Date) + " - " + log)
    log match {
      case LogStartedNode(nodeId) =>
        val nDB = data.nodesDB + (nodeId -> InfoNode("", "", "", "", processing = false))
        val levelCount = processLevel(log, data)
        val newActDB =
          if (data.actDB.contains(""))
            data.actDB + ("" -> data.actDB("").incNodeCount)
          else
            data.actDB + ("" -> ActDB(1, 0, 0, 0))
        GraphicInterfaceScala.addErrorEvents(dateFormat.format(new Date), "Node Started")
        GraphicInterfaceScala.addNode(nodeId)
        data.setNodesActandCount(nDB, newActDB, levelCount)
      case LogStartedGraph(graphId, message) =>
        GraphicInterfaceScala.addErrorEvents(dateFormat.format(new Date), log.message)
        val graphUID = (graphId.split(':').head, graphId.split(':').last)
        data.addUID(graphUID)
      case LogChangedAct(nodeId, actIdFrom, actIdTo, message) =>
        val oldNodeInfo = data.nodesDB(nodeId)
        val newNDB = data.nodesDB.updated(nodeId, oldNodeInfo.setChangeAct(actIdFrom, actIdTo))
        //update actDB
        val newActDB = {
          val actDB = if (!data.actDB.contains(actIdTo)) data.actDB + (actIdTo -> ActDB(0, 0, 0, 0)) else data.actDB
          val lastValueFrom = actDB(actIdFrom).nodeCount
          val lastValueTo = actDB(actIdTo).nodeCount
          val newValueFrom = if (lastValueFrom - 1 == 0) 0 else lastValueFrom - 1
          val updateIntermediate = actDB.updated(actIdFrom, actDB(actIdFrom).setNodeCount(lastValueFrom - 1))
          updateIntermediate.updated(actIdTo, actDB(actIdTo).setNodeCount(lastValueTo + 1))
        }
        val levelCount = processLevel(log, data)
        if (actIdTo.equals("@")) GraphicInterfaceScala.addErrorEvents(dateFormat.format(new Date), "Analyser Started : " + nodeId)
        data.setNodesActandCount(newNDB, newActDB, levelCount)
      case LogProcessingInput(nodeId, _, _, _) =>
        //update nodeDB
        val oldNodeInfo = data.nodesDB(nodeId)
        val newNDB = data.nodesDB.updated(nodeId, oldNodeInfo.setMessage(true))
        val levelCount = processLevel(log, data)
        val otherCount = data.otherCounters.incProcessing
        data.setNodesCountandOthers(newNDB, levelCount, otherCount)
      case LogFinishedProcessing(nodeId, actIdFrom, actIdTo, _, processingTime) =>
        val oldNodeInfo = data.nodesDB(nodeId)
        val newNDB = data.nodesDB.updated(nodeId, oldNodeInfo.setMessage(false))
        val nActDB = {
          val oldData = data.actDB(actIdFrom)
          data.actDB.updated(actIdFrom, oldData.updateProcessedAndAverage(processingTime))
        }
        val levelCount = processLevel(log, data)
        val otherCount = if (actIdTo.equals("<")) data.otherCounters.decProcessingFinal else data.otherCounters.decProcessing
        DataBase(newNDB, nActDB, levelCount, otherCount, data.graphUID)
      case LogDataRecovered(_, _, _, _) =>
        val levelCount = processLevel(log, data)
        val otherCount = levelCount.incRecovered
        data.setCountLev(otherCount)
      case LogInjected(_) =>
        val otherCount = data.otherCounters.incInjected
        data.setOtherCounters(otherCount)
      case LogCollected(_) =>
        val otherCount = data.otherCounters.incCollected
        data.setOtherCounters(otherCount)
      case LogErrorProcessing(_, _) =>
        //TODO should be updated on the next version
        data.setCountLev(data.countLevels.incError)
      case LogNodeShutdown(nodeId, _) =>
        val newNDB = data.nodesDB - nodeId
        val actProcessing = data.nodesDB(nodeId).actIDFrom
        val newACTDB = {
          val act = data.actDB(actProcessing).decNodeCount
          data.actDB.updated(actProcessing, act)
        }
        GraphicInterfaceScala.removeNode(nodeId)
        GraphicInterfaceScala.addErrorEvents(dateFormat.format(new Date), log.message + " : " + nodeId)
        data.setNodesAndAct(newNDB, newACTDB)
      case LogValuesLost(_, _) =>
        //TODO should be updated on the next version
        data.setCountLev(data.countLevels.incError)
      case LogActivityNotFound(_, _) =>
        //TODO should be updated on the next version
        data.setCountLev(data.countLevels.incError)
      case LogClassNotLoaded(_, _) =>
        //TODO should be updated on the next version
        data.setCountLev(data.countLevels.incWarn)
      case LogInformationLost(_, _, _, _) =>
        //TODO should be updated on the next version
        data.setCountLev(data.countLevels.incWarn)
    }

  }

  private def getTop5Activities(actDB: HashMap[String, ActDB]): List[(String, Int)] = {
    actDB.toList.sortWith(_._2.nodeCount > _._2.nodeCount).take(5).map(x => (x._1, x._2.nodeCount))
  }

  private def updateGraphics(database: DataBase, graphics: Graphics): (List[String], List[String]) = {
    //Update Activities Top
    val top5Activities = getTop5Activities(database.actDB)
    val listTop5 = top5Activities.map(x => x._1)
    val toRemove = graphics.top5.diff(listTop5)
    for (actName <- toRemove) {
      if (actName.length > 1) {
        val separated = actName.split(':')
        val graphID = separated.head
        val actID = separated.last
        val fullName = actID + ':' + database.graphUID.filter(_._1 == graphID).head._2
        GraphicInterfaceScala.removeTopActivity(fullName)
      } else GraphicInterfaceScala.removeTopActivity(actName)
    }
    for ((actName, value) <- top5Activities) {
      if (actName.length > 1) {
        val separated = actName.split(':')
        val graphID = separated.head
        val actID = separated.last
        val fullName = actID + ':' + database.graphUID.filter(_._1 == graphID).head._2
        GraphicInterfaceScala.updateTopActivity(fullName, value)
      } else GraphicInterfaceScala.updateTopActivity(actName, value)
    }

    //(error,warn, recovered)
    val levels = database.countLevels.getAll.toArray.drop(1)
    //(processing, injected, processed, collected)
    val counters = database.otherCounters.getAll.toArray

    //Update Processing
    GraphicInterfaceScala.addProcessingElement(counters(0))

    //Update Processed
    for (x <- SIGNIFICANT.indices)
      GraphicInterfaceScala.setSignificantEvents(SIGNIFICANT(x), levels(x))

    val countersFiltered = counters.drop(1)
    for (x <- COUNTERS.indices)
      GraphicInterfaceScala.setProcessedEvents(COUNTERS(x), countersFiltered(x))

    val actualAct = graphics.actData
    val diff = database.actDB.keySet.toList.diff(graphics.actData)

    for (act <- diff) {
      if (act.length > 1) {
        val separated = act.split(':')
        val graphID = separated.head
        val actID = separated.last
        val fullName = actID + ':' + database.graphUID.filter(_._1 == graphID).head._2
        GraphicInterfaceScala.addActivityRank(fullName, database.actDB(act).nodeCount.toString, database.actDB(act).avgTime.toString)
      } else GraphicInterfaceScala.addActivityRank(act, database.actDB(act).nodeCount.toString, database.actDB(act).avgTime.toString)
    }
    val nActRankList = diff.reverse ++ actualAct

    val size = actualAct.size
    for (act <- actualAct) {
      GraphicInterfaceScala.setActivityRank(database.actDB(act).nodeCount.toString, size - actualAct.indexOf(act) - 1, 1)
      GraphicInterfaceScala.setActivityRank(database.actDB(act).avgTime.toString, size - actualAct.indexOf(act) - 1, 2)
    }

    (listTop5, nActRankList)
  }

  private def startGraphics(graphics: Graphics): Unit = {
    //Update Level Count
    for (x <- SIGNIFICANT)
      GraphicInterfaceScala.addSignificantEvents(x, 0.0)

    //Update Level Count
    for (x <- COUNTERS)
      GraphicInterfaceScala.addProcessedEvents(x, 0.0)

    //set priorityEventsHeader
    GraphicInterfaceScala.activityRankHeader(Array("ID", "COUNT", "AVG Time"))

    //set priorityEventsHeader
    GraphicInterfaceScala.errorHeader(Array("Date", "Message"))
  }

  def processLevel(logType: LogType, data: DataBase): SignificantCount = logType match {
    case _: LogTypeInfo => data.countLevels.incInfo
    case _: LogTypeWarn => data.countLevels.incWarn
    case _: LogTypeError => data.countLevels.incError
  }

}


