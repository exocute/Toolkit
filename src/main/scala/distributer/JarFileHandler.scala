package distributer

import java.io.{File, FileInputStream, RandomAccessFile}
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.util.jar.{JarEntry, JarInputStream};


/**
  * Created by #ScalaTeam on 21/12/2016.
  */
object JarFileHandler {

  def getClassNames(file: File): List[String] = {
    var classNames: List[String] = Nil

    try {
      val jis: JarInputStream = new JarInputStream(new FileInputStream(file))
      var je: JarEntry = jis.getNextJarEntry
      while (je != null) {

        var entryName = je.getName

        if (entryName.endsWith(".class")) {
          entryName = entryName.replace(".class", "")
          entryName = entryName.replace('/', '.')
          classNames = entryName :: classNames
        }

        jis.closeEntry()

        je = jis.getNextJarEntry
      }
      jis.close()
    } catch {
      case e: Exception => e.printStackTrace()
    }
    classNames
  }

  def getJarBytes(file: File): Array[Byte] = {

    var jarAsBytes: Array[Byte] = null

    try {
      val roChannel: FileChannel = new RandomAccessFile(file, "r").getChannel
      val roBuf: ByteBuffer = roChannel.map(FileChannel.MapMode.READ_ONLY, 0, roChannel.size())
      roBuf.clear()
      jarAsBytes = Array.ofDim[Byte](roBuf.capacity())
      roBuf.get(jarAsBytes, 0, jarAsBytes.length)
      roChannel.close()
    } catch {
      case e: Exception => e.printStackTrace()
    }
    jarAsBytes
  }
}
