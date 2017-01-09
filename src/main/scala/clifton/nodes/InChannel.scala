package clifton.nodes

import clifton.nodes.exceptions.SpaceNotDefined
import com.zink.fly.FlyPrime
import java.io.Serializable

import exonode.clifton.node.ExoEntry

/**
  * Created by #ScalaTeam on 20/12/2016.
  */
abstract class InChannel(marker: String) {

  private val space: FlyPrime = getSpace
  private val tpl: ExoEntry = new ExoEntry(marker, null)

  def getSpace: FlyPrime

  private var TAKE_TIME = 0

  def getObject: Serializable = {
    if (space != null) {
      var result = new ExoEntry()
      try {
        result = space.take(tpl, TAKE_TIME)
      } catch {
        case e: Exception => e.printStackTrace()
      }

      if (result != null) {
        TAKE_TIME = 0
        try {
          return result.payload
        } catch {
          case e: Exception => e.printStackTrace()
        }
      } else TAKE_TIME = 1000
    }
    throw new SpaceNotDefined("SignalSpace")
  }

}


