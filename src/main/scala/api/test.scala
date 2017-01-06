package api

import java.io.File

/**
  * Created by #ScalaTeam on 04/01/2017.
  */
object test {

  def main(args: Array[String]): Unit = {
    val startExo = new StarterExoGraph("localhost","192.168.1.126" ,"localhost")

    val file = new File("examples\\abc.grp")
    val jar = List(new File("examples\\abc.jar"))

    println("Adding graph...")
    val (inj,col) = startExo.addGraph(file, jar, 0L)
    println("Done!")

    //inj.inject("ola")
    while(true){
      print(">")
      val input = scala.io.StdIn.readLine()
      if(input.charAt(0)=='i')
      inj.inject(input.substring(1))
      else
        println(col.collect)
    }



  }

}
