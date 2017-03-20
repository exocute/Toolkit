package toolkit.converters

import java.io._

import exocute.Activity
import exonode.clifton.node.CliftonClassLoader
import toolkit.converters.SwaveActivity.FunctionType

import scala.collection.mutable

/**
  * Created by #GrowinScala
  */
class SwaveActivity extends Activity {

  override def process(input: Serializable, params: Vector[String]): Serializable = {
    SwaveActivity.functions.getOrElseUpdate(params(0), {
      val swaveFunctionStr = params(1)
      loadFunction(swaveFunctionStr.toCharArray.map(_.toByte))
    }).apply(input)
  }

  def loadFunction(bytes: Array[Byte]): FunctionType = {
    def loadTry(): Object = {
      val myInputStream = new MyObjectInputStream(new ByteArrayInputStream(bytes))
      myInputStream.readObject()
    }

    class MyObjectInputStream(in: InputStream) extends ObjectInputStream(in) {
      private val loader = new CliftonClassLoader()

      override def resolveClass(desc: ObjectStreamClass): Class[_] = {
        val name = desc.getName
        try {
          loader.findClass(name)
        } catch {
          case _: java.lang.ClassNotFoundException => super.resolveClass(desc)
        }
      }
    }

    val obj: Object = loadTry()

    obj.asInstanceOf[FunctionType]
  }
}

object SwaveActivity {

  private type FunctionType = Serializable => Serializable

  private val functions = mutable.Map[String, FunctionType]()

  def FanOutBroadcast(n: Int): FunctionType = {
    input => (1 to n).map(_ => input).toVector
  }

  val FanInToTuple: FunctionType = {
    case Vector(a, b) => (a, b)
    case Vector(a, b, c) => (a, b, c)
    case Vector(a, b, c, d) => (a, b, c, d)
    case Vector(a, b, c, d, e) => (a, b, c, d, e)
  }

}