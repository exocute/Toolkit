package distributer

import java.io.File

import com.zink.scala.fly.ScalaFly
import exonode.clifton.Protocol._
import exonode.clifton.node.SpaceCache
import exonode.distributer.{FlyClassEntry, FlyJarEntry}

/**
  * Created by #ScalaTeam on 21-12-2016.
  */
class JarSpaceUpdater(space: ScalaFly = SpaceCache.getJarSpace) extends JarUpdater {

  private val fileHandler = new JarFileHandler

  override def update(jarFile: File): Unit = {
    updateJarEntry(jarFile)
    updateClassEntries(jarFile)
  }

  def updateJarEntry(jarFile: File): Unit = {
    val je = FlyJarEntry(jarFile.getName, null)
    space.take(je, 0L)
    val jeBytes = FlyJarEntry(jarFile.getName, fileHandler.getJarBytes(jarFile))
    space.write(jeBytes, JAR_LEASE_TIME)
  }

  private def updateClassEntries(jarFile: File) {
    val classNames = fileHandler.getClassNames(jarFile)
    println(classNames)
    for (className <- classNames) {
      updateClassEntry(jarFile, className)
    }
  }

  //	 ensure both the class and the source jar are matched
  private def updateClassEntry(jarFile: File, className: String) {
    val ce = FlyClassEntry(className, jarFile.getName)
    space.take(ce, 0L)
    space.write(ce, JAR_LEASE_TIME)
  }
}