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
            URL css = getClass().getResource("/resources/" + basename);
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
