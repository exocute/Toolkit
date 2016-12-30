package toolkit

import java.io.File

object Main {

  def getListOfFiles(dir: String): List[File] = {
    val d = new File(dir)
    if (d.exists && d.isDirectory) {
      d.listFiles.filter(_.isFile).toList
    } else {
      List[File]()
    }
  }

  def clearCommnents(code: String): String = {
    code.split("\n").map(str => {
      val index = str.indexOf("//")
      if (index == -1) str
      else str.substring(0, index)
    }).map(str => str.filterNot(_ == '\r')).mkString("\n")
  }

  def main(args: Array[String]): Unit = {
    def testFile(path: String, showResult: Boolean = true): Unit = {
      val pln = scala.io.Source.fromFile(path).mkString
      val plnClean = clearCommnents(pln)
      val parser = new ActivityParser(plnClean)
      println(parser.InputLine.run())
      //if (showResult)
      //  println(parser.graph)
      println()
    }

    getListOfFiles("tests\\correct").foreach { f => print(f.getName + ": "); testFile(f.getAbsolutePath, true) }
  }
}


