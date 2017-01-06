package api

import clifton.nodes.{AnaliserNode, CliftonNode,SpaceCache}

/**
  * Created by #ScalaTeam on 05/01/2017.
  */
object startExoNode {

  def main(args: Array[String]): Unit = {
    SpaceCache.signalHost = "localhost"
    SpaceCache.dataHost = "192.168.1.126"
    SpaceCache.jarHost = "localhost"

    new AnaliserNode(List("A","B","C"), "X").start()

    for{
      x <- 1 to 100
    } new CliftonNode().start()


  }

}
