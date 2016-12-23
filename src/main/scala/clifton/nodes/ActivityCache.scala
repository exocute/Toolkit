package clifton.nodes

import exocuteCommon.activity.Activity

import scala.collection.mutable

/**
  * Created by #ScalaTeam on 20-12-2016.
  */


object ActivityCache {

  private val _cache = new mutable.HashMap[String, Activity]()


  def getActivity(name: String): Option[Activity] = {
    _cache.get(name) match {
      case None => {
        try {
          // go get the required jar
          val cl = new CliftonClassLoader()
          val jar = CliftonClassLoader.getJarFromSpace(name)
          if (jar != null) {
            val acl = cl.loadClass(name)
            acl.newInstance match {
              case activity: Activity =>
                _cache.put(activity.getClass.getName, activity)
                Some(activity)
              case _ => None
            }
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
