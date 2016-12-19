package toolkit

import java.io.{File, Serializable}

/**
  * Created by #ScalaTeam on 14-12-2016.
  */
class StreamCreator(val rep: GraphRep) {

  var strConstruction = ""

  def runActivity[U <: Serializable](act: ActivityRep, input: U): Serializable = {
    val parameters = act.parameters
    act.asInstanceOf[ActivityTrait].process(input, parameters)
  }

  def run[U <: Serializable](input: Stream[U]): Stream[Serializable] = {
    val injector: ActivityRep = rep.getRoot.get
    strConstruction = "StreamSource.map(" + injector.name + ")"
    val stream = input.map(value => runAux1(value, injector))
    strConstruction += ".realise"
    stream
  }

  def runAux1[U <: Serializable](input: U, act: ActivityRep): Serializable = {
    val actOutput = runActivity(act, input)
    val connections = rep.getConnections(act)
    connections match {
      case Nil => actOutput
      case List(act1) => {
        strConstruction += ".map(" + act1.name + ")"
        runAux1(actOutput, act1)
      }
      case List(act1, act2) => {
        val (o1, o2) = actOutput.asInstanceOf[(Serializable, Serializable)]
        strConstruction += ".split(" + act1.name + ", " + act2.name + ")"
        val (v1, join) = runAuxJoin(o1, act1)
        val (v2, _) = runAuxJoin(o2, act2)
        strConstruction += ".join(" + join.name + ")"
        runActivity(join, (v1, v2))
      }
    }
  }

  def isAJoin(act: ActivityRep): Boolean = rep.isJoin(act)

  def runAuxJoin[U <: Serializable](input: Serializable, act: ActivityRep): (Serializable, ActivityRep) = {
    val actOutput = runActivity(act, input)
    val connections = rep.getConnections(act)
    connections match {
      case List(act1) =>
        if (isAJoin(act1))
          (actOutput, act1)
        else
          runAuxJoin(actOutput, act1)
      case List(act1, act2) => {
        val (v1, join) = runAuxJoin(actOutput, act1)
        val (v2, _) = runAuxJoin(actOutput, act2)
        ((v1, v2), join)
      }
    }
  }

}

object TestStream {

  abstract class ActivityRep2(id: String, name: String, parameters: Vector[String], importName: Vector[String],
                              exportName: String) extends ActivityRep(id, name, parameters, importName, exportName) with ActivityTrait

  def main(args: Array[String]): Unit = {

    def test1() = {

      val actA = new ActivityRep2("A", "SplitVector", Vector[String](), Vector[String](), "") {
        override def process(input: Serializable, params: Vector[String]) = {
          val vec = input.asInstanceOf[Vector[Int]]
          vec.splitAt(vec.size / 2)
        }
      }

      val actB = new ActivityRep2("B", "TestEven", Vector[String](), Vector[String](), "") {
        override def process(input: Serializable, params: Vector[String]) = {
          val value = input.asInstanceOf[Vector[Int]]
          value.forall(_ % 2 == 0)
        }
      }

      val actC = new ActivityRep2("C", "TestOdd", Vector[String](), Vector[String](), "") {
        override def process(input: Serializable, params: Vector[String]) = {
          val value = input.asInstanceOf[Vector[Int]]
          value.forall(_ % 2 == 1)
        }
      }

      val actD = new ActivityRep2("D", "HalfEvenAndOdd", Vector[String](), Vector[String](), "") {
        override def process(input: Serializable, params: Vector[String]) = {
          val (bool1, bool2) = input.asInstanceOf[(Boolean, Boolean)]
          bool1 && bool2
        }
      }


      val graph = new GraphRep("Test 1")
      graph.addSingleActivity(actA)
      graph.addSingleActivity(actB)
      graph.addSingleActivity(actC)
      graph.addSingleActivity(actD)
      graph.addConnection(actA.id, List(actB.id, actC.id))
      graph.addConnection(actB.id, actD.id)
      graph.addConnection(actC.id, actD.id)

      new StreamCreator(graph)
    }

    def test2(): StreamCreator = {

      val actA = new ActivityRep2("A", "FilterCommas", Vector("2"), Vector[String](), "") {
        override def process(input: Serializable, params: Vector[String]) = {
          val str = input.asInstanceOf[String].replace(",", "")
          (str, str)
        }
      }

      val actB = new ActivityRep2("B", "RemoveLast", Vector[String](), Vector[String](), "") {
        override def process(input: Serializable, params: Vector[String]) = {
          val str = input.asInstanceOf[String]
          str.init
        }
      }

      val actC = new ActivityRep2("C", "RemoveFirst", Vector[String](), Vector[String](), "") {
        override def process(input: Serializable, params: Vector[String]) = {
          val str = input.asInstanceOf[String]
          str.tail
        }
      }

      val actD = new ActivityRep2("D", "JoinStrings", Vector[String](), Vector[String](), "") {
        override def process(input: Serializable, params: Vector[String]) = {
          val (str1, str2) = input.asInstanceOf[(String, String)]
          str1 + str2
        }
      }

      val graph = new GraphRep("Test 2")
      graph.addSingleActivity(actA)
      graph.addSingleActivity(actB)
      graph.addSingleActivity(actC)
      graph.addSingleActivity(actD)
      graph.addConnection(actA.id, List(actB.id, actC.id))
      graph.addConnection(actB.id, actD.id)
      graph.addConnection(actC.id, actD.id)

      new StreamCreator(graph)
    }

    {
      val stream = test2()

      val input = Stream("a,b,c", "de,f", ",gh,i")
      //Stream.continually(randomAlpha(3))
      val output = stream.run(input)
      println("Graph: " + stream.strConstruction)

      println(input.take(10).toList.zip(output.take(10).force).map { case (a, b) => a + "->" + b }.mkString(" | "))
    }
    println()

    {
      val stream = test1()

      val input = Stream(Vector(4, 2, 3, 5), Vector(-3, 4, 5, 6), Vector())
      //Stream.continually(randomAlpha(3))
      val output = stream.run(input)
      println("Graph: " + stream.strConstruction)

      println(input.take(10).toList.zip(output.take(10).force).map { case (a, b) => a + "->" + b }.mkString(" | "))
    }
  }
}

trait ActivityTrait {

  /**
    * This is where you do anything you want to do to transform an object (i.e. change its state and return it)
    * or to create new object and send it on its way.
    *
    * The atk is there just in case you want to do groovey things like supply paramters to your processor,
    * or send objects out of band on a seperate channel.
    *
    * @param  input  The Serializable input object
    * @param  params The list of imports
    * @return output The Serializbale output object
    */
  def process(input: Serializable, params: Vector[String]): Serializable

}