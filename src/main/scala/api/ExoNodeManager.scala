package api

import clifton.nodes.{ExoEntry, SpaceCache}
import clifton.signals.{BootSignal, ProcessActivitySignal}

import scala.collection.mutable

/**
  * Created by #ScalaTeam on 02/01/2017.
  */
object ExoNodeManager extends Thread {

  val newNodes = new mutable.Queue[String]()
  val usedNodes = new mutable.Queue[String]()
  val deadNodes = new mutable.Queue[String]()
  val space = SpaceCache.getSignalSpace
  val TIME = 60 * 1000
  val grpFiles = new mutable.Queue[grpInfo]()

  override def run(): Unit = {
    val tmpl = new ExoEntry("MANAGER", null)
    while (true) {

      //look for new nodes

      val res = space.take(tmpl, TIME)
      if (res != null) {
        space.take(tmpl, TIME).payload match {
          case BootSignal(x) => newNodes+=x
          case _ => ???
        }
      }

      if(!grpFiles.isEmpty){
        val name = grpFiles.head.getName
        if(grpFiles.head.getActID.size <= newNodes.size) {
          grpFiles.head.getActID.foreach(x => {
            space.write(new ExoEntry(newNodes.dequeue(), ProcessActivitySignal(name + ":" + x)), TIME)
          })
          grpFiles.dequeue()
        }
      }
    }
  }

  def addGRP(grp : grpInfo) = grpFiles+=grp


}
