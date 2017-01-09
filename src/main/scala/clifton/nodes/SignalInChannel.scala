package clifton.nodes

import com.zink.fly.FlyPrime
import exonode.clifton.node.SpaceCache

/**
  * Created by #ScalaTeam on 20/12/2016.
  */
class SignalInChannel(marker: String) extends InChannel(marker) {

  def getSpace: FlyPrime = SpaceCache.getSignalSpace

}
