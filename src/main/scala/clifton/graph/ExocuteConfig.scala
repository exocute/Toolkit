package clifton.graph

import exonode.clifton.node.SpaceCache

/**
  * Created by #ScalaTeam on 07-02-2017.
  */
object ExocuteConfig {

  def setHosts(): StarterExoGraph = {
    setHosts("localhost", "localhost", "localhost")
  }

  def setHosts(signalHost: String, dataHost: String, jarHost: String): StarterExoGraph = {
    SpaceCache.signalHost = signalHost
    SpaceCache.dataHost = dataHost
    SpaceCache.jarHost = jarHost
    new StarterExoGraph
  }

}
