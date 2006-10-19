/*
 * Japex software ("Software")
 *
 * Copyright, 2004-2005 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This Software is distributed under the following terms:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, is permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * Redistribution in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * Neither the name of Sun Microsystems, Inc., 'Java', 'Java'-based names,
 * nor the names of contributors may be used to endorse or promote products
 * derived from this Software without specific prior written permission.
 *
 * The Software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES, INCLUDING
 * ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN AND ITS LICENSORS
 * SHALL NOT BE LIABLE FOR ANY DAMAGES OR LIABILITIES SUFFERED BY LICENSEE
 * AS A RESULT OF OR RELATING TO USE, MODIFICATION OR DISTRIBUTION OF THE
 * SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR ITS LICENSORS BE
 * LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT,
 * SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED
 * AND REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF OR
 * INABILITY TO USE SOFTWARE, EVEN IF SUN HAS BEEN ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGES.
 *
 * You acknowledge that the Software is not designed, licensed or intended
 * for use in the design, construction, operation or maintenance of any
 * nuclear facility.
 */

package com.sun.japex.report;

import org.jfree.chart.JFreeChart;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import java.awt.Shape;
import java.awt.BasicStroke;
import java.awt.Polygon;
import java.awt.Color;
import java.util.List;
import java.util.Collections;

import org.jfree.chart.*;
import org.jfree.chart.plot.*;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;

import static java.util.Calendar.*;
import static com.sun.japex.report.TrendReport.FILE_SEP;

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
