package clifton.graph

import api.{Collector, Injector}
import toolkit.GraphRep

/**
  * Created by #ScalaTeam on 15-03-2017.
  */
trait ExoGraph {

  val graph: GraphRep

  val injector: Injector

  val collector: Collector

  def closeGraph(): Unit

}
