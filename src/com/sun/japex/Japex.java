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

package com.sun.japex;

import com.sun.japex.testsuite.TestSuiteElement;
import java.io.*;
import java.text.*;
import java.util.Date;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.xml.bind.Marshaller;
import javax.xml.transform.*;
import javax.xml.transform.stream.*;

public class Japex {
    
    public static boolean html = true;
    public static boolean verbose = false;
    public static boolean silent = false;
    public static boolean resultPerLine = false;
    public static boolean test = false;
    public static boolean last = false;
    
    public static int exitCode = 0;
    
    public static Date TODAY = new Date();
    
    private static String identityTx = 
        "<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">" +
        "<xsl:template match=\"@*|node()\">" +
        "<xsl:copy><xsl:apply-templates select=\"@*|node()\"/></xsl:copy>" +
        "</xsl:template>" +
        "</xsl:stylesheet>";
    
    public Japex() {
    }
    
    public void setHtml(boolean html) {
        this.html = html;
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
        List<String> configFiles = new ArrayList();
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
            TestSuiteImpl testSuite = new Engine().start(configFiles);
            
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
            System.out.println("Generating reports ...");
            System.out.println("  " + 
                new File(outputDir + "/" + "report.xml").toURI().toURL());
            OutputStreamWriter osw = new OutputStreamWriter(
                new FileOutputStream(
                    new File(outputDir + fileSep + "report.xml")));
            osw.write(report.toString());
            osw.close();
            
            // Marshall the test suite
            TestSuiteElement testSuiteElement = testSuite.getTestSuiteElement();
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
            URL stylesheet = getClass().getResource("/resources/report.xsl");
            if (stylesheet != null) {
                TransformerFactory tf = TransformerFactory.newInstance();  
                Transformer transformer = tf.newTransformer(
                    new StreamSource(stylesheet.toExternalForm()));

                System.out.println("  " + 
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
