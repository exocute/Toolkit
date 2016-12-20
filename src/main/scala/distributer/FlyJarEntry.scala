package distributer

/**
  * Created by #ScalaTeam on 20/12/2016.
  */
class FlyJarEntry(var fileName:String, var bytes:Array[Byte]) {
  override def toString: String = "Filename ["+fileName+"] bytes ["+ bytes + "]"
}
