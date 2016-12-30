package ActivitiesInjectorAndCollector

import java.util.UUID
import com.zink.fly.FlyPrime

/**
  * Created by #EduardoRodrigues on 29/12/2016.
  */

class Injector(space: FlyPrime, init: String) {
  val PUTIME = 10 * 60 * 1000
  def put(input: String) = {
    val id = UUID.randomUUID().toString
    space.write(new info(List(init),"<",input, id),PUTIME)
    id
  }
}
