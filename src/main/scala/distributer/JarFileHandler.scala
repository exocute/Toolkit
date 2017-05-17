package distributer

import java.io.{File, FileInputStream, RandomAccessFile}
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.util.jar.{JarEntry, JarInputStream}

/**
  * Created by #GrowinScala
  */
object JarFileHandler {

  private val ClassExtension = ".class"

  def getClassNames(file: File): List[String] = {
    try {
      val jis: JarInputStream = new JarInputStream(new FileInputStream(file))

      def getAllJarEntries: Stream[JarEntry] = Option(jis.getNextJarEntry) match {
        case None => Stream.empty
        case Some(jarEntry) =>
          jarEntry #:: {
            jis.closeEntry()
            getAllJarEntries
          }
      }

      def getNextJarLoop(jarEntries: Stream[JarEntry]): List[String] = jarEntries match {
        case Stream() => Nil
        case je #:: others =>
          val originalName = je.getName

          if (originalName.endsWith(ClassExtension)) {
            val correctedName =
              originalName
                .replace(ClassExtension, "")
                .replace('/', '.')
                .replace('\\', '.')

            correctedName :: getNextJarLoop(others)
          } else {
            getNextJarLoop(others)
          }
      }

      val classNames = getNextJarLoop(getAllJarEntries)

      jis.close()

      classNames
    } catch {
      case e: Exception =>
        e.printStackTrace()
        List()
    }
  }

  def getJarBytes(file: File): Array[Byte] = {
    //    val resultJarAsBytes =
    //      try {
    val roChannel: FileChannel = new RandomAccessFile(file, "r").getChannel
    val roBuf: ByteBuffer = roChannel.map(FileChannel.MapMode.READ_ONLY, 0, roChannel.size())
    roBuf.clear()
    val jarAsBytes = Array.ofDim[Byte](roBuf.capacity())
    roBuf.get(jarAsBytes, 0, jarAsBytes.length)
    roChannel.close()
    jarAsBytes
    //      } catch {
    //        case e: Exception =>
    //          e.printStackTrace()
    //      }
    //    resultJarAsBytes
  }
}
