package distributer

import java.io.File

import com.zink.fly.Fly
import com.zink.fly.kit.{FlyFactory, FlyFinder}

/**
  * Created by #ScalaTeam on 21-12-2016.
  */
class JarSpaceUpdater(flyHost: String) extends JarUpdater {

  private val fileHandler = new JarFileHandler

  private val space: Fly =
    if (flyHost == null) {
      new FlyFinder().find(JarSpaceUpdater.TAG)
    } else {
      FlyFactory.makeFly(flyHost)
    }

  if (space == null)
    throw new RuntimeException("Cant find space with[" + JarSpaceUpdater.TAG + "]")

  override def update(directory: String, jarFile: String): Unit = {
    val file: File = new File(directory, jarFile)
    updateJarEntry(file)
    updateClassEntries(file)
  }

  def updateJarEntry(jarFile: File) = {
    val je = new FlyJarEntry
    je.fileName = jarFile.getName
    System.out.println("Updating Jar Entry " + je)
    space.take(je, 0L)
    je.bytes = fileHandler.getJarAsBytes(jarFile)
    space.write(je, JarSpaceUpdater.ENTRY_LEASE)
  }

  private def updateClassEntries(jarFile: File) {
    val classNames = fileHandler.getClassNames(jarFile)
    for (className <- classNames) {
      updateClassEntry(jarFile, className)
    }
  }

  //	 ensure both the class and the source jar are matched
  private def updateClassEntry(jarFile: File, className: String) {
    val ce = new FlyClassEntry
    ce.className = className
    ce.jarName = jarFile.getName
    System.out.println("Updating class entry " + ce)
    space.take(ce, 0L)
    space.write(ce, JarSpaceUpdater.ENTRY_LEASE)
  }
}

object JarSpaceUpdater {

  private val TAG = "JarSpace"
  private val ENTRY_LEASE: Long = 365L * 24 * 60 * 60 * 1000

}