/*
 * Japex software ("Software")
 *
 * Copyright, 2004-2007 Sun Microsystems, Inc. All Rights Reserved.
 *
 * Software is licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. You may
 * obtain a copy of the License at:
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations.
 *
 *    Sun supports and benefits from the global community of open source
 * developers, and thanks the community for its important contributions and
 * open standards-based technology, which Sun has adopted into many of its
 * products.
 *
 *    Please note that portions of Software may be provided with notices and
 * open source licenses from such communities and third parties that govern the
 * use of those portions, and any licenses granted hereunder do not alter any
 * rights and obligations you may have under such open source licenses,
 * however, the disclaimer of warranty and limitation of liability provisions
 * in this License will apply to all Software in this distribution.
 *
 *    You acknowledge that the Software is not designed, licensed or intended
 * for use in the design, construction, operation or maintenance of any nuclear
 * facility.
 *
 * Apache License
 * Version 2.0, January 2004
 * http://www.apache.org/licenses/
 *
 */

package com.sun.japex.report;

import static java.util.Calendar.HOUR_OF_DAY;
import static java.util.Calendar.MINUTE;
import static java.util.Calendar.SECOND;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Polygon;
import java.awt.Shape;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.DefaultDrawingSupplier;
import org.jfree.chart.plot.DrawingSupplier;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;

/**
 * Generator for various types of trend charts.
 *
 * @author Kohsuke.Kawaguchi@sun.com
 * @author Santiago.PericasGeertsen@sun.com
 *
 */
public class ChartGenerator {
    
    SimpleDateFormat _dateFormatter = new SimpleDateFormat("MM-dd");   
    SimpleDateFormat _dateTimeFormatter = new SimpleDateFormat("MM-dd HH:mm");
    
    List<? extends TestSuiteReport> _reports;
    
    public ChartGenerator(List<? extends TestSuiteReport> reports) {
        _reports = reports;
                
        // Sort report in ascending order based on timestamps
        Collections.sort(_reports);
    }
    
    /**
     * Create a chart for a single mean across all drivers.
     */
    public JFreeChart createTrendChart(MeanMode mean) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        
        final int size = _reports.size();
        for (int i = 0; i < size; i++) {
            TestSuiteReport report = _reports.get(i);
            SimpleDateFormat formatter = _dateFormatter;
            
            // If previous or next are on the same day, include time
            if (i > 0 && onSameDate(report, _reports.get(i - 1))) {
                formatter = _dateTimeFormatter;
            }
            if (i + 1 < size && onSameDate(report, _reports.get(i + 1))) {
                formatter = _dateTimeFormatter;
            }
            
            List<TestSuiteReport.Driver> drivers = report.getDrivers();
            for (TestSuiteReport.Driver driver : drivers) {
                double value = driver.getResult(mean);
                if (!Double.isNaN(value)) {
                    dataset.addValue(
                            driver.getResult(MeanMode.ARITHMETIC),
                            driver.getName(), 
                            formatter.format(report.getDate().getTime()));
                }
            }            
        }
        
       JFreeChart chart = ChartFactory.createLineChart(
                mean.toString(),
                "", "",
                dataset,
                PlotOrientation.VERTICAL,
                true, true, false);
        configureLineChart(chart);
        chart.setAntiAlias(true);        
         
        return chart;
    }
    
    /**
     * Create a chart for a single test case across all drivers.
     */
    public JFreeChart createTrendChart(String testCaseName) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        
        final int size = _reports.size();
        for (int i = 0; i < size; i++) {
            TestSuiteReport report = _reports.get(i);
            SimpleDateFormat formatter = _dateFormatter;
            
            // If previous or next are on the same day, include time
            if (i > 0 && onSameDate(report, _reports.get(i - 1))) {
                formatter = _dateTimeFormatter;
            }
            if (i + 1 < size && onSameDate(report, _reports.get(i + 1))) {
                formatter = _dateTimeFormatter;
            }
            
            List<TestSuiteReport.Driver> drivers = report.getDrivers();
            for (TestSuiteReport.Driver driver : drivers) {
                TestSuiteReport.TestCase testCase = driver.getTestCase(testCaseName);
                if (testCase != null) {
                    double value = testCase.getResult();
                    if (!Double.isNaN(value)) {
                        dataset.addValue(value, driver.getName(), 
                               formatter.format(report.getDate().getTime()));                    
                    }
                }
            }            
        }
        
       JFreeChart chart = ChartFactory.createLineChart(
                testCaseName,
                "", "",
                dataset,
                PlotOrientation.VERTICAL,
                true, true, false);
        configureLineChart(chart);
        chart.setAntiAlias(true);        
         
        return chart;
    }
    
    static protected void configureLineChart(JFreeChart chart) {
        CategoryPlot plot = chart.getCategoryPlot();
        
        final DrawingSupplier supplier = new DefaultDrawingSupplier(
            DefaultDrawingSupplier.DEFAULT_PAINT_SEQUENCE,
            DefaultDrawingSupplier.DEFAULT_OUTLINE_PAINT_SEQUENCE,
            DefaultDrawingSupplier.DEFAULT_STROKE_SEQUENCE,
            DefaultDrawingSupplier.DEFAULT_OUTLINE_STROKE_SEQUENCE,
            // Draw a small diamond 
            new Shape[] { new Polygon(new int[] {3, 0, -3, 0}, 
                                      new int[] {0, 3, 0, -3}, 4) }
        );
        plot.setDomainGridlinePaint(Color.black);
        plot.setRangeGridlinePaint(Color.black);
        plot.setDrawingSupplier(supplier);
        
        LineAndShapeRenderer renderer = (LineAndShapeRenderer) plot.getRenderer();
        renderer.setShapesVisible(true);
        renderer.setStroke(new BasicStroke(2.0f));        
        
        CategoryAxis axis = plot.getDomainAxis();
        axis.setCategoryLabelPositions(CategoryLabelPositions.DOWN_90);
    }    
    
    /**
     * Compare two calendar instances from test suite reports ignoring 
     * the time of the day.
     */
    static protected boolean onSameDate(TestSuiteReport r1, TestSuiteReport r2) {        
        Calendar cc1 = (Calendar) r1.getDate().clone();
        cc1.set(HOUR_OF_DAY, 0);
        cc1.set(MINUTE, 0);
        cc1.set(SECOND, 0);
        Calendar cc2 = (Calendar) r2.getDate().clone();
        cc2.set(HOUR_OF_DAY, 0);
        cc2.set(MINUTE, 0);
        cc2.set(SECOND, 0);
        return cc1.equals(cc2);
    }
}
