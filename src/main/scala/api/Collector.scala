package api

import java.io.Serializable

/**
  * Collector is a simple trait to use of the CliftonCollector
  * Collector allows you to collect results from the space
  *
  * Created by #ScalaTeam on 20/01/2017.
  */
trait Collector {

  /**
    * Will take from the space the first result that has already finished
    * all the processing.
    *
    * @return if there is nothing to collect it will return None, otherwise it will
    *         return Some(result)
    */
  def collect(): Option[Serializable]

  /**
    * Will take from the space the first result that is available
    * in the first waitTime ms.
    *
    * @param waitTime wait time in milliseconds
    * @return if there is nothing to collect it will return None, otherwise it will
    *         return Some(result)
    */
  def collect(waitTime: Long): Option[Serializable]

  /**
    * Returns the result with a specific inject index if it is available in the first waitTime ms.
    *
    * @param injectIndex the inject index of the result
    * @param waitTime    wait time in milliseconds
    * @return if there is nothing to collect it will return None, otherwise it will
    *         return Some(result)
    */
  def collectIndex(injectIndex: Int, waitTime: Long = 0): Option[Serializable]

  /**
    * Returns at most numObjects from the space that are available
    * in waitTime ms (must be > 0).
    * <p>
    * The collect method will return a list of objects
    * If numObjects is bigger than number of available objects, collect method
    * will return a List with all the available objects that were possible to get
    * in waitTime ms
    *
    * @param numObjects the maximum amount of objects to be returned
    * @param waitTime   wait time in milliseconds
    * @return the list of objects returned
    */
  def collectMany(numObjects: Int, waitTime: Long): List[Serializable]

}
