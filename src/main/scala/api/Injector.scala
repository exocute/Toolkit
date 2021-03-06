package api

import java.io.Serializable

/**
  * Created by #GrowinScala
  *
  * Injector is a simple trait to be used by the CliftonInjector
  * Injector allows you to inject inputs into the space
  */
trait Injector {

  def canInject: () => Boolean

  /**
    * Inserts into the space the input to be processed by the exocute platform
    *
    * @param input the input can be of any type that is serializable
    * @return The inject identifier for this input
    */
  def inject(input: Serializable): Int

  /**
    * Inserts into the space the same input occurrences times
    * to be processed by the exocute platform
    *
    * @param occurrences the number of times
    * @param input       the input can be of any type that is serializable
    * @return A list of inject identifiers for each input added
    */
  def inject(occurrences: Int, input: Serializable): Iterable[Int]

  /**
    * The injectMany will insert into the space a set of inputs to be processed
    *
    * @param inputs the collection of inputs. The input can be of any type that is serializable
    * @return inject identifier for each input
    */
  def injectMany(inputs: Iterable[Serializable]): Vector[Int]

}
