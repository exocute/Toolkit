package toolkit.converters

import java.io.{File, Serializable}

import clifton.graph.ExoGraph
import exocute.Activity
import exonode.clifton.node.CliftonClassLoader
import exonode.clifton.signals.{ActivityFilterType, ActivityFlatMapType, ActivityMapType, ActivityType}
import shapeless.{HNil, :: => @::}
import swave.core.StreamOps.SubStreamOps
import swave.core._
import swave.core.graph.GlyphSet
import toolkit.{ActivityRep, ValidGraphRep}

import scala.collection.{immutable, mutable}
import scala.language.existentials

/**
  * Created by #GrowinScala
  */
object ExoGraphToSwave {

  private trait ParameterLessActivity {
    def process(input: Serializable): Serializable
  }

  private class SimpleActivity(act: Activity, params: Vector[String]) extends ParameterLessActivity {
    def process(input: Serializable): Serializable = {
      act.process(input, params)
    }
  }

  private val classesCache = mutable.Map[String, Class[_]]()

  private def getActivity(loader: CliftonClassLoader, classPath: String, params: Vector[String]): ParameterLessActivity = {
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
      case ExoSimple(id, act, actType, next) => ExoSimple(id, act, actType, cutActivityAt(next))
      case ExoFork(id, act, actType, nextList, ExoJoin(joinId, joinAct, joinActType, joinNext)) =>
        ExoFork(id, act, actType, nextList.map(tree => cutActivityAt(tree)),
          ExoJoin(joinId, joinAct, joinActType, cutActivityAt(joinNext)))
      case ExoJoin(_, _, _, _) => ExoFinish
      case default => default
    }
  }

  private def cleanTree(tree: ExoTree): ExoTree = {
    tree match {
      case ExoStart(next) => ExoStart(cleanTree(next))
      case ExoSimple(id, act, actType, next) => ExoSimple(id, act, actType, cleanTree(next))
      case ExoFork(id, act, actType, nextList, ExoJoin(joinId, joinAct, joinActType, joinNext)) =>
        ExoFork(id, act, actType, nextList.map(tree => cutActivityAt(tree)), ExoJoin(joinId, joinAct, joinActType, cutActivityAt(joinNext)))
      case ExoJoin(id, act, actType, next) => ExoJoin(id, act, actType, cleanTree(next))
      case default => default
    }
  }

  private type NormalOp = SubStreamOps[_, HNil, Nothing, StreamOps[_]#FanOut[HNil, Nothing]]

  private def convertToExoTree(loader: CliftonClassLoader, graphRep: ValidGraphRep): ExoTree = {

    val seen = mutable.Map[String, ExoTree]()

    def convertToSwaveAux(activityRep: ActivityRep): ExoTree = {
      val id = activityRep.id
      val activity = try {
        getActivity(loader, activityRep.className, activityRep.parameters)
      } catch {
        case _: java.lang.LinkageError => return seen(id)
      }

      graphRep.getConnections(activityRep).toList match {
        case Nil =>
          seen.getOrElse(id, {
            val tree = {
              if (graphRep.getReverseConnections(activityRep).size <= 1)
                ExoSimple(id, activity, activityRep.actType, ExoFinish)
              else
                ExoJoin(id, activity, activityRep.actType, ExoFinish)
            }
            seen.update(id, tree)
            tree
          })
        case List(nextActRep) =>
          seen.getOrElse(id, {
            val tree = {
              if (graphRep.getReverseConnections(activityRep).size <= 1)
                ExoSimple(id, activity, activityRep.actType, convertToSwaveAux(nextActRep))
              else
                ExoJoin(id, activity, activityRep.actType, convertToSwaveAux(nextActRep))
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
              ExoFork(id, activity, activityRep.actType, acts, seen(joinAct))
            }
            seen.update(id, tree)
            tree
          })
      }
    }

    val start = graphRep.root
    ExoStart(convertToSwaveAux(start))
  }

  private def convertTreeToSwave(graph: ValidGraphRep, start: ExoTree, initial: Spout[_]): Spout[_] = {
    val seen = mutable.Set[String]()

    def addSwaveStage(initialSpout: Spout[_], activity: ParameterLessActivity, actType: ActivityType): Spout[_] = {
      actType match {
        case ActivityMapType =>
          initialSpout.map(input => activity.process(input.asInstanceOf[Serializable]))
        case ActivityFilterType =>
          initialSpout.filter(input => activity.process(input.asInstanceOf[Serializable]).asInstanceOf[Boolean])
        case ActivityFlatMapType =>
          initialSpout.flatMap(input => activity.process(input.asInstanceOf[Serializable]).asInstanceOf[immutable.Iterable[Serializable]])
      }
    }

    def convertToSwaveAux(exoTree: ExoTree, initialSpout: Spout[_]): Option[Spout[_]] = {
      exoTree match {
        case ExoFinish =>
          Some(initialSpout)
        case ExoStart(next) =>
          convertToSwaveAux(next, initialSpout)
        case ExoSimple(id, activity, actType, next) =>
          if (seen.contains(id))
            None
          else {
            seen.update(id, included = true)
            val spout = addSwaveStage(initialSpout, activity, actType)
            convertToSwaveAux(next, spout)
          }
        case ExoFork(id, activity, actType, next, joinTree) =>
          def addSubStages(_spout: AnyRef, after: List[(ExoTree, Int)]): AnyRef = after match {
            case Nil => _spout
            case (elem, index) :: others =>
              val spout = _spout.asInstanceOf[StreamOps[_]#FanOut[HNil, Nothing]]
              val activityWrapper = new ParameterLessActivity {
                override def process(inputVector: Serializable): Serializable = {
                  val input = inputVector.asInstanceOf[Vector[Serializable]](index)
                  elem.act.process(input)
                }
              }
              val elemConverted = convertToSwaveAux2(elem.setActivity(activityWrapper), spout.sub)
                .asInstanceOf[NormalOp].end
              addSubStages(elemConverted, others)
          }

          val forkSpout = convertToSwaveAux(ExoSimple(id, activity, actType, ExoFinish), initialSpout).get
          val forkBody = addSubStages(forkSpout.fanOutBroadcast(), next.zipWithIndex)
          val forkResult = forkBody.asInstanceOf[initialSpout.FanOut[Any @:: Any @:: HNil, Nothing]]
            .fanInMerge()
            .grouped(next.size)
            .map(_.toVector)
          convertToSwaveAux(joinTree, forkResult)
        case ExoJoin(id, activity, actType, next) =>
          if (seen.contains(id))
            None
          else {
            seen.update(id, included = true)
            val spout = addSwaveStage(initialSpout, activity, actType)
            convertToSwaveAux(next, spout)
          }
      }
    }

    def addSwaveStage2(_initialSpout: AnyRef, activity: ParameterLessActivity, actType: ActivityType): AnyRef = {
      val initialSpout = _initialSpout.asInstanceOf[NormalOp]
      actType match {
        case ActivityMapType =>
          initialSpout.map(input => activity.process(input.asInstanceOf[Serializable]))
        case ActivityFilterType =>
          initialSpout.filter(input => activity.process(input.asInstanceOf[Serializable]).asInstanceOf[Boolean])
        case ActivityFlatMapType =>
          initialSpout.flatMap(input => activity.process(input.asInstanceOf[Serializable]).asInstanceOf[immutable.Iterable[Serializable]])
      }
    }

    def convertToSwaveAux2(exoTree: ExoTree, _initialSpout: AnyRef): AnyRef = {
      val initialSpout = _initialSpout.asInstanceOf[NormalOp]

      exoTree match {
        case ExoFinish =>
          initialSpout
        case ExoStart(next) =>
          convertToSwaveAux2(next, initialSpout)
        case ExoSimple(id, activity, actType, next) =>
          if (seen.contains(id))
            None
          else {
            seen.update(id, included = true)
            val spout = addSwaveStage2(initialSpout, activity, actType)
            convertToSwaveAux2(next, spout)
          }
        case ExoFork(id, activity, actType, next, joinTree) =>
          def startLoop(a: AnyRef, after: List[(ExoTree, Int)]): AnyRef = after match {
            case Nil => a
            case (elem, index) :: others =>
              val spout = a.asInstanceOf[StreamOps[_]#FanOut[HNil, Nothing]]
              val activityWrapper = new ParameterLessActivity {
                override def process(inputVector: Serializable): Serializable = {
                  val input = inputVector.asInstanceOf[Vector[Serializable]](index)
                  elem.act.process(input)
                }
              }
              val elemConverted = convertToSwaveAux2(elem.setActivity(activityWrapper), spout.sub)
                .asInstanceOf[NormalOp].end
              startLoop(elemConverted, others)
          }

          val forkSpout = convertToSwaveAux2(ExoSimple(id, activity, actType, ExoFinish), initialSpout)
            .asInstanceOf[NormalOp]
          val forkBody = startLoop(forkSpout.fanOutBroadcast(), next.zipWithIndex)
          val forkResult = forkBody.asInstanceOf[initialSpout.FanOut[Any @:: Any @:: HNil, Nothing]]
            .fanInMerge()
            .grouped(next.size)
            .map(_.toVector)
          convertToSwaveAux2(joinTree, forkResult)
        case ExoJoin(id, activity, actType, next) =>
          if (seen.contains(id))
            None
          else {
            seen.update(id, included = true)
            val spout = addSwaveStage2(initialSpout, activity, actType)
            convertToSwaveAux2(next, spout)
          }
      }
    }

    convertToSwaveAux(start, initial).get
  }

  def convertExoGraphToSwave(jarList: List[File], graph: ValidGraphRep, initialSpout: Spout[_]): Spout[_] = {
    val jarsInBytes = jarList.map(jarFile => CliftonClassLoader.getJarAsBytes(jarFile).get)
    val loader = new CliftonClassLoader()
    for (bytes <- jarsInBytes)
      loader.init(bytes)
    val tree = cleanTree(convertToExoTree(loader, graph))
    println(tree)
    val convertedSpout = convertTreeToSwave(graph, tree, initialSpout)
    convertedSpout
  }

  def show(spout: Spout[_]): Unit =
    println(Graph.from(spout.stage).withGlyphSet(GlyphSet.`2x2 ASCII`).render)

  implicit class ExoGraphWithSwave(exoGraph: ExoGraph) {
    def toSwave(jarList: List[File], input: Iterable[_]): Spout[_] = {
      convertExoGraphToSwave(jarList, exoGraph.graph, Spout.fromIterable(input))
    }
  }

  private sealed trait ExoTree {
    val getId: Option[String] = None
    val nextList: List[ExoTree] = Nil
    val act: ParameterLessActivity = null
    val actType: ActivityType = null

    def setActivity(newAct: ParameterLessActivity): ExoTree =
      throw new Exception("This ExoTree doesn't have an activity")
  }

  private case class ExoStart(next: ExoTree) extends ExoTree {
    override def toString: String = s"Start->$next"
  }

  private case class ExoSimple(id: String, override val act: ParameterLessActivity, override val actType: ActivityType,
                               next: ExoTree) extends ExoTree {
    override val getId = Some(id)
    override val nextList = List(next)

    override def setActivity(newAct: ParameterLessActivity) = ExoSimple(id, newAct, actType, next)

    override def toString: String = s"Map($id)->$next"
  }

  private case class ExoFork(id: String, override val act: ParameterLessActivity, override val actType: ActivityType,
                             override val nextList: List[ExoTree], joinTree: ExoTree) extends ExoTree {
    override val getId = Some(id)

    override def setActivity(newAct: ParameterLessActivity) = ExoFork(id, newAct, actType, nextList, joinTree)

    override def toString: String = s"Fork($id to ${joinTree.getId.get})->${nextList.mkString("[", ", ", "]")} {$joinTree}"
  }

  private case class ExoJoin(id: String, override val act: ParameterLessActivity, override val actType: ActivityType,
                             next: ExoTree) extends ExoTree {
    override val getId = Some(id)
    override val nextList = List(next)

    override def setActivity(newAct: ParameterLessActivity) = ExoJoin(id, newAct, actType, next)

    override def toString: String = s"Join($id)->$next"
  }

  private case object ExoFinish extends ExoTree {
    override def toString: String = "Finish"
  }

}
