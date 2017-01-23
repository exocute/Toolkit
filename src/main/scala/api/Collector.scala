package api

import java.io.Serializable


/**
  *
  * Collector is a simple trait to a simple use of the CliftonCollector
  * Collector allows you to collect results from the space
  *
  * Created by #ScalaTeam on 20/01/2017.
  */
trait Collector {

  /**
    * The collect method will immediately take from the space one input that already finished
    * the all process.
    * If there is nothing to collect it will return a None, otherwise it will
    * return a Some(res)
    *
    * @return
    */
  def collect(): Option[Serializable]

  /**
    * The collect method will take the first the result from the space that is available
    * in the first waitTime.
    * If there is nothing to collect it will return a None, otherwise it will
    * return a Some(res)
    *
    * @param waitTime
    * @return
    */
  def collect(waitTime: Long): Option[Serializable]


  /**
    * The collect method will take numObjects from the space that are available
    * in waitTime.
    * The collect method will return a list of Objects
    * If numObjects is bigger than number of available objects, collect method
    * will return a List with all the available objects that were possible to get
    * in waitTime
    *
    * @param numObjects
    * @param waitTime
    * @return
    */
  def collect(numObjects: Int, waitTime: Long): List[Serializable]

}
