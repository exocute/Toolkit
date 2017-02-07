package executable

import java.io.File
import java.nio.file.{Files, Paths}

import com.zink.scala.fly.ScalaFly
import exonode.clifton.node._
import exonode.clifton.node.entries.{BackupEntry, BackupInfoEntry, DataEntry, ExoEntry}
import exonode.clifton.signals.KillSignal
import exonode.distributer.{FlyClassEntry, FlyJarEntry}

import scala.util.{Failure, Success}

/**
  * Created by #ScalaTeam on 04/01/2017.
  *
  * Allows users to insert a graph in the space, inject and collect inputs and results respectively
  */
object StartClientAPI {

  private val getHelpString: String = {
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
      |  Clean all information from the spaces.
      |--help
      |  display this help and exit.
      |--version
      |  output version information and exit.
    """.stripMargin
  }

  private val getReplHelp: String = {
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

  private def printlnExit(msg: String): Unit = {
    println(msg)
    System.exit(1)
  }

  def main(args: Array[String]): Unit = {

    var jarFile = ""
    var grpFile = ""
    var shouldClean = false

    def setHosts(): Unit = {
      val it = args.iterator
      while (it.hasNext) {
        val cmd = it.next
        cmd.toLowerCase match {
          case "-j" | "-jar" =>
            if (it.hasNext) SpaceCache.jarHost = it.next()
            else printlnExit(s"Command $cmd needs an argument (ip)")
          case "-s" | "-signal" =>
            if (it.hasNext) SpaceCache.signalHost = it.next()
            else printlnExit(s"Command $cmd needs an argument (ip)")
          case "-d" | "-data" =>
            if (it.hasNext) SpaceCache.dataHost = it.next()
            else printlnExit(s"Command $cmd needs an argument (ip)")
          case "-jarfile" =>
            val name = it.next()
            if (it.hasNext) jarFile = if (name.endsWith(".jar")) name else name + ".jar"
            else printlnExit("Command -jarfile needs an argument (file_name)")
          case "-cleanspaces" =>
            shouldClean = true
          case "--help" =>
            printlnExit(getHelpString)
          case "--version" =>
            //FIXME get version dynamically ?
            printlnExit("Exocute version: 0.1")
          case _ =>
            if (cmd.startsWith("-")) {
              println("Unknown command: " + cmd)
              printlnExit(getHelpString)
            } else if (it.hasNext) {
              printlnExit(getHelpString)
            } else {
              grpFile = if (cmd.endsWith(".grp")) cmd else cmd + ".grp"
            }
        }
      }
    }

    setHosts()

    if (jarFile.isEmpty || grpFile.isEmpty) {
      printlnExit(getHelpString)
    }

    if (shouldClean)
      SpaceCache.cleanAllSpaces()

    val startExo = new StarterExoGraph

    val file = new File(grpFile)
    val jars = List(new File(jarFile))

    println("  ______                      _       \n |  ____|                    | |      \n | |__  __  _____   ___ _   _| |_ ___ \n |  __| \\ \\/ / _ \\ / __| | | | __/ _ \\\n | |____ >  < (_) | (__| |_| | ||  __/\n |______/_/\\_\\___/ \\___|\\__,_|\\__\\___|\n                                      \n                                      ")

    startExo.addGraph(file, jars) match {
      case Failure(e) =>
        val msg = e.getMessage
        printlnExit(s"Error loading grp file:\n${if (msg == null) e else msg}")
      case Success((inj, col)) =>
        LogProcessor.start()

        while (true) {
          print("> ")
          val input = scala.io.StdIn.readLine().trim

          val (command, cmdData) = {
            val index = input.indexOf(" ")
            if (index == -1)
              (input, "")
            else
              input.splitAt(index + 1)
          }
          command.trim.toLowerCase match {
            case "i" | "inject" => println(s"Injected input with id: ${inj.inject(cmdData.trim)}.")
            case "n" => println(s"Injected integer input with id: ${inj.inject(cmdData.toLong)}.")
            case "im" if cmdData.contains(" ") =>
              val (a, input) = cmdData.splitAt(cmdData.indexOf(" "))
              if (isValidNatNumber(a)) {
                val n = a.toInt
                inj.inject(n, input.trim)
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
                val (maxElems, waitTime) =
                  if (cmdData.contains(" ")) {
                    val List(s1, s2) = cmdData.split(" ").toList
                    (s1.toInt, s2.toLong)
                  } else
                    (cmdData.toInt, 2000L)

                val results = col.collect(maxElems, waitTime)
                if (results.isEmpty)
                  println("No results to collect.")
                else {
                  println(s"Results (${results.size}):")
                  results.foreach(res => println(res))
                }
              }
            case "kill" if cmdData.nonEmpty => // DEBUG ONLY
              val entry = ExoEntry(cmdData.trim, KillSignal)
              SpaceCache.getSignalSpace.write(entry, 60 * 60 * 1000)
            case "help" =>
              println(getReplHelp)
            case "exit" =>
              // clear data from the spaces?
              val endStr = s"Finished the graph $grpFile"
              println(endStr)
              Log.info("GRAPH", endStr)
              System.exit(0)
            case "" => //just ignore
            case _ => println("Invalid command: " + command.trim)
          }
        }
    }
  }

  private def isValidNatNumber(str: String): Boolean = {
    str.forall(Character.isDigit) && {
      val long: Long = str.toLong
      long >= 0 && long <= Int.MaxValue
    }
  }

}