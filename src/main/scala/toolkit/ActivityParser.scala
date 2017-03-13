package toolkit

import exonode.clifton.signals.{ActivityType, _}
import org.parboiled2.{Rule1, _}
import shapeless.{::, HNil}

/**
  * Created by #ScalaTeam on 12/12/2016.
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
    GraphImportExport ~> ((graph: GraphRep) => {
      ReadActivity(graph) ~ zeroOrMore(ReadActivity(graph) | Connections(graph)) ~ push(graph)
    })
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
  private def ReadActivity(graphRep: GraphRep): Rule0 = rule {
    ActType ~ WS1 ~ Id ~ WS1 ~ Id ~> (
      (actType: ActivityType, id: String, name: String) => {
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
    zeroOrMore(ignoreCase("import") ~ WS1 ~ Type ~ NLS ~> ((act: ActivityRep, importName: String) => act.addImport(importName))
      | ignoreCase("export") ~ WS1 ~ Type ~ NLS ~> ((act: ActivityRep, exportName: String) => act.setExport(exportName)))
  }

  /**
    * collects the connections and dependencies between the different Graph Activities
    *
    * @param graph The GraphClass object to be instantiated
    * @return
    */
  private def Connections(graph: GraphRep): Rule0 = rule {
    ignoreCase("connection") ~ WS1 ~ Connection(graph) ~ zeroOrMore(WS0 ~ ":" ~ Connection(graph)) ~ NLS
  }

  /**
    * collect a single connection and dependencies between the different Graph Activities
    *
    * @param graph The GraphClass object to be instantiated
    * @return
    */
  private def Connection(graph: GraphRep): Rule0 = rule {
    WS0 ~ ((Id + (WS0 ~ ',' ~ WS0)) + (WS0 ~ "->" ~ WS0)) ~> ((connects: Seq[Seq[String]]) => {
      for {
        (conns1, conns2) <- connects.zip(connects.tail)
        conn1 <- conns1
        conn2 <- conns2
      } {
        graph.addConnection(conn1, conn2)
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
    capture(ignoreCase("activity") | ignoreCase("activityfilter") | ignoreCase("activityflatmap")) ~> {
      (actType: String) => mapStrToTypes(actType.toLowerCase)
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
    * catches one or more end of lines
    *
    * @return
    */
  private def NL: Rule0 = rule {
    oneOrMore('\n')
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
