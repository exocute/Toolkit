package api

import java.io.File
import java.util.UUID

import clifton._
import clifton.graph.{CliftonCollector, CliftonInjector, GraphCreator}
import com.zink.fly.FlyPrime
import distributer.{JarFileHandler, JarSpaceUpdater}
import exonode.clifton.Protocol
import exonode.clifton.node.SpaceCache
import toolkit.{ActivityParser, GraphRep}
import utilities.Utilities

import scala.util.{Failure, Success, Try}

/**
  * Created by #ScalaTeam on 02/01/2017.
  *
  * Receives a graph and loads the jar, inserts the representation of every activity of the graph in space and returns
  * an Injector and Collector to interact with the graph and API
  */
class StarterExoGraph {

  private val jarUpdater = new JarSpaceUpdater()
  private val fileHandler = new JarFileHandler()

  private val signalSpace: FlyPrime = SpaceCache.getSignalSpace

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

        //gets the space ready for nodes to start to interact
        new GrpChecker(graphId, grp.getActivities, signalSpace).start()

        (inj, coll)
      }
    }
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
      graphCreator.injectGraph(graphRep)
      val id = UUID.randomUUID().toString
      val injector = new CliftonInjector(Protocol.INJECT_SIGNAL_MARKER, graphRep.getRoot.get.id)
      val collector = new CliftonCollector(Protocol.COLLECT_SIGNAL_MARKER)
      (injector, collector, graphRep, id)
    })
  }

}
