package executable

import java.io.File
import java.util.UUID

import api.Exocute
import clifton.graph.{CliftonCollector, CliftonInjector, GraphCreator}
import clifton.utilities.Utilities
import distributer.JarSpaceUpdater
import exonode.clifton.Protocol._
import exonode.clifton.node.entries.ExoEntry
import exonode.clifton.node.SpaceCache
import toolkit.{ActivityParser, GraphRep}

import scala.util.{Failure, Success, Try}

/**
  * Created by #ScalaTeam on 02/01/2017.
  *
  * Receives a graph and loads the jar, inserts the representation of every activity of the graph in space and returns
  * an Injector and Collector to interact with the graph and the API
  */
class StarterExoGraph extends Exocute {

  private val jarUpdater = new JarSpaceUpdater()

  private val signalSpace = SpaceCache.getSignalSpace

  /**
    * Loads the jar files into the jar space and the grp file representation into the signal space.
    *
    * @param grpFile the file  in grp format
    * @param jars    the jar files to be loaded
    * @return A pair with the injector and the collector
    */
  def addGraph(grpFile: File, jars: List[File]): Try[(CliftonInjector, CliftonCollector)] = {

    init(Utilities.readFile(grpFile)).map {
      case (inj: CliftonInjector, coll: CliftonCollector, grp: GraphRep, graphId: String) => {

        loadJars(jars)

        addGraphToSpace(grp, graphId)

        (inj, coll)
      }
    }
  }

  def startGrpChecker(): Unit = new GrpChecker().start()

  private def addGraphToSpace(grp: GraphRep, graphId: String) = {
    val activities = grp.getActivities
    val exoEntry = ExoEntry(GRAPH_MARKER, (graphId, activities))
    signalSpace.write(exoEntry, ACT_SIGNAL_LEASE_TIME)
  }

  private def loadJars(jars: List[File]): Unit = {
    jars.foreach(file => jarUpdater.update(file))
  }

  private def getGraphRep(parser: ActivityParser): Try[GraphRep] = {
    val res: Try[GraphRep] = parser.InputLine.run()

    res.flatMap(graph => {
      if (graph.checkValidGraph()) Success(graph)
      else Failure(new Exception("Graph is not valid"))
    })
  }

  private def init(fileAsText: String): Try[(CliftonInjector, CliftonCollector, GraphRep, String)] = {
    val plnClean = Utilities.clearCommnents(fileAsText)
    val parser = new ActivityParser(plnClean)
    getGraphRep(parser).map(graphRep => {
      val graphCreator = new GraphCreator()
      val graphId = UUID.randomUUID().toString
      graphCreator.injectGraph(graphRep, graphId)
      val injector = new CliftonInjector(graphId + ":" + INJECT_SIGNAL_MARKER, graphId + ":" + graphRep.getRoot.get.id)
      val collector = new CliftonCollector(graphId + ":" + COLLECT_SIGNAL_MARKER)
      (injector, collector, graphRep, graphId)
    })
  }

  def setSignalSpace(signal: String): Unit = {
    SpaceCache.signalHost = signal
  }

  def setJarSpace(signal: String): Unit = {
    SpaceCache.jarHost = signal
  }

  def setDataSpace(signal: String): Unit = {
    SpaceCache.dataHost = signal
  }

}
