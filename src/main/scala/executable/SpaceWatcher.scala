package executable

import com.zink.scala.fly.ScalaFly
import exonode.clifton.node.SpaceCache
import exonode.clifton.node.SpaceCache._
import exonode.clifton.node.entries.{BackupEntry, BackupInfoEntry, DataEntry, ExoEntry}
import exonode.distributer.{FlyClassEntry, FlyJarEntry}

/**
  * Created by #ScalaTeam on 08-02-2017.
  */
object SpaceWatcher {

  private val DEFAULT_LIMIT = 100

  private case class Watcher(space: ScalaFly, name: String, matchTemplate: AnyRef)

  def main(args: Array[String]): Unit = {

    def count(watcher: Watcher): Unit = {
      import watcher._
      val size = space.readMany(matchTemplate, DEFAULT_LIMIT).size
      println(s"$name: $size in the space.")
    }

    def show(watcher: Watcher): Unit = {
      import watcher._
      space.readMany(matchTemplate, DEFAULT_LIMIT).foreach(println)
    }

    val jarSpace = getJarSpace
    val dataSpace = getDataSpace
    val signalSpace = getSignalSpace

    val entries = List("FlyJarEntry", "FlyClassEntry", "DataEntry", "BackupEntry", "BackupInfoEntry", "ExoEntry")

    val entriesMap = Map("FlyJarEntry" -> Watcher(jarSpace, "FlyJarEntry", FlyJarEntry(null, null)),
      "FlyClassEntry" -> Watcher(jarSpace, "FlyClassEntry", FlyClassEntry(null, null)),
      "DataEntry" -> Watcher(dataSpace, "DataEntry", DataEntry(null, null, null, null)),
      "BackupEntry" -> Watcher(dataSpace, "BackupEntry", BackupEntry(null, null, null, null)),
      "BackupInfoEntry" -> Watcher(dataSpace, "BackupInfoEntry", BackupInfoEntry(null, null, null)),
      "ExoEntry" -> Watcher(signalSpace, "ExoEntry", ExoEntry(null, null)))

    while (true) {
      println("Press enter for next count...")
      val cmd = scala.io.StdIn.readLine().trim
      cmd match {
        case "" =>
          entries.foreach(name => count(entriesMap(name)))
        case "clean" =>
          SpaceCache.cleanAllSpaces()
        case _ if entriesMap.contains(cmd) =>
          show(entriesMap(cmd))
      }
    }
  }

}
