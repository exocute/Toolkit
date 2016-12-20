package distributer

/**
  * Created by #ScalaTeam on 20/12/2016.
  */
class FlyClassEntry(var fileName:String, var jarName:String) {
  override def toString: String = "Filename ["+fileName+"] bytes ["+ jarName + "]"
}
