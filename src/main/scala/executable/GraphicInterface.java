package executable;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.util.Rotation;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
  * Created by #GrowinScala
  */
public class GraphicInterface extends JFrame {

    private static TimeSeries processingData = new TimeSeries("data");
    private static DefaultCategoryDataset priorityEvents = new DefaultCategoryDataset();
    private static DefaultCategoryDataset processedEvents = new DefaultCategoryDataset();
    private static DefaultPieDataset topActivities = new DefaultPieDataset();
    private static JTextArea logInfo = new JTextArea(10, 50);
    private static JScrollPane text = new JScrollPane(logInfo);
    private static DefaultTableModel activityRank = new DefaultTableModel();
    private static DefaultTableModel errorEvents = new DefaultTableModel();
    private static JTable actRank = new JTable();
    private static JTable errorData = new JTable();


    private static ChartPanel createProcessingChart() {

        TimeSeriesCollection dataset = new TimeSeriesCollection(processingData);

        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                "Processing Inputs",
                "Time",
                "Value",
                dataset,
                true,
                true,
                false
        );
        final XYPlot plot = chart.getXYPlot();
        plot.setNoDataMessage("No data to display");
        ValueAxis axis = plot.getDomainAxis();
        axis.setAutoRange(true);
        axis.setFixedAutoRange(60000.0);
        return new ChartPanel(chart);
    }

    private static ChartPanel createPieChart(DefaultPieDataset dataset, String title) {

        final JFreeChart chart = ChartFactory.createPieChart(
                title,  // chart title
                dataset,                // data
                true,                   // include legend
                true,
                false
        );

        final PiePlot plot = (PiePlot) chart.getPlot();
        plot.setStartAngle(290);
        plot.setDirection(Rotation.CLOCKWISE);
        plot.setForegroundAlpha(0.5f);
        plot.setNoDataMessage("No data to display");
        //chartPanel.setPreferredSize(new java.awt.Dimension(550, 400));
        return new ChartPanel(chart);

    }


    public static ChartPanel createBarChart(String title, String x, String y, DefaultCategoryDataset data) {

        JFreeChart barChart = ChartFactory.createBarChart(
                title,
                x,
                y,
                data,
                PlotOrientation.VERTICAL,
                true, true, false);

        //chartPanel.setPreferredSize(new java.awt.Dimension(550, 400));
        return new ChartPanel(barChart);
    }

    private static JScrollPane createTable(DefaultTableModel dt, JTable t) {
        JScrollPane tableContainer = new JScrollPane(t);
        t.setModel(dt);

        return tableContainer;
    }


    public GraphicInterface() throws InterruptedException {
        //Main Panel of the application
        JPanel jp = new JPanel();
        JFrame jf = new JFrame();
        jp.setLayout(new BorderLayout());
        jp.setVisible(true);
        JButton b = new JButton("Full Screen");
        JButton n = new JButton("Launch Nodes");

        JPanel p1 = new JPanel();
        JPanel p2 = new JPanel();
        JPanel p3 = new JPanel();
        JPanel p4 = new JPanel();
        JPanel p5 = new JPanel();
        JPanel p6 = new JPanel();

        p1.add(createProcessingChart());
        p1.add(createBarChart("Significant Events", "Parameters", "Quantity", priorityEvents), BorderLayout.CENTER);
        p2.add(createPieChart(topActivities, "Top Activities Running"), BorderLayout.CENTER);
        p2.add(createBarChart("Processed/Injected Inputs", "Parameters", "Quantity", processedEvents), BorderLayout.CENTER);
        p3.add(createTable(activityRank, actRank), BorderLayout.CENTER);
        p3.add(createTable(errorEvents, errorData), BorderLayout.CENTER);

        p1.setLayout(new BoxLayout(p1, BoxLayout.Y_AXIS));
        p2.setLayout(new BoxLayout(p2, BoxLayout.Y_AXIS));
        p3.setLayout(new BoxLayout(p3, BoxLayout.Y_AXIS));
        p4.setLayout(new BoxLayout(p4, BoxLayout.X_AXIS));
        p4.add(p1, BorderLayout.NORTH);
        p4.add(p2, BorderLayout.NORTH);
        p4.add(p3, BorderLayout.NORTH);
        jp.setLayout(new BoxLayout(jp, BoxLayout.Y_AXIS));
        jp.add(p4);

        logInfo.setEditable(false);
        p5.setLayout(new BorderLayout());
        p5.add(text, BorderLayout.CENTER);
        p6.add(b);
        p6.add(n);
        p6.setLayout(new BoxLayout(p6, BoxLayout.X_AXIS));
        b.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jf.setExtendedState(JFrame.MAXIMIZED_BOTH);
                jf.setUndecorated(true);
                jf.setVisible(true);
            }
        });

        n.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                String input = JOptionPane.showInputDialog("Number of nodes you want to launch");
                int nNodes = Integer.parseInt(input);
                LaunchNodes.startNodes(nNodes );

            }
        });
        jp.add(p5);
        jp.add(p6);
        DefaultCaret caret = (DefaultCaret) logInfo.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);


        jf.add(jp, BorderLayout.CENTER);
        jf.setSize(new Dimension(1600, 800));
        jf.setVisible(true);
        jf.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }

    public void addLogLine(String line) {
        logInfo.append(line + "\n");
    }

    public void addProcessingElement(Double value) {
        processingData.addOrUpdate(new Millisecond(), value);
    }

    public void setSignificantEvents(String parameter, Double value) {
        priorityEvents.setValue(value, parameter, "Quantity");
    }

    public void addSignificantEvents(String parameter, Double value) {
        priorityEvents.addValue(value, parameter, "Quantity");
    }


    public void setProcessedEvents(String parameter, Double value) {
        processedEvents.setValue(value, parameter, "Quantity");
    }

    public void addProcessedEvents(String parameter, Double value) {
        processedEvents.addValue(value, parameter, "Quantity");
    }

    public void removeTopActivity(String parameter) {
        topActivities.remove(parameter);
    }

    public void updateTopActivity(String parameter, int value) {
        topActivities.setValue(parameter, value);
    }

    public void addErrorEvents(String parameter) {

        errorEvents.addRow(new Object[]{parameter});
    }

    public void setRowActivity(String parameter, int row, int column) {
        errorEvents.setValueAt(parameter, row, column);
    }

    public void addActivityRank(String id, String count, String avgTime) {
        activityRank.addRow(new Object[]{id, count, avgTime});
    }

    public void setActivityRank(String parameter, int row, int column) {
        activityRank.setValueAt(parameter, row, column);
    }

    public void activityRankHeader(String[] header) {

        activityRank.setColumnIdentifiers(header);
        actRank.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        actRank.getColumnModel().getColumn(0).setMinWidth(230);
        actRank.getColumnModel().getColumn(1).setMaxWidth(60);
        actRank.getColumnModel().getColumn(2).setMinWidth(60);
        actRank.getColumnModel().getColumn(2).setMinWidth(60);

    }

    public void errorHeader(String[] header) {

        errorEvents.setColumnIdentifiers(header);
        errorData.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        errorData.getColumnModel().getColumn(0).setMaxWidth(100);
        errorData.getColumnModel().getColumn(0).setMinWidth(100);

    }

}
