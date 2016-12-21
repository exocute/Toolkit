package clifton.nodes

import exocuteCommon.activity.Activity

import scala.collection.mutable

/**
  * Created by #ScalaTeam on 20-12-2016.
  */

class ActivityCache

object ActivityCache {

  private val _cache = new mutable.HashMap[String, Activity]()

//  def getActivity(name: String): Activity = {
//    getActivityAux(name).get
//  }

  def getActivity(name: String): Option[Activity] = {
    _cache.get(name) match {
      case None => {
        try {
          // go get the required jar
          val jar = CliftonClassLoader.getJarFromSpace(name)
          if (jar != null) {
            val acl = classOf[ActivityCache].getClassLoader
            val initMethod = acl.getClass.getMethod("init", new Array[Byte](1).getClass)
            initMethod.invoke(acl, Array[AnyRef](jar))
            // make a class loader for the jar
            val clazz = Class.forName(name)
            // load class
            val activity = clazz.newInstance.asInstanceOf[Activity]
            _cache.put(activity.getClass.getName, activity)
            Some(activity)
          } else
            None
        } catch {
          case e: Exception => {
            e.printStackTrace()
            None
          }
        }
      }
      case some => some
    }

  }

}
