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

package com.sun.japex.jdsl.nativecode;

import com.sun.japex.*;
import com.sun.japex.Constants;
import com.sun.japex.Japex;
import com.sun.japex.JapexDriverBase;
import com.sun.japex.TestCase;
import com.sun.japex.TestCaseImpl;
import com.sun.japex.Util;
import java.io.File;

/**
 *
 * @author Paul.Sandoz@sun.com
 * @author Santiago.PericasGeertsen@sun.com
 */
public class JapexNativeDriver extends JapexDriverBase {
    
    public void setDriver(Driver driver) {
        super.setDriver(driver);
        
        String path = driver.getParam("libraryPath");
        String library = driver.getParam("libraryName");

        if (library == null) {
            throw new RuntimeException("JavaNativeDriver requires setting " +
                    "parameter 'libraryName'");            
        }
        
        /*
         * If no library path specified, use loadLibrary() which in 
         * turn uses the system property java.library.path. Otherwise
         * map name and load library from a file.
         */
        if (path == null) {       
            System.loadLibrary(library);
        }
        else {
            File file = new File(path + File.separator
                    + System.mapLibraryName(library));
            System.load(file.getAbsolutePath());
        }
    }
    
    /**
     * Execute the run phase. This method can be executed concurrently
     * by multiple threads. Care should be taken to ensure proper
     * synchronization. Note that parameter getters and setters are
     * already synchronized.
     *
     * This method defers to simple native methods that perform the
     * work of the loop (for time or for iterations).
     */
    public void run() {
        if (Japex.verbose) {
            System.out.println("             " + 
                Thread.currentThread().getName() + " run()"); 
        }

        TestCaseImpl tc = _testCase;
        
        double millis, startTime, duration;        
        int runIterations = 0;
        
        if (tc.hasParam(Constants.RUN_TIME)) {
            /*
             * Compute duration by substracting current time from _endTime.
             * This is needed to ensure that the Java and native drivers 
             * run for the same period of time, i.e. until _endTime.
             */             
            duration = _endTime - Util.currentTimeMillis();
            runIterations = runLoopDuration(duration, _userData);        
        }
        else {
            runIterations = tc.getIntParam(Constants.RUN_ITERATIONS);
            startTime = Util.currentTimeMillis();
	    runLoopIterations(runIterations, _userData);
            duration = Util.currentTimeMillis() - startTime;            
        }        
        
        // Accumulate actual number of iterations
        synchronized (tc) {
            long runIterationsSum =  
                tc.hasParam(Constants.RUN_ITERATIONS_SUM) ? 
                    tc.getLongParam(Constants.RUN_ITERATIONS_SUM) : 0L;
            tc.setLongParam(Constants.RUN_ITERATIONS_SUM, 
                            runIterationsSum + runIterations);
            double runTimeSum =
                tc.hasParam(Constants.RUN_TIME_SUM) ?
                    tc.getDoubleParam(Constants.RUN_TIME_SUM) : 0.0;
            tc.setDoubleParam(Constants.RUN_TIME_SUM,
                              runTimeSum + duration);
        }        
        
        if (Japex.verbose) {
            System.out.println("               " + 
                Thread.currentThread().getName() + 
                " japex.actualRunIterations = " + runIterations);        
            System.out.println("               " + 
                Thread.currentThread().getName() + 
                " japex.actualRunTime (ms) = " + duration);        
        }                    
    }

    // JapexDriver Interface ---------------------------------------------
    
    protected Object _userData = null;
    
    /**
     * Called once when the class is loaded.
     */
    public void initializeDriver() {
        _userData = initializeDriver(_userData);
    }
  
    native public Object initializeDriver(Object userData);
    
    /**
     * Execute prepare phase. 
     */
    public void prepare(TestCase testCase) {
        prepare(testCase, _userData);
    }
    
    native public void prepare(TestCase testCase, Object userData);
    
    /**
     * Called once or more for every test, before calling run. Default 
     * implementation is to call run().
     */
    public void warmup(TestCase testCase) {
        warmup(testCase, _userData);
    }
    
    native public void warmup(TestCase testCase, Object userData);
    
    /**
     * Called once or more for every test to obtain perf data.
     */
    public void run(TestCase testCase) {
        run(testCase, _userData);
    }
    
    native public void run(TestCase testCase, Object userData);
    
    /**
     * Called exactly once after calling run. 
     */
    public void finish(TestCase testCase) {
        finish(testCase, _userData);
    }
    
    native public void finish(TestCase testCase, Object userData);
    
    /**
     * Called after all tests are completed.
     */
    public void terminateDriver() {
        terminateDriver(_userData);
    }        
    
    native public void terminateDriver(Object userData);
    
    // Internal JNI Interface --------------------------------------------
    
    /**
     * Called for looping over a specified duration
     */
    native public int runLoopDuration(double duration, Object userData);

    /**
     * Called for looping over a specified number iterations
     * TODO: change iterations from int to long
     */
    native public void runLoopIterations(int iterations, Object userData);
    
}
