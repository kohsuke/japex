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

import java.io.File;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import org.xml.sax.SAXException;
import java.net.URL;
import java.util.List;
import java.util.ArrayList;

/**
 * Trend report tool.
 *
 * @author Santiago.PericasGeertsen@sun.com
 * @author Joe.Wang@sun.com
 */
public class TrendReport {
    
    public static final String FILE_SEP = System.getProperty("file.separator");
    
    List<TestSuiteReport> _reports = new ArrayList<TestSuiteReport>();
    
    public static void main(String[] args) {
        TrendReportParams params = new TrendReportParams(args);
        new TrendReport().run(params);            
        System.exit(0);        
    }
    
    public void run(TrendReportParams params) {       
        try {
            // Parse reports based on filter options            
            File cwd = new File(params.reportPath());
            ReportFilter filter = new ReportFilter(params.dateFrom(), params.dateTo());
            File[] reportDirs = cwd.listFiles(filter);
            
            // If we haven't found any reports then exit
            if (reportDirs == null) {
                System.err.println("Error: No Japex reports found in '"
                        + params.reportPath() + "' between '" 
                        + params.dateFrom().getTime() + "' and '" 
                        + params.dateTo().getTime() + "'");
                System.exit(1);
            }
        
            // Create list of TestSuiteReport instances
            for (File dir : reportDirs) {
                File report = new File(dir.getAbsolutePath() + FILE_SEP 
                        + "report.xml");

                try {
                    _reports.add(new TestSuiteReport(report));                
                }
                catch (SAXException e) {
                    System.err.println("Warning: Skipping malformed test suite report '" 
                            + report.toString() + "'");
                }
                catch (IOException e) {
                    System.err.println("Warning: Skipping unreadable test suite report '" 
                            + report.toString() + "'");
                }
            }                                    
                    
            // Create generator and produce report
            new ReportGenerator(params, _reports).createReport();            
            
            // Copy some resources to output directory
            copyResource("report.css", params.outputPath());
            copyResource("small_japex.gif", params.outputPath());
        }            
        catch (RuntimeException e) {
            throw e;
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void copyResource(String basename, String outputDir) {
        InputStream is;
        OutputStream os;
        
        try {
            int c;
            URL css = getClass().getResource("/" + basename);
            if (css != null) {
                is = css.openStream();
                os = new BufferedOutputStream(new FileOutputStream(
                        new File(outputDir + FILE_SEP + basename)));
                
                while ((c = is.read()) != -1) {
                    os.write(c);
                }
                is.close();
                os.close();
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
