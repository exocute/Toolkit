package toolkit

import java.io.{File, Serializable}

import clifton.graph.{ExoGraph, ExocuteConfig}
import exonode.clifton.config.BackupConfig._
import exonode.clifton.config.ProtocolConfig.{AnalyserTable, TableType}
import exonode.clifton.config.{BackupConfig, ProtocolConfig}
import exonode.clifton.node._
import exonode.clifton.node.entries.ExoEntry
import exonode.clifton.signals.KillSignal
import org.scalatest.{BeforeAndAfter, FlatSpec}

import scala.util.{Failure, Random, Success}

/**
  * Created by #GrowinScala
  */
class IntegrationTest extends FlatSpec with BeforeAndAfter {

  private val jarFile = new File("tests\\classes.jar")

  private val config = ProtocolConfig.DEFAULT
  private val KILL_TIME = 60 * 1000
  private val MAX_TIME_FOR_EACH_TEST = 60 * 60 * 1000
  private val EXPECTED_TIME_TO_CONSENSUS = 10 * 1000 +
    config.CONSENSUS_MAX_SLEEP_TIME * (1 + config.CONSENSUS_LOOPS_TO_FINISH)

  private val signalSpace = SpaceCache.getSignalSpace

  private def startGraph(grpFile: String): ExoGraph = {
    ExocuteConfig.setHosts().addGraphFile(new File(grpFile), List(jarFile), MAX_TIME_FOR_EACH_TEST) match {
      case Success(exoGraph) => exoGraph
      case Failure(e) => throw e
    }
  }

  private def startGraphManual(grpText: String): ExoGraph = {
    ExocuteConfig.setHosts().addGraphText(grpText, List(jarFile), MAX_TIME_FOR_EACH_TEST) match {
      case Success(exoGraph) => exoGraph
      case Failure(e) => throw e
    }
  }

  private var allNodes = List[CliftonNode]()

  private def launchNNodes(nodes: Int, conf: BackupConfig = BackupConfigDefault): List[CliftonNode] = {
    val nodesList =
      for (_ <- 1 to nodes) yield {
        val node = new CliftonNode()(conf)
        allNodes = node :: allNodes
        node
      }
    nodesList.foreach(node => node.start())
    nodesList.toList
  }

  private def killNodes(nodes: List[CliftonNode]) = {
    nodes.foreach(node => signalSpace.write(ExoEntry(node.nodeId, KillSignal), KILL_TIME))
    nodes.foreach(node => node.join())
  }

  private def getTable: Option[TableType] = {
    signalSpace.read(ExoEntry[AnalyserTable](ProtocolConfig.TABLE_MARKER, null), 0L).map(_.payload.table)
  }

  before {
    SpaceCache.cleanAllSpaces()
    println("Space Cleaned")
    Thread.sleep(500)
  }

  after {
    allNodes.foreach(node => signalSpace.write(ExoEntry(node.nodeId, KillSignal), KILL_TIME))
    allNodes.foreach(node => node.join())
    allNodes = Nil
  }

  "Stabilize distribution at boot" should "follow an equal distribution in few cycles after boot" in {
    startGraphManual(
      """Graph graph
        |Activity A exocute.classes.DoubleString
        |Activity B exocute.classes.Reverse
        |Activity C exocute.classes.UpperCase
        |Connection A->B->C""".stripMargin)

    val expectedDistribution = 5
    val nNodes = expectedDistribution * 3 + 1

    //Launch nodes to process activities
    launchNNodes(nNodes)

    Thread.sleep(EXPECTED_TIME_TO_CONSENSUS)

    //Wait a few seconds for the nodes to stabilize between the activities
    Thread.sleep(config.NODE_CHECK_TABLE_TIME * 4)

    val table = getTable
    assert(table.isDefined)

    //check if table is distributed evenly
    assert(table.get.forall { case (_, i) => math.abs(i - expectedDistribution) <= 1 })
  }

  "Stabilize distribution after long work" should "follow a normal distribution after a few seconds" in {
    val exoGraph = startGraphManual(
      """Graph graph
        |Activity A exocute.classes.DoubleString
        |Activity B exocute.classes.Reverse:120
        |Activity C exocute.classes.UpperCase
        |Connection A->B->C""".stripMargin)

    val expectedDistribution = 5
    val nNodes = expectedDistribution * 3 + 1

    //Launch nodes to process activities
    launchNNodes(nNodes)

    Thread.sleep(EXPECTED_TIME_TO_CONSENSUS)

    exoGraph.injector.inject(5, "")

    // Wait for processing to be completed
    val (a, b, c) = (1000, 120 * 1000, 1000)
    Thread.sleep(a + b + c)

    //Wait a few seconds for the nodes to stabilize between the activities
    Thread.sleep(config.NODE_CHECK_TABLE_TIME * 3)

    val table = getTable
    assert(table.isDefined)

    //check if table is distributed evenly
    assert(table.get.forall { case (_, i) => math.abs(i - expectedDistribution) <= 1 })
  }

  "Inject and collect results" should "collect the results of several inputs after a few seconds" in {
    val exoGraph = startGraphManual(
      """Graph graph
        |Activity A exocute.classes.DoubleString
        |Activity B exocute.classes.Reverse
        |Activity C exocute.classes.UpperCase
        |Connection A->B->C""".stripMargin)
    val inj = exoGraph.injector
    val coll = exoGraph.collector

    val amountOfInputs = 10
    launchNNodes(9)

    val someStrings = randomAlphaNumericString(10).take(amountOfInputs)

    //inject input
    val injIndex = inj.injectMany(someStrings)

    //wait a few seconds
    Thread.sleep(config.NODE_CHECK_TABLE_TIME * 3)

    injIndex.flatMap(coll.collectAllByIndex) match {
      case list =>
        val expected = someStrings.map(s => (s + s).reverse.map(_.toUpper)).toList
        assert(list == expected)
    }
  }

  // 6 - random alphanumeric
  def randomAlphaNumericString(length: Int): Stream[String] = {
    val chars = ('a' to 'z') ++ ('A' to 'Z') ++ ('0' to '9')
    Stream.continually(randomStringFromCharList(length, chars))
  }

  // 7 - random alpha
  def randomAlpha(length: Int): String = {
    val chars = ('a' to 'z') ++ ('A' to 'Z')
    randomStringFromCharList(length, chars)
  }

  // used by #6 and #7
  def randomStringFromCharList(length: Int, chars: Seq[Char]): String = {
    val sb = new StringBuilder
    for (i <- 1 to length) {
      val randomNum = util.Random.nextInt(chars.length)
      sb.append(chars(randomNum))
    }
    sb.toString
  }

  "Inject, stop nodes, collect" should "collect results even if some nodes fail while processing" in {
    val exoGraph = startGraphManual(
      s"""Graph test
         |Activity a exocute.classes.DoubleString:60""".stripMargin)
    val inj = exoGraph.injector
    val coll = exoGraph.collector

    val nNodes = 5
    val nNodesToKill = 2
    val nInputs = nNodes

    val fastConf = new BackupConfig {
      override def BACKUP_TIMEOUT_TIME: Long = 60 * 1000
    }

    // analyser
    launchNNodes(1, fastConf)
    Thread.sleep(EXPECTED_TIME_TO_CONSENSUS)
    val nodes = launchNNodes(nNodes, fastConf)

    // wait for all nodes to get the activity
    Thread.sleep(30 * 1000)
    val table = getTable
    assert(table.isDefined)
    assert(table.get.values.sum == nNodes)

    val inputs = randomAlphaNumericString(10).take(nInputs).toList
    inj.injectMany(inputs)

    Thread.sleep(30 * 1000)

    // make some of the nodes crash
    val nodesToKill = scala.util.Random.shuffle(nodes).take(nNodesToKill)
    killNodes(nodesToKill)

    val results = coll.collectMany(nInputs, 6 * 60 * 1000)
    assert(results.size == nInputs)
    assert(results.map(_.toString).sorted == inputs.map(i => i + i).sorted)

  }

  "FilterActivity" should "collect the correct results after a few seconds" in {
    val MAX = 20

    val exoGraph = startGraphManual(
      s"""Graph graph
         |Activity A exocute.classes.Double
         |ActivityFilter B exocute.classes.IsGreaterThan:$MAX
         |Connection A->B""".stripMargin)
    val inj = exoGraph.injector
    val coll = exoGraph.collector

    val amountOfInputs = 10
    launchNNodes(7)

    val someInput = Stream.continually(Random.nextInt(MAX)).take(amountOfInputs)

    //inject input
    val injIndex = inj.injectMany(someInput.map(_.asInstanceOf[Serializable]))

    //wait a few seconds
    Thread.sleep(config.NODE_CHECK_TABLE_TIME * 3)

    injIndex.flatMap(coll.collectAllByIndex) match {
      case list =>
        val expected = someInput.map(n => n * 2).filter(_ > MAX).map(_.asInstanceOf[Serializable])
        assert(list == expected)
    }
  }

  "FilterFlatMap" should "collect the correct results after a few seconds" in {
    val MAX = 20

    val exoGraph = startGraphManual(
      s"""Graph graph
         |Activity A exocute.classes.Double
         |ActivityFlatMap B exocute.classes.TestFlatStream:$MAX
         |Connection A->B""".stripMargin)
    val inj = exoGraph.injector
    val coll = exoGraph.collector

    val amountOfInputs = 10
    launchNNodes(7)

    val someInput = Stream.continually(Random.nextInt(MAX)).take(amountOfInputs)

    //inject input
    val injIndex = inj.injectMany(someInput.map(_.asInstanceOf[Serializable]))

    //wait a few seconds
    Thread.sleep(config.NODE_CHECK_TABLE_TIME * 3)

    injIndex.flatMap(coll.collectAllByIndex) match {
      case list =>
        val expected = someInput.map(n => n * 2).flatMap(n => 1 to Math.min(n, MAX)).map(_.asInstanceOf[Serializable])
        assert(list == expected)
    }
  }

}
