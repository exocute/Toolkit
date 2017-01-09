package clifton.nodes

import com.zink.fly.FlyPrime
import exonode.clifton.node.SpaceCache

/**
  * Created by #ScalaTeam on 20/12/2016.
  */
class SignalOutChannel(marker: String, entryLifeTime: Long = 60 * 1000) extends OutChannel(marker, entryLifeTime) {

  def getSpace: FlyPrime = SpaceCache.getSignalSpace

}