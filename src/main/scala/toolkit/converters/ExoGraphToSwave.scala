package toolkit.converters

import java.io.{File, Serializable}

import clifton.graph.ExocuteConfig
import exocute.Activity
import exonode.clifton.node.CliftonClassLoader
import shapeless.{HNil, :: => @::}
import swave.core.StreamOps.SubStreamOps
import swave.core._
import swave.core.graph.GlyphSet
import toolkit.{ActivityRep, GraphRep}

import scala.collection.mutable
import scala.language.existentials
import scala.util.{Failure, Success}

/**
  * Created by #ScalaTeam on 23-02-2017.
  */
object ExoGraphToSwave {

  private trait Simple {
    def process(input: Serializable): Serializable
  }

  private class SimpleActivity(act: Activity, params: Vector[String]) extends Simple {
    def process(input: Serializable): Serializable = {
      act.process(input, params)
    }
  }

  private val classesCache = mutable.Map[String, Class[_]]()

  private def getActivity(loader: CliftonClassLoader, classPath: String, params: Vector[String]): Simple = {
    val classFound = classesCache.getOrElse(classPath, {
      val c = loader.findClass(classPath)
      classesCache.update(classPath, c)
      c
    })
    val act = classFound.newInstance().asInstanceOf[Activity]
    val result = new SimpleActivity(act, params)
    result
  }

  private def getAll(act: ExoTree): List[ExoTree] = {
    act.getId match {
      case None => Nil
      case Some(_) => act :: act.nextList.flatMap(act => getAll(act))
    }
  }

  private def cutActivityAt(tree: ExoTree): ExoTree = {
    tree match {
      case ExoStart(next) => ExoStart(cutActivityAt(next))
      case ExoMap(id, act, next) => ExoMap(id, act, cutActivityAt(next))
      case ExoFork(id, act, nextList, ExoJoin(joinId, joinAct, joinNext)) =>
        ExoFork(id, act, nextList.map(tree => cutActivityAt(tree)), ExoJoin(joinId, joinAct, cutActivityAt(joinNext)))
      case ExoJoin(_, _, _) => ExoFinish
      case default => default
    }
  }

  private def cleanTree(tree: ExoTree): ExoTree = {
    tree match {
      case ExoStart(next) => ExoStart(cleanTree(next))
      case ExoMap(id, act, next) => ExoMap(id, act, cleanTree(next))
      case ExoFork(id, act, nextList, ExoJoin(joinId, joinAct, joinNext)) =>
        ExoFork(id, act, nextList.map(tree => cutActivityAt(tree)), ExoJoin(joinId, joinAct, cutActivityAt(joinNext)))
      case ExoJoin(id, act, next) => ExoJoin(id, act, cleanTree(next))
      case default => default
    }
  }

  private def convertToExoTree(loader: CliftonClassLoader, graphRep: GraphRep): ExoTree = {
    val seen = mutable.Map[String, ExoTree]()

    def convertToSwaveAux(activityRep: ActivityRep): ExoTree = {
      val id = activityRep.id
      val activity = try {
        getActivity(loader, activityRep.name, activityRep.parameters)
      } catch {
        case _: java.lang.LinkageError => return seen(id)
      }

      graphRep.getConnections(activityRep) match {
        case Nil =>
          seen.getOrElse(id, {
            val tree = {
              if (graphRep.getReverseConnections(activityRep).size == 1)
                ExoMap(id, activity, ExoFinish)
              else
                ExoJoin(id, activity, ExoFinish)
            }
            seen.update(id, tree)
            tree
          })
        case List(nextActRep) =>
          seen.getOrElse(id, {
            val tree = {
              if (graphRep.getReverseConnections(activityRep).size == 1)
                ExoMap(id, activity, convertToSwaveAux(nextActRep))
              else
                ExoJoin(id, activity, convertToSwaveAux(nextActRep))
            }
            seen.update(id, tree)
            tree
          })
        case list =>
          seen.getOrElse(id, {
            val tree = {
              val acts = list.map(actRep => convertToSwaveAux(actRep))
              val actsMap = acts.map(act => getAll(act).flatMap(_.getId))
              val act1 :: act2 :: _ = actsMap
              val joinAct = act1.toStream.filter(s => act2.contains(s)).head
              ExoFork(id, activity, acts, seen(joinAct))
            }
            seen.update(id, tree)
            tree
          })
      }
    }

    val start = graphRep.getRoot.get
    ExoStart(convertToSwaveAux(start))
  }

  private def convertTreeToSwave(graph: GraphRep, start: ExoTree, initial: Spout[_]): Spout[_] = {
    val seen = mutable.Set[String]()

    def convertToSwaveAux(exoTree: ExoTree, initialSpout: Spout[_]): Option[Spout[_]] = {
      exoTree match {
        case ExoFinish =>
          Some(initialSpout)
        case ExoStart(next) =>
          convertToSwaveAux(next, initialSpout)
        case ExoMap(id, activity, next) =>
          if (seen.contains(id))
            None
          else {
            seen.update(id, included = true)
            val spout = initialSpout.map(input => activity.process(input.asInstanceOf[Serializable]))
            convertToSwaveAux(next, spout)
          }
        case ExoFork(id, activity, next, joinTree) =>
          def startLoop(a: AnyRef, after: List[(ExoTree, Int)]): AnyRef = after match {
            case Nil => a
            case (elem, index) :: others =>
              val spout = a.asInstanceOf[StreamOps[_]#FanOut[HNil, Nothing]]
              val activityWrapper = new Simple {
                override def process(inputVector: Serializable): Serializable = {
                  val input = inputVector.asInstanceOf[Vector[Serializable]](index)
                  elem.act.process(input)
                }
              }
              val elemConverted = convertToSwaveAux2(elem.setActivity(activityWrapper), spout.sub)
                .asInstanceOf[SubStreamOps[_, HNil, Nothing, StreamOps[_]#FanOut[HNil, Nothing]]].end
              startLoop(elemConverted, others)
          }

          val forkSpout = convertToSwaveAux(ExoMap(id, activity, ExoFinish), initialSpout).get
          val forkBody = startLoop(forkSpout.fanOutBroadcast(), next.zipWithIndex)
          val forkResult = forkBody.asInstanceOf[initialSpout.FanOut[Any @:: Any @:: HNil, Nothing]].fanInMerge().grouped(next.size).map(_.toVector)
          convertToSwaveAux(joinTree, forkResult)
        case ExoJoin(id, activity, next) =>
          if (seen.contains(id))
            None
          else {
            seen.update(id, included = true)
            val spout = initialSpout.map(input => activity.process(input.asInstanceOf[Serializable]))
            convertToSwaveAux(next, spout)
          }
      }
    }

    def convertToSwaveAux2(exoTree: ExoTree, _initialSpout: AnyRef): AnyRef = {
      val initialSpout = _initialSpout.asInstanceOf[SubStreamOps[_, HNil, Nothing, StreamOps[_]#FanOut[HNil, Nothing]]]

      exoTree match {
        case ExoFinish =>
          initialSpout
        case ExoStart(next) =>
          convertToSwaveAux2(next, initialSpout)
        case ExoMap(id, activity, next) =>
          if (seen.contains(id))
            None
          else {
            seen.update(id, included = true)
            val spout = initialSpout.map(input => activity.process(input.asInstanceOf[Serializable]))
            convertToSwaveAux2(next, spout)
          }
        case ExoFork(id, activity, next, joinTree) =>
          def startLoop(a: AnyRef, after: List[(ExoTree, Int)]): AnyRef = after match {
            case Nil => a
            case (elem, index) :: others =>
              val spout = a.asInstanceOf[StreamOps[_]#FanOut[HNil, Nothing]]
              val activityWrapper = new Simple {
                override def process(inputVector: Serializable): Serializable = {
                  val input = inputVector.asInstanceOf[Vector[Serializable]](index)
                  elem.act.process(input)
                }
              }
              val elemConverted = convertToSwaveAux2(elem.setActivity(activityWrapper), spout.sub)
                .asInstanceOf[SubStreamOps[_, HNil, Nothing, StreamOps[_]#FanOut[HNil, Nothing]]].end
              startLoop(elemConverted, others)
          }

          val forkSpout = convertToSwaveAux2(ExoMap(id, activity, ExoFinish), initialSpout)
            .asInstanceOf[SubStreamOps[_, HNil, Nothing, StreamOps[_]#FanOut[HNil, Nothing]]]
          val forkBody = startLoop(forkSpout.fanOutBroadcast(), next.zipWithIndex)
          val forkResult = forkBody.asInstanceOf[initialSpout.FanOut[Any @:: Any @:: HNil, Nothing]].fanInMerge().grouped(next.size).map(_.toVector)
          convertToSwaveAux2(joinTree, forkResult)
        case ExoJoin(id, activity, next) =>
          if (seen.contains(id))
            None
          else {
            seen.update(id, included = true)
            val spout = initialSpout.map(input => activity.process(input.asInstanceOf[Serializable]))
            convertToSwaveAux2(next, spout)
          }
      }
    }

    convertToSwaveAux(start, initial).get
  }

  def convertExoGraphToSwave(jars: List[File], graph: GraphRep, initialSpout: Spout[_]): Spout[_] = {
    val jarsInBytes = jars.map(jarFile => CliftonClassLoader.getJarAsBytes(jarFile).get)
    val loader = new CliftonClassLoader()
    for (bytes <- jarsInBytes)
      loader.init(bytes)
    val tree = cleanTree(convertToExoTree(loader, graph))
    val convertedSpout = convertTreeToSwave(graph, tree, initialSpout)
    convertedSpout
  }

  def show(spout: Spout[_]): Unit =
    println(Graph.from(spout.stage).withGlyphSet(GlyphSet.`2x2 ASCII`).render)

  def main(args: Array[String]): Unit = {
//    val file = new File("examples\\multi_fork_1a.grp")
        val file = new File("examples\\multi_fork_2a.grp")
    //    val file = new File("examples\\multi_fork_3a.grp")
    val jars = List(new File("examples\\classes.jar"))

    val starterExoGraph = ExocuteConfig.setHosts()
    starterExoGraph.addGraph(file, jars, 60 * 60 * 1000) match {
      case Failure(e) => e.printStackTrace()
      case Success(exoGraph) =>
        val graph = exoGraph.graph
        implicit val env = StreamEnv()

        val input = Stream.from(1).map(_.toLong)
        val initialSpout = Spout.fromIterable(input)

        val convertedSpout = convertExoGraphToSwave(jars, graph, initialSpout)
        show(convertedSpout)

        convertedSpout
          .logSignal("Signal Catcher")
          .take(10)
          .drainTo(Drain.foreach(println))
    }
  }

  private sealed trait ExoTree {
    val getId: Option[String] = None
    val nextList: List[ExoTree] = Nil
    val act: Simple = null

    def setActivity(newAct: Simple): ExoTree = ???
  }

  private case class ExoStart(next: ExoTree) extends ExoTree {
    override def toString: String = s"Start->$next"
  }

  private case class ExoMap(id: String, override val act: Simple, next: ExoTree) extends ExoTree {
    override val getId = Some(id)
    override val nextList = List(next)

    override def setActivity(newAct: Simple) = ExoMap(id, newAct, next)

    override def toString: String = s"Map($id)->$next"
  }

  private case class ExoFork(id: String, override val act: Simple, override val nextList: List[ExoTree], joinTree: ExoTree) extends ExoTree {
    override val getId = Some(id)

    override def setActivity(newAct: Simple) = ExoFork(id, newAct, nextList, joinTree)

    override def toString: String = s"Fork($id to ${joinTree.getId.get})->${nextList.mkString("[", ", ", "]")} {$joinTree}"
  }

  private case class ExoJoin(id: String, override val act: Simple, next: ExoTree) extends ExoTree {
    override val getId = Some(id)
    override val nextList = List(next)

    override def setActivity(newAct: Simple) = ExoJoin(id, newAct, next)

    override def toString: String = s"Join($id)->$next"
  }

  private case object ExoFinish extends ExoTree {
    override def toString: String = "Finish"
  }

}
