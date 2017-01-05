package api

import java.io.File
import java.util.UUID

import clifton._
import clifton.graph.{CliftonCollector, CliftonInjector, GraphCreator}
import clifton.nodes.{CliftonClassLoader, SpaceCache}
import com.zink.fly.FlyPrime
import distributer.{FlyClassEntry, FlyJarEntry, JarSpaceUpdater}
import toolkit.{ActivityParser, GraphRep}

import scala.util.{Success, Try}

/**
  * Created by #ScalaTeam on 02/01/2017.
  */
class StarterExoGraph(signalHost: String, dataHost: String, jarHost: String) {

  val TIME: Long = 10 * 60 * 1000

  val jarUpdater = new JarSpaceUpdater(jarHost)

  setSignals()

  private val space: FlyPrime = SpaceCache.getSignalSpace

  def loadJars(jars: List[File]): Unit = {
    jars.foreach(file => jarUpdater.updateJarEntry(file))
  }

  def addGraph(grp: File, jars: List[File], time: Int): (CliftonInjector, CliftonCollector) = {

    init(readFile(grp.toPath.toString)) match {
      case Some((x: CliftonInjector, y: CliftonCollector, grp: GraphRep, id)) =>
        loadJars(jars)

        //gets the space ready for nodes to start interact
        new GrpChecker(new GrpInfo(id, grp.getVectorActivities), space).start()

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

  private def init(fileAsText: String) = {
    val plnClean = clearCommnents(fileAsText)
    val parser = new ActivityParser(plnClean)
    getGraphRep(parser) match {
      case Some(graphREP) =>
        val graphCreator = new GraphCreator()
        graphCreator.injectGraph(graphREP)
        val id = UUID.randomUUID().toString
        Some(new CliftonInjector(id), new CliftonCollector(id), graphREP, id)
      case _ => None
    }
  }

  private def setSignals() = {
    if (signalHost != null) SpaceCache.signalHost = signalHost
    if (dataHost != null) SpaceCache.dataHost = dataHost
    if (jarHost != null) SpaceCache.jarHost = jarHost
  }

}
