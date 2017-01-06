package clifton.graph

import clifton.nodes.{ExoEntry, SpaceCache}
import java.io.Serializable
import java.util.UUID

import api.DataSignal
import clifton.graph.exceptions.InjectException
import com.zink.fly.FlyPrime

/**
  * Created by #ScalaTeam on 21/12/2016.
  */
class CliftonInjector(marker: String, rootAct : String) {

  var ent = new ExoEntry(rootAct, null)
  val INJECTION_LEASE = 2 * 60 * 1000
  val space: FlyPrime = SpaceCache.getDataSpace

  def inject(input: Serializable): Unit = {

    ent.payload = new DataSignal(rootAct, marker, input, UUID.randomUUID().toString)

    try {
      println(ent)
      space.write(ent, INJECTION_LEASE)
    } catch {
      case e: Exception => throw new InjectException("Internal Inject Error")
    }
  }

  def inject(ocurrences: Int, input: Serializable): Unit = {
    if (ocurrences < 1) throw new InjectException("Too few occurrences. Occurrences should be >= 1")
    for {
      x <- 0 to ocurrences
    } inject(input)
  }

  def inject(inputs: Array[Serializable]): Unit = {
    inputs.foreach(x => inject(x))
  }

  def getMarker = marker
}
