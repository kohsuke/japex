/*
 * Japex ver. 0.1 software ("Software")
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

import com.sun.japex.Util;

import java.io.File;
import java.util.Calendar;
import java.util.Map;
import java.text.SimpleDateFormat;
import java.awt.Shape;
import java.awt.BasicStroke;
import java.awt.Polygon;
import java.awt.Color;

import org.jfree.chart.*;
import org.jfree.chart.plot.*;
import org.jfree.data.xy.*;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.data.general.SeriesException;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;

import static java.util.Calendar.*;
import static com.sun.japex.TrendReport.FILE_SEP;

/*
 * The original design of TrendReport was to create one chart at a time, e.g. to
 * create a full report, TrendReport would have to executed multiple times using
 * TrendDataset. The new feature request is for this class to generate full
 * report in accordance with report type.
 */
public class ReportGenerator {
    
    static final int CHART_WIDTH  = 750;
    static final int CHART_HEIGHT = 450;
    
    TrendReportParams _params;
    
    Map[] _japexTestResults;
    
    Calendar[] _dates;
    
    String[] _tests;
    
    String[] _drivers;
    
    boolean _hasReports = true;
    
    IndexPage _indexPage = null;
    
    SimpleDateFormat _dateFormatter;
    
    SimpleDateFormat _dateTimeFormatter;
    
    public ReportGenerator(TrendReportParams params, ParseReports japexReports) {
        _params = params;
        _japexTestResults = japexReports.getReports();
        if (_japexTestResults != null) {
            _dates = japexReports.getDates();
        } 
        else {
            _hasReports = false;
        }
        
        // Initialize list of drivers using domain of first map
        Map map = _japexTestResults[0];
        _drivers = new String[map.keySet().size()];
        int i = 0;
        for (Object obj : map.keySet()) {
            _drivers[i++] = (String) obj;
        }
        
        ResultPerDriver result = 
                (ResultPerDriver) _japexTestResults[0].get(_drivers[0]);
        _tests = result.getTests();        
        _dateFormatter = new SimpleDateFormat("MM-dd");   
        _dateTimeFormatter = new SimpleDateFormat("MM-dd HH:mm");
        _indexPage = new IndexPage(_params, true);
    }
    
    public boolean createReport() {
        if (!_hasReports)
            return false;
        
        // Only one type of test report is supported
        singleMeansChart();
        oneTestcaseChart();
        
        return true;
    }
    
    private void oneTestcaseChart() {        
        DefaultCategoryDataset dataset;
        ResultPerDriver result = null;
        
        for (int k = 0; k < _tests.length; k++) {
            dataset = new DefaultCategoryDataset();
            
            for (int ii = 0; ii < _drivers.length; ii++) {
                for (int i = 0; i < _japexTestResults.length; i++) {
                    result = (ResultPerDriver) _japexTestResults[i].get(_drivers[ii]);
                    if (result != null) {
                        SimpleDateFormat formatter = _dateFormatter;
                        
                        // If previous or next are on the same day, include time
                        if (i > 0 && onSameDate(_dates[i], _dates[i - 1])) {
                            formatter = _dateTimeFormatter;
                        }
                        if (i + 1 < _japexTestResults.length
                                && onSameDate(_dates[i], _dates[i + 1])) {
                            formatter = _dateTimeFormatter;
                        }
                        
                        if (!Double.isNaN(result.getResult(_tests[k]))) {
                            dataset.addValue(result.getResult(_tests[k]),
                                    _drivers[ii], formatter.format(_dates[i].getTime()));
                        }
                    }
                }
            }
            
            String chartName = Util.getFilename(_tests[k]) + ".jpg";
            _params.setTitle(_tests[k]);
            _indexPage.updateContent(chartName);
            saveChart(_tests[k], dataset, chartName, CHART_WIDTH, CHART_HEIGHT);
        }
        
        _indexPage.writeContent();        
    }
        
    private void singleMeansChart() {
        DefaultCategoryDataset aritDataset = new DefaultCategoryDataset();
        DefaultCategoryDataset geomDataset = new DefaultCategoryDataset();
        DefaultCategoryDataset harmDataset = new DefaultCategoryDataset();
        
        ResultPerDriver result = null;
        
        for (int ii = 0; ii < _drivers.length; ii++) {
            for (int i = 0; i < _japexTestResults.length; i++) {
                result = (ResultPerDriver) _japexTestResults[i].get(_drivers[ii]);
                
                if (result != null && result.hasValidMeans()) {
                    SimpleDateFormat formatter = _dateFormatter;
                    
                    // If previous or next are on the same day, include time
                    if (i > 0 && onSameDate(_dates[i], _dates[i - 1])) {
                        formatter = _dateTimeFormatter;                        
                    }
                    if (i + 1 < _japexTestResults.length 
                        && onSameDate(_dates[i], _dates[i + 1])) {
                        formatter = _dateTimeFormatter;
                    }
                    
                    aritDataset.addValue(result.getAritMean(),
                            _drivers[ii], formatter.format(_dates[i].getTime()));
                    geomDataset.addValue(result.getGeomMean(),
                            _drivers[ii], formatter.format(_dates[i].getTime()));
                    harmDataset.addValue(result.getHarmMean(),
                            _drivers[ii], formatter.format(_dates[i].getTime()));
                }
            }
        }
        
        //IndexPage indexPage = new IndexPage(_params, true);
        _params.setTitle("Arithmetic Means");
        saveChart("Arithmetic Means", aritDataset, "ArithmeticMeans.jpg", 
                CHART_WIDTH, CHART_HEIGHT);
        _indexPage.updateContent("ArithmeticMeans.jpg");
        saveChart("Geometric Means", geomDataset, "GeometricMeans.jpg", 
                CHART_WIDTH, CHART_HEIGHT);
        _params.setTitle("Geometric Means");
        _indexPage.updateContent("GeometricMeans.jpg");
        saveChart("Harmonic Means", harmDataset, "HarmonicMeans.jpg", 
                CHART_WIDTH, CHART_HEIGHT);
        _params.setTitle("Harmonic Means");
        _indexPage.updateContent("HarmonicMeans.jpg");
        _indexPage.writeContent();
    }
        
    private void saveChart(String title, DefaultCategoryDataset dataset,
            String fileName, int width, int height) 
    {
        JFreeChart chart = ChartFactory.createLineChart(
                title,
                "", "",
                dataset,
                PlotOrientation.VERTICAL,
                true, true, false);
        configureLineChart(chart);
        chart.setAntiAlias(true);
        
        try {
            // Converts chart in JPEG file named [name].jpg
            File file = new File(_params.outputPath());
            if (!file.exists()) {
                file.mkdirs();
            }
            ChartUtilities.saveChartAsJPEG(
                    new File(_params.outputPath() + FILE_SEP + fileName),
                    chart, width, height);
        } 
        catch (RuntimeException e) {
            throw e;
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    static private void configureLineChart(JFreeChart chart) {
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
     * Compare two calendar instances ignoring the time of the day.
     */
    private static boolean onSameDate(Calendar c1, Calendar c2) {
        Calendar cc1 = (Calendar) c1.clone();
        cc1.set(HOUR_OF_DAY, 0);
        cc1.set(MINUTE, 0);
        cc1.set(SECOND, 0);
        Calendar cc2 = (Calendar) c2.clone();
        cc2.set(HOUR_OF_DAY, 0);
        cc2.set(MINUTE, 0);
        cc2.set(SECOND, 0);
        return cc1.equals(cc2);
    }
    
}
