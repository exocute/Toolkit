package clifton.graph

import java.io._
import java.util.UUID
import java.util.zip.{ZipEntry, ZipOutputStream}

import clifton.utilities.Utilities
import toolkit.converters.ClassLoaderChecker
import toolkit.{ActivityParser, GraphRep}

import scala.util.{Failure, Success, Try}

/**
  * Created by #ScalaTeam on 02/01/2017.
  *
  * Receives a graph and loads the jar, inserts the representation of every activity of the graph in space and returns
  * an Injector and Collector to interact with the graph and the API
  */
class StarterExoGraph {

  /**
    * Loads the jar files into the jar space and the grp file representation into the signal space.
    *
    * @param grpFile the file in grp format
    * @param jars    the jar files to be loaded
    * @return A pair with the injector and the collector
    */
  def addGraph(grpFile: File, jars: List[File], graphTimeOut: Long): Try[ExoGraph] = {
    addGraph(Utilities.readFile(grpFile), jars, graphTimeOut)
  }

  def addGraph(grpFileText: String, jars: List[File], graphTimeOut: Long): Try[ExoGraph] = {
    getGraphRep(grpFileText).map { graph: GraphRep =>
      new ExoGraphTimeOut(jars, graph, UUID.randomUUID().toString, graphTimeOut)
    }
  }

  def generateJar(graph: GraphRep): Try[File] = {
    val checker = new ClassLoaderChecker()

    for (act <- graph.getAllActivityReps)
      checker.loadClass(act.name)

    val sources = checker.getAllClassNames

    // Create a buffer for reading the files
    val buf = Array.ofDim[Byte](1024)

    Try {
      val jarFile = File.createTempFile("classes", ".jar")
      val out: ZipOutputStream = new ZipOutputStream(new FileOutputStream(jarFile))

      val loader = getClass.getClassLoader

      // Compress the file
      for {
        source <- sources
        if !source.startsWith("java") && source != "exocute.Activity"
      } {
        val path = source.replace(".", "/") + ".class"
        val resourceStream: InputStream = loader.getResourceAsStream(path)
        Option(resourceStream).foreach {
          in =>
            // Add ZIP entry to output stream.
            out.putNextEntry(new ZipEntry(path))

            // Transfer bytes from the file to the ZIP file
            var len = 0
            while ( {
              len = in.read(buf)
              len > 0
            }) {
              out.write(buf, 0, len)
            }

            // Complete the entry
            out.closeEntry()
            in.close()
        }
      }

      // Complete the ZIP file
      out.close()

      jarFile
    }
  }

  def getGraphRep(fileAsText: String): Try[GraphRep] = {
    val plnClean = Utilities.clearCommnents(fileAsText)
    val parser = new ActivityParser(plnClean)
    val res: Try[GraphRep] = parser.InputLine.run()
    res.flatMap(graph => {
      if (graph.checkValidGraph()) Success(graph)
      else Failure(new Exception("Graph is not valid"))
    })
  }

}
