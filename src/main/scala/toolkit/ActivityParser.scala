package toolkit

import org.parboiled2.{Rule1, _}
import shapeless.{::, HNil}
import exonode.clifton.signals._

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
  def GraphRule: Rule1[GraphRep] = rule {
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
  def CreateGraph: Rule[GraphRep :: HNil, GraphRep :: HNil] = rule {
    GraphImportExport ~> ((graph: GraphRep) => {
      ReadActivity(graph) ~ zeroOrMore(ReadActivity(graph) | Connections(graph)) ~ push(graph)
    })
  }

  def GraphImportExport: Rule[GraphRep :: HNil, GraphRep :: HNil] = rule {
    zeroOrMore((ignoreCase("import") ~ WS1 ~ Type ~ NLS ~> ((graph: GraphRep, importName: String) => graph.setImport(importName)))
      | (ignoreCase("export") ~ WS1 ~ Type ~ NLS ~> ((graph: GraphRep, exportName: String) => graph.setExport(exportName))))
  }

  /**
    * collects the imports, exports and parameters for each Activity
    *
    * @return
    */
  def ReadActivity(graphRep: GraphRep): Rule0 = rule {
    //TODO: probably we can write this without duplication of code
    (ignoreCase("activity") ~ WS1 ~ Id ~ WS1 ~ Id ~> (
      (id: String, name: String) => {
        push(ActivityRep(id, name, ActivityMapType)) ~ ReadActivityParams ~ NLS ~ ReadActivityIE ~> {
          (act: ActivityRep) => graphRep.addActivity(act)
        }
      })
      |
      ignoreCase("activityfilter") ~ WS1 ~ Id ~ WS1 ~ Id ~> (
        (id: String, name: String) => {
          push(ActivityRep(id, name, ActivityFilterType)) ~ ReadActivityParams ~ NLS ~ ReadActivityIE ~> {
            (act: ActivityRep) => graphRep.addActivity(act)
          }
        })
      |
      ignoreCase("activityflatmap") ~ WS1 ~ Id ~ WS1 ~ Id ~> (
        (id: String, name: String) => {
          push(ActivityRep(id, name, ActivityFlatMapType)) ~ ReadActivityParams ~ NLS ~ ReadActivityIE ~> {
            (act: ActivityRep) => graphRep.addActivity(act)
          }
        }))
  }

  /**
    * collects the parameters of an Activity and saves it in the ActivityClass
    *
    * @return
    */
  def ReadActivityParams: Rule[ActivityRep :: HNil, ActivityRep :: HNil] = rule {
    zeroOrMore(':' ~ ParamStr ~> ((act: ActivityRep, parameter: String) => act.addParameter(parameter)))
  }

  /**
    * collects the imports and exports of an Activity and saves it in the ActivityClass
    *
    * @return
    */
  def ReadActivityIE: Rule[ActivityRep :: HNil, ActivityRep :: HNil] = rule {
    zeroOrMore(ignoreCase("import") ~ WS1 ~ Type ~ NLS ~> ((act: ActivityRep, importName: String) => act.addImport(importName))
      | ignoreCase("export") ~ WS1 ~ Type ~ NLS ~> ((act: ActivityRep, exportName: String) => act.setExport(exportName)))
  }

  /**
    * collects the connections and dependencies between the different Graph Activities
    *
    * @param graph The GraphClass object to be instantiated
    * @return
    */
  def Connections(graph: GraphRep): Rule0 = rule {
    ignoreCase("connection") ~ WS1 ~ Connection(graph) ~ zeroOrMore(WS0 ~ ":" ~ Connection(graph)) ~ NLS
  }

  /**
    * collect a single connection and dependencies between the different Graph Activities
    *
    * @param graph The GraphClass object to be instantiated
    * @return
    */
  def Connection(graph: GraphRep): Rule0 = rule {
    //    WS0 ~ Id ~ WS0 ~ "->" ~ WS0 ~ Id ~ zeroOrMore(WS0 ~ "," ~ WS0 ~ Id) ~> (
    //      (a: String, b: String, seq: Any) => {
    //        graph.addConnection(a, b :: seq.asInstanceOf[Seq[String]].toList)
    //      })

    WS0 ~ Id ~ WS0 ~ "->" ~ WS0 ~> ((a: String) => {
      (Id * (WS0 ~ ',' ~ WS0)) ~> (_ match {
        case seq: Seq[String] =>
          graph.addConnection(a, seq.toList)
      })
    })
  }

  /**
    * reads a alpha numeric word and also allows '_' and '.'
    *
    * @return The string containing the alpha numeric word
    */
  def Id: Rule1[String] = rule {
    capture(oneOrMore(CharPredicate.AlphaNum | '_' | '.')) ~> ((str: String) => str)
  }

  def Type: Rule1[String] = rule {
    capture(oneOrMore(CharPredicate.AlphaNum | '_' | '.' | '[' | ']')) ~> ((str: String) => str)
  }

  /**
    * reads one paramater of an Activity and creates a String. Parameters are separated by ':'.
    *
    * @return The string containing the parameter
    */
  def ParamStr: Rule1[String] = rule {
    capture(oneOrMore(noneOf(":\n"))) ~ WS0
  }

  /**
    * catches any type of white spaces
    *
    * @return
    */
  def ANY_WS: Rule0 = rule {
    zeroOrMore(anyOf(" \t\n"))
  }

  /**
    * catches one or more white spaces
    *
    * @return
    */
  def WS1: Rule0 = rule {
    quiet(oneOrMore(anyOf(" \t")))
  }

  /**
    * catches zero or more white spaces
    *
    * @return
    */
  def WS0: Rule0 = rule {
    quiet(zeroOrMore(anyOf(" \t")))
  }

  /**
    * catches one or more end of lines
    *
    * @return
    */
  def NL: Rule0 = rule {
    oneOrMore('\n')
  }

  /**
    * catches end of lines plus optional white spaces
    *
    * @return
    */
  def NLS: Rule0 = rule {
    WS0 ~ optional('\n') ~ ANY_WS
  }

}
