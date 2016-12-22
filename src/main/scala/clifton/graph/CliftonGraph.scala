package clifton.graph

import java.io.{File, FileReader, Reader, StringReader}

import clifton.nodes.SpaceCache
import toolkit.{ActivityParser, GraphRep}
import clifton.{clearCommnents, readFile}

import scala.util.{Success, Try}

/**
  * Created by #ScalaTeam on 21/12/2016.
  */
class CliftonGraph {

  private var graphRep: GraphRep = _
  private var injector: CliftonInjector = _
  private var collector: CliftonCollector = _

  def this(file: File, signalHost: String = null, dataHost: String = null) = {
    this()
    init(readFile(file.toPath.toString), signalHost, dataHost)
  }

  def this(fileAsText: String, signalHost: String = null, dataHost: String = null) = {
    this()
    init(fileAsText, signalHost, dataHost)
  }

  def this(graph: GraphRep, signalHost: String = null, dataHost: String = null) = {
    this()
    setInfo(graph, signalHost, dataHost)
  }

  def getGraphRep(parser: ActivityParser): Option[GraphRep] = {
    val res: Try[GraphRep] = parser.InputLine.run()
    res match {
      case Success(graph) => {
        if (graph.checkValidGraph()) Some(graph)
        else None
      }
      case _ => None
    }
  }

  def setSignals(signalHost: String, dataHost: String) = {
    if (!signalHost.isEmpty) SpaceCache.signalHost = signalHost
    if (!dataHost.isEmpty) SpaceCache.dataHost = dataHost
  }

  def init(fileAsText: String, signalHost: String, dataHost: String) = {
    val plnClean = clearCommnents(fileAsText)
    val parser = new ActivityParser(plnClean)
    getGraphRep(parser) match {
      case Some(x) => {
        setInfo(x, signalHost, dataHost)
      }
      case _ => ()
    }
  }

  def setInfo(graph: GraphRep, signalHost: String, dataHost: String) = {
    graphRep = graph
    setSignals(signalHost, dataHost)
    val graphCreator = new GraphCreator()
    graphCreator.injectGraph(graph)
    injector = new CliftonInjector(graphCreator.getInjectMarker)
    collector = new CliftonCollector(graphCreator.getCollectMarker)
  }

}
