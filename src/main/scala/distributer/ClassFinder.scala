package distributer

import clifton.nodes.exceptions.SpaceNotDefined
import com.zink.fly.kit.FlyFinder
import exonode.distributer.FlyClassEntry

import scala.collection.JavaConverters._

/**
  * Created by #ScalaTeam on 21/12/2016.
  */
class ClassFinder {

  def main(args: Array[String]): Unit = {

    val TAG = "exocute"

    val finder = new FlyFinder()
    val space = finder.find(TAG)

    if(space == null)
      throw new SpaceNotDefined(TAG)

    println("Please enter class to find on space: ")
    val className = scala.io.StdIn.readLine()

    println("Looking for ["+className+"]")

    val ce = new FlyClassEntry(null,null)

    val entries = space.readMany(ce,1000000).asScala.toList

    println("Found "+ entries.size + " classes in jar space")

    for{
      (e : FlyClassEntry) <- entries
    } if(e.className.contains(className)) println("Found ["+e.className + "] in jar ["+e.jarName+"]")

    println("Done!")
  }

}
