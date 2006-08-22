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

import com.sun.japex.testsuite.DriverElement;
import com.sun.japex.testsuite.DriverGroupElement;
import com.sun.japex.testsuite.ParamElement;
import com.sun.japex.testsuite.ParamGroupElement;
import com.sun.japex.testsuite.TestCaseElement;
import com.sun.japex.testsuite.TestCaseGroupElement;
import com.sun.japex.testsuite.TestSuiteElement;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Create an instance of {@link TestSuite} by merging configuration files.
 * <p>
 * @author Paul.Sandoz@Sun.Com
 */
public class ConfigFileMerger extends ConfigFileLoader {
    
    public ConfigFileMerger(List<String> fileNames) throws ConfigFileException {
        TestSuiteElement testSuiteElement = null;
        String testSuiteName = "";
        
        List objects = new ArrayList();
        for (String fileName : fileNames) {
            try {
                if (testSuiteName != "") testSuiteName += ",";
                testSuiteName += new File(fileName).getName();
                
                Object o = loadFile(fileName);
                if (o instanceof TestSuiteElement) {
                    if (testSuiteElement != null)
                        merge(testSuiteElement, o);
                    else
                        testSuiteElement = (TestSuiteElement)o;
                } else {
                    objects.add(o);
                }
            } catch(Exception e) {
                throw new RuntimeException(e);
            }            
        }

        // If the "testSuite" is not present create a default instance
        if (testSuiteElement == null) {
            testSuiteElement = new TestSuiteElement();
            testSuiteElement.setName("Merged Test Suite");
        }
        
        // Add other objects after the test suite(s)
        for (Object o : objects) {
            merge(testSuiteElement, o);
        }
        
        _testSuite = createTestSuite(testSuiteElement, testSuiteName);
    }
    
    private void merge(TestSuiteElement to, Object o) {
        if (o instanceof TestSuiteElement) {
            TestSuiteElement from = (TestSuiteElement)o;
            appendTestSuiteName(to, from.getName());
            
            for(Object fromo : from.getParamOrParamGroup())
                to.getParamOrParamGroup().add(fromo);
            for(Object fromo : from.getTestCaseOrTestCaseGroup())
                to.getTestCaseOrTestCaseGroup().add(fromo);
            for(Object fromo : from.getDriverOrDriverGroup())
                to.getDriverOrDriverGroup().add(fromo);
        } else if (o instanceof ParamElement || o instanceof ParamGroupElement) {
            to.getParamOrParamGroup().add(o);
            
            if (o instanceof ParamGroupElement)
                appendTestSuiteName(to, ((ParamGroupElement)o).getName());
        } else if (o instanceof TestCaseElement || o instanceof TestCaseGroupElement) {
            to.getTestCaseOrTestCaseGroup().add(o);
            
            if (o instanceof TestCaseGroupElement)
                appendTestSuiteName(to, ((TestCaseGroupElement)o).getName());
        } else if (o instanceof DriverElement || o instanceof DriverGroupElement) {
            to.getDriverOrDriverGroup().add(o);
            
            if (o instanceof DriverGroupElement)
                appendTestSuiteName(to, ((DriverGroupElement)o).getName());
        }
    }
    
    private void appendTestSuiteName(TestSuiteElement to, String name) {
        if (name != null && name.length() > 0)
            to.setName(to.getName() + ", " + name);
    }
}