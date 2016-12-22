package clifton.graph

import java.io.Serializable

import clifton.graph.exceptions.{CollectException, InjectException}
import clifton.nodes.{ExoEntry, SpaceCache}
import com.zink.fly.FlyPrime

/**
  * Created by #ScalaTeam on 21/12/2016.
  */
class CliftonCollector(marker : String) {

  var tpl = new ExoEntry(marker,null)

  def collect : Serializable = {
    collect(0L)
  }

  def collect(waitTime : Long) : Serializable = {
    val space : FlyPrime = SpaceCache.getDataSpace
    val ent : ExoEntry = new ExoEntry()
    try{
      val ent = space.take(tpl,waitTime)
    } catch{
      case e : Exception => throw new CollectException("Collector Error")
    }
    if(ent != null) ent.payload
    else null
  }

  def collect(numObjects : Int, waitTime : Long) : Serializable = {
    var serializable: List[Serializable] = Nil
    val start = System.currentTimeMillis()
    var remainingTime = waitTime
    var totalObjects = 0

    while (totalObjects < numObjects && remainingTime > 1L) {
      val ser: Serializable = collect
      if (ser != null) {
        serializable = ser :: serializable
        totalObjects += 1
      } else {
        try {
          Thread.sleep(1)
        } catch {
          case e: InterruptedException => e.printStackTrace()
        }
      }
      remainingTime = waitTime - (System.currentTimeMillis() - start)
    }
  serializable
  }

}
