/*
 * Japex ver. 1.0 software ("Software")
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

import java.io.*;
import java.net.URL;
import javax.xml.transform.*;
import javax.xml.transform.stream.*;

public class RegressionTracker {
    
    static String outputDirectory;
    static String reportsDirectory;
    
    public RegressionTracker() {
    }
    
    public static void main(String[] args) {
        if (args.length < 1 || args.length > 4) {
            displayUsageAndExit();
        }

        // Parse command-line arguments
        String configFile = null;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-outputdir")) {
                outputDirectory = args[++i];
            }
            else {
                reportsDirectory = args[i];
            }
        }
        
        if (outputDirectory == null) {
            outputDirectory = System.getProperty("user.dir");
        }
        
        if (reportsDirectory == null) {
            displayUsageAndExit();
        }

        new RegressionTracker().run(outputDirectory, reportsDirectory);
    }

    private static void displayUsageAndExit() {
        System.err.println("Usage: regression-tracker [-outputdir dir] reports-directory");
        System.exit(1);        
    }
    
    public void run(String outputDirectory, String reportsDirectory) {  
        try {            
            String fileSep = System.getProperty("file.separator");

            File reportsDir = new File(reportsDirectory);
            if (!reportsDir.isDirectory() || !reportsDir.canWrite()) {
                System.err.println("Reports directory '" + reportsDirectory + "' is not a valid directory");
                System.exit(1);
            }
            
            File outputDir = new File(outputDirectory);
            if (!outputDir.exists()) {
                outputDir.mkdir();
            }
            if (!outputDir.isDirectory() || !outputDir.canWrite()) {
                System.err.println("Output directory '" + outputDirectory + "' is not a valid directory");
                System.exit(1);
            }
            
            String[] files = reportsDir.list(new FilenameFilter () { 
                public boolean accept(File f, String n) {
                    // Simple check for "????_??_??_??_??"
                    return n.length() == 16 && n.charAt(4) == '_' && n.charAt(7) == '_' 
                        && n.charAt(10) == '_' && n.charAt(13) == '_';
                }
            });

            if (files.length < 2) {
                System.err.println("Not enough reports in '" + reportsDirectory 
                    + "' to generate regression report");
                System.exit(1);
            }
            
            String outputReportXml = null;
            String lastReport = files[files.length - 2] + fileSep + "report.xml";
            String nextReport = files[files.length - 1] + fileSep + "report.xml";
            
            TransformerFactory tf = TransformerFactory.newInstance();        
            URL stylesheet = getClass().getResource("/resources/regression-report.xsl");
            if (stylesheet != null) {
                Transformer transformer = tf.newTransformer(
                    new StreamSource(stylesheet.toExternalForm()));

                System.out.println("Input reports: ");
                transformer.setParameter("lastReport", 
                    new URL("file", null, reportsDir.toString() + fileSep + lastReport).toExternalForm());
                transformer.setParameter("nextReport", 
                    new URL("file", null, reportsDir.toString() + fileSep + nextReport).toExternalForm());
                System.out.println("\t" + transformer.getParameter("lastReport"));                
                System.out.println("\t" + transformer.getParameter("nextReport"));
                                
                outputReportXml = outputDirectory + fileSep + "report.xml";
                System.out.println("Output reports: ");
                System.out.println("\t" + 
                    new URL("file", null, outputReportXml).toExternalForm());
                
                transformer.transform(
                    new StreamSource(stylesheet.toExternalForm()),      // unused
                    new StreamResult(new FileOutputStream(outputReportXml)));         
            }                
            
            // Now output report in HTML form
            stylesheet = getClass().getResource("/resources/regression-report-html.xsl");
            if (stylesheet != null) {
                Transformer transformer = tf.newTransformer(
                    new StreamSource(stylesheet.toExternalForm()));

                String outputReportHtml = outputDirectory + fileSep + "report.html";
                if (outputReportHtml != null) {
                    System.out.println("\t" + 
                        new URL("file", null, outputReportHtml).toExternalForm());
                }
                
                transformer.transform(
                    new StreamSource(new FileInputStream(outputReportXml)),
                    new StreamResult(new FileOutputStream(outputReportHtml)));            
            }
            
            // Copy some resources to output directory
            copyResource("report.css", outputDirectory, fileSep);
            copyResource("small_japex.gif", outputDirectory, fileSep);
        }
        catch (RuntimeException e) {
            throw e;
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    private void copyResource(String basename, String outputDir, String fileSep) {
        InputStream is;
        OutputStream os;
        
        try {
            int c;
            URL css = getClass().getResource("/resources/" + basename);
            if (css != null) {
                is = css.openStream();
                os = new BufferedOutputStream(new FileOutputStream(
                        new File(outputDir + fileSep + basename)));

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
