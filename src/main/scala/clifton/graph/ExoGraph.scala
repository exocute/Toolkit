package clifton.graph

import api.{Collector, Injector}
import toolkit.ValidGraphRep

/**
  * Created by #GrowinScala
  */
trait ExoGraph {

  val graph: ValidGraphRep

  val injector: Injector

  val collector: Collector

  def closeGraph(): Unit

}
