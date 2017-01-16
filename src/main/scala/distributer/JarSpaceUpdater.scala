package distributer

import java.io.File

import com.zink.fly.{Fly, FlyPrime}
import com.zink.fly.kit.{FlyFactory, FlyFinder}
import exonode.distributer.{FlyClassEntry, FlyJarEntry}
import exonode.clifton.Protocol._
import exonode.clifton.node.SpaceCache

/**
  * Created by #ScalaTeam on 21-12-2016.
  */
class JarSpaceUpdater(flyHost: String = null) extends JarUpdater {

  private val fileHandler = new JarFileHandler

  private val space: FlyPrime =
    if (flyHost == null) {
      SpaceCache.getJarSpace
    } else {
      FlyFactory.makeFly(flyHost)
    }

  if (space == null)
    throw new RuntimeException("Cant find jar space")

  override def update(jarFile: File): Unit = {
    updateJarEntry(jarFile)
    updateClassEntries(jarFile)
  }

  def updateJarEntry(jarFile: File): Unit = {
    val je = new FlyJarEntry
    je.fileName = jarFile.getName
    space.take(je, 0L)
    je.bytes = fileHandler.getJarBytes(jarFile)
    space.write(je, JAR_LEASE_TIME)
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
    val ce = new FlyClassEntry
    ce.className = className
    ce.jarName = jarFile.getName
    space.take(ce, 0L)
    space.write(ce, JAR_LEASE_TIME)
  }
}