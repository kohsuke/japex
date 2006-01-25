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
            _testName = testCase.getParam("testName").intern();
            _methodName = testCase.getParam("methodName").intern();
        
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
