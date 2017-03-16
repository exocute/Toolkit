package executable

import java.awt._
import java.awt.event.{ActionEvent, ActionListener}
import javax.swing.table.DefaultTableModel
import javax.swing.text.DefaultCaret
import javax.swing.{JFrame, _}

import org.jfree.chart.plot.{PiePlot, PlotOrientation}
import org.jfree.chart.{ChartFactory, ChartPanel}
import org.jfree.data.category.DefaultCategoryDataset
import org.jfree.data.general.DefaultPieDataset
import org.jfree.data.time.{Millisecond, TimeSeries, TimeSeriesCollection}
import org.jfree.util.Rotation

import scala.collection.mutable

/**
  * Created by #GrowinScala
  */
object GraphicInterfaceScala {


  private val processingData = new TimeSeries("data", classOf[Millisecond])
  private val priorityEvents = new DefaultCategoryDataset
  private val processedEvents = new DefaultCategoryDataset
  private val topActivities = new DefaultPieDataset
  private val nodeIDs = mutable.Set[String]()
  private val logInfo = new JTextArea(10, 50)
  private val text = new JScrollPane(logInfo)
  private val activityRank = new DefaultTableModel
  private val errorEvents = new DefaultTableModel
  private val actRank = new JTable()
  private val errorData = new JTable()


  //Main Panel of the application
  val mainPanel = new JPanel
  val mainFrame = new JFrame
  mainPanel.setLayout(new BorderLayout)
  mainPanel.setVisible(true)
  val fullScreenButton = new JButton("Full Screen")
  val launchNodesButton = new JButton("Launch Nodes")
  val killNodesButton = new JButton("Kill Node")
  val p1 = new JPanel
  val p2 = new JPanel
  val p3 = new JPanel
  val p4 = new JPanel
  val p5 = new JPanel
  val p6 = new JPanel

  p1.add(createProcessingChart)
  p1.add(createBarChart("Significant Events", "Parameters", "Quantity", priorityEvents), BorderLayout.CENTER)
  p2.add(createPieChart(topActivities, "Top Activities Running"), BorderLayout.CENTER)
  p2.add(createBarChart("Processed/Injected Inputs", "Parameters", "Quantity", processedEvents), BorderLayout.CENTER)
  p3.add(createTable(activityRank, actRank), BorderLayout.CENTER)
  p3.add(createTable(errorEvents, errorData), BorderLayout.CENTER)

  p1.setLayout(new BoxLayout(p1, BoxLayout.Y_AXIS))
  p2.setLayout(new BoxLayout(p2, BoxLayout.Y_AXIS))
  p3.setLayout(new BoxLayout(p3, BoxLayout.Y_AXIS))
  p4.setLayout(new BoxLayout(p4, BoxLayout.X_AXIS))

  p4.add(p1, BorderLayout.NORTH)
  p4.add(p2, BorderLayout.NORTH)
  p4.add(p3, BorderLayout.NORTH)

  mainPanel.add(p4)

  p5.setLayout(new BorderLayout)
  p5.add(text, BorderLayout.CENTER)
  mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS))

  logInfo.setEditable(false)
  p6.add(fullScreenButton)
  p6.add(launchNodesButton)
  p6.add(killNodesButton)
  p6.setLayout(new BoxLayout(p6, BoxLayout.X_AXIS))

  fullScreenButton.addActionListener(new ActionListener() {
    def actionPerformed(e: ActionEvent) {
      mainFrame.setExtendedState(Frame.MAXIMIZED_BOTH)
      mainFrame.setUndecorated(true)
      mainFrame.setVisible(true)
    }
  })

  launchNodesButton.addActionListener(new ActionListener() {
    def actionPerformed(e: ActionEvent) {
      val input = JOptionPane.showInputDialog("Number of nodes you want to launch")
      if (Option(input).isDefined) {
        val nNodes = input.toInt
        LaunchNodes.startNodes(nNodes)
      }
    }
  })

  killNodesButton.addActionListener(new ActionListener() {
    def actionPerformed(e: ActionEvent) {
      val nodeIDS = nodeIDs.toArray
      if (nodeIDS.nonEmpty) {
        val node = ListDialog.showDialog(mainFrame, killNodesButton, "Choose the node ID you want to kill:", "Kill Nodes", nodeIDS, null, null)
        LaunchNodes.killNode(node)
      }
    }
  })
  mainPanel.add(p5)
  mainPanel.add(p6)

  //leave the caret always in the bottom of the text box
  val caret: DefaultCaret = logInfo.getCaret.asInstanceOf[DefaultCaret]
  caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE)

  mainFrame.add(mainPanel, BorderLayout.CENTER)
  mainFrame.setSize(new Dimension(1600, 800))
  mainFrame.setVisible(true)
  mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)


  private def createProcessingChart = {
    val dataset = new TimeSeriesCollection(processingData)
    val chart = ChartFactory.createTimeSeriesChart("Processing Inputs", "Time", "Value", dataset, true, true, false)
    val plot = chart.getXYPlot
    plot.setNoDataMessage("No data to display")
    val axis = plot.getDomainAxis
    axis.setAutoRange(true)
    axis.setFixedAutoRange(60000.0)
    val label = new ChartPanel(chart)
    label
  }

  private def createPieChart(dataset: DefaultPieDataset, title: String) = {
    val chart = ChartFactory.createPieChart(title, // chart title
      dataset, // data
      true, // include legend
      true, false)
    val plot = chart.getPlot.asInstanceOf[PiePlot]
    plot.setStartAngle(290)
    plot.setDirection(Rotation.CLOCKWISE)
    plot.setForegroundAlpha(0.5f)
    plot.setNoDataMessage("No data to display")
    val chartPanel = new ChartPanel(chart)
    //chartPanel.setPreferredSize(new java.awt.Dimension(550, 400));
    chartPanel
  }


  def createBarChart(title: String, x: String, y: String, data: DefaultCategoryDataset): ChartPanel = {
    val barChart = ChartFactory.createBarChart(title, x, y, data, PlotOrientation.VERTICAL, true, true, false)
    val chartPanel = new ChartPanel(barChart)
    //chartPanel.setPreferredSize(new java.awt.Dimension(550, 400));
    chartPanel
  }

  def addNode(nodeID: String) = {
    nodeIDs.add(nodeID)
  }

  def removeNode(nodeID: String) = {
    nodeIDs.remove(nodeID)
  }

  private def createTable(dt: DefaultTableModel, t: JTable) = {
    val tableContainer = new JScrollPane(t)
    t.setModel(dt)
    tableContainer
  }

  def addLogLine(line: String) {
    logInfo.append(line + "\n")
  }

  def addProcessingElement(value: Double) {
    processingData.addOrUpdate(new Millisecond, value)
  }

  def setSignificantEvents(parameter: String, value: Double) {
    priorityEvents.setValue(value, parameter, "Quantity")
  }

  def addSignificantEvents(parameter: String, value: Double) {
    priorityEvents.addValue(value, parameter, "Quantity")
  }

  def setProcessedEvents(parameter: String, value: Double) {
    processedEvents.setValue(value, parameter, "Quantity")
  }

  def addProcessedEvents(parameter: String, value: Double) {
    processedEvents.addValue(value, parameter, "Quantity")
  }

  def removeTopActivity(parameter: String) {
    topActivities.remove(parameter)
  }

  def updateTopActivity(parameter: String, value: Int) {
    topActivities.setValue(parameter, value)
  }

  def addErrorEvents(date: String, parameter: String) {
    errorEvents.insertRow(0, Array[AnyRef](date, parameter))
  }

  def setRowActivity(parameter: String, row: Int, column: Int) {
    errorEvents.setValueAt(parameter, row, column)
  }

  def addActivityRank(id: String, count: String, avgTime: String) {
    activityRank.addRow(Array[AnyRef](id, count, avgTime))
  }

  def setActivityRank(parameter: String, row: Int, column: Int) {
    activityRank.setValueAt(parameter, row, column)
  }

  def activityRankHeader(header: Array[AnyRef]) {
    activityRank.setColumnIdentifiers(header)
    actRank.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN)
    actRank.getColumnModel.getColumn(0).setMinWidth(230)
    actRank.getColumnModel.getColumn(1).setMaxWidth(60)
    actRank.getColumnModel.getColumn(2).setMinWidth(60)
    actRank.getColumnModel.getColumn(2).setMinWidth(60)
  }

  def errorHeader(header: Array[AnyRef]) {
    errorEvents.setColumnIdentifiers(header)
    errorData.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN)
    errorData.getColumnModel.getColumn(0).setMaxWidth(125)
    errorData.getColumnModel.getColumn(0).setMinWidth(125)

  }
}
