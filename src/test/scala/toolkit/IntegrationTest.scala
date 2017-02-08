package toolkit

import java.io.File

import clifton.graph.{ExoGraph, ExocuteConfig}
import exonode.clifton.Protocol._
import exonode.clifton.node._
import exonode.clifton.node.entries.{DataEntry, ExoEntry}
import exonode.clifton.signals.KillSignal
import org.scalatest.{BeforeAndAfter, FlatSpec}

import scala.util.{Failure, Success}

/**
  * Created by #ScalaTeam on 20/01/2017.
  */
class IntegrationTest extends FlatSpec with BeforeAndAfter {

  private val DEFAULT_GRP_FILE = "examples\\abc.grp"
  private val jarFile = new File("examples\\classes.jar")

  private val KILL_TIME = 60 * 1000
  private val MAX_TIME_FOR_EACH_TEST = 60 * 60 * 1000
  private val EXPECTED_TIME_TO_CONSENSUS = 10 * 1000 + CONSENSUS_MAX_SLEEP_TIME * (1 + CONSENSUS_LOOPS_TO_FINISH)

  private val signalSpace = SpaceCache.getSignalSpace

  private def startGraph(grpFile: String = DEFAULT_GRP_FILE): ExoGraph = {
    ExocuteConfig.setHosts().addGraph(new File(grpFile), List(jarFile), MAX_TIME_FOR_EACH_TEST) match {
      case Success(exoGraph) => exoGraph
      case Failure(e) => throw new Exception
    }
  }

  private var allNodes = List[CliftonNode]()

  private def launchNNodes(nodes: Int): List[CliftonNode] = {
    val nodesList = for {
      _ <- 1 to nodes
    } yield {
      val node = new CliftonNode()
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

  private val genericEntry = ExoEntry(null, null)
  private val dataEntry = DataEntry(null, null, null, null)

  private def getTable: Option[ExoEntry] = {
    signalSpace.read(ExoEntry(TABLE_MARKER, null), 0L)
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

  "shouldStabilizeAtBegin" should "follow a normal distribution after a few seconds" in {
    startGraph("examples\\abc.grp")

    val expectedDistribution = 5
    val nNodes = expectedDistribution * 3 + 1

    //Launch nodes to process activities
    launchNNodes(nNodes)

    Thread.sleep(EXPECTED_TIME_TO_CONSENSUS)

    //Wait a few seconds for the nodes to stabilize between the activities
    Thread.sleep(NODE_CHECK_TABLE_TIME * 3)

    val tableEntry = getTable
    assert(tableEntry.isDefined)

    //check if table is distributed evenly
    val table: TableType = tableEntry.get.payload.asInstanceOf[TableType]
    assert(table.forall { case (_, i) => math.abs(i - expectedDistribution) <= 1 })
  }

  "shouldStabilizeAfterWork" should "follow a normal distribution after a few seconds" in {
    val exoGraph = startGraph("examples\\ab2c.grp")

    val expectedDistribution = 5
    val nNodes = expectedDistribution * 3 + 1

    //Launch nodes to process activities
    launchNNodes(nNodes)

    Thread.sleep(EXPECTED_TIME_TO_CONSENSUS)

    exoGraph.injector.inject(5, "")

    // Wait for processing to be completed
    Thread.sleep(60 * 1000)

    //Wait a few seconds for the nodes to stabilize between the activities
    Thread.sleep(NODE_CHECK_TABLE_TIME * 3)

    val tableEntry = getTable
    assert(tableEntry.isDefined)

    //check if table is distributed evenly
    val table: TableType = tableEntry.get.payload.asInstanceOf[TableType]
    assert(table.forall { case (_, i) => math.abs(i - expectedDistribution) <= 1 })
  }

  "InjectAndCollect" should "The output be ready after a time" in {
    val exoGraph = startGraph("examples\\abc.grp")

    val amountOfInputs = 10
    launchNNodes(9)

    val someStrings = Stream.continually(randomAlphaNumericString(10)).take(amountOfInputs)

    //inject input
    exoGraph.injector.injectMany(someStrings)

    //wait a few seconds
    Thread.sleep(NODE_CHECK_TABLE_TIME * 2)

    exoGraph.collector.collect(amountOfInputs, 0) match {
      case vec if vec.length == amountOfInputs =>
        val expected = someStrings.map(s => (s + s).reverse.map(_.toUpper)).toList
        assert(vec == expected)
      case _ => assert(false)
    }
  }

  // 6 - random alphanumeric
  def randomAlphaNumericString(length: Int): String = {
    val chars = ('a' to 'z') ++ ('A' to 'Z') ++ ('0' to '9')
    randomStringFromCharList(length, chars)
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

}
