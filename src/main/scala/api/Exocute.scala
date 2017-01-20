package api

import java.io.File

import clifton.graph.{CliftonCollector, CliftonInjector}

import scala.util.Try

/**
  * Exocute is designed to make the job of distributing, coordinatating and executing computer
  * software over an arbitrary number of computing nodes simple. We call these processing elements
  * Activities. These can be written in either Java or a ‘host’ programming language such as C or
  * Fortran.
  *
  * Created by #ScalaTeam on 20/01/2017.
  */
trait Exocute {

  /**
    * The addGraphMethod allows you to starts a graph in the space
    * grpFile should follow the rules and all the documentation available HERE
    * Make sure you set all the spaces before used this method
    * Use Injector and Collector to inject and collect, respectively, inputs and
    * results from the space
    *
    * @param grpFile
    * @param jars
    * @return
    */
  def addGraph(grpFile: File, jars: List[File]): Try[(CliftonInjector, CliftonCollector)]

  /**
    * SignalSpace is responsible for saving the graph representation, log info
    * and other information changed between nodes.
    *
    * SignalSpace uses a FlySpace @License by ZinkDigital
    *
    * @param signal ip where the FlySpace is running
    */
  def setSignalSpace(signal: String)


  /**
    * JarSpace is responsible for saving the information of jars and classes used
    * by the graph.
    *
    * JarSpace uses a FlySpace @License by ZinkDigital
    *
    * @param signal ip where the FlySpace is running
    */
  def setJarSpace(signal: String)


  /**
    * DataSpace is responsible for saving the information of the inputs, intermidiate results
    * and final results
    *
    * @param signal ip where the FlySpace is running
    */
  def setDataSpace(signal: String)

}
