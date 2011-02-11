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
        
        List<Object> objects = new ArrayList<Object>();
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