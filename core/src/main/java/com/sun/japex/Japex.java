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

import java.io.*;
import java.text.*;
import java.util.Date;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.xml.bind.Marshaller;
import javax.xml.transform.*;
import javax.xml.transform.stream.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Top-level driver for Japex. This class class orchestrates running the benchmarks 
 * and building the reports of the results.<br/>
 * In normal operation, Japex builds class loaders for drivers from class paths specified 
 * in the XML configuration files. However, this class includes support for 'named class loaders'
 * that allow an outer framework to define class loaders. Use {@link #getNamedClasspaths()}
 * to obtain a Map&lt;String, ClassLoader&gt;, and class loaders registered here are available
 * to config files via japex.namedClassPath.
  */
public class Japex {
	private final static Logger LOG = LoggerFactory.getLogger(Japex.class);
    
    public static boolean html = true;
    public static boolean verbose = false;
    public static boolean silent = false;
    public static boolean resultPerLine = false;
    public static boolean test = false;
    public static boolean last = false;
    
    public static int exitCode = 0;
    
    public static Date TODAY = new Date();
    
    private Engine engine;
    
    public Japex() {
    	engine = new Engine();
    }

    /**
     * @return a map that defines named class loaders. Drivers may be defined in terms of these
     * loaders via japex.namedClassPath.
     */
    public Map<String, ClassLoader> getNamedClasspaths() {
    	return engine.getNamedClassPaths();
    }
    
    public void setHtml(boolean html) {
        Japex.html = html;
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            displayUsageAndExit();
        }

        // Parse command-line arguments
        boolean merge = false;
        List<String> configFiles = new ArrayList<String>();
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-nohtml")) {
                html = false;
            }
            else if (args[i].equals("-verbose")) {
                verbose = true;
            }
            else if (args[i].equals("-silent")) {
                silent = true;
            }
            else if (args[i].equals("-line")) {
                resultPerLine = true;
            }
            else if (args[i].equals("-test")) {
                test = true;
            }
            else if (args[i].equals("-last")) {
                last = true;
            }
            else if (args[i].equals("-merge")) {
                merge = true;
            }
            else {
                configFiles.add(args[i]);
            }
        }
        
        if (configFiles.isEmpty()) {
            displayUsageAndExit();
        }

        if (configFiles.size() > 1 && !merge) {
            displayUsageAndExit();            
        }
        
        new Japex().run(configFiles);
        
        System.exit(exitCode);
    }

    private static void displayUsageAndExit() {
        System.err.println(
            "Usage: japex [-verbose] [-silent] [-nohtml] [-line] [-test] [-merge] japex-config-file(s)\n" +
            "   -verbose: Display additional information about the benchmark's execution\n" +
            "   -silent : Do not display exceptions thrown by a driver\n" +
            "   -nohtml : Do not generate HTML report (only XML report)\n" +
            "   -line   : Insert additional newlines to separate test case results\n" +
            "   -test   : Test configuration file without producing any output reports\n" +
            "   -last   : Copy the report directory into a directory named 'last'\n" +
            "   -merge  : Merge japex-config-files\n" +
            "             An error will result if this option is absent and more than one\n" +
            "             japex-config-file is present"
                );
        System.exit(1);        
    }
    
    public void run(String configFile) {  
        run(Collections.singletonList(configFile));
    }
    
    public void run(List<String> configFiles) {  
        try {            
            // Create testsuite object from configuration file
            TestSuiteImpl testSuite = engine.start(configFiles);
            
            // If running in test mode, just return
            if (test) return;
            
            // Create report directory
            String fileSep = System.getProperty("file.separator");
            DateFormat df = new SimpleDateFormat("yyyy_MM_dd_HH_mm");
            String outputDir = testSuite.getParam(Constants.REPORTS_DIRECTORY) 
                + fileSep + df.format(TODAY);          
            String lastDir = testSuite.getParam(Constants.REPORTS_DIRECTORY) 
                + fileSep + "last";          

            // Generate report to string buffer
            StringBuffer report = new StringBuffer();
            testSuite.serialize(report);            

            // Output report to file
            new File(outputDir).mkdirs();
            LOG.info("Generating reports ...");
            LOG.info("  " + 
                new File(outputDir + "/" + "report.xml").toURI().toURL());
            OutputStreamWriter osw = new OutputStreamWriter(
                new FileOutputStream(
                    new File(outputDir + fileSep + "report.xml")));
            osw.write(report.toString());
            osw.close();
            
            // Marshall the test suite
            Marshaller m = ConfigFileLoader.context.createMarshaller();
            File configOutput = new File(outputDir + fileSep + testSuite.getParam(Constants.CONFIG_FILE));
            m.marshal(testSuite.getTestSuiteElement(), new FileOutputStream(configOutput));
            
            // Return if no HTML needs to be output
            if (!html) {
                if (last) {
                    Util.copyDirectory(new File(outputDir), new File(lastDir));
                }
                return;
            }
            
            // Generate charts
            final String resultChart = "result.jpg";
            ChartGenerator chartGenerator = new ChartGenerator(testSuite);
            chartGenerator.generateDriverChart(outputDir + fileSep 
                + resultChart);
            final String testCaseChartBase = "testcase";
            int nOfCharts = chartGenerator.generateTestCaseCharts(outputDir
                + fileSep + testCaseChartBase, ".jpg");
            
            // Extend report with chart info
            StringBuffer extendedReport = new StringBuffer();
            extendedReport.append("<extendedTestSuiteReport " + 
                "xmlns=\"http://www.sun.com/japex/extendedTestSuiteReport\">\n")
                .append(" <resultChart>" + resultChart + "</resultChart>\n");
            for (int i = 0; i < nOfCharts; i++) {
                extendedReport.append(" <testCaseChart>" + 
                    testCaseChartBase + i + ".jpg" + "</testCaseChart>\n");
            }
            extendedReport.append(report);
            extendedReport.append("</extendedTestSuiteReport>\n");

            // Generate HTML report
            URL stylesheet = getClass().getResource("/report.xsl");
            if (stylesheet != null) {
                TransformerFactory tf = TransformerFactory.newInstance();  
                Transformer transformer = tf.newTransformer(
                    new StreamSource(stylesheet.toExternalForm()));

                LOG.info("  " + 
                    new File(outputDir + "/" + "report.html").toURI().toURL());
                
                File htmlReport = new File(outputDir + fileSep + "report.html");
                transformer.transform(
                    new StreamSource(new StringReader(extendedReport.toString())),
                    new StreamResult(new FileOutputStream(htmlReport)));
                
                // For convenience copy report.html to index.html
                InputStream is = new BufferedInputStream(new FileInputStream(htmlReport));
                OutputStream os = new BufferedOutputStream(new FileOutputStream(
                                    new File(outputDir + fileSep + "index.html")));
                int c;
                while ((c = is.read()) != -1) {
                    os.write(c);
                }
                is.close();
                os.close();                    
              
                // Copy some resources to output directory
                Util.copyResource("report.css", outputDir, fileSep);
                Util.copyResource("small_japex.gif", outputDir, fileSep);
                
                // Copy report to 'last' if option specified
                if (last) {
                    Util.copyDirectory(new File(outputDir), new File(lastDir));
                }
            }
        }
        catch (RuntimeException e) {
            throw e;
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
}
