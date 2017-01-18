package clifton.graph

import java.io.Serializable
import java.util.UUID

import clifton.graph.exceptions.InjectException
import com.zink.fly.FlyPrime
import exonode.clifton.node.{DataEntry, SpaceCache}
import exonode.clifton.Protocol._

/**
  * Created by #ScalaTeam on 21/12/2016.
  *
  * Injects into the space the input
  */
class CliftonInjector(marker: String, rootActivity: String) {

  private val space = SpaceCache.getDataSpace
  private val templateData: DataEntry = DataEntry(rootActivity, marker, null, null)

  def inject(input: Serializable): String = {
    val id = UUID.randomUUID().toString
    try {
      val dataEntry = templateData.setInjectId(id).setData(input)
      space.write(dataEntry, INJECTOR_LEASE_TIME)
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
}
