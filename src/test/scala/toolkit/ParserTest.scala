package toolkit

import java.io.File
import org.scalatest._

import scala.util.{Try, Success}


/**
  * Takes two directories: correct and incorrect
  * correct directory should have: file.in with the pln file and file.out with the expected representation of the file
  * incorrect directory just needs the incorrect input files
  */
class AutomaticTesterOfParser extends FlatSpec {

  /**
    * takes a path directory and returns a list of Files that are from the type ".in"
    * @param dir - String with the path
    * @return List of files that respect condictions
    */
  def getListOfFiles(dir: String): List[File] = {
    val d = new File(dir)
    if (d.exists && d.isDirectory) {
      d.listFiles.filter(file => file.isFile && isInputFile(file)).toList
    } else {
      List[File]()
    }
  }

  /**
    * checks if the file's extension is ".in"
    * @param file
    * @return returns true if file's extension is ".in", false otherwise
    */
  def isInputFile(file: File) : Boolean = file.getName.endsWith(".in")


  /**
    * takes a string with the all file and removes all '\r' and removes all comments '//'
    * @param file
    * @return a new String filtered
    */
  def clearCommnents(file: String): String = {
    file.split("\n").map(str => {
      val index = str.indexOf("//")
      if (index == -1) str
      else str.substring(0, index)
    }).map(str => str.filterNot(_ == '\r')).mkString("\n")
  }

  /**
    * takes a path of a file and returns is context
    * @param path
    * @return file in string
    */
  def readFile(path: String) = scala.io.Source.fromFile(path).mkString

  /**
    * takes a path with the in file of a test and returns the out file of the same test
    * example: test.in => test.out
    * @param path
    * @return string with the path of the out file
    */
  def getResultFile(path: String) :String = {
    path.substring(0,path.size-2)+"out"
  }

  /**
    * takes two strings with the context of the result and the path of the expected file
    * removes all 'r' from the out files
    * compares the two strings
    * @param result
    * @param path
    * @return true if the strings are equals, false otherwise
    */
  def validateFiles(result: String, path: String) : Boolean = {
    val expected: String = readFile(getResultFile(path))
    result == expected.filterNot(_ == '\r')
  }

  /**
    * tests if the pln file is correct
    *
    * @param path
    * @param testContent true if needs to compare with output file
    * @return true if it's correct, false otherwise
    */
  def testFile(path: String, testContent: Boolean): Boolean = {
    val pln = readFile(path)
    val plnClean = clearCommnents(pln)
    val parser = new ActivityParser(plnClean)
    val res: Try[Unit] = parser.InputLine.run()
    println(parser.graph.toString)
    res match {
      case Success(_) =>
        if (!testContent) parser.graph.checkValidGraph()
        else validateFiles(parser.graph.toString, path) && parser.graph.checkValidGraph()
      case _ => false
    }
  }

  /**
    * tests the files supposed to be correct
    */
  getListOfFiles("tests" + File.separatorChar + "correct").foreach {
    f =>
      f.getName should "succeed" in {
        assert(testFile(f.getAbsolutePath, true))
      }
  }

  /**
    * tests the files supposed to be incorrect
    */
  getListOfFiles("tests" + File.separatorChar + "incorrect").foreach {
    f =>
      f.getName should "Not Succeed" in {
        assert(!testFile(f.getAbsolutePath, false))
      }
  }

}
