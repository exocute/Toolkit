package toolkit

import org.parboiled2._

/**
  * This parser receives a .pln file containing a graph structure and parse it.
  *
  * @param input ParserInput
  */
class ActivityParser(val input: ParserInput) extends Parser {

  //  /**
  //    * GraphClass
  //    * Representation of a graph:
  //    *   imp: imports
  //    *   exp: exports
  //    *   acts: activities performed by the graph
  //    *   cnt: connections and dependencies between the different activities
  //    * @param name
  //    */
  //  class GraphClass(name: String) {
  //    var imp: String = _
  //    var exp: String = _
  //    var acts: List[ActivityClass] = Nil
  //    var cnt: List[(String, List[String])] = Nil
  //    override def toString: String = s"$name:\n Import: $imp,\n Export: $exp,\n Activities: $acts, $cnt"
  //  }
  //
  //  class ActivityClass(id: String, name: String) {
  //    var imp: List[String] = Nil
  //    var exp: String = _
  //    var params: List[String] = Nil
  //    override def toString: String = s"$id, $name: $imp, $exp, $params"
  //  }

  def InputLine = rule {
    ANY_WS ~ GraphRule ~ ANY_WS ~ EOI
  }

  var graph: GraphRep = _

  /**
    * instantiates a GraphClass object
    *
    * @return
    */
  def GraphRule: Rule0 = rule {
    ignoreCase("graph") ~ WS1 ~ Id ~ NLS ~>
      ((graphName: String) => {
        graph = new GraphRep(graphName)
        CreateGraph(graph)
      })
  }

  /**
    * is responsible for instantiating all the fields of the GraphClass
    *
    * @param graph The GraphClass object to be instantiated
    * @return
    */
  def CreateGraph(graph: GraphRep): Rule0 = rule {
    zeroOrMore(ignoreCase("import") ~ WS1 ~ Type ~ NLS ~> ((importName: String) => graph.importName = importName)
      | ignoreCase("export") ~ WS1 ~ Type ~ NLS ~> ((exportName: String) => graph.exportName = exportName)) ~
      ReadActivity(graph) ~ zeroOrMore(ReadActivity(graph) | Connections(graph))
  }

  /**
    * collects the imports, exports and parameters for each Activity
    *
    * @param graph The GraphClass object to have his List[ActivityClass] instantiated
    * @return
    */
  def ReadActivity(graph: GraphRep): Rule0 = rule {
    ignoreCase("activity") ~ WS1 ~ Id ~ WS1 ~ Id ~> (
      (id: String, name: String) => {
        val act = ActivityRep(id, name)
        graph.addSingleActivity(act)
        ReadActivityParams(act) ~ NLS ~ ReadActivityIE(act)
      })
  }

  /**
    * collects the parameters of an Activity and saves it in the ActivityClass
    *
    * @param act The ActivityClass object to be instantiated
    * @return
    */
  def ReadActivityParams(act: ActivityRep) = rule {
    zeroOrMore(':' ~ ParamStr ~> ((param: String) => act.parameters = param :: act.parameters))
  }

  /**
    * collects the imports and exports of an Activity and saves it in the ActivityClass
    *
    * @param act The ActivityClass object to be instantiated
    * @return
    */
  def ReadActivityIE(act: ActivityRep) = rule {
    zeroOrMore(ignoreCase("import") ~ WS1 ~ Type ~ NLS ~> ((importName: String) => act.importName = importName :: act.importName)
      | ignoreCase("export") ~ WS1 ~ Type ~ NLS ~> ((exportName: String) => act.exportName = exportName))
  }

  /**
    * collects the connections and dependencies between the different Graph Activities
    *
    * @param graph The GraphClass object to be instantiated
    * @return
    */
  def Connections(graph: GraphRep): Rule0 = rule {
    ignoreCase("connection") ~ oneOrMore(WS0 ~ "[" ~ WS0 ~ Id ~ WS0 ~ ";"
      ~ WS0 ~ Id ~ zeroOrMore(WS0 ~ ',' ~ WS0 ~ Id) ~ WS0 ~ "]" ~> (
      (a, b, seq) => {
        graph.addConnection(a, b :: seq.asInstanceOf[Seq[String]].toList)
      })) ~ NLS
  }

  /**
    * reads one paramater of an Activity and creates a String. Parameters are separated by ':'.
    *
    * @return The string containing the parameter
    */
  def ParamStr: Rule1[String] = rule {
    capture(oneOrMore(noneOf(":\n"))) ~ WS0 ~> ((str: String) => str)
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
  def NL = rule {
    oneOrMore('\n')
  }

  /**
    * catches end of lines plus optional white spaces
    *
    * @return
    */
  def NLS = rule {
    WS0 ~ optional('\n') ~ ANY_WS
  }
}
