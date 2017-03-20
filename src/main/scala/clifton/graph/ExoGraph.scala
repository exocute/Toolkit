package clifton.graph

import api.{Collector, Injector}
import toolkit.GraphRep

/**
  * Created by #GrowinScala
  */
trait ExoGraph {

  val graph: GraphRep

  val injector: Injector

  val collector: Collector

  def closeGraph(): Unit

}
