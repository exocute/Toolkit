package api

import clifton.nodes.AnaliserNode
import exonode.clifton.node.{CliftonNode, SpaceCache}

/**
  * Created by #ScalaTeam on 05/01/2017.
  */
object startExoNode {

  def main(args: Array[String]): Unit = {

    println("  ______           _   _           _         _____ _             _            \n |  ____|         | \\ | |         | |       / ____| |           | |           \n | |__  __  _____ |  \\| | ___   __| | ___  | (___ | |_ __ _ _ __| |_ ___ _ __ \n |  __| \\ \\/ / _ \\| . ` |/ _ \\ / _` |/ _ \\  \\___ \\| __/ _` | '__| __/ _ \\ '__|\n | |____ >  < (_) | |\\  | (_) | (_| |  __/  ____) | || (_| | |  | ||  __/ |   \n |______/_/\\_\\___/|_| \\_|\\___/ \\__,_|\\___| |_____/ \\__\\__,_|_|   \\__\\___|_|   \n                                                                              \n                                                                              ")

    println("Press ENTER to set default parameters to the spaces")
    print("JarSpace IP\n>")
    val ipJar = scala.io.StdIn.readLine()
    if (ipJar != "") {
      setInput("jar", ipJar)
      print("SignalSpace IP\n>")
      val ipSignal = scala.io.StdIn.readLine()
      setInput("signal", ipSignal)
      print("DataSpace IP\n>")
      val ipData = scala.io.StdIn.readLine()
      setInput("data", ipData)
    }else println("SPACES SET TO DEFAULT PARAMETERS\n")

    println("SignalHost -> "+SpaceCache.signalHost)
    println("JarHost -> "+SpaceCache.jarHost)
    println("DataHost -> "+SpaceCache.dataHost)

    print("\nSelect the number of nodes you want to start\n>")
    val nodes = scala.io.StdIn.readLine()
    if(isAllDigits(nodes)){
      for{
        x <- 1 to nodes.toInt
      } new CliftonNode()

      println("Started "+nodes+" nodes.")
    } else println("Not a valid number of nodes")

  }

  def isAllDigits(x: String) = x forall Character.isDigit

  def setInput(space: String, ip: String) = {
    if (ip != null)
      setSpace(space, ip)
  }

  def setSpace(space: String, ip: String) = {
    space match {
      case "jar" => SpaceCache.jarHost = ip
      case "signal" => SpaceCache.signalHost = ip
      case "data" => SpaceCache.dataHost = ip
    }
  }

}
