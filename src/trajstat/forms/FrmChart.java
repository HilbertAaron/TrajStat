/* Copyright 2014 - Yaqiang Wang,
 * yaqiang.wang@gmail.com
 * 
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 */
package trajstat.forms;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RectangleEdge;
import org.meteoinfo.global.event.IShapeSelectedListener;
import org.meteoinfo.global.event.ShapeSelectedEvent;
import org.meteoinfo.layer.VectorLayer;
import org.meteoinfo.map.MouseTools;
import org.meteoinfo.plugin.IApplication;
import org.meteoinfo.shape.PointZ;
import org.meteoinfo.shape.PolylineZShape;
import org.meteoinfo.shape.ShapeTypes;

/**
 *
 * @author Yaqiang Wang
 */
public class FrmChart extends JDialog {
    // <editor-fold desc="Variables">

    private IApplication app;
    private javax.swing.JToolBar toolBar;
    private ChartPanel chartPanel;
    private javax.swing.JButton button_Sel;
    private javax.swing.JButton button_Remove;
    private javax.swing.JButton button_RemoveAll;
    private List<Date> dates = new ArrayList<Date>();
    private List<PolylineZShape> trajShapes = new ArrayList<PolylineZShape>();
    // </editor-fold>
    // <editor-fold desc="Constructor">

    /**
     * Constructor
     */
    public FrmChart(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();

        this.app = (IApplication)parent;
        app.getMapDocument().getActiveMapFrame().getMapView().addShapeSelectedListener(new IShapeSelectedListener() {
            @Override
            public void shapeSelectedEvent(ShapeSelectedEvent event) {
                onShapeSelected();
            }
        });
        this.setTitle("Pressure profile plot");

        //Set icon image
        BufferedImage image = null;
        try {
            image = ImageIO.read(this.getClass().getResource("/trajstat/resources/TrajStat_Logo.png"));
        } catch (Exception e) {
        }
        this.setIconImage(image);
    }

    private void initComponents() {
        toolBar = new javax.swing.JToolBar();
        chartPanel = new ChartPanel(null);
        button_Sel = new javax.swing.JButton();
        button_Remove = new javax.swing.JButton();
        button_RemoveAll = new javax.swing.JButton();

        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.setSize(800, 400);

        //Tool bar
        ImageIcon icon = new ImageIcon(this.getClass().getResource("/trajstat/resources/Select.png"));
        button_Sel.setIcon(icon);
        button_Sel.setToolTipText("Select Trajectory");
        button_Sel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onSelTrajClick(e);
            }            
        });
        toolBar.add(button_Sel);
        
        icon = new ImageIcon(this.getClass().getResource("/trajstat/resources/Remove.png"));
        button_Remove.setIcon(icon);
        button_Remove.setToolTipText("Remove Last Trajectory");
        button_Remove.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onRemoveClick(e);
            }            
        });
        toolBar.add(button_Remove);
        
        icon = new ImageIcon(this.getClass().getResource("/trajstat/resources/RemoveAll.png"));
        button_RemoveAll.setIcon(icon);
        button_RemoveAll.setToolTipText("Remove All Trajectories");
        button_RemoveAll.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onRemoveAllClick(e);
            }            
        });
        toolBar.add(button_RemoveAll);
        
        this.add(toolBar, BorderLayout.NORTH);

        //Chart panel                
        chartPanel.setBackground(Color.white);
        this.add(chartPanel, BorderLayout.CENTER);
    }
    // </editor-fold>
    // <editor-fold desc="Get Set Methods">
    // </editor-fold>
    // <editor-fold desc="Methods">
    private void onSelTrajClick(ActionEvent e){
        app.setCurrentTool((JButton) e.getSource());
        app.getMapDocument().getActiveMapFrame().getMapView().setMouseTool(MouseTools.SelectFeatures_Rectangle);
    }
    
    private void onShapeSelected() {
        VectorLayer trajLayer = (VectorLayer) app.getMapDocument().getActiveMapFrame().getMapView().getSelectedLayer();
        if (trajLayer != null) {
            if (trajLayer.getShapeType() == ShapeTypes.PolylineZ) {
                Calendar cal = Calendar.getInstance();
                for (int i = 0; i < trajLayer.getShapeNum(); i++) {
                    if (trajLayer.getShapes().get(i).isSelected()) {
                        if (trajLayer.getFieldIdxByName("Date") >= 0) {
                            Date aDate = (Date) trajLayer.getCellValue("Date", i);
                            int hour = Integer.parseInt(trajLayer.getCellValue("Hour", i).toString());
                            cal.setTime(aDate);
                            cal.set(Calendar.HOUR_OF_DAY, hour);
                            aDate = cal.getTime();

                            if (dates.contains(aDate)) {
                                return;
                            }
                            dates.add(aDate);
                        }

                        PolylineZShape aPLZ = (PolylineZShape) trajLayer.getShapes().get(i);
                        trajShapes.add(aPLZ);
                        updateChart();
                    }
                }
            }
        }
    }

    private void updateChart() {
        String title = "";
        String serieName;
        XYSeriesCollection xyseriescollection = new XYSeriesCollection();
        int i = 0;
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHH");
        for (PolylineZShape shape : this.trajShapes) {
            if (dates.size() > 0 && dates.size() >= i)
                serieName = format.format(dates.get(i));
            else
                serieName = "Line " + String.valueOf(i);
            XYSeries xySeries = new XYSeries(serieName);
            for (int j = 0; j < shape.getPoints().size(); j++) {
                xySeries.add(0 - j, ((PointZ)shape.getPoints().get(j)).Z);
            }
            xyseriescollection.addSeries(xySeries);
            i++;
        }
        JFreeChart chart = ChartFactory.createXYLineChart(title, "Age Hour", "Pressure",
                xyseriescollection, PlotOrientation.VERTICAL, true, true, false);        
        XYPlot plot = chart.getXYPlot();
        plot.setDomainPannable(true);
        plot.setRangePannable(true);
        plot.setBackgroundPaint(null);
        plot.setRangeGridlinePaint(Color.gray);
        LegendTitle legend = (LegendTitle)chart.getSubtitle(0);
        legend.setPosition(RectangleEdge.TOP);
        NumberAxis yAxis = (NumberAxis)plot.getRangeAxis();
        ValueAxis xAxis = plot.getDomainAxis();
        yAxis.setInverted(true);
        yAxis.setAutoRangeIncludesZero(false);
        yAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        xAxis.setInverted(true);
        XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer)plot.getRenderer();
        renderer.setBaseShapesVisible(true);
        renderer.setBaseShapesFilled(false);
        chartPanel.setChart(chart);
        chartPanel.repaint();
    }

    private void onRemoveClick(ActionEvent e){
        if (this.trajShapes.size() > 0){
            this.trajShapes.remove(this.trajShapes.size() - 1);
            this.dates.remove(dates.size() - 1);
            this.updateChart();
        }
    }
    
    private void onRemoveAllClick(ActionEvent e){
        if (this.trajShapes.size() > 0){
            this.trajShapes.clear();
            this.dates.clear();
            this.updateChart();
        }
    }
    // </editor-fold>
}
