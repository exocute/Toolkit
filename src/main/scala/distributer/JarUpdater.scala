package distributer


import java.io.File
/**
  * Created by #ScalaTeam on 21-12-2016.
  */
trait JarUpdater {

  def update(jarFiles: Array[File]): Unit =
    jarFiles.foreach(jarFile => update(jarFile))

  def update(jarFile: File): Unit

}
