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

import java.io.*;
import javax.xml.bind.*;
import com.sun.japex.testsuite.*;
import javax.xml.transform.sax.SAXSource;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

public class ConfigFileLoader {
    public static final JAXBContext context = getJABXContext();
    
    private static JAXBContext getJABXContext() {
        try {
            return JAXBContext.newInstance("com.sun.japex.testsuite");
        } catch(Exception e) {
            throw new RuntimeException("Japex cannot create JAXB context", e);
        }
    }
    
    TestSuiteImpl _testSuite;
    
    protected ConfigFileLoader() { }

    
    public ConfigFileLoader(String fileName) throws ConfigFileException {
        try {
            TestSuiteElement testSuiteElement = (TestSuiteElement)loadFile(fileName);
            
            _testSuite = createTestSuite(testSuiteElement, new File(fileName).getName());
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    public TestSuiteImpl getTestSuite() {
        return _testSuite;        
    }
    
    protected Object loadFile(String fileName) throws Exception {
        System.out.println("Reading configuration file '" + fileName + "' ...");

        // Run config file through conditional processor
        ConditionalProcessor processor = new ConditionalProcessor();
        Reader config = processor.process(fileName);

        // Get an XInclude aware XML reader
        XMLReader reader = Util.getXIncludeXMLReader();

        // Unmarshall using SAXSource to pass XInclude SAX parser
        Unmarshaller u = context.createUnmarshaller();
        InputSource is = new InputSource(config);
        is.setSystemId(fileName);       // Needed for XInclude to get base
        return u.unmarshal(new SAXSource(reader, is));
    }
    
    protected TestSuiteImpl createTestSuite(TestSuiteElement tse, String name) {
        // Map JAXB object model to internal object model
        TestSuiteImpl testSuite = new TestSuiteImpl(tse);         

        // Defined japex.configFile here
        if (testSuite.getParam(Constants.CONFIG_FILE) == null) {
            testSuite.setParam(Constants.CONFIG_FILE, name);
        }
        return testSuite;
    }
}
