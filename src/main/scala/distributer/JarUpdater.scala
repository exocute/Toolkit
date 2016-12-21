package distributer

/**
  * Created by #ScalaTeam on 21-12-2016.
  */
trait JarUpdater {

  def update(directory: String, jarFiles: Array[String]): Unit =
    jarFiles.foreach(jarFile => update(directory, jarFile))

  def update(directory: String, jarFile: String): Unit

}
