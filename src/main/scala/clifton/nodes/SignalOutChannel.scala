package clifton.nodes

import com.zink.fly.FlyPrime

/**
  * Created by #ScalaTeam on 20/12/2016.
  */
class SignalOutChannel(marker:String) extends OutChannel(marker){

  def getSpace : FlyPrime = SpaceCache.getSignalSpace

}