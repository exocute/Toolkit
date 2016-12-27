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

  def this(file: File, signalHost: String, dataHost: String, jarHost: String) = {
    this()
    SpaceCache.jarHost = jarHost
    init(readFile(file.toPath.toString), signalHost, dataHost)
  }

  def this(fileAsText: String, signalHost: String, dataHost: String) = {
    this()
    init(fileAsText, signalHost, dataHost)
  }

  def this(graph: GraphRep, signalHost: String, dataHost: String) = {
    this()
    setInfo(graph, signalHost, dataHost)
  }

  private def getGraphRep(parser: ActivityParser): Option[GraphRep] = {
    val res: Try[GraphRep] = parser.InputLine.run()
    res match {
      case Success(graph) => {
        if (graph.checkValidGraph()) Some(graph)
        else None
      }
      case _ =>
        println("ERRO")
        None
    }
  }

  private def setSignals(signalHost: String, dataHost: String) = {
    if (signalHost != null) SpaceCache.signalHost = signalHost
    if (dataHost != null) SpaceCache.dataHost = dataHost
  }

  private def init(fileAsText: String, signalHost: String, dataHost: String) = {
    val plnClean = clearCommnents(fileAsText)
    val parser = new ActivityParser(plnClean)
    getGraphRep(parser) match {
      case Some(x) => {
        setInfo(x, signalHost, dataHost)
      }
      case _ => ()
    }
  }

  private def setInfo(graph: GraphRep, signalHost: String, dataHost: String) = {
    graphRep = graph
    setSignals(signalHost, dataHost)
    val graphCreator = new GraphCreator()
    graphCreator.injectGraph(graph)
    injector = new CliftonInjector(graphCreator.getInjectMarker)
    collector = new CliftonCollector(graphCreator.getCollectMarker)
  }

  def getInject = injector

  def getCollector = collector

}
