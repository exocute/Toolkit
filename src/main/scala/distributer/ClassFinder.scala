package distributer

import java.util

import com.zink.fly.kit.FlyFinder

/**
  * Created by #ScalaTeam on 21/12/2016.
  */
class ClassFinder {

  def main(args: Array[String]): Unit = {
    val TAG = "exocute"

    val finder = new FlyFinder()
    val space = finder.find(TAG)

    if(space == null)
      throw new Exception() //FIXME

    println("Please enter class to find on space: ")
    val className = scala.io.StdIn.readLine()

    println("Looking for ["+className+"]")

    val je = new FlyJarEntry(null,null)
    val ce = new FlyClassEntry(null,null)

    val entries:util.Collection[FlyClassEntry] = space.readMany(ce,1000000) //FIXME

    println("Found "+ entries.size() + " classes in jar space")

    for{
      e <- entries
    } if(e.fileName.contains(className)) println("Found ["+e.fileName + "] in jar ["+e.jarName+"]")

    println("Done!")
  }

}
