package executable

import com.zink.scala.fly.ScalaFly
import exonode.clifton.node.SpaceCache
import exonode.clifton.node.SpaceCache._
import exonode.clifton.node.entries._
import exonode.distributer.{FlyClassEntry, FlyJarEntry}

import scala.collection.mutable

/**
  * Created by #GrowinScala
  */
object SpaceWatcher {

  private val DEFAULT_LIMIT = 500

  private case class Watcher(space: ScalaFly, name: String, matchTemplate: AnyRef)

  def main(args: Array[String]): Unit = {

    val filters = mutable.Set[String]()

    def count(watcher: Watcher): Unit = {
      import watcher._
      val size = space.readMany(matchTemplate, DEFAULT_LIMIT).size
      println(s"$name: $size${if (size == DEFAULT_LIMIT) "+" else ""} in the space.")
    }

    def show(watcher: Watcher): Unit = {
      import watcher._
      space.readMany(matchTemplate, DEFAULT_LIMIT).foreach {
        case entry: exonode.distributer.FlyJarEntry => println(s"Filename ${entry.fileName} (size ${entry.bytes.toVector.size})")
        case elem =>
          val str = elem.toString
          if (filters.forall(s => !str.contains(s)))
            println(elem)
      }
    }

    val jarSpace = getJarSpace
    val dataSpace = getDataSpace
    val signalSpace = getSignalSpace

    val entriesMap = Map("FlyJarEntry" -> Watcher(jarSpace, "FlyJarEntry", FlyJarEntry(null, null)),
      "FlyClassEntry" -> Watcher(jarSpace, "FlyClassEntry", FlyClassEntry(null, null)),
      "DataEntry" -> Watcher(dataSpace, "DataEntry", DataEntry(null, null, null, null, null)),
      "BackupEntry" -> Watcher(dataSpace, "BackupEntry", BackupEntry(null, null, null, null, null)),
      "BackupInfoEntry" -> Watcher(dataSpace, "BackupInfoEntry", BackupInfoEntry(null, null, null, null)),
      "FlatMapEntry" -> Watcher(dataSpace, "FlatMapEntry", FlatMapEntry(null, null, null)),
      "ExoEntry" -> Watcher(signalSpace, "ExoEntry", ExoEntry(null, null)),
      "GraphEntry" -> Watcher(signalSpace, "GraphEntry", GraphEntry(null, null))
    )

    while (true) {
      println("Press enter for next count...")
      val cmd = scala.io.StdIn.readLine().trim
      cmd match {
        case "" =>
          entriesMap.keys.foreach(name => count(entriesMap(name)))
        case "clean" =>
          SpaceCache.cleanAllSpaces()
        case "all" =>
          entriesMap.values.foreach(show)
        case _ if cmd.startsWith("filter ") =>
          val f = cmd.substring("filter ".length)
          filters.add(f)
        case _ if entriesMap.contains(cmd) =>
          show(entriesMap(cmd))
        case _ => //ignore command
      }
    }
  }

}
