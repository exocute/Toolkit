package toolkit

import java.io.File
import org.scalatest._

import scala.util.{Try, Success}

class StackSpec extends FlatSpec {

  def isInputFile(file: File) : Boolean = file.getName.endsWith(".in")

  def getListOfFiles(dir: String): List[File] = {
    val d = new File(dir)
    if (d.exists && d.isDirectory) {
      d.listFiles.filter(file => file.isFile && isInputFile(file)).toList
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


  def readFile(path: String) = scala.io.Source.fromFile(path).mkString

  def getResultFile(path: String) :String = {
    path.substring(0,path.size-2)+"out"
  }

  def validateFiles(result: String, path: String) : Boolean = {
    val expected: String = readFile(getResultFile(path))
    result == expected.filterNot(_ == '\r')
  }

  def testFile(path: String, testContent: Boolean): Boolean = {
    val pln = readFile(path)
    val plnClean = clearCommnents(pln)
    val parser = new ActivityParser(plnClean)
    val res: Try[Unit] = parser.InputLine.run()
    res match {
      case Success(_) =>
        if (!testContent) true
        else validateFiles(parser.graph.toString, path)
      case _ => false
    }
  }

  getListOfFiles("tests" + File.separatorChar + "correct").foreach {
    f =>
      f.getName should "succeed" in {
        assert(testFile(f.getAbsolutePath, true))
      }
  }

  getListOfFiles("tests" + File.separatorChar + "incorrect").foreach {
    f =>
      f.getName should "Not Succeed" in {
        assert(!testFile(f.getAbsolutePath, false))
      }
  }

}
