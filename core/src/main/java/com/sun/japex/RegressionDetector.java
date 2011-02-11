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

import javax.xml.namespace.NamespaceContext;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Result;
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
     * Threshold to send e-mail notifications. If the delta of arithmetic
     * mean (of any driver) is greater than the threshold, an e-mail will be
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
                public Iterator<String> getPrefixes(String namespaceURI) { return null; }
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
            stylesheet = RegressionDetector.class.getResource("/regression-report.xsl");
            REGRESSION_REPORT = tf.newTemplates(
                    new StreamSource(stylesheet.toExternalForm()));

            stylesheet = RegressionDetector.class.getResource("/regression-report-html.xsl");
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

            transformer.setParameter("threshold", Double.toString(threshold));
            transformer.setParameter("lastReport", oldReport.toURI().toURL().toExternalForm());
            transformer.setParameter("nextReport", newReport.toURI().toURL().toExternalForm());
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
    public void generateHtmlReport(File xmlReport, File htmlReport) throws IOException {
        generateHtmlReport(new StreamSource(xmlReport),new StreamResult(htmlReport));
    }

    /**
     * Generates an HTML report from an XML report.
     */
    public void generateHtmlReport(Source xmlReport, Result htmlReport) throws IOException {
        try {
            Transformer transformer = REGRESSION_REPORT_HTML.newTransformer();

            transformer.transform( xmlReport, htmlReport);
        } catch (TransformerException e) {
            throw new RuntimeException(e);
        }
    }
}
