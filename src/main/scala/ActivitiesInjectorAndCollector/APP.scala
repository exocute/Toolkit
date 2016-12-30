package ActivitiesInjectorAndCollector

import com.zink.fly.FlyPrime
import com.zink.fly.kit.FlyFactory

/**
  * Created by #EduardoRodrigues on 29/12/2016.
  */
object APP {
  def main(args: Array[String]): Unit = {
    val A = (s: String) => s + s
    val B = (s: String) => s.count(x => x.isLetter).toString
    val C = (s: String) => s.count(x => x.isDigit).toString
    val D = (s1: String, s2: String) => "The text has " + s1 + " letters and " + s2 + " digits"
    println("Creating Space!")
    val space: FlyPrime = FlyFactory.makeFly()
    val inj = new Injector(space, "A")
    val col = new Collector(space, "<")
    println("Creating Activities")
    val act1 = new Activity("A", List("B", "C"), List("<"), space, A, null)
    val act2 = new Activity("B", List("D"), List("A"), space, B, null)
    val act3 = new Activity("C", List("D"), List("A"), space, C, null)
    val act4, act5, act6 = new Activity("D", List("<"), List("B", "C"), space, null, D)
    println("Starting Activities")
    List(act1, act2, act3, act4, act5, act6).foreach(x => x.start())
    Thread.sleep(10)
    while (true) {
      print(">")
      val res = scala.io.StdIn.readLine()
      res.charAt(0) match {
        case 'i' =>
          val input = res.drop(2)
          if (!input.isEmpty) println("Your ID Request " + inj.put(input))
          else println("Invalid comand. Inject should have at least one input")
        case 'c' => println("Result:" + col.get)
        case 's' =>
          val input = res.drop(2)
          if (!input.isEmpty) println("Result: " + col.get(input))
          else println("Invalid comand. Special Collect should have at least one input")
        case _ => ()
      }
    }
  }
}

