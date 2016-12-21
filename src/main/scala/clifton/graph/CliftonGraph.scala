package clifton.graph

import java.io.{File, FileReader, Reader, StringReader}

import toolkit.GraphRep

/**
  * Created by #ScalaTeam on 21/12/2016.
  */
class CliftonGraph {

  var injector: CliftonInjector = _
  var collector: CliftonCollector = _

  def this(file: File, signalHost: String = null, dataHost: String = null) = {
    this()
    init(new FileReader(file), signalHost, dataHost)
  }

  def this(fileAsText: String, signalHost: String = null, dataHost: String = null) = {
    this()
    init(new StringReader(fileAsText), signalHost, dataHost)
  }

  def this(graph : GraphRep , signalHost : String = null , dataHost : String = null) = {
    this()
    init(graph,signalHost,dataHost)
  }

  def init(reader: Reader, signalHost: String, dataHost: String) = {
    //TODO
  }

  def init(reader: GraphRep, signalHost: String, dataHost: String) = {
    //TODO
  }
}
