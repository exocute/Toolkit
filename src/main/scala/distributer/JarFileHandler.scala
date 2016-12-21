package distributer

import java.util.jar.JarInputStream
import java.io.{File, FileInputStream, RandomAccessFile}
import java.nio.channels.FileChannel
import java.util.jar.JarEntry
import java.nio.ByteBuffer;


/**
  * Created by #ScalaTeam on 21/12/2016.
  */
class JarFileHandler {

  def getClassNames(file: File): List[String] = {
    var classNames: List[String] = Nil

    try {
      val jis: JarInputStream = new JarInputStream(new FileInputStream(file))
      var je: JarEntry = _
      je = jis.getNextJarEntry
      while (je != null) {

        var entryName = je.getName

        if (entryName.endsWith("CLASS_EXTENSION")) {
          entryName = entryName.replace("CLASS_EXTENSION", "")
          entryName = entryName.replace('/', '.')
        }
        if (entryName.endsWith("PIPELINE_EXTENSION")) classNames = entryName :: classNames

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

    var roChannel: FileChannel = _
    var jarAsBytes: Array[Byte] = _

    try {
      roChannel = new RandomAccessFile(file, "r").getChannel
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
