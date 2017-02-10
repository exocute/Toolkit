package clifton.graph

import java.io.File
import java.util.UUID

import api.{Collector, Injector}
import clifton.utilities.Utilities
import toolkit.{ActivityParser, GraphRep}

import scala.util.{Failure, Success, Try}

/**
  * Created by #ScalaTeam on 02/01/2017.
  *
  * Receives a graph and loads the jar, inserts the representation of every activity of the graph in space and returns
  * an Injector and Collector to interact with the graph and the API
  */
class StarterExoGraph {

  /**
    * Loads the jar files into the jar space and the grp file representation into the signal space.
    *
    * @param grpFile the file in grp format
    * @param jars    the jar files to be loaded
    * @return A pair with the injector and the collector
    */
  def addGraph(grpFile: File, jars: List[File], graphTimeOut: Long): Try[ExoGraph] = {
    addGraph(Utilities.readFile(grpFile), jars, graphTimeOut)
  }

  def addGraph(grpFileText: String, jars: List[File], graphTimeOut: Long): Try[ExoGraph] = {
    init(grpFileText).map {
      case (graph: GraphRep, graphId: String) =>
        new ExoGraph(jars, graph, graphId, graphTimeOut)
    }
  }

  private def getGraphRep(parser: ActivityParser): Try[GraphRep] = {
    val res: Try[GraphRep] = parser.InputLine.run()

    res.flatMap(graph => {
      if (graph.checkValidGraph()) Success(graph)
      else Failure(new Exception("Graph is not valid"))
    })
  }

  private def init(fileAsText: String): Try[(GraphRep, String)] = {
    val plnClean = Utilities.clearCommnents(fileAsText)
    val parser = new ActivityParser(plnClean)
    getGraphRep(parser).map(graphRep => {
      val graphId = UUID.randomUUID().toString
      (graphRep, graphId)
    })
  }

}

object StarterExoGraph {
  type GraphIO = (Injector, Collector)
}