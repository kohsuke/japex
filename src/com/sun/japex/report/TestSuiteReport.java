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
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.io.IOException;
import java.util.Date;
import java.util.Calendar;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import javax.xml.parsers.*;
import org.xml.sax.SAXException;
import org.w3c.dom.*;

import static com.sun.japex.Constants.DATE_TIME;
import static com.sun.japex.Constants.DEFAULT_DATE_TIME_FORMAT;

/**
 * Representation of a test suite report.
 *
 * @author Santiago.PericasGeertsen@sun.com
 * @author Kohsuke.Kawaguchi@sun.com
 *
 */
public class TestSuiteReport implements Comparable<TestSuiteReport> {

    final static DocumentBuilderFactory dbf;
    final static String TESTSUITE_REPORT_URI 
            = "http://www.sun.com/japex/testSuiteReport";
    
    static {
        dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
    }
            
    Document _rep;    
    ArrayList<Driver> _drivers = new ArrayList<Driver>();
    HashMap<String, String> _params = new HashMap<String, String>();
    
    Calendar _date = Calendar.getInstance();
    
    /**
     * Parse a {@link TestReport} from a file.
     */
    public TestSuiteReport(File src) throws SAXException, IOException {
        if (!src.exists()) {
            throw new IllegalArgumentException("File '" + src + "' does not exist");       
        }
        
        try {
            // Parse report into a DOM
            _rep = dbf.newDocumentBuilder().parse(src);            
        }
        catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
        
        // Build internal representation for this report
        NodeList list = _rep.getDocumentElement().getChildNodes();            
        for (int i = 0; i < list.getLength(); i++) {
            Node n = list.item(i);                
            if (n instanceof Element) {
                Element e = (Element) n;
                if (e.getLocalName().equals("driver") && e.hasAttribute("name")) {
                    _drivers.add(new Driver(e));
                }
                else {
                    _params.put(e.getLocalName(), 
                                e.getFirstChild().getNodeValue());
                }
            }
        }
        
        SimpleDateFormat format = new SimpleDateFormat(DEFAULT_DATE_TIME_FORMAT);
        try {
            // Get global param dateTime from report
            Date d = format.parse(_params.get("dateTime"));
            _date.setTime(d);
        }
        catch (ParseException e) {
            throw new RuntimeException(e);
        }        
    }

    public Calendar getDate() {
        return _date;
    }
    
    public int compareTo(TestSuiteReport other) {
        return _date.compareTo(other._date);
    }
    
    /**
     * Returns all the global parameters.
     *
     * This kind of Java-friendly binding is nice, though not required.
     */
    public Map<String,String> getParameters() {
        return _params;
    }

    public List<Driver> getDrivers() {
        return _drivers;
    }
    
    public Driver getDriver(String name) {
        for (Driver driver : _drivers) {
            if (driver.getName().equals(name)) {
                return driver;
            }
        }
        return null;
    }

    public class Driver {

        Element _rep;        
        HashMap<String, String> _params = new HashMap<String, String>();
        ArrayList<TestCase> _testCases = new ArrayList<TestCase>();
        
        public Driver(Element rep) {
            _rep = rep;
            
            NodeList list = _rep.getChildNodes();            
            for (int i = 0; i < list.getLength(); i++) {
                Node n = list.item(i);                
                if (n instanceof Element) {
                    Element e = (Element) n;
                    if (e.getLocalName().equals("testCase") && e.hasAttribute("name")) {
                        _testCases.add(new TestCase(e));
                    }
                    else {
                        _params.put(e.getLocalName(), 
                                    e.getFirstChild().getNodeValue());                        
                    }
                }
            }
        }
        
        public Map<String,String> getParameters() {
            return _params;
        }
        
        public String getName() { 
            return _rep.getAttribute("name"); 
        }
        
        public double getResult(MeanMode m) {
            switch (m) {
                case ARITHMETIC:
                    return getElementValueAsDouble(_rep, TESTSUITE_REPORT_URI,
                            "resultAritMean");
                case GEOMETRIC:
                    return getElementValueAsDouble(_rep, TESTSUITE_REPORT_URI,
                            "resultGeomMean");
                case HARMONIC:
                    return getElementValueAsDouble(_rep, TESTSUITE_REPORT_URI,
                            "resultHarmMean");
            }
            return Double.NaN;
        }
        
        public TestCase getTestCase(String name) {
            for (TestCase testCase : _testCases) {
                if (testCase.getName().equals(name)) {
                    return testCase;
                }
            }
            return null;
        }
        
        public List<TestCase> getTestCases() {
            return _testCases;
        }
    }

    public class TestCase {
        
        Element _rep;
        
        public TestCase(Element rep) {
            _rep = rep;
        }
        
        public String getName() { 
            return _rep.getAttribute("name"); 
        }
        
        public double getResult() { 
            return getElementValueAsDouble(_rep, TESTSUITE_REPORT_URI,
                    "resultValue");
        }
    }
    
    static double getElementValueAsDouble(Element parent, String uri, 
            String localName) 
    {
        try {
            return Double.parseDouble(
                    parent.getElementsByTagNameNS(uri, localName)
                        .item(0).getFirstChild().getNodeValue());
        }
        catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

}
