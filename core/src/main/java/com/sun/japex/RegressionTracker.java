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

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.xml.transform.stream.StreamSource;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Date;
import java.util.Properties;

/**
 * CLI for {@link RegressionDetector}.
 */
public class RegressionTracker {
    /**
     * Directory where to output XML and HTML reports.
     */
    private File outputDirectory;

    /**
     * Directory where the Japex reports are found. Only the last two
     * (based on by their timestamp) are consider to produce the reports.
     */
    private File reportsDirectory;

    private final RegressionDetector detector = new RegressionDetector();

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
            for (i = 0; i < args.length; i++) {
                if (args[i].equals("-threshold")) {
                    detector.setThreshold(Double.parseDouble(args[++i]));
                }
                else if (args[i].equals("-baseurl")) {
                    detector.setBaseURL(new URL(args[++i]));
                }
                else {
                    break;
                }
            }

            if (i != args.length - 2) {
                displayUsageAndExit();
            }

            reportsDirectory = new File(args[args.length - 2]);
            outputDirectory = new File(args[args.length - 1]);
        } catch (NumberFormatException e) {
            displayUsageAndExit();
        } catch (ArrayIndexOutOfBoundsException e) {
            displayUsageAndExit();
        } catch (MalformedURLException e) {
            displayUsageAndExit();
        }
    }

    public void run(String[] args) {
        try {
            parseCommandLine(args);

            if (!reportsDirectory.isDirectory() || !reportsDirectory.canWrite()) {
                System.err.println("Error: Reports directory '" + reportsDirectory
                    + "' is not a valid directory");
                System.exit(1);
            }

            if (!outputDirectory.exists()) {
                outputDirectory.mkdir();
            }
            if (!outputDirectory.isDirectory() || !outputDirectory.canWrite()) {
                System.err.println("Error: Output directory '" + outputDirectory
                    + "' is not a valid directory");
                System.exit(1);
            }

            String[] files = reportsDirectory.list(new FilenameFilter() {
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

            // Sort directories to ensure proper ordering
            Arrays.sort(files);

            File lastReport = new File(new File(reportsDirectory,files[files.length - 2]),"report.xml");
            File nextReport = new File(new File(reportsDirectory,files[files.length - 1]),"report.xml");

            detector.setOldReport(lastReport);
            detector.setNewReport(nextReport);

            System.out.println("Input reports: ");
            File outputReportXml = new File(outputDirectory,"report.xml");
            System.out.println("\t" + lastReport.toURI().toURL());
            System.out.println("\t" + nextReport.toURI().toURL());

            System.out.println("Output reports: ");
            System.out.println("\t" + outputReportXml.toURI().toURL());

            detector.generateXmlReport(outputReportXml);

            File outputReportHtml = new File(outputDirectory,"report.html");
            System.out.println("\t" + outputReportHtml.toURI().toURL());

            detector.generateHtmlReport(outputReportXml,outputReportHtml);

            // Copy some resources to output directory
            copyResource("report.css", outputDirectory);
            copyResource("small_japex.gif", outputDirectory);

            // Send e-mail notifications if necessary
            if(detector.checkThreshold(new StreamSource(outputReportXml)))
                sendMail(outputReportHtml);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void sendMail(File htmlReport) throws IOException {
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

    private void copyResource(String basename, File outputDir) {
        InputStream is;
        OutputStream os;

        try {
            int c;
            URL css = getClass().getResource("/" + basename);
            if (css != null) {
                is = css.openStream();
                os = new BufferedOutputStream(new FileOutputStream(
                        new File(outputDir,basename)));

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
}
