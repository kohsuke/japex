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

public class JapexDriverBase implements JapexDriver, Params {
    
    /**
     * Object containing information about this driver. Parameter
     * getters and setters must be delegated to this object.
     */
    protected Driver _driver;    
    
    /**
     * Reference to test suite being processed.
     */
    protected TestSuiteImpl _testSuite;    
    
    /**
     * A reference to the the current test case being executed.
     */
    protected TestCaseImpl _testCase;
    
    /**
     * Flag indicating if warmup phase is completed.
     */
    protected boolean _needWarmup = true;

    /**
     * This time is set externally by the engine whenever 
     * japex.warmupTime or japex.runTime are defined. The warmup and 
     * run phases will be executed until this time is reached. Letting 
     * the engine compute this value ensures that all threads will stop 
     * at or about the same time.
     */
    protected long _endTime;
    
    public JapexDriverBase() {
    }
    
    public void setDriver(Driver driver) {
        _driver = driver;
    }
    
    public void setTestSuite(TestSuiteImpl testSuite) {
        _testSuite = testSuite;
    }
    
    public void setTestCase(TestCaseImpl testCase) {
        _testCase = testCase;
        _needWarmup = true;
    }
    
    protected TestSuiteImpl getTestSuite() {
        return _testSuite;
    }
    
    public void setEndTime(long endTime) {
        _endTime = endTime;
    }
    
    // -- Internal interface ---------------------------------------------
    
    /**
     * Execute prepare phase. Even in multi-threaded tests, this method 
     * will only be executed in single-threaded mode, so there's no
     * need for additional synchronization.
     */
    public void prepare() {
        if (Japex.verbose) {
            System.out.println("             " + 
                Thread.currentThread().getName() + " prepare()"); 
        }
      
        TestCaseImpl tc = _testCase;
        
        long millis = Util.currentTimeMillis();
        prepare(tc);
        tc.setDoubleParam(Constants.ACTUAL_PREPARE_TIME, 
            Util.currentTimeMillis() - millis);
    }
    
    /**
     * Execute the warmup phase. This method can be executed concurrently
     * by multiple threads. Care should be taken to ensure proper
     * synchronization. Note that parameter getters and setters are
     * already synchronized.
     */
    public void warmup() {
        if (Japex.verbose) {
            System.out.println("             " + 
                Thread.currentThread().getName() + " warmup()"); 
        }

        TestCaseImpl tc = _testCase;
        
        long warmupIterations = 0;
        long millis, startTime;         
        
        if (tc.hasParam(Constants.WARMUP_TIME)) {
            startTime = millis = Util.currentTimeMillis();

            while (_endTime > millis) {
                warmup(tc);      // Call warmup
                warmupIterations++;
                millis = Util.currentTimeMillis();
            }             
        }
        else {
            warmupIterations = tc.getLongParam(Constants.WARMUP_ITERATIONS);
            
            startTime = Util.currentTimeMillis();
            for (long i = 0; i < warmupIterations; i++) {
                warmup(tc);      // Call warmup
            }
            millis = Util.currentTimeMillis();            
        }
        
        // In multi-threaded mode, last thread that ends sets these
        tc.setLongParam(Constants.ACTUAL_WARMUP_ITERATIONS, warmupIterations);
        tc.setDoubleParam(Constants.ACTUAL_WARMUP_TIME, millis - startTime);            
        
        if (Japex.verbose) {
            System.out.println("               " + 
                Thread.currentThread().getName() + 
                " japex.actualWarmupIterations = " + warmupIterations);        
            System.out.println("               " + 
                Thread.currentThread().getName() + 
                " japex.actualWarmupTime (ms) = " + (millis - startTime));        
        }            
    }
    
    /**
     * Execute the run phase. This method can be executed concurrently
     * by multiple threads. Care should be taken to ensure proper
     * synchronization. Note that parameter getters and setters are
     * already synchronized.
     */
    public void run() {
        if (Japex.verbose) {
            System.out.println("             " + 
                Thread.currentThread().getName() + " run()"); 
        }

        TestCaseImpl tc = _testCase;
        
        long runIterations = 0;
        long millis, startTime, duration;
        
        if (tc.hasParam(Constants.RUN_TIME)) {
            startTime = Util.currentTimeMillis();
            
            // Run phase
            do {
                run(tc);      // Call run
                runIterations++;
                millis = Util.currentTimeMillis();
            } while (_endTime >= millis);
            
            duration = millis - startTime;
            
            // Accumulate number of iterations
            synchronized (tc) {
                long runIterationsSum =  
                    tc.hasParam(Constants.RUN_ITERATIONS_SUM) ? 
                        tc.getLongParam(Constants.RUN_ITERATIONS_SUM) : 0L;
                tc.setLongParam(Constants.RUN_ITERATIONS_SUM, 
                               runIterationsSum + runIterations);
            }        
        }
        else {
            runIterations = tc.getLongParam(Constants.RUN_ITERATIONS);

            // Run phase
            startTime = Util.currentTimeMillis();
            for (long i = 0; i < runIterations; i++) {
                run(tc);      // Call run
            }
            duration = Util.currentTimeMillis() - startTime;
            
            // Accumulate run time (use millis for this sum)
            synchronized (tc) {
                double runTimeSum =
                    tc.hasParam(Constants.RUN_TIME_SUM) ?
                        tc.getDoubleParam(Constants.RUN_TIME_SUM) : 0.0;
                tc.setDoubleParam(Constants.RUN_TIME_SUM,
                                  runTimeSum + duration);
            }        
        }
        
        // In multi-threaded mode, last thread that ends sets these
        tc.setLongParam(Constants.ACTUAL_RUN_ITERATIONS, runIterations);
        tc.setDoubleParam(Constants.ACTUAL_RUN_TIME, duration);
        
        if (Japex.verbose) {
            System.out.println("               " + 
                Thread.currentThread().getName() + 
                " japex.actualRunIterations = " + runIterations);        
            System.out.println("               " + 
                Thread.currentThread().getName() + 
                " japex.actualRunTime (ms) = " + duration);        
        }            
    }
        
    /**
     * Called exactly once after calling run. 
     */
    public void finish() {        
        if (Japex.verbose) {
            System.out.println("             " + 
                Thread.currentThread().getName() + " finish()"); 
        }
        
        // Call finish(testCase) in user's driver
        finish(_testCase);
    }
    
    // -- Callable interface ------------------------------------------

    /**
     * Concurrently execute the warmup phase the first time it is 
     * called, and then the run phase the second time it is called.
     * Care should be taken to ensure proper synchronization. Note 
     * that parameter getters and setters are already synchronized.
     */
    public Object call() {
        if (_needWarmup) {
            warmup(); 
            _needWarmup = false;
        }
        else {
            run();
            _needWarmup = true;
        }
        return null;
    }    
    
    // -- Params interface -----------------------------------------------
    
    public boolean hasParam(String name) {
        return _driver.hasParam(name);
    }
    
    public void setParam(String name, String value) {
        _driver.setParam(name, value);
    }
    
    public String getParam(String name) {
        return _driver.getParam(name);
    }
       
    public void setBooleanParam(String name, boolean value) {
        _driver.setBooleanParam(name, value);
    }
    
    public boolean getBooleanParam(String name) {
        return _driver.getBooleanParam(name);
    }
    
    public void setIntParam(String name, int value) {
        _driver.setIntParam(name, value);
    }    
    
    public int getIntParam(String name) {
        return _driver.getIntParam(name);
    }
    
    public void setLongParam(String name, long value) {
        _driver.setLongParam(name, value);
    }    
    
    public long getLongParam(String name) {
        return _driver.getLongParam(name);
    }
    
    public void setDoubleParam(String name, double value) {
        _driver.setDoubleParam(name, value);
    }
    
    public double getDoubleParam(String name) {
        return _driver.getDoubleParam(name);
    }
    
    // -- JapexDriver interface ------------------------------------------
    
    /**
     * Called once when the class is loaded.
     */
    public void initializeDriver() {
    }
    
    /**
     * Called exactly once for every test, before calling warmup.
     */
    public void prepare(TestCase testCase) {
    }
    
    /**
     * Called once or more for every test, before calling run. Default 
     * implementation is to call run().
     */
    public void warmup(TestCase testCase) {   
        run(testCase);
    }
    
    /**
     * Called once or more for every test to obtain perf data.
     */
    public void run(TestCase testCase) {
    }
    
    /**
     * Called exactly once after calling run. 
     */
    public void finish(TestCase testCase) {
    }
    
    /**
     * Called after all tests are completed.
     */
    public void terminateDriver() {
    }

}
