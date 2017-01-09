package api

import clifton.nodes.AnaliserNode
import exonode.clifton.node.{CliftonNode, SpaceCache}

/**
  * Created by #ScalaTeam on 05/01/2017.
  */
object startExoNode {

  def main(args: Array[String]): Unit = {
    SpaceCache.signalHost = "localhost"
    SpaceCache.dataHost = "localhost"
    SpaceCache.jarHost = "localhost"
    val NODES = 5

    new AnaliserNode(List("A", "B", "C"), "X").start()

    println("Started " + NODES + " nodes...")
    for {
      x <- 1 to NODES
    } new CliftonNode().start()

  }

}
