package api

import java.io.File

import com.zink.fly.FlyPrime
import exonode.clifton.node.{DataEntry, ExoEntry, Log, SpaceCache}
import exonode.distributer.{FlyClassEntry, FlyJarEntry}

/**
  * Created by #ScalaTeam on 04/01/2017.
  */
object startClientAPI {

  def cleanSpaces(): Unit = {
    def clean(space: FlyPrime, cleanTemplate: Any): Unit = {
      while (space.take(cleanTemplate, 0) != null) {}
    }

    clean(SpaceCache.getJarSpace, new FlyJarEntry(null, null))
    clean(SpaceCache.getJarSpace, new FlyClassEntry(null, null))
    clean(SpaceCache.getDataSpace, new DataEntry())
    clean(SpaceCache.getSignalSpace, new ExoEntry(null, null))
  }

  def main(args: Array[String]): Unit = {
    val startExo = new StarterExoGraph("localhost", "localhost", "localhost")

    cleanSpaces()

    LogProcessor.start()

//    val exampleName = "ab3c"
    val exampleName = "numbers"

    val file = new File(s"examples${File.separator}$exampleName.grp")
    val jars = List(new File(s"examples${File.separator}classes.jar"))

    println("Adding graph...")
    val (inj, col) = startExo.addGraph(file, jars)
    println("Done!")

    Log.info(s"Started to exocute the graph $exampleName.grp")
    while (true) {
      print(">")
      val input = scala.io.StdIn.readLine()

      val (command, cmdData) = {
        val index = input.indexOf(" ")
        if (index == -1)
          (input, "")
        else
          input.splitAt(index + 1)
      }
      command.trim match {
        case "i" | "inject" => println("Injected with id: " + inj.inject(cmdData))
        case "n" => println("Injected with id: " + inj.inject(cmdData.toLong))
        case "to" =>
          val List(a, b) = cmdData.split(" ").toList.map(_.toInt)
          println(s"Injected inputs from $a to $b.")
          (a to b).foreach(n => inj.inject(n.toLong))
        case "c" | "collect" | "take" =>
          if (cmdData.isEmpty)
            println("Result: " + col.collect)
          else {
            val results = col.collect(cmdData.toInt, 2000)
            if (results.isEmpty)
              println("No results to collect.")
            else
              results.foreach(res => println("Result: " + res))
          }
        case _ => println("Invalid Input")
      }
    }
  }

}
