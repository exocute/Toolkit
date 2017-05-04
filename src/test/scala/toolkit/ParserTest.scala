package toolkit

import java.io.{File, FileNotFoundException}

import org.parboiled2.ParseError
import org.scalatest._
import toolkit.exceptions.ExocuteParseError

import scala.util.{Failure, Success, Try}

/**
  * Created by #GrowinScala
  *
  * Takes two directories: correct and incorrect
  * correct directory should have: file.in with the pln file and file.out with the expected representation of the file
  * incorrect directory just needs the incorrect input files
  */

class ParserTest extends FlatSpec {

  /**
    * takes a path directory and returns a list of Files that are from the type ".in"
    *
    * @param dir - String with the path
    * @return List of files that respect conditions
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
    *
    * @param file
    * @return returns true if file's extension is ".in", false otherwise
    */
  def isInputFile(file: File): Boolean = file.getName.endsWith(".in")

  /**
    * takes a string with the all file and removes all '\r' and removes all comments '//'
    *
    * @param file
    * @return a new String filtered
    */
  def clearComments(file: String): String = {
    file.split("\n").map(str => {
      val index = str.indexOf("//")
      if (index == -1) str
      else str.substring(0, index)
    }).map(str => str.filterNot(_ == '\r')).mkString("\n")
  }

  /**
    * takes a path of a file and returns is context
    *
    * @param path
    * @return file in string
    */
  def readFile(path: String): String = scala.io.Source.fromFile(path).mkString

  /**
    * takes a path with the in file of a test and returns the out file of the same test
    * example: test.in => test.out
    *
    * @param path
    * @return string with the path of the out file
    */
  def getResultFile(path: String): String = {
    path.substring(0, path.length - 2) + "out"
  }

  /**
    * tests if the pln file is correct
    *
    * @param path
    * @return true if it's correct, false otherwise
    */
  def testFile(path: String): Boolean = {
    val pln = readFile(path)
    val plnClean = clearComments(pln)
    val parser = new ActivityParser(plnClean)
    val res: Try[GraphRep] = parser.InputLine.run()
    res match {
      case Success(graph) =>
        println(path + ":")

        {
          try {
            val expected: String = clearComments(readFile(getResultFile(path)))
            val validOut = graph.toString == expected
            if (!validOut) {
              println(
                s"""---
                   |${graph.toString}
                   |---
                   |$expected
                   |---
                   |""".stripMargin)
              false
            } else {
              new ActivityParser(expected).InputLine.run() match {
                case Success(expectedOutIntoGraph) =>
                  if (expectedOutIntoGraph.toString != expected) {
                    println(
                      s"""Out file is not consistent with GraphRep.toString representation:
                         |---
                         |${expectedOutIntoGraph.toString}
                         |---
                         |$expected
                         |---
                         |""".stripMargin)
                    false
                  } else
                    true
                case Failure(e: ParseError) =>
                  println("Out file is not consistent with GraphRep.toString representation")
                  throw new ExocuteParseError(parser.formatError(e))
                case Failure(e) =>
                  throw e
              }
            }
          } catch {
            case _: FileNotFoundException =>
              println("Out file should be:")
              println(graph.toString)
              false
          }
        } && {
          graph.checkValidGraph() match {
            case Success(_) => true
            case Failure(e) => throw e
          }
        }
      case Failure(e: ParseError) =>
        throw new ExocuteParseError(parser.formatError(e))
      case Failure(e) =>
        throw e
    }
  }

  /**
    * tests if the pln file is correct
    *
    * @param path
    * @return true if it's correct, false otherwise
    */
  def testFileShouldFail(path: String): Boolean = {
    val expectedException: Try[String] = Try(readFile(getResultFile(path)))

    val pln = readFile(path)
    val plnClean = clearComments(pln)
    val parser = new ActivityParser(plnClean)
    val res: Try[GraphRep] = parser.InputLine.run()
    res.flatMap(_.checkValidGraph()) match {
      case Success(_) =>
        if (expectedException.isSuccess) {
          println(path + s": should have failed with ${expectedException.get} exception")
        } else {
          println(path + ": should have failed")
        }
        false
      case Failure(originalException) =>
        val err = originalException match {
          case parseError: ParseError =>
            new ExocuteParseError(parser.formatError(parseError))
          case e => e
        }

        if (expectedException.isSuccess) {
          val expected = expectedException.get
          val same = originalException.toString startsWith expected
          if (!same)
            println(s"Exception expected: $expected, found: ${err.toString}")
          same
        } else {
          println(s"Exception caught: ${err.toString.replace("\t", " ")}")
          false
        }
    }
  }

  /**
    * tests the files supposed to be correct
    */
  getListOfFiles("tests" + File.separatorChar + "correct").foreach {
    file =>
      file.getName should "succeed" in {
        assert({
          val v = testFile(file.getAbsolutePath)
          if (!v) Thread.sleep(500)
          v
        })
      }
  }

  /**
    * tests the files supposed to be incorrect
    */
  getListOfFiles("tests" + File.separatorChar + "incorrect").foreach {
    file =>
      file.getName should "Not Succeed" in {
        assert({
          val v = testFileShouldFail(file.getAbsolutePath)
          if (!v) Thread.sleep(500)
          v
        })
      }
  }

}
