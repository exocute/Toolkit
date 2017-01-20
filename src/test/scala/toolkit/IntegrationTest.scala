package toolkit

import api.StarterExoGraph
import org.scalatest.FlatSpec
import java.io.File

import clifton.graph.{CliftonCollector, CliftonInjector}
import com.zink.scala.fly.ScalaFly
import exonode.clifton.Protocol.TableType
import exonode.clifton.node._
import exonode.clifton.signals.KillSignal
import exonode.distributer.{FlyClassEntry, FlyJarEntry}

import scala.util.{Failure, Success}

/**
  * Created by #ScalaTeam on 20/01/2017.
  */
class IntegrationTest extends FlatSpec {

  val grpFile = new File("examples\\ab2c.grp")
  val jarFile = new File("examples\\classes.jar")
  val KILL_TIME = 60 * 1000
  val signalSpace = SpaceCache.getSignalSpace
  val jarSpace = SpaceCache.getJarSpace
  val dataSpace = SpaceCache.getDataSpace

  def startGraph(): (CliftonInjector, CliftonCollector) = {
    val startExoGraph = new StarterExoGraph
    startExoGraph.addGraph(grpFile, List(jarFile)) match {
      case Success((i, c)) => (i, c)
      case Failure(e) => throw new Exception
    }
  }

  def startNodes(num: Int): List[CliftonNode] = {
    val listNodes = for (x <- 1 to num)
      yield new CliftonNode

    listNodes.foreach(node => node.start())

    listNodes.toList
  }

  def killNodes(nodesList: List[CliftonNode]) = {
    nodesList.foreach(node => signalSpace.write(ExoEntry(node.nodeId, KillSignal), KILL_TIME))
    nodesList.foreach(node => node.join())
  }

  val genericEntry = ExoEntry(null, null)
  val dataEntry = DataEntry(null, null, null, null)

  def cleanSpace(): Unit = {
    def clean(space: ScalaFly, cleanTemplate: AnyRef): Unit = {
      while (space.take(cleanTemplate, 0).isDefined) {}
    }

    clean(SpaceCache.getJarSpace, FlyJarEntry(null, null))
    clean(SpaceCache.getJarSpace, FlyClassEntry(null, null))
    clean(SpaceCache.getDataSpace, DataEntry(null, null, null, null))
    clean(SpaceCache.getSignalSpace, ExoEntry(null, null))
  }

  "shouldStabilizeAtBegin" should "Nodes should follow a normal distribuition after a few seconds" in {
    cleanSpace()
    startGraph()
    //Launch the first node to be the analyser
    val analyser = startNodes(1)
    Thread.sleep(3000)

    //Launch 9 nodes to process activities
    val nodes = startNodes(9)

    //Wait 30 seconds for the nodes to stabilize
    Thread.sleep(60 * 1000)

    //
    val table = signalSpace.read(ExoEntry("TABLE", null), 0L)

    //delete the running threads
    killGenericNodes(nodes,analyser)

    //test
    if (table.isDefined) {
      val tab = table.get.payload.asInstanceOf[TableType]
      assert(!tab.exists { case (_, i) => i > 4 || i < 1 })
    } else assert(false)
  }

  "shouldStabilizeAfterWork" should "Nodes should follow a normal distribuition after a few seconds" in {
    cleanSpace()
    val (inj, _) = startGraph()
    //Launch the first node to be the analyser
    val analyser = startNodes(1)
    Thread.sleep(3000)

    //Launch 9 nodes to process activities
    val nodes = startNodes(9)

    //Wait 30 seconds for the nodes to stabilize
    Thread.sleep(30 * 1000)

    //inject 10 times one input
    inj.inject(10, 3)

    Thread.sleep(2 * 60 * 1000)


    //get an updated table
    val table = signalSpace.read(ExoEntry("TABLE", null), 0L)

    //test
    if (table.isDefined) {
      val tab = table.get.payload.asInstanceOf[TableType]
      val stats = !tab.exists { case (_, i) => i > 4 || i < 1 }
      //delete the running threads
      killGenericNodes(nodes,analyser)
      assert(stats)
    } else assert(false)
  }

  "AnalyserReborn" should "Analyser node should be assumed by another node" in {
    cleanSpace()
    startGraph()
    //Launch the first node to be the analyser
    val analyser = startNodes(1)
    Thread.sleep(3000)

    //Launch 9 nodes to process activities
    val nodes = startNodes(9)

    //10 seconds to stabilize
    Thread.sleep(10*1000)

    //kill analyser
    killNodes(analyser)

    //Wait for the table in the space
    Thread.sleep(3 * 70 * 1000)

    //get updated table
    val table = signalSpace.read(ExoEntry("TABLE", null), 0L)

    //kill the rest of the threads
    killNodes(nodes)

    //test
    if (table.isDefined) {
      val tab = table.get.payload.asInstanceOf[TableType]
      assert(tab("@")==1)
    } else assert(false)
  }

  def killGenericNodes(nodes: List[CliftonNode], analyser: List[CliftonNode]) ={
    killNodes(nodes)
    killNodes(analyser)
  }

  "InjectAndCollect" should "The output be ready after a time" in {
    cleanSpace()
    val (inj, col) = startGraph()
    //Launch the first node to be the analyser
    val analyser = startNodes(1)
    Thread.sleep(3000)

    //Launch 9 nodes to process activities
    val nodes = startNodes(9)

    //10 seconds to stabilize
    Thread.sleep(10*1000)

    //inject input
    inj.inject("test")

    //x + 1min + x
    Thread.sleep(70 * 1000)

    col.collect() match{
      case Some(x) =>
        killGenericNodes(nodes,analyser)
        assert(x.equals("TSETTSET"))
      case None => assert(false)
    }
  }
}
