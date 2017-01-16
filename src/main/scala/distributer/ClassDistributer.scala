package distributer

import java.io.File

/**
  * Created by #ScalaTeam on 21/12/2016.
  */
@deprecated
class ClassDistributer {

  def main(args: Array[String]): Unit = {
    val directoryName = args(0)
    val directory: File = new File(directoryName)

    if (!directory.isDirectory)
      throw new IllegalArgumentException(directoryName + " is not valid directory")
    else {
      val filter = new JarFileFilter
      val writer: JarUpdater =
        if (args.length > 1)
          new JarSpaceUpdater(args(1))
        else
          new JarSpaceUpdater()

      while (!Thread.interrupted) {
        //writer.update(directory.list(filter))
        Thread.sleep(100)
      }
    }
  }

}
