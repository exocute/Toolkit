package toolkit.converters

import java.io.{File, InputStream}

import scala.collection.mutable

class ClassLoaderChecker extends ClassLoader(getClass.getClassLoader) {

  private val CLASS_EXTENSION = ".class"

  private val classNames = mutable.Set[String]()

  override def loadClass(name: String): Class[_] = {
    findClass(name)
  }

  override def findClass(name: String): Class[_] = {
    classNames.update(name, included = true)

    val newName: String = name.replace('.', File.separatorChar).concat(CLASS_EXTENSION)
    val resourceStream: InputStream = getParent.getResourceAsStream(newName)
    try {
      val length = resourceStream.available()

      val classBytes = Array.ofDim[Byte](length)

      resourceStream.read(classBytes)

      defineClass(name, classBytes, 0, classBytes.length)
    } catch {
      case _: NullPointerException =>
        getParent.loadClass(name)
      case _: java.lang.LinkageError =>
        getParent.loadClass(name)
    }
  }

  def getAllClassNames: Set[String] = classNames.toSet

}
