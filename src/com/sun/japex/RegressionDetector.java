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

import javax.xml.namespace.NamespaceContext;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;

/**
 * Computes XML/HTML that compares two runs.
 *
 * <h2>Usage</h2>
 * <ol>
 *  <li>Set a bunch of properties. {@link #setOldReport(File)} and {@link #setNewReport(File)}
 *      are mandatory.
 *  <li>Call one of the <tt>generate</tt> methods.
 *  <li>Use {@link #checkThreshold(Source)}
 */
public class RegressionDetector {

    /**
     * Threshold to send e-mail notifications. If the delta of any mean
     * (of any driver) is greater than the threshold, an e-mail will be
     * sent to the list of recipients.
     */
    private double threshold = -1.0;

    /**
     * Base URL used to include live links in regression report. This URL
     * is prepended to the a report's relative URL.
     */
    private URL baseURL;

    /**
     * Two reports to compare against.
     * We check if the new report looks much worse than the old one.
     */
    private File oldReport,newReport;

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    public void setBaseURL(URL baseURL) {
        this.baseURL = baseURL;
    }

    public void setOldReport(File oldReport) {
        this.oldReport = oldReport;
    }

    public void setNewReport(File newReport) {
        this.newReport = newReport;
    }

    public boolean checkThreshold(Source xmlReport) {
        try {
            DOMResult dom = new DOMResult();
            tf.newTransformer().transform(xmlReport,dom);

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
                    dom.getNode(), XPathConstants.STRING);

            // is notification needed?
            return o.equals("true");
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static final TransformerFactory tf = TransformerFactory.newInstance();
    private static final Templates REGRESSION_REPORT, REGRESSION_REPORT_HTML;
    static {
        try {
            URL stylesheet;
            stylesheet = RegressionDetector.class.getResource("/resources/regression-report.xsl");
            REGRESSION_REPORT = tf.newTemplates(
                    new StreamSource(stylesheet.toExternalForm()));

            stylesheet = RegressionDetector.class.getResource("/resources/regression-report-html.xsl");
            REGRESSION_REPORT_HTML = tf.newTemplates(
                    new StreamSource(stylesheet.toExternalForm()));
        } catch (TransformerConfigurationException e) {
            throw new Error(e); // this must be a deployment error
        }
    }

    /**
     * Generates XML report file.
     */
    public void generateXmlReport(File outputReportXmlFile) throws IOException {
        try {
            Transformer transformer = REGRESSION_REPORT.newTransformer();

            System.out.println("Input reports: ");
            transformer.setParameter("threshold", Double.toString(threshold));
            transformer.setParameter("lastReport", oldReport.toURL().toExternalForm());
            transformer.setParameter("nextReport", newReport.toURL().toExternalForm());
            if (baseURL != null) {
                transformer.setParameter("lastReportHref",getHref(oldReport));
                transformer.setParameter("nextReportHref",getHref(newReport));
            }

            transformer.transform(
                    new StreamSource(new StringReader("<foo/>")),      // unused
                    new StreamResult(outputReportXmlFile));
        } catch (TransformerException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Computes the URL to the report online.
     */
    private String getHref(File reportXml) throws MalformedURLException {
        String fileName = reportXml.getPath();
        String href = fileName.substring(0, fileName.lastIndexOf('.')) + ".html";
        return new URL(baseURL,href).toExternalForm();
    }

    /**
     * Generates an HTML report from an XML report.
     */
    public void generateHtmlReport(Source xmlReport, File htmlReport) throws IOException {
        try {
            Transformer transformer = REGRESSION_REPORT_HTML.newTransformer();

            transformer.transform( xmlReport, new StreamResult(htmlReport));
        } catch (TransformerException e) {
            throw new RuntimeException(e);
        }
    }
}
