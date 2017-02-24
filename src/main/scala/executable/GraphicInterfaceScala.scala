package executable

import org.jfree.chart.ChartFactory
import org.jfree.chart.ChartPanel
import org.jfree.chart.plot.PiePlot
import org.jfree.chart.plot.PlotOrientation
import org.jfree.data.category.DefaultCategoryDataset
import org.jfree.data.general.DefaultPieDataset
import org.jfree.data.time.Millisecond
import org.jfree.data.time.TimeSeries
import org.jfree.data.time.TimeSeriesCollection
import org.jfree.util.Rotation
import javax.swing._
import javax.swing.table.DefaultTableModel
import javax.swing.text.DefaultCaret
import java.awt._

import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.JFrame

/**
  * Created by #ScalaTeam on 21/02/2017.
  */
object GraphicInterfaceScala {


  private val processingData = new TimeSeries("data", classOf[Millisecond])
  private val priorityEvents = new DefaultCategoryDataset
  private val processedEvents = new DefaultCategoryDataset
  private val topActivities = new DefaultPieDataset
  private val logInfo = new JTextArea(10, 50)
  private val text = new JScrollPane(logInfo)
  private val activityRank = new DefaultTableModel
  private val errorEvents = new DefaultTableModel
  private val actRank = new JTable()
  private val errorData = new JTable()

  //Main Panel of the application
  val jp = new JPanel
  val jf = new JFrame
  jp.setLayout(new BorderLayout)
  jp.setVisible(true)
  val b = new JButton("Full Screen")
  val n = new JButton("Launch Nodes")
  val k = new JButton("Kill Node")
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
  jp.add(p4)
  p5.setLayout(new BorderLayout)
  p5.add(text, BorderLayout.CENTER)
  jp.setLayout(new BoxLayout(jp, BoxLayout.Y_AXIS))

  logInfo.setEditable(false)
  p6.add(b)
  p6.add(n)
  p6.add(k)
  p6.setLayout(new BoxLayout(p6, BoxLayout.X_AXIS))

  b.addActionListener(new ActionListener() {
    def actionPerformed(e: ActionEvent) {
      jf.setExtendedState(Frame.MAXIMIZED_BOTH)
      jf.setUndecorated(true)
      jf.setVisible(true)
    }
  })

  n.addActionListener(new ActionListener() {
    def actionPerformed(e: ActionEvent) {
      val input = JOptionPane.showInputDialog("Number of nodes you want to launch")
      val nNodes = input.toInt
      LaunchNodes.startNodes(nNodes)
    }
  })

  k.addActionListener(new ActionListener() {
    def actionPerformed(e: ActionEvent) {
      val node = JOptionPane.showInputDialog("ID of the node you want to kill")
      LaunchNodes.killNode(node)
    }
  })
  jp.add(p5)
  jp.add(p6)
  val caret: DefaultCaret = logInfo.getCaret.asInstanceOf[DefaultCaret]
  caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE)
  jf.add(jp, BorderLayout.CENTER)
  jf.setSize(new Dimension(1600, 800))
  jf.setVisible(true)
  jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)


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
