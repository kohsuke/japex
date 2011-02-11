/*
 * Japex software ("Software")
 *
 * Copyright, 2004-2009 Sun Microsystems, Inc. All Rights Reserved.
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

import org.junit.Before;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.AfterClass;

/**
 * <p>Implementation of a JUnit driver. The JUnit test class is
 * defined by the 'testName' parameter, which can be either a 
 * driver or testcase parameter; the method name is defined by 
 * the 'methodName' parameter which is always a testcase parameter.
 * 
 * This version support JUnit 4.X which requires 'testName' to
 * be a driver parameter. It supports the following annotations:
 * @BeforeClass, @AfterClass, @Before and @After.
 * These methods are called, respectively, from initializeDriver(),
 * terminateDriver(), prepare(TestCase) and finish(TestCase).</p>
 *
 * @author Sameer.Tyagi@sun.com
 * @author Smitha.Prabhu@sun.com
 * @author Farrukh.Najmi@sun.com
 * @author Santiago.PericasGeertsen@sun.com
 */
public class JUnitDriver extends JapexDriverBase {

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

    /**
     * JUnit test class loaded.
     */
    Class<?> _testClass;

    /**
     * Method in JUnit4 test class annotated by @BeforeClass.
     */
    Method _beforeClassMethod;

    /**
     * Method in JUnit4 test class annotated by @AfterClass.
     */
    Method _afterClassMethod;

    /**
     * Method in JUnit4 test class annotated by @Before.
     */
    Method _beforeMethod;

    /**
     * Method in JUnit4 test class annotated by @After.
     */
    Method _afterMethod;

    @Override
    public void initializeDriver() {
        super.initializeDriver();

        try {
            // Is testName specified as driver param?
            String testName = getParam("testName");
            if (testName != null) {
                _testClass = getClass().getClassLoader().loadClass(testName);

                // JUnit4 annotations @Before(Class) and @After(Class)
                findJUnit4Methods(_testClass);

                // Call @BeforeClass method now
                if (_beforeClassMethod != null) {
                    _beforeClassMethod.invoke(null);
                }
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void prepare(TestCase testCase) {
        try {
            String testName = testCase.getParam("testName");
            // If testName here, reload class for backward compatibility
            if (testName != null) {
                _testClass = getClass().getClassLoader().loadClass(testName);
            }

            _methodName = testCase.getParam("methodName");

            if (_methodName == null) {
                Method suiteMethod = _testClass.getMethod("suite", (Class[]) null);
                _testSuite = (junit.framework.Test)
                    suiteMethod.invoke((Object) null, (Object[]) null);
            }
            else {
                _method = _testClass.getMethod(_methodName, (Class[]) null);

                Constructor<?> con = null;
                try {
                    // Try <init>(String) first
                    con = _testClass.getConstructor(
                            new Class[] { new String().getClass() });
                } catch (NoSuchMethodException _) {
                    // falls through
                }
                if (con == null) {
                    try {
                        // Try <init>() default constructor
                        con = _testClass.getConstructor(new Class[] { });
                    } catch (NoSuchMethodException _) {
                        // falls through
                    }
                    if (con == null) {
                        throw new RuntimeException("Unable to find suitable " +
                            "constructor in class '" + _testClass.getName() + "'");
                    } else {
                        _object = con.newInstance(new Object[] { });
                    }
                } else {
                    _object = con.newInstance(new Object[] { testName });
                }
            }

            // Call @Before method now
            if (_beforeMethod != null) {
                _beforeMethod.invoke(_object);
            }
        }
        catch (RuntimeException e) {
            throw e;
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
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

    @Override
    public void finish(TestCase testCase) {
        super.finish(testCase);

        try {
            // Call @After method now
            if (_afterMethod != null) {
                _afterMethod.invoke(_object);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void terminateDriver() {
        super.terminateDriver();

        try {
            // Call @AfterClass method now
            if (_afterClassMethod != null) {
                _afterClassMethod.invoke(null);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void findJUnit4Methods(Class<?> testClass) {
        try {
            int notFound = 4;       // # of methods to find

            do {
                Method[] mm = testClass.getDeclaredMethods();
                for (int i = 0; i < mm.length; i++) {
                    if (_afterMethod == null) {
                        After after = mm[i].getAnnotation(After.class);
                        if (after != null) {
                            _afterMethod = mm[i];
                            notFound--;
                        }
                    }
                    if (_beforeMethod == null) {
                        Before before = mm[i].getAnnotation(Before.class);
                        if (before != null) {
                            _beforeMethod = mm[i];
                            notFound--;
                        }
                    }
                    if (_afterClassMethod == null) {
                        AfterClass afterClass = mm[i].getAnnotation(AfterClass.class);
                        if (afterClass != null) {
                            _afterClassMethod = mm[i];
                            notFound--;
                        }
                    }
                    if (_beforeClassMethod == null) {
                        BeforeClass beforeClass = mm[i].getAnnotation(BeforeClass.class);
                        if (beforeClass != null) {
                            _beforeClassMethod = mm[i];
                            notFound--;
                        }
                    }
                }
                // Now lets inspect super class
                testClass = testClass.getSuperclass();
            } while (notFound > 0 && testClass != Object.class);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
