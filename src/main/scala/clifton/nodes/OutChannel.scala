package clifton.nodes

import clifton.nodes.exceptions.SpaceNotDefined
import com.zink.fly.FlyPrime
import exonode.clifton.node.ExoEntry

/**
  * Created by #ScalaTeam on 20/12/2016.
  */
abstract class OutChannel(marker: String, entryLifeTime: Long = 60 * 1000) {

  private val space: FlyPrime = getSpace
  private val out = new ExoEntry(marker, null)

  def getSpace: FlyPrime

  def putObject(obj: Serializable): Unit = {
    if (space != null && marker != null && obj != null) {
      out.payload = obj
      try {
        space.write(out, entryLifeTime)
      } catch {
        case e: Exception => throw new SpaceNotDefined("Output Channel Broken")
      }
    }
  }
}
