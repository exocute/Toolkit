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

import scala.util.{Success, Try}

/**
  * Created by #ScalaTeam on 02/01/2017.
  */
class StarterExoGraph {

  //FIXME: there is no error handling in this class! Use Try instead of Option ?

  val jarUpdater = new JarSpaceUpdater(null)
  val fileHandler = new JarFileHandler()

  private val signalSpace: FlyPrime = SpaceCache.getSignalSpace

  def loadJars(jars: List[File]): Unit = {
    jars.foreach(file => jarUpdater.update(file))
  }

  def addGraph(grp: File, jars: List[File]): (CliftonInjector, CliftonCollector) = {

    init(readFile(grp.toPath.toString)) match {
      case Some((x: CliftonInjector, y: CliftonCollector, grp: GraphRep, graphId)) =>

        loadJars(jars)

        //gets the space ready for nodes to start interact
        new GrpChecker(new GrpInfo(graphId, grp.getVectorActivities), signalSpace).start()

        (x, y)
      case _ => throw new Exception
    }
  }

  private def getGraphRep(parser: ActivityParser): Option[GraphRep] = {
    val res: Try[GraphRep] = parser.InputLine.run()
    res match {
      case Success(graph) => {
        if (graph.checkValidGraph()) Some(graph)
        else None
      }
      case _ =>
        println("ERROR")
        None
    }
  }

  private def init(fileAsText: String): Option[(CliftonInjector, CliftonCollector, GraphRep, String)] = {
    val plnClean = clearCommnents(fileAsText)
    val parser = new ActivityParser(plnClean)
    getGraphRep(parser) match {
      case Some(graphRep) =>
        val graphCreator = new GraphCreator()
        graphCreator.injectGraph(graphRep)
        val id = UUID.randomUUID().toString
        val injector = new CliftonInjector(Protocol.INJECT_SIGNAL_MARKER, graphRep.getRoot.get.id)
        val collector = new CliftonCollector(Protocol.COLLECT_SIGNAL_MARKER)
        Some(injector, collector, graphRep, id)
      case _ => None
    }
  }

}
