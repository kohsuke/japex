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

import com.sun.japex.Util;

import java.io.File;
import java.util.List;
import java.util.ArrayList;

import org.jfree.chart.*;

import static java.util.Calendar.*;
import static com.sun.japex.report.TrendReport.FILE_SEP;

/**
 * Trend report generator class.
 *
 * @author Joe.Wang@sun.com
 * @author Santiago.PericasGeertsen@sun.com
 */
public class ReportGenerator extends ChartGenerator {
    
    static final int CHART_WIDTH  = 750;
    static final int CHART_HEIGHT = 450;
    
    IndexPage _indexPage;
    
    TrendReportParams _params;

    /**
     * Complete set of test case names. Use a list to preserve 
     * order from test suite reports.
     */
    List<String> _testCaseNames = new ArrayList<String>();
        
    public ReportGenerator(TrendReportParams params, 
            List<? extends TestSuiteReport> reports) 
    {
        super(reports);
        _params = params;        
        _indexPage = new IndexPage(_params, true);
        
        // Populate set of test cases across all reports
        for (TestSuiteReport report : reports) {
            for (TestSuiteReport.Driver driver : report.getDrivers()) {
                for (TestSuiteReport.TestCase test : driver.getTestCases()) {
                    String testName = test.getName();                    
                    if (!_testCaseNames.contains(testName)) {
                        _testCaseNames.add(testName);
                    }
                }
            }
        }
    }
    
    public void createReport() {
        singleMeansChart();
        oneTestcaseChart();
    }
    
    private void singleMeansChart() {
        // Chart for arithmetic means
        JFreeChart chart = createTrendChart(MeanMode.ARITHMETIC);                
        _params.setTitle("Arithmetic Means");
        saveChart(chart, "ArithmeticMeans.jpg", CHART_WIDTH, CHART_HEIGHT);       
        _indexPage.updateContent("ArithmeticMeans.jpg");
        
        // Chart for geometric means
        chart = createTrendChart(MeanMode.GEOMETRIC);
        saveChart(chart, "GeometricMeans.jpg", CHART_WIDTH, CHART_HEIGHT);
        _params.setTitle("Geometric Means");
        _indexPage.updateContent("GeometricMeans.jpg");
        
        // Chart for harmonic means
        chart = createTrendChart(MeanMode.HARMONIC);
        saveChart(chart, "HarmonicMeans.jpg", CHART_WIDTH, CHART_HEIGHT);
        _params.setTitle("Harmonic Means");
        _indexPage.updateContent("HarmonicMeans.jpg");
        
        // Write content to index page
        _indexPage.writeContent();
    }
    
    private void oneTestcaseChart() {
        // Generate a chart of each test case in our set
        for (String testCaseName : _testCaseNames) {
            JFreeChart chart = createTrendChart(testCaseName);            
            String chartName = Util.getFilename(testCaseName) + ".jpg";
            _params.setTitle(testCaseName);
            _indexPage.updateContent(chartName);
            saveChart(chart, chartName, CHART_WIDTH, CHART_HEIGHT);
        }
        
        // Write content to index page
        _indexPage.writeContent();
    }
                
    private void saveChart(JFreeChart chart, String fileName, int width, 
            int height) 
    {
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
    
}
