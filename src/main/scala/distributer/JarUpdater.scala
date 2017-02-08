package distributer

import java.io.File

/**
  * Created by #ScalaTeam on 21-12-2016.
  */
trait JarUpdater {

  def update(jarFiles: Array[File], leaseTime: Long): Unit =
    jarFiles.foreach(jarFile => update(jarFile, leaseTime))

  def update(jarFile: File, leaseTime: Long): Unit

  def remove(jarFiles: Array[File]): Unit =
    jarFiles.foreach(jarFile => remove(jarFile))

  def remove(jarFile: File): Unit

}
