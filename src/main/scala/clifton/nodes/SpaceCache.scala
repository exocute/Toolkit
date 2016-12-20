package clifton.nodes

import com.zink.fly.FlyPrime
import com.zink.fly.kit.{FlyFactory, FlyFinder}

import scala.collection.mutable.HashMap

/**
  * Created by #ScalaTeam on 20/12/2016.
  */
object SpaceCache {
  val data = "DataSpace"
  val jar = "JarSpace"
  val signal = "SignalSpace"
  var signalHost: String = ""
  var jarHost: String = ""
  var dataHost: String = ""
  val spaceMap = new HashMap[String, FlyPrime]()


  def getSpace(tag: String, host: String): Option[FlyPrime] = {
    spaceMap.get(tag) match {
      case Some(space) => Some(space)
      case None => {
        try {
          if (host.isEmpty) {
            val finder: FlyFinder = new FlyFinder()
            spaceMap.put(tag, finder.find(tag))
          } else spaceMap.put(tag, FlyFactory.makeFly(host))
          spaceMap.get(tag)
        } catch {
          case e: Exception => Log.error("Failed to locate space")
            None
        }
      }
    }
  }


  def getSignalSpace: FlyPrime = getSpace(signal, signalHost).get

  def getDataSpace: FlyPrime = getSpace(data, dataHost).get

  def getJarSpace: FlyPrime = getSpace(jar, jarHost).get
}
