package clifton.graph

import java.io.Serializable
import java.util.UUID

import clifton.graph.exceptions.InjectException
import com.zink.fly.FlyPrime
import exonode.clifton.node.{DataEntry, ExoEntry, SpaceCache}

/**
  * Created by #ScalaTeam on 21/12/2016.
  */
class CliftonInjector(marker: String, rootAct: String) {

  val INJECTION_LEASE = 2 * 60 * 1000
  val space: FlyPrime = SpaceCache.getDataSpace

  def inject(input: Serializable): String = {
    val id = UUID.randomUUID().toString
    try {
      val dataEntry = new DataEntry(rootAct, marker, id, input)
      space.write(dataEntry, INJECTION_LEASE)
    } catch {
      case e: Exception => throw new InjectException("Internal Inject Error")
    }
    id
  }

  def inject(occurrences: Int, input: Serializable): Unit = {
    if (occurrences < 1) throw new InjectException("Too few occurrences. Occurrences should be >= 1")
    for {
      x <- 0 to occurrences
    } inject(input)
  }

  def inject(inputs: Array[Serializable]): Unit = {
    inputs.foreach(x => inject(x))
  }

  def getMarker = marker
}
