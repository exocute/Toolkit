package clifton.graph

import com.zink.fly.kit.FlyFactory
import com.zink.fly.{Fly, FlyPrime}

/**
  * Created by #ScalaTeam on 19/12/2016.
  */
object HowToUseFly {

  def main(args: Array[String]): Unit = {
    val space : FlyPrime = FlyFactory.makeFly("192.168.1.126")
    val a = new animal("gato",13)
    space.write(a,5000)
    val template:animal = new animal(null,null)
    Thread.sleep(50)
    println(space.take(template,40))
    space.write(new gato("gato","58"),5000)
    val b = new gato(null,null)
    println(space.read(b,500))
    println(space.take(b,500))
  }
}

class animal(var name:String, var idd:BigInt){
  override def toString: String = name +" "+idd
}

class gato(var name:String, var idd:String){
  override def toString: String = name +" "+idd
}

