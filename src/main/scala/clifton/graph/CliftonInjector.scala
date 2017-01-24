package clifton.graph

import java.io.Serializable
import java.util.UUID

import api.Injector
import clifton.graph.exceptions.InjectException
import exonode.clifton.Protocol._
import exonode.clifton.node.SpaceCache
import exonode.clifton.node.entries.DataEntry

/**
  * Created by #ScalaTeam on 21/12/2016.
  *
  * Injects input into the data space
  */
class CliftonInjector(marker: String, rootActivity: String) extends Injector {

  private val dataSpace = SpaceCache.getDataSpace
  private val templateData: DataEntry = DataEntry(rootActivity, marker, null, null)

  def inject(input: Serializable): String = {
    val id = UUID.randomUUID().toString
    try {
      val dataEntry = templateData.setInjectId(id).setData(input)
      dataSpace.write(dataEntry, INJECTOR_LEASE_TIME)
    } catch {
      case e: Exception => throw new InjectException("Internal Inject Error")
    }
    id
  }

  def inject(occurrences: Int, input: Serializable): Iterable[String] = {
    if (occurrences < 1)
      throw new InjectException("Too few occurrences. Occurrences should be >= 1")
    else
      (0 to occurrences).map(_ => inject(input))
  }

  def inject(inputs: Iterable[Serializable]): Iterable[String] = {
    inputs.map(x => inject(x))
  }
}
