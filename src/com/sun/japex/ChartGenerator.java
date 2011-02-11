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

package com.sun.japex;

import java.util.*;
import java.awt.Shape;
import java.awt.BasicStroke;
import java.awt.Polygon;
import java.awt.Color;
import java.awt.Font;
import java.io.File;

import org.jfree.chart.*;
import org.jfree.chart.plot.*;
import org.jfree.data.xy.*;
import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.StandardCategoryToolTipGenerator;
import org.jfree.data.general.SeriesException;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer3D;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;

public class ChartGenerator {

    /**
     * Test suite for which chart will be generated.
     */
    TestSuiteImpl _testSuite;

    /**
     * Chart width and height as a function of the number of
     * drivers in the test suite.
     */
    int _chartWidth, _chartHeight;

    /**
     * Suggested group size to use when plotting test cases. This
     * is a maximum value. The actual number of tests per plot may
     * vary but will not exceed this number.
     */
    int _plotGroupSize;

    /**
     * By default, test cases are plotted over the list of drivers.
     * By setting this flag, drivers are plotted over the list of
     * test cases instead.
     */
    boolean _plotDrivers;

    public ChartGenerator(TestSuiteImpl testSuite) {
        _testSuite = testSuite;

        _plotGroupSize = testSuite.getIntParam(Constants.PLOT_GROUP_SIZE);
        _plotDrivers = testSuite.getBooleanParam(Constants.PLOT_DRIVERS);

        // Calculate charts width and height (min = 750, max = 1500)
        List<DriverImpl> driverInfoList = _testSuite.getDriverInfoList();
        int n = !_plotDrivers ? driverInfoList.size() :
            ((DriverImpl) driverInfoList.get(0)).getAggregateTestCases().size();
        _chartWidth = Math.min(Math.max(n * 80, 750), 1500);
        _chartHeight = (int) Math.round(_chartWidth * 0.6);
    }

    public void generateDriverChart(String fileName) {
        try {
            JFreeChart chart = null;
            String chartType = _testSuite.getParam(Constants.CHART_TYPE);

            if (chartType.equalsIgnoreCase("barchart")) {
                chart = generateDriverBarChart();
            }
            else if (chartType.equalsIgnoreCase("scatterchart")) {
                chart = generateDriverScatterChart();
            }
            else if (chartType.equalsIgnoreCase("linechart")) {
                chart = generateDriverLineChart();
            }
            else {
                assert false;
            }

            chart.setAntiAlias(true);
            ChartUtilities.saveChartAsJPEG(new File(fileName), chart,
                _chartWidth, _chartHeight);
        }
        catch (RuntimeException e) {
            throw e;
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private JFreeChart generateDriverBarChart() {
        try {
            String resultUnit = _testSuite.getParam(Constants.RESULT_UNIT);
            String resultUnitX = _testSuite.getParam(Constants.RESULT_UNIT_X);
            
            // Ensure japex.resultUnitX is not null
            if (resultUnitX == null) {
                resultUnitX = "";
            }
            
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            DefaultCategoryDataset datasetX = new DefaultCategoryDataset();

            // Find first normalizer driver (if any) and adjust unit            
            DriverImpl normalizerDriver = null;
            for (DriverImpl driver : _testSuite.getDriverInfoList()) {
                if (driver.isNormal()) {
                    normalizerDriver = driver;
                    break;
                }
            }

            // Check if normalizer driver can be used as such
            if (normalizerDriver != null) {
                if (normalizerDriver.getDoubleParamNoNaN(Constants.RESULT_ARIT_MEAN) == 0.0
                    || normalizerDriver.getDoubleParamNoNaN(Constants.RESULT_GEOM_MEAN) == 0.0
                    || normalizerDriver.getDoubleParamNoNaN(Constants.RESULT_HARM_MEAN) == 0.0)
                {
                    System.out.println("Warning: Driver '" + normalizerDriver.getName() +
                            "' cannot be used to normalize results");
                    normalizerDriver = null;
                }
                else {
                    resultUnit = "% of " + resultUnit;
                    if (resultUnitX != null) {
                        resultUnitX = "% of " + resultUnitX;
                    }
                }
            }
            
            boolean hasValueX = false;

            // Generate charts
            for (DriverImpl di : _testSuite.getDriverInfoList()) {
                if (normalizerDriver != null) {
                    dataset.addValue(
                        normalizerDriver == di ? 100.0 :
                            (100.0 * di.getDoubleParamNoNaN(Constants.RESULT_ARIT_MEAN) /
                                normalizerDriver.getDoubleParamNoNaN(Constants.RESULT_ARIT_MEAN)),
                        di.getName(),
                        "Arithmetic Mean");
                    dataset.addValue(
                        normalizerDriver == di ? 100.0 :
                            (100.0 * di.getDoubleParamNoNaN(Constants.RESULT_GEOM_MEAN) /
                                normalizerDriver.getDoubleParamNoNaN(Constants.RESULT_GEOM_MEAN)),
                        di.getName(),
                        "Geometric Mean");
                    dataset.addValue(
                        normalizerDriver == di ? 100.0 :
                            (100.0 * di.getDoubleParamNoNaN(Constants.RESULT_HARM_MEAN) /
                                normalizerDriver.getDoubleParamNoNaN(Constants.RESULT_HARM_MEAN)),
                        di.getName(),
                        "Harmonic Mean");
                    
                    if (di.hasParam(Constants.RESULT_ARIT_MEAN_X)) {
                        datasetX.addValue(
                        normalizerDriver == di ? 100.0 :
                            (100.0 * di.getDoubleParamNoNaN(Constants.RESULT_ARIT_MEAN_X) /
                                normalizerDriver.getDoubleParamNoNaN(Constants.RESULT_ARIT_MEAN_X)),
                            di.getName(),
                            "Arithmetic Mean");
                        datasetX.addValue(
                        normalizerDriver == di ? 100.0 :
                            (100.0 * di.getDoubleParamNoNaN(Constants.RESULT_GEOM_MEAN_X) /
                                normalizerDriver.getDoubleParamNoNaN(Constants.RESULT_GEOM_MEAN_X)),
                            di.getName(),
                            "Geometric Mean");
                        datasetX.addValue(
                            (100.0 * di.getDoubleParamNoNaN(Constants.RESULT_HARM_MEAN_X) /
                                normalizerDriver.getDoubleParamNoNaN(Constants.RESULT_HARM_MEAN_X)),
                            di.getName(),
                            "Harmonic Mean");                    
                        hasValueX = true;
                    }
                } 
                else {
                    dataset.addValue(
                        di.getDoubleParamNoNaN(Constants.RESULT_ARIT_MEAN),
                        di.getName(),
                        "Arithmetic Mean");
                    dataset.addValue(
                        di.getDoubleParamNoNaN(Constants.RESULT_GEOM_MEAN),
                        di.getName(),
                        "Geometric Mean");
                    dataset.addValue(
                        di.getDoubleParamNoNaN(Constants.RESULT_HARM_MEAN),
                        di.getName(),
                        "Harmonic Mean");
                    
                    if (di.hasParam(Constants.RESULT_ARIT_MEAN_X)) {
                        datasetX.addValue(
                            di.getDoubleParamNoNaN(Constants.RESULT_ARIT_MEAN_X),
                            di.getName(),
                            "Arithmetic Mean");
                        datasetX.addValue(
                            di.getDoubleParamNoNaN(Constants.RESULT_GEOM_MEAN_X),
                            di.getName(),
                            "Geometric Mean");
                        datasetX.addValue(
                            di.getDoubleParamNoNaN(Constants.RESULT_HARM_MEAN_X),
                            di.getName(),
                            "Harmonic Mean");                    
                        hasValueX = true;
                    }                    
                }
                

            }

            int nextPlotIndex = 1;
            CombinedDomainCategoryPlot plot = new CombinedDomainCategoryPlot();

            // Use same renderer in combine charts to get same colors
            BarRenderer3D renderer = new BarRenderer3D();
            renderer.setToolTipGenerator(new StandardCategoryToolTipGenerator());

            // Bar chart for secondary data set based on japex.resultValueX
            if (hasValueX) {
                NumberAxis rangeAxisX = new NumberAxis(resultUnitX);
                CategoryPlot subplotX = new CategoryPlot(datasetX, null, rangeAxisX,
                        renderer);
                
                // Set transparency and clear legend for this plot
                subplotX.setForegroundAlpha(0.7f);
                subplotX.setFixedLegendItems(new LegendItemCollection());
                
                plot.add(subplotX, nextPlotIndex++);
                _chartHeight += 50;    // Adjust chart height
            }
            else {
            }
            
            // Bar chart for main data set based on japex.resultValue
            NumberAxis rangeAxis = new NumberAxis(resultUnit);
            CategoryPlot subplot = new CategoryPlot(dataset, null, rangeAxis,
                    renderer);
            subplot.setForegroundAlpha(0.7f);      // transparency
            plot.add(subplot, nextPlotIndex);
            
            // Create chart and save it as JPEG
            String chartTitle = "Result Summary";
            JFreeChart chart = new JFreeChart(
                    hasValueX ? chartTitle : (chartTitle + "(" + resultUnit + ")"),
                    new Font("SansSerif", Font.BOLD, 14),
                    plot, true);
            chart.setAntiAlias(true);
            return chart;
        }
        catch (RuntimeException e) {
            throw e;
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private JFreeChart generateDriverScatterChart() {
        try {
            DefaultTableXYDataset xyDataset = new DefaultTableXYDataset();

            // Generate charts
            for (DriverImpl di : _testSuite.getDriverInfoList()) {
                if (!di.hasParam(Constants.RESULT_ARIT_MEAN_X)) {
                    System.out.println("Error: Driver '" + di.getName() + "' does not define"
                        + " any values for the X axis needed to generate a scatter chart");
                    System.exit(1);
                }

                XYSeries xySeries = new XYSeries(di.getName(), true, false);
                xySeries.add(di.getDoubleParamNoNaN(Constants.RESULT_ARIT_MEAN_X),
                    di.getDoubleParamNoNaN(Constants.RESULT_ARIT_MEAN));
                xySeries.add(di.getDoubleParamNoNaN(Constants.RESULT_GEOM_MEAN_X),
                    di.getDoubleParamNoNaN(Constants.RESULT_GEOM_MEAN));
                xySeries.add(di.getDoubleParamNoNaN(Constants.RESULT_HARM_MEAN_X),
                    di.getDoubleParamNoNaN(Constants.RESULT_HARM_MEAN));
                xyDataset.addSeries(xySeries);
            }

            String resultUnit = _testSuite.getParam(Constants.RESULT_UNIT);
            String resultUnitX = _testSuite.getParam(Constants.RESULT_UNIT_X);

            JFreeChart chart = ChartFactory.createScatterPlot("Result Summary",
                resultUnitX, resultUnit,
                xyDataset, PlotOrientation.VERTICAL,
                true, true, false);

            // Set log scale depending on japex.resultAxis[_X]
            XYPlot plot = chart.getXYPlot();
            if (_testSuite.getParam(Constants.RESULT_AXIS_X).equalsIgnoreCase("logarithmic")) {
                LogarithmicAxis logAxisX = new LogarithmicAxis(resultUnitX);
                logAxisX.setAllowNegativesFlag(true);
                plot.setDomainAxis(logAxisX);
            }
            if (_testSuite.getParam(Constants.RESULT_AXIS).equalsIgnoreCase("logarithmic")) {
                LogarithmicAxis logAxis = new LogarithmicAxis(resultUnit);
                logAxis.setAllowNegativesFlag(true);
                plot.setRangeAxis(logAxis);
            }

            return chart;
        }
        catch (RuntimeException e) {
            throw e;
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private JFreeChart generateDriverLineChart() {
        try {
            String resultUnit = _testSuite.getParam(Constants.RESULT_UNIT);
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();

            // Generate charts
            for (DriverImpl di : _testSuite.getDriverInfoList()) {
                dataset.addValue(
                    di.getDoubleParamNoNaN(Constants.RESULT_ARIT_MEAN),
                    "Arithmetic Mean",
                    di.getName());
                dataset.addValue(
                    di.getDoubleParamNoNaN(Constants.RESULT_GEOM_MEAN),
                    "Geometric Mean",
                    di.getName());
                dataset.addValue(
                    di.getDoubleParamNoNaN(Constants.RESULT_HARM_MEAN),
                    "Harmonic Mean",
                    di.getName());
            }

            JFreeChart chart = ChartFactory.createLineChart(
                "Result Summary (" + resultUnit + ")",
                "", resultUnit,
                dataset,
                PlotOrientation.VERTICAL,
                true, true, false);

            configureLineChart(chart);
            return chart;
        }
        catch (RuntimeException e) {
            throw e;
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public int generateTestCaseCharts(String baseName, String extension) {
        String chartType = _testSuite.getParam(Constants.CHART_TYPE);
        if (chartType.equalsIgnoreCase("barchart")) {
            return generateTestCaseBarCharts(baseName, extension);
        }
        else if (chartType.equalsIgnoreCase("scatterchart")) {
            return generateTestCaseScatterCharts(baseName, extension);
        }
        else if (chartType.equalsIgnoreCase("linechart")) {
            return generateTestCaseLineCharts(baseName, extension);
        }
        else {
            assert false;
        }
        return 0;
    }

    private int generateTestCaseBarCharts(String baseName, String extension) {
        int nOfFiles = 0;
        List<DriverImpl> driverInfoList = _testSuite.getDriverInfoList();

        // Get number of tests from first driver
        final int nOfTests =
            driverInfoList.get(0).getAggregateTestCases().size();

        int groupSizesIndex = 0;
        int[] groupSizes = calculateGroupSizes(nOfTests, _plotGroupSize);

        try {
            String resultUnit = _testSuite.getParam(Constants.RESULT_UNIT);
            String resultUnitX = _testSuite.getParam(Constants.RESULT_UNIT_X);
            
            // Ensure japex.resultUnitX is not null
            if (resultUnitX == null) {
                resultUnitX = "";
            }

            // Find first normalizer driver (if any)
            DriverImpl normalizerDriver = null;
            for (DriverImpl di : driverInfoList) {
                if (di.isNormal()) {
                    normalizerDriver = di;
                    break;
                }
            }

            // Check if normalizer driver can be used as such
            if (normalizerDriver != null) {
                if (normalizerDriver.getDoubleParamNoNaN(Constants.RESULT_ARIT_MEAN) == 0.0
                    || normalizerDriver.getDoubleParamNoNaN(Constants.RESULT_GEOM_MEAN) == 0.0
                    || normalizerDriver.getDoubleParamNoNaN(Constants.RESULT_HARM_MEAN) == 0.0)
                {
                    normalizerDriver = null;
                }
                else {
                    resultUnit = "% of " + resultUnit;
                    if (resultUnitX != null) {
                        resultUnitX = "% of " + resultUnitX;
                    }
                }
            }

            // Generate charts 
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            DefaultCategoryDataset datasetX = new DefaultCategoryDataset();
            
            boolean hasValueX = false;

            int i = 0, thisGroupSize = 0;
            for (; i < nOfTests; i++) {

                for (DriverImpl di : driverInfoList) {
                    TestCaseImpl tc = (TestCaseImpl) di.getAggregateTestCases().get(i);

                    // User normalizer driver if defined
                    if (normalizerDriver != null) {
                        TestCaseImpl normalTc =
                            (TestCaseImpl) normalizerDriver.getAggregateTestCases().get(i);
                        
                        dataset.addValue(normalizerDriver == di ? 100.0 :
                            (100.0 * tc.getDoubleParamNoNaN(Constants.RESULT_VALUE) /
                                normalTc.getDoubleParamNoNaN(Constants.RESULT_VALUE)),
                            _plotDrivers ? tc.getName() : di.getName(),
                            _plotDrivers ? di.getName() : tc.getName());
                        
                        if (tc.hasParam(Constants.RESULT_VALUE_X)) {
                            datasetX.addValue(
                                (100.0 * tc.getDoubleParamNoNaN(Constants.RESULT_VALUE_X) /
                                    normalTc.getDoubleParamNoNaN(Constants.RESULT_VALUE_X)),
                                _plotDrivers ? tc.getName() : di.getName(),
                                _plotDrivers ? di.getName() : tc.getName());
                            hasValueX = true;
                        }
                    } 
                    else {
                        dataset.addValue(
                            tc.getDoubleParamNoNaN(Constants.RESULT_VALUE),
                            _plotDrivers ? tc.getName() : di.getName(),
                            _plotDrivers ? di.getName() : tc.getName());
                        
                        if (tc.hasParam(Constants.RESULT_VALUE_X)) {
                            datasetX.addValue(
                            tc.getDoubleParamNoNaN(Constants.RESULT_VALUE_X),
                                _plotDrivers ? tc.getName() : di.getName(),
                                _plotDrivers ? di.getName() : tc.getName());
                            hasValueX = true;
                        }
                    }
                    
                }
                
                thisGroupSize++;

                // Generate chart for this group if complete
                if (thisGroupSize == groupSizes[groupSizesIndex]) {
                    int nextPlotIndex = 1;
                    CombinedDomainCategoryPlot plot = new CombinedDomainCategoryPlot();
                    
                    // Use same renderer in combine charts to get same colors
                    BarRenderer3D renderer = new BarRenderer3D();
                    renderer.setToolTipGenerator(new StandardCategoryToolTipGenerator());

                    // Bar chart for secondary data set based on japex.resultValueX
                    if (hasValueX) {                        
                        NumberAxis rangeAxisX = new NumberAxis(resultUnitX);
                        CategoryPlot subplotX = new CategoryPlot(datasetX, null, rangeAxisX,
                            renderer);
                        
                        // Set transparency and clear legend for this plot
                        subplotX.setForegroundAlpha(0.7f);
                        subplotX.setFixedLegendItems(new LegendItemCollection());
                        
                        plot.add(subplotX, nextPlotIndex++);                        
                        _chartHeight += 50;    // Adjust chart height
                    }
                    
                    // Bar chart for main data set based on japex.resultValue
                    NumberAxis rangeAxis = new NumberAxis(resultUnit);
                    CategoryPlot subplot = new CategoryPlot(dataset, null, rangeAxis,
                            renderer);
                    subplot.setForegroundAlpha(0.7f);      // transparency
                    plot.add(subplot, nextPlotIndex);

                    // Create chart and save it as JPEG
                    String chartTitle = _plotDrivers ? "Results per Driver" 
                            : "Results per Test";                       
                    JFreeChart chart = new JFreeChart(
                        hasValueX ? chartTitle : (chartTitle + "(" + resultUnit + ")"),
                        new Font("SansSerif", Font.BOLD, 14),
                        plot, true);                                        
                    chart.setAntiAlias(true);
                    ChartUtilities.saveChartAsJPEG(
                        new File(baseName + Integer.toString(nOfFiles) + extension),
                        chart, _chartWidth, _chartHeight);

                    nOfFiles++;
                    groupSizesIndex++;
                    thisGroupSize = 0;
                    
                    // Create fresh data sets
                    dataset = new DefaultCategoryDataset();
                    datasetX = new DefaultCategoryDataset();
                }
            }
        }
        catch (RuntimeException e) {
            throw e;
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return nOfFiles;
    }

    private int generateTestCaseLineCharts(String baseName, String extension) {
        int nOfFiles = 0;
        List<DriverImpl> driverInfoList = _testSuite.getDriverInfoList();

        // Get number of tests from first driver
        final int nOfTests =
            driverInfoList.get(0).getAggregateTestCases().size();

        int groupSizesIndex = 0;
        int[] groupSizes = calculateGroupSizes(nOfTests, _plotGroupSize);

        try {
            String resultUnit = _testSuite.getParam(Constants.RESULT_UNIT);

            // Generate charts 
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();

            int i = 0, thisGroupSize = 0;
            for (; i < nOfTests; i++) {

                for (DriverImpl di : driverInfoList) {
                    TestCaseImpl tc = (TestCaseImpl) di.getAggregateTestCases().get(i);

                    dataset.addValue(
                        tc.getDoubleParamNoNaN(Constants.RESULT_VALUE),
                        _plotDrivers ? di.getName() : tc.getName(),
                        _plotDrivers ? tc.getName() : di.getName());
                }

                thisGroupSize++;

                // Generate chart for this group if complete
                if (thisGroupSize == groupSizes[groupSizesIndex]) {
                    JFreeChart chart = ChartFactory.createLineChart(
                        (_plotDrivers ? "Results per Driver (" : "Results per Test (")
                            + resultUnit + ")",
                        "", resultUnit,
                        dataset,
                        PlotOrientation.VERTICAL,
                        true, true, false);

                    configureLineChart(chart);

                    chart.setAntiAlias(true);
                    ChartUtilities.saveChartAsJPEG(
                        new File(baseName + Integer.toString(nOfFiles) + extension),
                        chart, _chartWidth, _chartHeight);

                    nOfFiles++;
                    groupSizesIndex++;
                    thisGroupSize = 0;
                    dataset = new DefaultCategoryDataset();
                }
            }
        }
        catch (RuntimeException e) {
            throw e;
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return nOfFiles;
    }

    private int generateTestCaseScatterCharts(String baseName, String extension) {
        int nOfFiles = 0;
        List<DriverImpl> driverInfoList = _testSuite.getDriverInfoList();

        try {
            // Get number of tests from first driver
            final int nOfTests =
                driverInfoList.get(0).getAggregateTestCases().size();

            DefaultTableXYDataset xyDataset = new DefaultTableXYDataset();

            // Generate charts
            for (DriverImpl di : driverInfoList) {
                XYSeries xySeries = new XYSeries(di.getName(), true, false);
                for (int j = 0; j < nOfTests; j++) {
                    TestCaseImpl tc = (TestCaseImpl) di.getAggregateTestCases().get(j);
                    try {
                        xySeries.add(tc.getDoubleParamNoNaN(Constants.RESULT_VALUE_X),
                            tc.getDoubleParamNoNaN(Constants.RESULT_VALUE));
                    }
                    catch (SeriesException e) {
                        // Ignore duplicate x-valued points
                    }

                }
                xyDataset.addSeries(xySeries);
            }

            String resultUnit = _testSuite.getParam(Constants.RESULT_UNIT);
            String resultUnitX = _testSuite.getParam(Constants.RESULT_UNIT_X);

            JFreeChart chart = ChartFactory.createScatterPlot("Results Per Test",
                resultUnitX, resultUnit,
                xyDataset, PlotOrientation.VERTICAL,
                true, true, false);

            // Set log scale depending on japex.resultAxis[_X]
            XYPlot plot = chart.getXYPlot();
            if (_testSuite.getParam(Constants.RESULT_AXIS_X).equalsIgnoreCase("logarithmic")) {
                LogarithmicAxis logAxisX = new LogarithmicAxis(resultUnitX);
                logAxisX.setAllowNegativesFlag(true);
                plot.setDomainAxis(logAxisX);
            }
            if (_testSuite.getParam(Constants.RESULT_AXIS).equalsIgnoreCase("logarithmic")) {
                LogarithmicAxis logAxis = new LogarithmicAxis(resultUnit);
                logAxis.setAllowNegativesFlag(true);
                plot.setRangeAxis(logAxis);
            }

            chart.setAntiAlias(true);
            ChartUtilities.saveChartAsJPEG(
                new File(baseName + Integer.toString(nOfFiles) + extension),
                chart, _chartWidth, _chartHeight);
            nOfFiles++;
        }
        catch (RuntimeException e) {
            throw e;
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return nOfFiles;
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
    }

    /**
     * Calculate group sizes for tests to avoid a very small final group. 
     * For example, calculateGroupSizes(21, 5) return { 5,5,5,3,3 } instead
     * of { 5,5,5,5,1 }.
     */
    private static int[] calculateGroupSizes(int nOfTests, int maxGroupSize) {
        if (nOfTests <= maxGroupSize) {
            return new int[] { nOfTests };
        }

        int[] result = new int[nOfTests / maxGroupSize +
                               ((nOfTests % maxGroupSize > 0) ? 1 : 0)];

        // Var m1 represents the number of groups of size maxGroupSize
        int m1 = (nOfTests - maxGroupSize) / maxGroupSize;
        for (int i = 0; i < m1; i++) {
            result[i] = maxGroupSize;
        }

        // Var m2 represents the number of tests not allocated into groups
        int m2 = nOfTests - m1 * maxGroupSize;
        if (m2 <= maxGroupSize) {
            result[result.length - 1] = m2;
        }
        else {
            // Allocate last two groups
            result[result.length - 2] = (int) Math.ceil(m2 / 2.0);
            result[result.length - 1] = m2 - result[result.length - 2];
        }
        return result;
    }

}
