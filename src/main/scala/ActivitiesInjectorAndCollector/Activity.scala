package ActivitiesInjectorAndCollector

import com.zink.fly.FlyPrime

/**
  * Created by #EduardoRodrigues on 29/12/2016.
  */

class Activity(name: String, to: List[String], from: List[String], space: FlyPrime, single: String => String, join: (String, String) => String) extends Thread {

  val TAKETIME = 10
  val PUTIME = 10 * 60 * 1000
  val nameInList = List(name)
  val doForks = to.size == 2
  val doJoins = from.size == 2


  def writeInSpace(res: String, id: String) = {
    space.write(info(to, name, res, id), PUTIME)
  }

  def sendResult(res: String, id: String) = {
    space.write(info(List(to.head), name, res, id), PUTIME)
    space.write(info(List(to.last), name, res, id), PUTIME)
  }

  def response(res: String, id: String) = {
    if (doForks)
      sendResult(res, id)
    else
      writeInSpace(res, id)
  }

  def process(act1: info, act2: info) = {
    val res = join(act1.res, act2.res)
    val id = act1.id
    response(res, id)
  }

  def process(act1: info) = {
    val res = single(act1.res)
    response(res, act1.id)

  }

  override def run(): Unit = {

    val from1: String = if (doJoins) from.head else from.head
    val from2: String = if (doJoins) from.tail.head else ""

    println("Running " + name + "!")

    while (true) {
      if (doJoins) {
        val act1 = getFromSpace(nameInList, from1, null, null)
        if (act1 != null) {
          val act2 = getFromSpace(nameInList, from2, null, act1.id)
          if (act2 == null) {
            space.write(act1, PUTIME)
          }
          else process(act1, act2)
        }
      } else {
        val act1 = getFromSpace(nameInList, from1, null, null)
        if (act1 != null) {
          process(act1)

        }
      }

    }
  }

  def getFromSpace(to: List[String], from: String, res: String, id: String) = {
    space.take(info(to, from, res, id), TAKETIME)
  }

  def getFromSpace = {
    space.take(info(nameInList, null, null, null), TAKETIME)
  }


}
