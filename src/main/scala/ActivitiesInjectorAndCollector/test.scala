package ActivitiesInjectorAndCollector

/**
  * Created by #EduardoRodrigues on 29/12/2016.
  */
object test {

  def main(args: Array[String]): Unit = {
    val a = new WhileThread
    val b = new WhileThread

    println("1")
    a.start()
    println("2")
    b.start()
    println("3")
  }

}
