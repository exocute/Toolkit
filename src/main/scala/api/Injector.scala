package api

import java.io.Serializable

/**
  *
  * Injector is a simple trait to a simple use of the CliftonInjector
  * Injector allows you to inject inputs to the space
  *
  * Created by #ScalaTeam on 20/01/2017.
  */
trait Injector {

  /**
    * The inject method will insert something to be processed by the exocute
    *
    * @param input
    * @return
    */
  def inject(input: Serializable): String


  /**
    * The inject method will insert into the space occurrences times of the
    * some input to be processed
    *
    * @param occurrences
    * @param input
    */
  def inject(occurrences: Int, input: Serializable): Unit


  /**
    * The inject method will insert into the space a set of inputs to be processed
    *
    * @param inputs
    */
  def inject(inputs: Array[Serializable]): Unit

}
