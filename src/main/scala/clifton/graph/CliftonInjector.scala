package clifton.graph

import clifton.nodes.{ExoEntry, SpaceCache}
import java.io.Serializable

import clifton.graph.exceptions.InjectException
import com.zink.fly.FlyPrime

/**
  * Created by #ScalaTeam on 21/12/2016.
  */
class CliftonInjector(marker : String) {

  var ent = new ExoEntry(marker,null)
  val INJECTION_LEASE = 2 *60 * 1000

  def inject(input: Serializable) =  {
    ent.payload = input

    val space : FlyPrime = SpaceCache.getDataSpace

    try{
      space.write(ent,INJECTION_LEASE)
    } catch {
      case e:Exception => throw new InjectException("Internal Inject Error")
    }
  }

  def inject(ocurrences: Int,input:Serializable) = {
    if(ocurrences<1) throw new InjectException("Too few occurrences. Occurrences should be >= 1")
    for{
      x <- 0 to ocurrences
    } inject(input)
  }

  def inject(inputs:Array[Serializable]) = {
    inputs.foreach(x=>inject(x))
  }
}
