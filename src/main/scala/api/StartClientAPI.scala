package api

import java.io.File
import java.nio.file.{Files, Paths}

import com.zink.fly.FlyPrime
import exonode.clifton.node.{DataEntry, ExoEntry, Log, SpaceCache}
import exonode.distributer.{FlyClassEntry, FlyJarEntry}

import scala.util.{Failure, Success, Try}

/**
  * Created by #ScalaTeam on 04/01/2017.
  *
  * Allows users to insert a graph in the space, inject and collect inputs and results respectively
  */
object StartClientAPI {

  def cleanSpaces(): Unit = {
    def clean(space: FlyPrime, cleanTemplate: Any): Unit = {
      while (space.take(cleanTemplate, 0) != null) {}
    }

    clean(SpaceCache.getJarSpace, new FlyJarEntry(null, null))
    clean(SpaceCache.getJarSpace, new FlyClassEntry(null, null))
    clean(SpaceCache.getDataSpace, new DataEntry())
    clean(SpaceCache.getSignalSpace, new ExoEntry(null, null))
  }

  val getHelpString: String = {
    """
      |Usage:
      |  toolkit [options] file_name[.grp]
      |
      |options:
      |-j, -jar ip
      |  Sets the jarHost.
      |-s, -signal ip
      |  Sets the signalHost.
      |-d, -data ip
      |  Sets the dataHost.
      |-jarfile file_name[.jar]
      |  Sets the jar file containing all classes that will be used in the grp.
      |-cleanspaces
      |  Clean all information from the spaces
      |--help
      |  display this help and exit.
      |--version
      |  output version information and exit.
    """.stripMargin
  }

  val getReplHelp: String = {
    """
      |Available commands:
      |i <input>        -> injects <input> as a string into the data space.
      |im <n> <input>   -> injects <input> as a string <n> times into the data space.
      |n <input>        -> injects <input> as a number (Long) into the data space.
      |file <file_name> -> injects the bytes of the file <file_name> into the data space.
      |c                -> collects 1 result.
      |c <n>            -> collects at most n results.
    """.stripMargin
  }

  def main(args: Array[String]): Unit = {

    var jarFile = ""
    var grpFile = ""
    var shouldClean = false

    def setHosts(): Unit = {
      val it = args.iterator
      while (it.hasNext) {
        val cmd = it.next
        cmd match {
          case "-j" | "-jar" =>
            if (it.hasNext) SpaceCache.jarHost = it.next()
            else println(s"Command $cmd needs an argument (ip)")
          case "-s" | "-signal" =>
            if (it.hasNext) SpaceCache.signalHost = it.next()
            else println(s"Command $cmd needs an argument (ip)")
          case "-d" | "-data" =>
            if (it.hasNext) SpaceCache.dataHost = it.next()
            else println(s"Command $cmd needs an argument (ip)")
          case "-jarfile" =>
            val name = it.next()
            if (it.hasNext) jarFile = if (name.endsWith(".jar")) name else name + ".jar"
            else println("Command -jarfile needs an argument (file_name)")
          case "-cleanspaces" =>
            shouldClean = true
          case "--help" =>
            println(getHelpString)
            System.exit(0)
          case "--version" =>
            //FIXME get version dinamically ?
            println("Exocute version: 0.1")
            System.exit(0)
          case _ =>
            if (cmd.startsWith("-")) {
              println("Unknown command: " + cmd)
              println(getHelpString)
              System.exit(0)
            } else {
              grpFile = if (cmd.endsWith(".grp")) cmd else cmd + ".grp"
            }
        }
      }
    }

    if (jarFile.isEmpty || grpFile.isEmpty) {
      println(getHelpString)
      System.exit(0)
    }

    setHosts()

    if (shouldClean)
      cleanSpaces()

    val startExo = new StarterExoGraph

    val file = new File(grpFile)
    val jars = List(new File(jarFile))

    println("  ______                      _       \n |  ____|                    | |      \n | |__  __  _____   ___ _   _| |_ ___ \n |  __| \\ \\/ / _ \\ / __| | | | __/ _ \\\n | |____ >  < (_) | (__| |_| | ||  __/\n |______/_/\\_\\___/ \\___|\\__,_|\\__\\___|\n                                      \n                                      ")

    startExo.addGraph(file, jars) match {
      case Failure(e) =>
        val msg = e.getMessage
        if (msg == null)
          println(s"Error loading grp file:\n$e")
        else
          println(s"Error loading grp file:\n$msg")
      case Success((inj, col)) =>
        LogProcessor.start()

        Log.info(s"Started to 'exocute' the graph $grpFile")
        while (true) {
          print("> ")
          val input = scala.io.StdIn.readLine()

          val (command, cmdData) = {
            val index = input.indexOf(" ")
            if (index == -1)
              (input, "")
            else
              input.splitAt(index + 1)
          }
          command.trim match {
            case "i" | "inject" => println(s"Injected input with id: ${inj.inject(cmdData)}.")
            case "n" => println(s"Injected integer input with id: ${inj.inject(cmdData.toLong)}.")
            case "im" if cmdData.contains(" ") =>
              val (a, input) = cmdData.splitAt(cmdData.indexOf(" "))
              if (isValidInt(a)) {
                val n = a.toInt
                for (_ <- 1 to n)
                  inj.inject(input)
                println(s"Injected input $n times.")
              } else {
                println("Number not valid: " + a)
              }
            case "file" =>
              val bytes: Array[Byte] = Files.readAllBytes(Paths.get(cmdData))
              inj.inject(bytes)
            case "c" | "collect" | "take" =>
              if (cmdData.isEmpty)
                println("Result: " + col.collect)
              else {
                val results = col.collect(cmdData.toInt, 2000)
                if (results.isEmpty)
                  println("No results to collect.")
                else {
                  println(s"Results (${results.size}):")
                  results.foreach(res => println(res))
                }
              }
            case "-help" | "help" =>
              println(getReplHelp)
            case _ => println("Invalid command")
          }
        }
    }
  }

  def isValidInt(x: String): Boolean = {
    x.forall(Character.isDigit) && {
      val long: Long = x.toLong
      long <= Int.MaxValue
    }
  }

}
