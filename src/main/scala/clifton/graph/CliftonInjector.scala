package clifton.graph

import java.io.Serializable
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

import api.Injector
import clifton.graph.exceptions.InjectException
import exonode.clifton.config.Protocol._
import exonode.clifton.node.SpaceCache
import exonode.clifton.node.entries.DataEntry
import exonode.clifton.node.Log
import exonode.clifton.node.Log.{INFO, ERROR, WARN, ND}
import exonode.clifton.signals.LoggingSignal

/**
  * Created by #ScalaTeam on 21/12/2016.
  *
  * Injects input into the data space
  */
class CliftonInjector(uuid: String, marker: String, rootActivity: String) extends Injector {

  private val dataSpace = SpaceCache.getDataSpace
  private val templateData: DataEntry = DataEntry(rootActivity, marker, null, null)
  private val nextIndex = new AtomicInteger(0)

  def inject(input: Serializable): Int = {
    val currentIndex = nextIndex.getAndIncrement()
    val injectId = s"$uuid:$currentIndex"
    try {
      val dataEntry = templateData.setInjectId(injectId).setData(input)
      Log.receiveLog(LoggingSignal(INJECTED,INFO,ND,ND,ND,ND,ND,"Injected Input "+injectId,0))
      dataSpace.write(dataEntry, INJECTOR_LEASE_TIME)
    } catch {
      case e: Exception => throw new InjectException("Internal Inject Error")
    }
    currentIndex
  }

  def inject(occurrences: Int, input: Serializable): Iterable[Int] = {
    if (occurrences < 1)
      throw new InjectException("Too few occurrences. Occurrences should be >= 1")
    else
      (0 to occurrences).map(_ => inject(input))
  }

  def injectMany(inputs: Iterable[Serializable]): Vector[Int] = {
    inputs.map(x => inject(x)).toVector
  }

}
