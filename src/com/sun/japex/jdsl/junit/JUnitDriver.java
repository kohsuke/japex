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

package com.sun.japex.jdsl.junit;

import java.lang.reflect.Method;
import java.lang.reflect.Constructor;

import com.sun.japex.JapexDriverBase;
import com.sun.japex.TestCase;
import junit.framework.Test;
import junit.textui.TestRunner;     

/**
 *
 * @author Sameer.Tyagi@sun.com
 * @author Smitha.Prabhu@sun.com
 * @author Farrukh.Najmi@sun.com
 * @author Santiago.PericasGeertsen@sun.com
 */
public class JUnitDriver extends JapexDriverBase {

    /**
     * The name of the Junit test. It is set as a test case
     * parameter in the Japex configuration file. 
     */
    String _testName;

    /**
     * The name of the method in the JUnit test program. It is
     * set as a test case parameter in the Japex configuration
     * file. 
     */
    String _methodName;

    /**
     * JUnit test suite to run when no methodName is specified.
     */
    Test _testSuite;
    
    /**
     * Object instance to invoke _method when methodName is
     * specified.
     */
    Object _object;
    
    /**
     * Method instance to invoke on _object when methodName is
     * specified.
     */
    Method _method;
    
    public void prepare(TestCase testCase) {
        try {
            _testName = testCase.getParam("testName");
            _methodName = testCase.getParam("methodName");
        
            // Use class loader that loaded this class
            Class testClass = getClass().getClassLoader().loadClass(_testName);
            
            if (_methodName == null) {
                Method suiteMethod = testClass.getMethod("suite", (Class[]) null);
                _testSuite = (junit.framework.Test) 
                    suiteMethod.invoke((Object) null, (Object[]) null);
            } 
            else {
                _method = testClass.getMethod(_methodName, (Class[]) null);
                Constructor con = 
                    testClass.getConstructor(new Class[] { new String().getClass() });
                _object = con.newInstance(new Object[] { _testName });
            }
        }
        catch (RuntimeException e) {
            throw e;
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    public void run(TestCase testCase) {
        try {
            if (_methodName == null) {
                // Run entire testSuite for Junit test class
                TestRunner.run(_testSuite);
            } 
            else {
                // The methodName is specified, run specified method in JUnit test
                _method.invoke(_object, (Object[]) null);
            }
        }
        catch (RuntimeException e) {
            throw e;
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }    
}
