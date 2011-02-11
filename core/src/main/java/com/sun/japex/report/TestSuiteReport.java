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

import static com.sun.japex.Constants.DEFAULT_DATE_TIME_FORMAT;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
     * Parse a {@link TestSuiteReport} from a file.
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
        
        SimpleDateFormat format = new SimpleDateFormat(DEFAULT_DATE_TIME_FORMAT, Locale.ENGLISH);
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
