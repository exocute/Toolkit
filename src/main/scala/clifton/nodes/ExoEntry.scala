package clifton.nodes


import java.io.Serializable
/**
  * Created by #ScalaTeam on 20/12/2016.
  */

@SerialVersionUID(1000L)
class ExoEntry(var marker: String, var payload: Serializable) {
  def this() = {
    this(null, null)
  }

  override def toString: String = "Marker: "+marker
}
