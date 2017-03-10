package toolkit.converters

import java.io._
import java.util.UUID
import java.util.zip.{ZipEntry, ZipOutputStream}

import clifton.graph.ExoGraph
import exonode.clifton.signals.{ActivityFilterType, ActivityMapType, ActivityType}
import swave.core._
import swave.core.graph.GlyphSet
import toolkit.{ActivityRep, GraphRep}

import scala.collection.mutable
import scala.concurrent.Future
import scala.language.implicitConversions
import scala.util.Try

/**
  * Created by #ScalaTeam on 03-02-2017.
  */
object SwaveToExoGraph {

  private type FunctionType = Serializable => Serializable

  private var id: String = "A"
  private val activitiesMap = mutable.Map[String, Array[Byte]]()
  private val swaveStagesMap = mutable.Map[String, String]()

  private def createNewId(): String = {
    val next = id
    id = (id(0) + 1).toChar.toString
    next
  }

  private def ObjectToBytes(obj: AnyRef): Array[Byte] = {
    val bos = new ByteArrayOutputStream()
    try {
      val out = new ObjectOutputStream(bos)
      out.writeObject(obj)
      out.flush()
      bos.toByteArray
    } finally {
      try {
        bos.close()
      } catch {
        case _: IOException =>
        // ignore close exception
      }
    }
  }

  private def getSwaveCompositionToGraph[T](startStage: Stage): GraphRep = {
    val graph = new GraphRep("SwaveConverter")

    def toActivityRep(stage: Stage, functionData: (Array[Byte], ActivityType)): ActivityRep = {
      val stageStr = stage.toString
      swaveStagesMap.get(stageStr) match {
        case None =>
          val newId = createNewId()
          swaveStagesMap.update(stageStr, newId)
          val (functionBytes, functionType) = functionData
          val functionInStr = new String(functionBytes.map(_.toChar))
          activitiesMap.update(newId, functionBytes)
          val actRep = new ActivityRep(newId, "toolkit.converters.SwaveActivity", functionType, Vector(s"swave.$newId", functionInStr), Vector(), "")
          graph.addActivity(actRep)
          actRep
        case Some(activityId) => graph.activityById(activityId)
      }
    }

    def stageToActivityRep(stage: Stage): Option[ActivityRep] =
      stageToActivity(stage).map(rep => toActivityRep(stage, rep))

    def firstStage(stages: List[Stage]): Unit = {
      stages match {
        case Nil =>
        case List(stage) =>
          stageToActivityRep(stage) match {
            case None =>
              firstStage(stage.inputStages)
            case Some(actRep) =>
              otherStages(actRep, stage.inputStages)
          }
        case _ =>
          throw new NotAValidSwave
      }
    }

    def otherStages(act: ActivityRep, stages: List[Stage]): Unit = {
      stages match {
        case Nil =>
        case List(stage) =>
          stageToActivityRep(stage) match {
            case None =>
              otherStages(act, stage.inputStages)
            case Some(actRep) =>
              Try(graph.addConnection(actRep.id, act.id))
              otherStages(actRep, stage.inputStages)
          }
        case _ =>
          for {
            stage <- stages
            activity <- stageToActivityRep(stage)
          } {
            graph.addConnection(activity.id, act.id) // try ?
            otherStages(activity, stage.inputStages)
          }
      }
    }

    swaveInput = Stream.empty
    firstStage(List(startStage))
    graph
  }

  private var swaveInput: Stream[Any] = _

  private def stageToActivity(stage: Stage): Option[(Array[Byte], ActivityType)] = {
    (stage.kind.name, stage.params) match {
      case ("Map", it) =>
        Some(ObjectToBytes(it.next().asInstanceOf[FunctionType]), ActivityMapType)
      case ("Filter", it) =>
        Some(ObjectToBytes(it.next().asInstanceOf[FunctionType]), ActivityFilterType)
      case ("Spout.FromFuture", it) =>
        swaveInput = swaveInput #::: Stream(it.next().asInstanceOf[Future[Any]].value.get.get)
        None
      case ("Spout.FromIterator", it) =>
        swaveInput = swaveInput #::: it.next().asInstanceOf[Iterator[Any]].toStream
        None
      case ("FanIn.ToTuple", _) =>
        Some(ObjectToBytes(SwaveActivity.FanInToTuple), ActivityMapType)
      case ("FanOut.Broadcast", _) =>
        val n = stage.outputStages.size
        Some(ObjectToBytes(SwaveActivity.FanOutBroadcast(n)), ActivityMapType)
      case ("Nop", _) =>
        //ignore this stage
        None
      case other =>
        println(s"Unknown swave stage (${other._1}, ignoring...")
        None
    }
  }

  def show(spout: Spout[_]): Unit =
    println(Graph.from(spout.stage).withGlyphSet(GlyphSet.`2x2 ASCII`).render)

  private def getGraphRep(swaveObj: Spout[_]): Option[GraphRep] = {
    val s = swaveObj.stage

    val graphRep = getSwaveCompositionToGraph(s)
    if (graphRep.checkValidGraph())
      Some(graphRep)
    else {
      println(graphRep)
      None
    }
  }

  private def runExoGraph(exoGraph: ExoGraph): Vector[Serializable] = {
    val inj = exoGraph.injector
    val col = exoGraph.collector

    val input = swaveInput.map(_.asInstanceOf[Serializable])
    val injIds = inj.injectMany(input)
    val elems = injIds.map(i => col.collectIndex(i, 60 * 60 * 1000))

    exoGraph.closeGraph()

    // filter all empty results (filtered)
    elems.flatMap(_.get.toList)
  }

  private def stringListToFile(strList: List[String]): String = {
    strList.reduceLeft((acc, s) => acc + File.separatorChar + s)
  }

  private val NECESSARY_SWAVE_CLASSES = List(stringListToFile(List("toolkit", "converters", "SwaveActivity.class")),
    stringListToFile(List("toolkit", "converters", "SwaveActivity$.class")),
    stringListToFile(List("toolkit", "converters", "SwaveActivity$MyObjectInputStream$1.class")))

  def loadSwaveObj(swaveObj: Spout[_], jars: Iterable[File], graphTimeOut: Long = 60 * 60 * 1000): Vector[Serializable] = {
    catchExceptions {
      getGraphRep(swaveObj) match {
        case Some(graphRep) =>
          val sourceFiles = NECESSARY_SWAVE_CLASSES
          val tempJarFile = new File("temp", "swaveClasses.jar")
          tempJarFile.getParentFile.mkdir()
          createJar(tempJarFile, sourceFiles)
          val jars = List(tempJarFile)
          val uuid = UUID.randomUUID().toString
          val exoGraph = new ExoGraph(jars, graphRep, uuid, graphTimeOut)
          tempJarFile.delete()

          runExoGraph(exoGraph)
        case None =>
          Vector()
      }
    }
  }

  def loadSwaveObj(swaveObj: Spout[_]): Vector[Serializable] = {
    loadSwaveObj(swaveObj, 60 * 60 * 1000)
  }

  /**
    * Converts a spout representation in an ExoGraph and runs it in the default space
    * Tries to create a jar with all the necessary classes needed
    *
    * @param swaveObj the spout object to be converted
    * @return the r
    */
  def loadSwaveObj(swaveObj: Spout[_], graphTimeOut: Long): Vector[Serializable] = {
    catchExceptions {
      getGraphRep(swaveObj) match {
        case Some(graphRep) =>
          val swaveSourcesSets = for {
            (_, bytes) <- activitiesMap
            classPath <- {
              val checker = new ObjectInputStreamChecker(new ByteArrayInputStream(bytes))
              val _ = checker.readObject()
              checker.getClassesNeeded.filterNot(_.startsWith("[L")).map(path => (path.replace(".", " ") + ".class").split(" "))
            }
          } yield classPath
          val swaveSources = swaveSourcesSets.toSet.map((path: Array[String]) => stringListToFile(path.toList))

          val sourceFiles: Set[String] = swaveSources ++ NECESSARY_SWAVE_CLASSES
          val tempJarFile = File.createTempFile("swaveClasses", ".jar")
          createJar(tempJarFile, sourceFiles)
          val jars = List(tempJarFile)
          val uuid = UUID.randomUUID().toString
          val exoGraph = new ExoGraph(jars, graphRep, uuid, graphTimeOut)
          tempJarFile.delete()

          show(swaveObj)

          runExoGraph(exoGraph)
        case None =>
          Vector()
      }
    }
  }

  private def catchExceptions[T](b: T): T = {
    try {
      b
    } catch {
      case e: NotAValidSwave => throw e
      case _: Exception => throw new NotAValidSwave
    }
  }

  private def createJar(jarFile: File, sources: Iterable[String]): Unit = {
    // Create a buffer for reading the files
    val buf = Array.ofDim[Byte](1024)

    try {
      jarFile.getParentFile.mkdirs()
      val out: ZipOutputStream = new ZipOutputStream(new FileOutputStream(jarFile))

      val loader = getClass.getClassLoader

      // Compress the file
      for (source <- sources) {
        try {
          val resourceStream: InputStream = loader.getResourceAsStream(source)
          Option(resourceStream).foreach {
            in =>
              // Add ZIP entry to output stream.
              out.putNextEntry(new ZipEntry(source))

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
        } catch {
          case _: FileNotFoundException =>
          //ignore class
        }
      }

      // Complete the ZIP file
      out.close()
    } catch {
      case e: IOException => e.printStackTrace()
    }
  }

  private class ObjectInputStreamChecker(in: InputStream) extends ObjectInputStream(in) {
    private val loader = new ClassLoaderChecker()
    private val classNames = mutable.Set[String]()

    override def resolveClass(desc: ObjectStreamClass): Class[_] = {
      val name = desc.getName
      try {
        val c = loader.findClass(name)
        loader.getAllClassNames.foreach(classNames.update(_, included = true))
        c
      } catch {
        case _: java.lang.ClassNotFoundException =>
          super.resolveClass(desc)
      }
    }

    def getClassesNeeded: Set[String] = classNames.toSet
  }

  class NotAValidSwave extends Exception("This swave object can't be converted to an ExoGraph")

}
