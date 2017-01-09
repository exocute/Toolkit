//import clifton.graph.CliftonGraph
//import java.io.File
//import java.util.UUID
//import java.util.concurrent.atomic.AtomicInteger
//
//import clifton.nodes.{ActivityCache, CliftonClassLoader, ExoEntry, SpaceCache}
//import clifton.signals.ActivitySignal
//import com.zink.fly.FlyPrime
//
///**
//  * Created by #ScalaTeam on 23/12/2016.
//  */
//object EXOGRAPH_API {
//
//  val TIME: Int = 10 * 60 * 1000
//  val TIMETAKE: Int = 10
//
//  var signal: FlyPrime = _
//  var data: FlyPrime = _
//  var jarS: FlyPrime = _
//
//  var cg: CliftonGraph = _
//  var results: Stream[Any] = Stream.empty
//
//  def main(args: Array[String]): Unit = {
//    println("***********************************")
//    println("*   WELCOME TO EXOGRAPH_DEMO_V1   *")
//    println("***********************************")
//
//    println("Do you want to use Initial Parameters to servers?")
//    val yesOrNot = scala.io.StdIn.readLine()
//    if (!yesOrNot.isEmpty) {
//
//      print("\nINTRODUCE JARHOST\n>")
//      val jarHost = scala.io.StdIn.readLine()
//      if (!jarHost.isEmpty) print("Ok!")
//      print("\nINTRODUCE DATAHOST\n>")
//      val dataHost = scala.io.StdIn.readLine()
//      if (!dataHost.isEmpty) print("Ok!")
//      print("\nINTRODUCE SINGALHOST\n>")
//      val signalHost = scala.io.StdIn.readLine()
//      if (!signalHost.isEmpty) print("Ok!")
//      print("\nINTRODUCE GRP NAME\n>")
//      val grpName = scala.io.StdIn.readLine()
//      if (!grpName.isEmpty) println("Ok!")
//      print("\nINTRODUCE JAR NAME\n>")
//      val jarName = scala.io.StdIn.readLine()
//      if (!jarName.isEmpty) println("Ok!")
//      cg = new CliftonGraph(new File(grpName), signalHost, dataHost, jarHost)
//
//    } else {
//      val dataHost = "localhost"
//      val signalHost = "localhost"
//      val jarHost = "192.168.1.126"
//      val grpName = "examples\\abc.grp"
//      println("Connected to " + jarHost + " " + dataHost + " " + signalHost)
//      cg = new CliftonGraph(new File(grpName), signalHost, dataHost, jarHost)
//    }
//
//
//    print("Loading GRP FILE...")
//    println(" Ok!")
//
//
//
//    signal = SpaceCache.getSignalSpace
//    data = SpaceCache.getDataSpace
//    jarS = SpaceCache.getJarSpace
//
//    while (true) {
//      print("\n\nINTRODUCE i to Inject and c to Collect\n>")
//      val line = scala.io.StdIn.readLine()
//      val input = line.drop(2)
//      if (!input.isEmpty) {
//        line.charAt(0) match {
//          case 'i' => inject(input)
//          case 'c' => collect(input.toInt)
//          case _ => println("Introduce a valid input")
//        }
//      } else println("Invalid Input. i <input> or c <number to collect>")
//    }
//  }
//
//  def hasNextActivity(dataEntryTemplate: dataEntry): Boolean = {
//    val entry = new ActivitySignal(dataEntryTemplate.toExecute, null, null, null)
//    val res = signal.read(entry, TIMETAKE)
//    res.outMarkers.head.last != '<'
//  }
//
//  def inject(input: String) = {
//    results = results.:+ {
//      var finalRes: Any = null
//      val id = UUID.randomUUID().toString
//
//      val tmpl = new ActivitySignal(null, null, Vector(cg.getInject.getMarker), null)
//
//      val ex1 = signal.read(tmpl, TIMETAKE)
//
//      val xa = ActivityCache.getActivity(ex1.name).get
//
//      val res = xa.process(input, ex1.params)
//
//      val d1 = new dataEntry(id, ex1.outMarkers.head, res)
//
//      data.write(d1, TIME)
//
//      def restActivities: java.io.Serializable = {
//        val dataEntry = data.take(new dataEntry(id, null, null), TIMETAKE)
//        val nextActName = dataEntry.toExecute
//        val nextAct = ActivityCache.getActivity(nextActName).get
//        val actSign = signal.read(new ActivitySignal(dataEntry.toExecute, null, null, null), TIMETAKE)
//        val nextRes = nextAct.process(dataEntry.result, actSign.params)
//        if (hasNextActivity(dataEntry)) {
//          data.write(new dataEntry(id, actSign.outMarkers.head, nextRes), TIME)
//          restActivities
//        } else nextRes
//      }
//      restActivities
//    }
//  }
//
//  def collect(i: Int) = {
//    val res = results.take(i).force
//    if (!res.isEmpty) {
//      println(res)
//      results = results.drop(i)
//    } else println("Nothing to Collect")
//  }
//
//}
//
//class dataEntry(val id: String, var toExecute: String, var result: java.io.Serializable) {
//  override def toString: String = id + " " + toExecute + " " + result
//}