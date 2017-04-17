package api

import java.io.Serializable

/**
  * Created by #GrowinScala
  *
  * Collector is a simple trait to use of the CliftonCollector
  * Collector allows you to collect results from the space
  */
trait Collector {

  def canCollect: () => Boolean

  /**
    * Will take from the space the first result that has already finished
    * all the processing.
    *
    * @return if there is nothing to collect it will return None, otherwise it will
    *         return Some(result)
    */
  def collect(): Option[Option[Serializable]]

  /**
    * Will take from the space the first result that is available
    * in the first waitTime ms.
    *
    * @param waitTime wait time in milliseconds
    * @return if there is nothing to collect it will return None, otherwise it will
    *         return Some(result)
    */
  def collect(waitTime: Long): Option[Option[Serializable]]

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

  /**
    * Returns at most numObjects from the space that are available
    * in waitTime ms (must be > 0).
    * <p>
    * The results will maintain the order of the injector.
    * If you use other methods to collect this method will have an undefined behavior.
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
  def collectManyOrdered(numObjects: Int, waitTime: Long): List[Serializable]

  /**
    * Returns all the results with a specific inject id.
    *
    * @param injectIndex the inject index of the result
    * @return if there is nothing to collect it will return None, otherwise it will
    *         return Some(result)
    */
  def collectAllByIndex(injectIndex: Int): List[Serializable]

}
