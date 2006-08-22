/*
 * Japex ver. 0.1 software ("Software")
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
import javax.xml.bind.*;
import com.sun.japex.testsuite.*;
import java.net.URI;
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
        Reader config = processor.process(new FileReader(fileName));

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
        testSuite.setParam(Constants.CONFIG_FILE, name);
        return testSuite;
    }
}
