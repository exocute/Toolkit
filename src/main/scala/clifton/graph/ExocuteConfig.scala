package clifton.graph

import exonode.clifton.node.SpaceCache

/**
  * Created by #GrowinScala
  */
object ExocuteConfig {

  def setDefaultHosts(): Unit = {
    setHosts("localhost", "localhost", "localhost")
  }

  /**
    * FlySpace @License by ZinkDigital
    *
    * @param signalHost
    * SignalSpace is responsible for saving the graph representation, log info
    * and other information exchanged between nodes.
    * @param dataHost
    * DataSpace is responsible for saving the information of the inputs, intermediate results
    * and final results.
    * @param jarHost
    * JarSpace is responsible for saving the information of jars and classes used
    * by the graph.
    */
  def setHosts(signalHost: String = SpaceCache.signalHost,
               dataHost: String = SpaceCache.dataHost,
               jarHost: String = SpaceCache.jarHost): Unit = {
    SpaceCache.signalHost = signalHost
    SpaceCache.dataHost = dataHost
    SpaceCache.jarHost = jarHost
  }

}
