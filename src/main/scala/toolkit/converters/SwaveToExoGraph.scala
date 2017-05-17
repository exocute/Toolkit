package toolkit.converters

import java.io.{InputStream, _}
import java.util.UUID
import java.util.zip.{ZipEntry, ZipOutputStream}

import api.{Collector, Injector}
import clifton.graph.{ExoGraph, ExoGraphTimeOut}
import exonode.clifton.signals.{ActivityFilterType, ActivityFlatMapType, ActivityMapType, ActivityType}
import swave.core._
import swave.core.graph.GlyphSet
import toolkit.converters.SwaveToExoGraph.{ExoGraphWithResults, FunctionType}
import toolkit.{ActivityRep, GraphRep, ValidGraphRep}

import scala.collection.mutable
import scala.io.Source
import scala.language.implicitConversions
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

/**
  * Created by #GrowinScala
  *
  * @param swaveObj the spout object to be converted
  */
private class SwaveToExoGraph(swaveObj: Spout[_]) {

  private var id: String = "A"
  private val activitiesMap = mutable.Map[String, Array[Byte]]()
  private val functionsMap = mutable.Map[String, FunctionType]()
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
      val bytes = bos.toByteArray
      bytes
    } finally {
      try {
        bos.close()
      } catch {
        case _: IOException =>
        // ignore close exception
      }
    }
  }

  private def getSwaveCompositionToGraph(startStage: Stage, uuid: String): GraphRep = {
    def toActivityRep(graph: GraphRep, stage: Stage,
                      functionData: (FunctionType, Array[Byte], ActivityType)): (GraphRep, ActivityRep) = {
      val stageStr = stage.toString

      val newId = createNewId()
      swaveStagesMap.update(stageStr, newId)
      val (function, functionBytes, functionType) = functionData
      val functionInStr = new String(functionBytes.map(_.toChar))
      activitiesMap.update(newId, functionBytes)
      functionsMap.update(newId, function)
      val actRep = ActivityRep(newId, "toolkit.converters.SwaveActivity", functionType, Vector(s"$uuid.$newId", functionInStr), Vector(), "")
      (graph.addActivity(actRep), actRep)
    }

    def stageToActivityRep(graph: GraphRep, stage: Stage): (GraphRep, Option[ActivityRep]) = {
      val stageStr = stage.toString
      swaveStagesMap.get(stageStr) match {
        case Some(activityId) => (graph, Some(graph.getActivity(activityId)))
        case None => stageToActivity(stage).map(rep => toActivityRep(graph, stage, rep)) match {
          case Some((updatedGraph, act)) => (updatedGraph, Some(act))
          case None => (graph, None)
        }
      }
    }

    def firstStage(graph: GraphRep, stages: List[Stage]): GraphRep = {
      stages match {
        case Nil =>
          graph
        case List(stage) =>
          stageToActivityRep(graph, stage) match {
            case (updatedGraph, None) =>
              firstStage(updatedGraph, stage.inputStages)
            case (updatedGraph, Some(actRep)) =>
              otherStages(updatedGraph, actRep, stage.inputStages)
          }
        case _ =>
          throw new NotAValidSwave(new Exception("The first swave stage can only have one source"))
      }
    }

    def otherStages(graph: GraphRep, act: ActivityRep, stages: List[Stage]): GraphRep = {
      stages match {
        case Nil =>
          graph
        case List(stage) =>
          stageToActivityRep(graph, stage) match {
            case (updatedGraph, None) =>
              otherStages(updatedGraph, act, stage.inputStages)
            case (updatedGraph, Some(actRep)) =>
              Try(updatedGraph.addConnection(actRep.id, act.id)) match {
                case Success(updatedGraph2) =>
                  otherStages(updatedGraph2, actRep, stage.inputStages)
                case Failure(_) =>
                  updatedGraph
              }
          }
        case list =>
          list.foldLeft(graph)((updatedGraph, stage) => {
            stageToActivityRep(updatedGraph, stage) match {
              case (updatedGraph2, Some(activity)) =>
                val updatedGraph3 = updatedGraph2.addConnection(activity.id, act.id)
                otherStages(updatedGraph3, activity, stage.inputStages)
              case (updatedGraph2, None) =>
                updatedGraph2
            }
          })
      }
    }

    swaveInput = Stream.empty
    val graph = firstStage(new GraphRep("SwaveConverter"), List(startStage))
    graph
  }

  private var swaveInput: Stream[Serializable] = _

  private def stageToActivity(stage: Stage): Option[(FunctionType, Array[Byte], ActivityType)] = {
    stage.kind match {
      case inOut: Stage.Kind.InOut =>
        import Stage.Kind.InOut
        inOut match {
          case InOut.Nop =>
            None
          case InOut.Map(fAny) =>
            val f = fAny.asInstanceOf[FunctionType]
            Some(f, ObjectToBytes(f), ActivityMapType)
          case InOut.Filter(fBool, negated) =>
            val f: FunctionType = SwaveActivity.negateBool(negated, fBool)
            Some(f, ObjectToBytes(f), ActivityFilterType)
          case others =>
            throw new NotAValidSwave(new Exception(s"Swave method not supported: ${others.name}"))
        }
      case input: Stage.Kind.Spout =>
        import Stage.Kind.Spout
        input match {
          case Spout.FromFuture(future) =>
            swaveInput = swaveInput #::: Stream(future.value.get.get.asInstanceOf[Serializable])
          case Spout.FromIterator(it) =>
            swaveInput = swaveInput #::: it.toStream.map(_.asInstanceOf[Serializable])
          case others =>
            throw new NotAValidSwave(new Exception(s"Swave method not supported: ${others.name}"))
        }
        None
      case fanOut: Stage.Kind.FanOut =>
        import Stage.Kind.FanOut
        fanOut match {
          case FanOut.Broadcast(_) =>
            // the previous function should have this one connections (and be a flatMap)
            val f = SwaveActivity.Identity
            Some(f, ObjectToBytes(f), ActivityMapType)
          case others =>
            throw new NotAValidSwave(new Exception(s"Swave method not supported: ${others.name}"))
        }
      case fanIn: Stage.Kind.FanIn =>
        import Stage.Kind.FanIn
        fanIn match {
          case FanIn.ToTuple =>
            val f = SwaveActivity.FanInToTuple
            Some(f, ObjectToBytes(f), ActivityMapType)
          case others =>
            throw new NotAValidSwave(new Exception(s"Swave method not supported: ${others.name}"))
        }
      case flatten: Stage.Kind.Flatten =>
        import Stage.Kind.Flatten
        flatten match {
          case Flatten.Concat(_) =>
            // the previous function should be FlatMap
            val f = SwaveActivity.Identity
            Some(f, ObjectToBytes(f), ActivityFlatMapType)
          case others =>
            throw new NotAValidSwave(new Exception(s"Swave method not supported: ${others.name}"))
        }
      case others =>
        throw new NotAValidSwave(new Exception(s"Swave method not supported: ${others.name}"))
    }

  }

  def show(): Unit = SwaveToExoGraph.show(swaveObj)

  private def getGraphRep(swaveObj: Spout[_], uuid: String): Try[ValidGraphRep] = {
    val s = swaveObj.stage

    val graphRep = getSwaveCompositionToGraph(s, uuid)
    graphRep.checkValidGraph()
  }

  private def injectSwaveData(exoGraph: ExoGraph): Vector[Int] = {
    val injector = exoGraph.injector

    val injIds = injector.injectMany(swaveInput)
    injIds
  }

  private val (necessarySwaveClassesBar, necessarySwaveClassesDot) = {
    val list = List("toolkit.converters.SwaveActivity",
      "toolkit.converters.SwaveActivity$",
      "toolkit.converters.SwaveActivity$MyObjectInputStream$1",
      "toolkit.converters.SwaveToExoGraph",
      "toolkit.converters.SwaveToExoGraph$",
      "toolkit.converters.SwaveToExoGraph$ExoGraphWithResults",
      "toolkit.converters.SwaveToExoGraph$SpoutWithExocute"
    )

    (list.map(dotToBar), list)
  }

  private def dotToBar(className: String) = className.replace(".", "/") + ".class"

  def loadSwaveObj(userJars: Iterable[File], graphTimeOut: Long = 60 * 60 * 1000): ExoGraphWithResults = {
    catchExceptions {
      val uuid = UUID.randomUUID().toString
      getGraphRep(swaveObj, uuid) match {
        case Success(graphRep) =>
          val sourceFiles = necessarySwaveClassesBar
          val tempJarFile = File.createTempFile("swaveClasses", ".jar")
          createJar(tempJarFile, sourceFiles)
          val jars = List(tempJarFile) ++ userJars
          val exoGraph = new ExoGraphTimeOut(jars, graphRep, uuid, graphTimeOut)
          tempJarFile.deleteOnExit()

          new ExoGraphWithResults(exoGraph, injectSwaveData(exoGraph))
        case Failure(e) =>
          throw new NotAValidSwave(e)
      }
    }
  }

  private def getNeededClasses(listOfClasses: List[String]): Set[String] = {
    val rt: Runtime = Runtime.getRuntime
    val batFile = File.createTempFile("command", ".bat")
    val jdepsOutput = File.createTempFile("jdeps", ".txt")
    val classFile = File.createTempFile("classCode", ".class")

    val loader = getClass.getClassLoader

    def getNeededClassesAux(classesToCheck: List[String], classesNeeded: Set[String]): Set[String] = {
      classesToCheck match {
        case Nil => classesNeeded
        case className :: othersToCheck =>
          //          print(s"checking class $className ")

          val bytes = getClassBytes(loader.getResourceAsStream(className))
          val bos = new BufferedOutputStream(new FileOutputStream(classFile))
          Stream.continually(bos.write(bytes))
          bos.close() // You may end up with 0 bytes file if not calling close.

          // full command
          val command = String.format("jdeps -v -P \"%s\" > \"%s\"",
            classFile.getAbsolutePath, jdepsOutput.getAbsolutePath)
          //          println(command)

          // write command in at file
          new PrintWriter(batFile) {
            write(command)
            close()
          }

          // execute the bat file
          val pr: Process = rt.exec(batFile.getPath)
          pr.waitFor()

          val fileContents = Source.fromFile(jdepsOutput.getAbsolutePath).getLines.toList
          val neededClasses = parseJdeps(fileContents).filterNot(knownClasses).map(dotToBar)

          val allNeededToCheck = (neededClasses.toSet -- classesNeeded).toList ++ othersToCheck
          //          println(s"[$allNeededToCheck]")
          getNeededClassesAux(allNeededToCheck, classesNeeded ++ neededClasses)
      }
    }

    val list = listOfClasses.filterNot(knownClasses)
    val classes = getNeededClassesAux(list, list.toSet)

    batFile.deleteOnExit()
    jdepsOutput.deleteOnExit()
    classFile.deleteOnExit()

    classes
  }

  private def parseJdeps(text: List[String]): List[String] = {
    text.dropWhile(_.head != ' ').map(line => {
      val line2 = line.drop(line.indexOf(">") + 2)
      val className = line2.takeWhile(_ != ' ')
      val jarExists = line2.drop(className.length).trim
      (className, jarExists)
    }).filter(_._2 == "not found").map(_._1)
  }

  private def knownClasses(className: String): Boolean = {
    className.startsWith("java") || className.startsWith("scala") ||
      className.startsWith("shapeless") || className.startsWith("swave") ||
      necessarySwaveClassesDot.contains(className)
  }

  /**
    * Converts a spout representation in an ExoGraph and runs it in the default space
    * Tries to create a jar with all the necessary classes needed
    *
    * @return the r
    */
  def loadSwaveObj(graphTimeOut: Long): ExoGraphWithResults = {
    catchExceptions {
      val uuid = UUID.randomUUID().toString
      getGraphRep(swaveObj, uuid) match {
        case Success(graphRep) =>
          val swaveSourcesSets = for {
            (_, bytes) <- activitiesMap
            classPath <- {
              val checker = new ObjectInputStreamChecker(new ByteArrayInputStream(bytes))
              val _ = checker.readObject()
              checker.getClassesNeeded.filterNot(_.startsWith("[L"))
            }
          } yield classPath
          val swaveSources = swaveSourcesSets.toSet.map(dotToBar)

          val all = getNeededClasses((swaveSources -- necessarySwaveClassesBar).toList)

          val sourceFiles: Set[String] = all ++ necessarySwaveClassesBar
          val tempJarFile = File.createTempFile("swaveClasses", ".jar")
          createJar(tempJarFile, sourceFiles)
          val jars = List(tempJarFile)
          val exoGraph = new ExoGraphTimeOut(jars, graphRep, uuid, graphTimeOut)
          tempJarFile.deleteOnExit()

          //          show(swaveObj)

          new ExoGraphWithResults(exoGraph, injectSwaveData(exoGraph))
        case Failure(e) =>
          throw new NotAValidSwave(e)
      }
    }
  }

  private def catchExceptions[T](b: T): T = {
    try {
      b
    } catch {
      case e: NotAValidSwave => throw e
      case NonFatal(e) => throw new NotAValidSwave(e)
    }
  }

  private def createJar(jarFile: File, sources: Iterable[String]): Unit = {
    // Create a buffer for reading the files
    val buf = Array.ofDim[Byte](1024)

    try {
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
              def writeBuffer(): Unit = {
                val len = in.read(buf)
                if (len > 0) {
                  out.write(buf, 0, len)
                  writeBuffer()
                }
              }

              writeBuffer()

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

  private def getClassBytes(in: InputStream): Array[Byte] = {
    val buffer: ByteArrayOutputStream = new ByteArrayOutputStream()

    var nRead = 0
    val data = Array.ofDim[Byte](16384)

    def writeBuffer(): Unit = {
      val len = in.read(data, 0, data.length)
      if (len > 0) {
        buffer.write(data, 0, len)
        writeBuffer()
      }
    }

    writeBuffer()

    buffer.flush()

    buffer.toByteArray
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

  class NotAValidSwave(e: Throwable) extends Exception(s"This swave object can't be converted to an ExoGraph ($e)")

}

object SwaveToExoGraph {

  private type FunctionType = Serializable => Serializable

  class ExoGraphWithResults(exoGraph: ExoGraph, injectIds: Vector[Int]) extends ExoGraph {

    override val graph: ValidGraphRep = exoGraph.graph

    override val injector: Injector = exoGraph.injector
    override val collector: Collector = exoGraph.collector

    override def closeGraph(): Unit = exoGraph.closeGraph()

    def result: Vector[Serializable] = {
      val elems = injectIds.flatMap(i => collector.collectAllByIndex(i))

      closeGraph()

      elems
    }
  }

  def loadSwaveObj(swaveObj: Spout[_],
                   userJars: Iterable[File],
                   graphTimeOut: Long = 60 * 60 * 1000): ExoGraphWithResults = {
    new SwaveToExoGraph(swaveObj).loadSwaveObj(userJars, graphTimeOut)
  }

  def loadSwaveObj(swaveObj: Spout[_]): ExoGraphWithResults = {
    loadSwaveObj(swaveObj, 60 * 60 * 1000)
  }

  def loadSwaveObj(swaveObj: Spout[_], graphTimeOut: Long): ExoGraphWithResults = {
    new SwaveToExoGraph(swaveObj).loadSwaveObj(graphTimeOut)
  }

  implicit class SpoutWithExocute(swave: Spout[_]) {
    def toExoGraph: ExoGraphWithResults = {
      SwaveToExoGraph.loadSwaveObj(swave)
    }

    def show(): Unit = SwaveToExoGraph.show(swave)
  }

  def show(spout: Spout[_]): Unit =
    println(Graph.from(spout.stage).withGlyphSet(GlyphSet.`2x2 ASCII`).render)

}