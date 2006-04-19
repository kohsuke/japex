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
import javax.mail.*;
import javax.mail.internet.*;
import java.util.Iterator;
import java.util.Properties;
import java.util.Date;
import javax.xml.xpath.*;
import javax.xml.namespace.NamespaceContext;
import org.xml.sax.InputSource;

public class RegressionTracker {
    
    /**
     * Directory where to output XML and HTML reports.
     */
    String outputDirectory;
    
    /**
     * Directory where the Japex reports are found. Only the last two
     * (based on by their timestamp) are consider to produce the reports.
     */
    String reportsDirectory;
    
    /**
     * Threshold to send e-mail notifications. If the delta of any mean
     * (of any driver) is greater than the threshold, an e-mail will be
     * sent to the list of recipients.
     */
    double threshold = -1.0;
    
    /**
     * Base URL used to include live links in regression report. This URL
     * is prepended to the a report's relative URL.
     */
    String baseURL;
    
    public RegressionTracker() {
    }
    
    public static void main(String[] args) {
        new RegressionTracker().run(args);
    }
    
    private static void displayUsageAndExit() {
        System.err.println("Usage: regression-tracker [-threshold percentage] " +
                "[-baseurl URL] reports-directory output-directory");
        System.exit(1);
    }
    
    public void parseCommandLine(String[] args) {
        try {
            if (args.length < 2 || args.length > 6) {
                displayUsageAndExit();
            }
            
            // Parse command-line arguments
            int i;
            String configFile = null;
            for (i = 0; i < args.length; i++) {
                if (args[i].equals("-threshold")) {
                    threshold = Double.parseDouble(args[++i]);
                } 
                else if (args[i].equals("-baseurl")) {
                    baseURL = args[++i];
                } 
                else {
                    break;
                }
            }
            
            if (i != args.length - 2) {
                displayUsageAndExit();
            }
            
            reportsDirectory = args[args.length - 2];
            outputDirectory = args[args.length - 1];
        } 
        catch (NumberFormatException e) {
            displayUsageAndExit();
        } catch (ArrayIndexOutOfBoundsException e) {
            displayUsageAndExit();
        }
    }
    
    public void run(String[] args) {
        try {
            parseCommandLine(args);
            
            String fileSep = System.getProperty("file.separator");
            
            File reportsDir = new File(reportsDirectory);
            if (!reportsDir.isDirectory() || !reportsDir.canWrite()) {
                System.err.println("Error: Reports directory '" + reportsDirectory 
                    + "' is not a valid directory");
                System.exit(1);
            }
            
            File outputDir = new File(outputDirectory);
            if (!outputDir.exists()) {
                outputDir.mkdir();
            }
            if (!outputDir.isDirectory() || !outputDir.canWrite()) {
                System.err.println("Error: Output directory '" + outputDirectory 
                    + "' is not a valid directory");
                System.exit(1);
            }
            
            String[] files = reportsDir.list(new FilenameFilter() {
                public boolean accept(File f, String n) {
                    // Simple check for "????_??_??_??_??"
                    return n.length() == 16 && n.charAt(4) == '_' && n.charAt(7) == '_'
                            && n.charAt(10) == '_' && n.charAt(13) == '_';
                }
            });
            
            if (files.length < 2) {
                System.err.println("Error: Not enough reports in '" + reportsDirectory
                        + "' to generate regression report");
                System.exit(1);
            }
            
            String outputReportXml = null;
            String outputReportHtml = null;
            
            String lastReport = reportsDir + fileSep + files[files.length - 2] 
                    + fileSep + "report.xml";
            String nextReport = reportsDir + fileSep + files[files.length - 1] 
                    + fileSep + "report.xml";
            
            File lastReportFile = new File(lastReport);
            File nextReportFile = new File(nextReport);
            
            TransformerFactory tf = TransformerFactory.newInstance();
            URL stylesheet = getClass().getResource("/resources/regression-report.xsl");
            Transformer transformer = tf.newTransformer(
                    new StreamSource(stylesheet.toExternalForm()));
            
            System.out.println("Input reports: ");
            transformer.setParameter("threshold", Double.toString(threshold));
            transformer.setParameter("lastReport",
                    new URL("file", null, lastReportFile.getAbsolutePath()).toExternalForm());
            transformer.setParameter("nextReport",
                    new URL("file", null, nextReportFile.getAbsolutePath()).toExternalForm());
            if (baseURL != null) {
                String lastReportHtml = 
                    lastReport.substring(0, lastReport.lastIndexOf('.')) + ".html";
                String nextReportHtml = 
                    nextReport.substring(0, nextReport.lastIndexOf('.')) + ".html";
                
                transformer.setParameter("lastReportHref", 
                        new URL(baseURL + lastReportHtml).toExternalForm());
                transformer.setParameter("nextReportHref",
                        new URL(baseURL + nextReportHtml).toExternalForm());
            }
            
            System.out.println("\t" + transformer.getParameter("lastReport"));
            System.out.println("\t" + transformer.getParameter("nextReport"));
            
            outputReportXml = outputDirectory + fileSep + "report.xml";
            File outputReportXmlFile = new File(outputReportXml);
            System.out.println("Output reports: ");
            System.out.println("\t" +
                    new URL("file", null, outputReportXmlFile.getAbsolutePath()).toExternalForm());
            
            transformer.transform(
                    new StreamSource(stylesheet.toExternalForm()),      // unused
                    new StreamResult(new FileOutputStream(outputReportXml)));
            
            // Now output report in HTML form
            stylesheet = getClass().getResource("/resources/regression-report-html.xsl");
            transformer = tf.newTransformer(
                    new StreamSource(stylesheet.toExternalForm()));
            
            outputReportHtml = outputDirectory + fileSep + "report.html";
            File outputReportHtmlFile = new File(outputReportHtml);
            System.out.println("\t" +  new URL("file", null, 
                    outputReportHtmlFile.getAbsolutePath()).toExternalForm());
            
            transformer.transform(
                    new StreamSource(new FileInputStream(outputReportXml)),
                    new StreamResult(new FileOutputStream(outputReportHtml)));
            
            // Copy some resources to output directory
            copyResource("report.css", outputDirectory, fileSep);
            copyResource("small_japex.gif", outputDirectory, fileSep);
            
            // Send e-mail notifications if necessary
            checkThresholdAndNotify(outputReportXml, outputReportHtml);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    private void checkThresholdAndNotify(String xmlReport, String htmlReport) {
        try {
            XPath query = XPathFactory.newInstance().newXPath();
            
            query.setNamespaceContext(new NamespaceContext() {
                public String getNamespaceURI(String prefix) {
                    return prefix.equals("reg") ? "http://www.sun.com/japex/regressionReport" : null;
                }
                public String getPrefix(String namespaceURI) { return null; }
                public Iterator getPrefixes(String namespaceURI) { return null; }
            });
            
            // Find the value of the notify attribute
            Object o = query.evaluate("/reg:regressionReport/@notify",
                    new InputSource(xmlReport), XPathConstants.STRING);
            
            // If notification needed, then send e-mail message
            if (((String) o).equals("true")) {
                
                // Read html report file into a String
                int c;
                StringBuffer htmlBuffer = new StringBuffer();
                FileReader reader = new FileReader(htmlReport);
                while ((c = reader.read()) != -1) {
                    htmlBuffer.append((char) c);
                }
                
                // Use JavaMail to send e-mail notification
                sendMail(htmlBuffer.toString());
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    private static String checkProperty(String name, boolean error) {
        String value = System.getProperty(name);
        if (value == null) {
            System.err.println((error ? "Error: " : "Warning: ") +
                    "Property '" + name + "' not set");
            if (error) {
                System.exit(1);
            }
        }
        return value;
    }
    
    /**
     * Must defined mail.smtp.host as a system property
     */
    private static void sendMail(String body) {
        try {
            Properties props = System.getProperties();
            
            String host = checkProperty("mail.smtp.host", true);
            String recipients = checkProperty("mail.recipients", true);
            String subject = checkProperty("mail.subject", false);
            
            props.put("mail.smtp.host", host);
            Session session = Session.getDefaultInstance(props, null);
            
            Message msg = new MimeMessage(session);
            msg.setRecipients(Message.RecipientType.TO,
                    InternetAddress.parse(recipients, false));
            msg.setSubject(subject);
            msg.setText(body);
            msg.setHeader("Content-Type", "text/html");
            msg.setSentDate(new Date());
            Transport.send(msg);
            
            System.out.println("E-mail message sent to '" + recipients + "'");
        } catch (Exception ex) {
            ex.printStackTrace();
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
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
}
