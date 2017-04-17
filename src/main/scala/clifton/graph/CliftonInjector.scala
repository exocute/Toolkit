package clifton.graph

import java.io.Serializable
import java.util.concurrent.atomic.AtomicInteger

import api.Injector
import clifton.graph.exceptions.InjectException
import exonode.clifton.config.Protocol._
import exonode.clifton.node.Log.{INFO, ND}
import exonode.clifton.node.entries.DataEntry
import exonode.clifton.node.{Log, SpaceCache}
import exonode.clifton.signals.LoggingSignal

/**
  * Created by #GrowinScala
  *
  * Standard Injector <br>
  * Injects input into the data space
  */
class CliftonInjector(uuid: String, marker: String, rootActivity: String, val canInject: () => Boolean) extends Injector {

  private val dataSpace = SpaceCache.getDataSpace
  private val templateData: DataEntry = DataEntry(rootActivity, marker, null, null, null)
  private val nextIndex = new AtomicInteger(0)

  def inject(input: Serializable): Int = {
    if (!canInject())
      throw new InjectException("Internal Inject Error")

    val currentIndex = nextIndex.getAndIncrement()
    val injectId = s"$uuid:$currentIndex"
    val dataEntry = templateData.setInjectId(injectId).setOrderId(s"$currentIndex").setData(Some(input))
    try {
      Log.receiveLog(LoggingSignal(LOGCODE_INJECTED, INFO, ND, ND, ND, ND, ND, "Injected Input " + injectId, 0))
      dataSpace.write(dataEntry, INJECTOR_LEASE_TIME)
    } catch {
      case _: Exception => throw new InjectException("Internal Inject Error")
    }
    currentIndex
  }

  def inject(occurrences: Int, input: Serializable): Iterable[Int] = {
    if (occurrences < 1)
      throw new InjectException("Too few occurrences. Occurrences should be >= 1")
    else
      (0 until occurrences).map(_ => inject(input))
  }

  def injectMany(inputs: Iterable[Serializable]): Vector[Int] = {
    inputs.map(x => inject(x)).toVector
  }

}
