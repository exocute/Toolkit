package api

import java.io.File

/**
  * Created by #ScalaTeam on 04/01/2017.
  */
object startClientAPI {

  def main(args: Array[String]): Unit = {
    val startExo = new StarterExoGraph("localhost", "192.168.1.126", "localhost")

    val file = new File("examples\\abc.grp")
    val jar = List(new File("examples\\abc.jar"))

    println("Adding graph...")
    val (inj, col) = startExo.addGraph(file, jar, 0L)
    println("Done!")

    while (true) {
      print(">")
      val input = scala.io.StdIn.readLine()
      input.charAt(0) match {
        case 'i' if input.charAt(1) == ' ' => println("Injected with id: " + inj.inject(input.substring(2)))
        case 'c' if input.charAt(1) == ' ' => println("Result: " + col.collect)
        case _ => println("Invalid Input")
      }
    }
  }

}
