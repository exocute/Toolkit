package clifton.nodes

/**
  * Created by #ScalaTeam on 20/12/2016.
  */
class ExoEntry(marker: String, var payload: Serializable) {
  def this() = {
    this(null, null)
  }
}
