package ActivitiesInjectorAndCollector

import com.zink.fly.FlyPrime

/**
  * Created by #EduardoRodrigues on 29/12/2016.
  */
class Collector(space : FlyPrime, end : String) {
  val TAKETIME = 10
  val infoTakeTemplate = info(List("<"),null,null,null)
  def get : String = {
    val res = space.take(infoTakeTemplate,TAKETIME)
    if(res==null) "Nothing to collect" else res.res
  }

  def get(id : String) = {
    val res = space.take(info(List("<"),null,null,id),TAKETIME)
    if(res==null) "Nothing to collect or Invalid ID." else res.res
  }
}
