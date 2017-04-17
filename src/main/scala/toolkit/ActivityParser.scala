package toolkit

import exonode.clifton.signals.{ActivityType, _}
import org.parboiled2.{Rule1, _}
import shapeless.{::, HNil}

/**
  * Created by #GrowinScala
  *
  * This parser receives a .pln file containing a graph structure and parse it.
  *
  * @param input ParserInput
  */
class ActivityParser(val input: ParserInput) extends Parser {

  def InputLine: Rule1[GraphRep] = rule {
    ANY_WS ~ GraphRule ~ ANY_WS ~ EOI
  }

  /**
    * instantiates a GraphClass object
    *
    * @return
    */
  private def GraphRule: Rule1[GraphRep] = rule {
    ignoreCase("graph") ~ WS1 ~ Id ~ NLS ~>
      ((graphName: String) => {
        push(new GraphRep(graphName)) ~ CreateGraph
      })
  }

  /**
    * is responsible for instantiating all the fields of the GraphClass
    *
    * @return
    */
  private def CreateGraph: Rule[GraphRep :: HNil, GraphRep :: HNil] = rule {
    GraphImportExport ~ ReadActivity ~ zeroOrMore(ReadActivity | Connections)
  }

  private def GraphImportExport: Rule[GraphRep :: HNil, GraphRep :: HNil] = rule {
    zeroOrMore((ignoreCase("import") ~ WS1 ~ Type ~ NLS ~> ((graph: GraphRep, importName: String) => graph.setImport(importName)))
      | (ignoreCase("export") ~ WS1 ~ Type ~ NLS ~> ((graph: GraphRep, exportName: String) => graph.setExport(exportName))))
  }

  /**
    * collects the imports, exports and parameters for each Activity
    *
    * @return
    */
  private def ReadActivity: Rule[GraphRep :: HNil, GraphRep :: HNil] = rule {
    ActType ~ WS1 ~ Id ~ WS1 ~ Id ~> (
      (graphRep: GraphRep, actType: ActivityType, id: String, name: String) => {
        push(ActivityRep(id, name, actType)) ~ ReadActivityParams ~ NLS ~ ReadActivityIE ~> {
          (act: ActivityRep) => graphRep.addActivity(act)
        }
      })
  }

  /**
    * collects the parameters of an Activity and saves it in the ActivityClass
    *
    * @return
    */
  private def ReadActivityParams: Rule[ActivityRep :: HNil, ActivityRep :: HNil] = rule {
    zeroOrMore(':' ~ ParamStr ~> ((act: ActivityRep, parameter: String) => act.addParameter(parameter)))
  }

  /**
    * collects the imports and exports of an Activity and saves it in the ActivityClass
    *
    * @return
    */
  private def ReadActivityIE: Rule[ActivityRep :: HNil, ActivityRep :: HNil] = rule {
    zeroOrMore(ignoreCase("import") ~ WS1 ~ (Type + (WS0 ~ ',' ~ WS0)) ~ NLS ~>
      ((initialAct: ActivityRep, importNames: Seq[String]) =>
        importNames.foldLeft(initialAct)((act, importName) => act.addImport(importName)))
      | ignoreCase("export") ~ WS1 ~ Type ~ NLS ~>
      ((act: ActivityRep, exportName: String) =>
        act.setExport(exportName))
    )
  }

  /**
    * collects the connections and dependencies between the different Graph Activities
    *
    * @return
    */
  private def Connections: Rule[GraphRep :: HNil, GraphRep :: HNil] = rule {
    ignoreCase("connection") ~ WS1 ~ Connection ~ zeroOrMore(WS0 ~ ":" ~ Connection) ~ NLS
  }

  /**
    * collect a single connection and dependencies between the different Graph Activities
    *
    * @return
    */
  private def Connection: Rule[GraphRep :: HNil, GraphRep :: HNil] = rule {
    WS0 ~ ((Id + (WS0 ~ ',' ~ WS0)) + (WS0 ~ "->" ~ WS0)) ~> ((initGraphRep: GraphRep, connects: Seq[Seq[String]]) => {
      connects.zip(connects.tail).foldLeft(initGraphRep) {
        case (graphRep, (conns1, conns2)) =>
          conns1.foldLeft(graphRep)((graphRep, conn1) =>
            conns2.foldLeft(graphRep)((graphRep, conn2) =>
              graphRep.addConnection(conn1, conn2)))
      }
    })
  }

  /**
    * reads a alpha numeric word and also allows '_' and '.'
    *
    * @return The string containing the alpha numeric word
    */
  private def Id: Rule1[String] = rule {
    capture(oneOrMore(CharPredicate.AlphaNum | '_' | '.')) ~> ((str: String) => str)
  }

  private def Type: Rule1[String] = rule {
    capture(oneOrMore(CharPredicate.AlphaNum | '_' | '.' | '[' | ']' | '#')) ~> ((str: String) => str)
  }

  private val mapStrToTypes: Map[String, ActivityType] =
    Map("activity" -> ActivityMapType,
      "activityfilter" -> ActivityFilterType,
      "activityflatmap" -> ActivityFlatMapType)

  private def ActType: Rule1[ActivityType] = rule {
    capture(ignoreCase("activityfilter") | ignoreCase("activityflatmap") | ignoreCase("activity")) ~> {
      (actType: String) =>
        mapStrToTypes(actType.toLowerCase)
    }
  }

  /**
    * reads one parameter of an Activity and creates a String. Parameters are separated by ':'.
    *
    * @return The string containing the parameter
    */
  private def ParamStr: Rule1[String] = rule {
    capture(oneOrMore(noneOf(":\n"))) ~ WS0
  }

  /**
    * catches any type of white spaces
    *
    * @return
    */
  private def ANY_WS: Rule0 = rule {
    zeroOrMore(anyOf(" \t\n"))
  }

  /**
    * catches one or more white spaces
    *
    * @return
    */
  private def WS1: Rule0 = rule {
    quiet(oneOrMore(anyOf(" \t")))
  }

  /**
    * catches zero or more white spaces
    *
    * @return
    */
  private def WS0: Rule0 = rule {
    quiet(zeroOrMore(anyOf(" \t")))
  }

  /**
    * catches end of lines plus optional white spaces
    *
    * @return
    */
  private def NLS: Rule0 = rule {
    WS0 ~ optional('\n') ~ ANY_WS
  }

}
