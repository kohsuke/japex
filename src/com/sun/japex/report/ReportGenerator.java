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
