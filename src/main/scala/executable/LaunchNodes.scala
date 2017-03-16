package executable

import exonode.clifton.node.CliftonNode
import exonode.clifton.node.SpaceCache
import exonode.clifton.node.entries.ExoEntry
import exonode.clifton.signals.KillSignal

/**
  * Created by #GrowinScala
  */
object LaunchNodes {

  def startNodes(n: Int): Unit = {
    for (x <- 0 until n)
      new CliftonNode().start()
  }

  def killNode(id : String) : Unit = {
    SpaceCache.getSignalSpace.write(ExoEntry(id,KillSignal),1000*60)
  }

}
