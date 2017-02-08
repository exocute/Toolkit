package distributer

import java.io.File

import com.zink.scala.fly.ScalaFly
import exonode.clifton.node.SpaceCache
import exonode.distributer.{FlyClassEntry, FlyJarEntry}

/**
  * Created by #ScalaTeam on 21-12-2016.
  */
class JarSpaceUpdater() extends JarUpdater {

  private val signalSpace: ScalaFly = SpaceCache.getJarSpace

  override def update(jarFile: File, leaseTime: Long): Unit = {
    updateJarEntry(jarFile, leaseTime)
    updateClassEntries(jarFile, leaseTime)
  }

  override def remove(jarFile: File): Unit = {
    removeJarEntry(jarFile)
    removeClassEntries(jarFile)
  }

  private def updateJarEntry(jarFile: File, leaseTime: Long): Unit = {
    val je = FlyJarEntry(jarFile.getName, null)
    signalSpace.take(je, 0L)
    val jeBytes = FlyJarEntry(jarFile.getName, JarFileHandler.getJarBytes(jarFile))
    signalSpace.write(jeBytes, leaseTime)
  }

  private def updateClassEntries(jarFile: File, leaseTime: Long): Unit = {
    val classList = JarFileHandler.getClassNames(jarFile)
    //    println(classList) // show this line in debug mode ?
    for (className <- classList) {
      updateClassEntry(jarFile, className, leaseTime)
    }
  }

  //	 ensure both the class and the source jar are matched
  private def updateClassEntry(jarFile: File, className: String, leaseTime: Long): Unit = {
    val ce = FlyClassEntry(className, jarFile.getName)
    signalSpace.take(ce, 0L)
    signalSpace.write(ce, leaseTime)
  }

  private def removeJarEntry(jarFile: File): Unit = {
    val je = FlyJarEntry(jarFile.getName, null)
    signalSpace.take(je, 0L)
  }

  private def removeClassEntries(jarFile: File): Unit = {
    val classList = JarFileHandler.getClassNames(jarFile)
    for (className <- classList) {
      removeClassEntry(jarFile, className)
    }
  }

  private def removeClassEntry(jarFile: File, className: String): Unit = {
    val ce = FlyClassEntry(className, jarFile.getName)
    signalSpace.take(ce, 0L)
  }
}