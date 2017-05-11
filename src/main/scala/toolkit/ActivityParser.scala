package toolkit

import exonode.clifton.signals.{ActivityType, _}
import org.parboiled2.{Rule1, _}
import shapeless.{::, HNil}
import toolkit.exceptions.InvalidType

/**
  * Created by #GrowinScala
  *
  * This parser receives a .pln file content containing a graph structure and parse it.
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
    ignoreCase("graph") ~ WS1 ~ Id ~ NL ~>
      ((graphName: String) => {
        push(new GraphRep(graphName)) ~ CreateGraph
      })
  }

  /**
    * Collects the GraphRep
    *
    * @return
    */
  private def CreateGraph: Rule[GraphRep :: HNil, GraphRep :: HNil] = rule {
    GraphImportExport ~ ReadActivity ~ zeroOrMore(ReadActivity | ReadConnection)
  }

  /**
    * Collects the import and export of the graph
    *
    * @return
    */
  private def GraphImportExport: Rule[GraphRep :: HNil, GraphRep :: HNil] = rule {
    zeroOrMore((ignoreCase("import") ~ WS1 ~ Type ~ NL ~>
      ((graph: GraphRep, importName: String) => graph.setImport(importName)))
      | (ignoreCase("export") ~ WS1 ~ Type ~ NL ~>
      ((graph: GraphRep, exportName: String) => graph.setExport(exportName))))
  }

  /**
    * collects the imports, exports and parameters for each Activity
    *
    * @return
    */
  private def ReadActivity: Rule[GraphRep :: HNil, GraphRep :: HNil] = rule {
    ANY_WS ~ ActType ~ WS1 ~ Id ~ WS1 ~ PathId ~> (
      (graphRep: GraphRep, actType: ActivityType, id: String, name: String) => {
        push(ActivityRep(id, name, actType)) ~ ReadActivityParams ~ NL ~ ReadActivityIE ~> {
          (act: ActivityRep) => graphRep.addActivity(act)
        }
      })
  }

  /**
    * collects the parameters of an Activity in the ActivityRep
    *
    * @return
    */
  private def ReadActivityParams: Rule[ActivityRep :: HNil, ActivityRep :: HNil] = rule {
    zeroOrMore(':' ~ ParamStr ~> ((act: ActivityRep, parameter: String) => act.addParameter(parameter)))
  }

  /**
    * collects the imports and exports of an ActivityRep
    *
    * @return
    */
  private def ReadActivityIE: Rule[ActivityRep :: HNil, ActivityRep :: HNil] = rule {
    zeroOrMore(ignoreCase("import") ~ WS1 ~ (Type + (WS0 ~ ',' ~ WS0)) ~ NL ~>
      ((initialAct: ActivityRep, importNames: Seq[String]) =>
        importNames.foldLeft(initialAct)((act, importName) => act.addImport(importName)))
      | ignoreCase("export") ~ WS1 ~ Type ~ NL ~>
      ((act: ActivityRep, exportName: String) => {
        if (act.actType == ActivityFilterType && exportName != "Boolean")
          throw new InvalidType(exportName, "Boolean")
        else
          act.setExport(exportName)
      })
    )
  }

  /**
    * collects the connections and dependencies between the activities
    *
    * @return
    */
  private def ReadConnection: Rule[GraphRep :: HNil, GraphRep :: HNil] = {
    def ReadConnectionAux: Rule[GraphRep :: HNil, GraphRep :: HNil] = rule {
      ConnectionLinkMain ~> ((initGraphRep: GraphRep, connections: Connection) => {
        connections.foldLeft(initGraphRep) {
          case (graphRep1, conns) =>
            conns.zip(conns.tail).foldLeft(graphRep1) {
              case (graphRep2, (conn1, conn2)) =>
                graphRep2.addConnection(conn1, conn2)
            }
        }
      })
    }

    rule {
      ignoreCase("connection") ~ WS1 ~ (ReadConnectionAux + (WS0 ~ ':' ~ WS0 | NLS)) ~ ANY_WS
    }
  }

  private type Connection = List[List[String]]

  private def ReadMultipleConnection: Rule1[Connection] = rule {
    push(List(List[String]())) ~
      (ConnectionLinkAux + (WS0 ~ ',' ~ WS0)) ~> ((connection: Connection, newConnections: Seq[Connection]) =>
      newConnections.toList.flatMap(connNew => connNew.flatMap(conn => connection.map(_ ++ conn))))
  }

  private def ConnectionLinkMain: Rule1[Connection] = rule {
    (ConnectionBase ~ WS0 ~ "->" ~ WS0 ~ (ConnectionBase + (WS0 ~ "->" ~ WS0))) ~> (
      (newConnectionFirst: Connection, newConnectionOthers: Seq[Connection]) =>
        newConnectionOthers.fold(newConnectionFirst)(
          (list, connA) => list.flatMap(connB => connA.map(connB ++ _)))
      )
  }

  private def ConnectionLinkAux: Rule1[Connection] = rule {
    (ConnectionBase + (WS0 ~ "->" ~ WS0)) ~> (
      (newConnections: Seq[Connection]) =>
        newConnections.fold(List(List[String]()))(
          (list, connA) => list.flatMap(connB => connA.map(connB ++ _)))
      )
  }

  private def ConnectionBase: Rule1[Connection] = rule {
    Id ~> ((id: String) => List(List(id))) |
      ('(' ~ ReadMultipleConnection ~ ')')
  }

  /**
    * @return An identifier that starts with a letter and can contain digits and underscores
    */
  private def Id: Rule1[String] = rule {
    capture(CharPredicate.Alpha ~ zeroOrMore(CharPredicate.AlphaNum | '_')) ~> ((str: String) => str)
  }

  /**
    * @return An identifier that can be prefixed by a path (that is separated by '.' characters)
    */
  private def PathId: Rule1[String] = rule {
    capture(CharPredicate.Alpha ~ zeroOrMore(CharPredicate.AlphaNum | '_' |
      ('.' ~ CharPredicate.Alpha))) ~> ((str: String) => str)
  }

  private def Types: Rule1[String] = rule {
    WS0 ~ (Type + (WS0 ~ ',' ~ WS0)) ~> ((typesStr: Seq[String]) => typesStr.mkString(","))
  }

  private def Type: Rule1[String] = rule {
    ((PathId ~ TypeArgs) ~> ((pathId: String, typeArgs: String) => pathId + typeArgs)) | PathId
  }

  private def TypeArgs: Rule1[String] = rule {
    '[' ~ Types ~ ']' ~> ((typesStr: String) => "[" + typesStr + "]")
  }

  private val mapStrToTypes: Map[String, ActivityType] =
    Map("activity" -> ActivityMapType,
      "activityfilter" -> ActivityFilterType,
      "activityflatmap" -> ActivityFlatMapType)

  private def ActType: Rule1[ActivityType] = rule {
    capture(ignoreCase("activity") | ignoreCase("activityfilter") | ignoreCase("activityflatmap")) ~> {
      (actType: String) =>
        mapStrToTypes(actType.toLowerCase)
    }
  }

  /**
    * reads one parameter of an Activity and returns a String.
    *
    * @return The string containing the parameter
    */
  private def ParamStr: Rule1[String] = rule {
    capture(zeroOrMore(noneOf(":\n")))
  }

  /**
    * catches any type of white spaces
    *
    * @return
    */
  private def ANY_WS: Rule0 = rule {
    quiet(zeroOrMore(anyOf(" \t\n")))
  }

  /**
    * catches one or more white space characters
    *
    * @return
    */
  private def WS1: Rule0 = rule {
    quiet(oneOrMore(anyOf(" \t")))
  }

  /**
    * catches zero or more white space characters
    *
    * @return
    */
  private def WS0: Rule0 = rule {
    quiet(zeroOrMore(anyOf(" \t")))
  }

  /**
    * catches at least one end of line plus optional white spaces
    *
    * @return
    */
  private def NLS: Rule0 = rule {
    WS0 ~ '\n' ~ ANY_WS
  }

  /**
    * catches at least one end of line (or end of input) plus optional white spaces
    *
    * @return
    */
  private def NL: Rule0 = rule {
    WS0 ~ ('\n' | &(EOI)) ~ ANY_WS
  }

}
