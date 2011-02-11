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
    protected double _endTime;
    
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
    
    public void setEndTime(double endTime) {
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
        
        double millis = Util.currentTimeMillis();
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
        double millis, startTime, duration;         
        
        if (tc.hasParam(Constants.WARMUP_TIME)) {
            startTime = millis = Util.currentTimeMillis();

            while (_endTime > millis) {
                warmup(tc);      // Call warmup
                warmupIterations++;
                millis = Util.currentTimeMillis();
            }             
            
            duration = millis - startTime;            
        }
        else {
            warmupIterations = tc.getLongParam(Constants.WARMUP_ITERATIONS);
            
            startTime = Util.currentTimeMillis();
            for (long i = 0; i < warmupIterations; i++) {
                warmup(tc);      // Call warmup
            }
            
            duration = Util.currentTimeMillis() - startTime;            
        }
        
        // Accumulate number of iterations and duration
        synchronized (tc) {
            long warmupIterationsSum =  
                tc.hasParam(Constants.WARMUP_ITERATIONS_SUM) ? 
                    tc.getLongParam(Constants.WARMUP_ITERATIONS_SUM) : 0L;
            tc.setLongParam(Constants.WARMUP_ITERATIONS_SUM, 
                            warmupIterationsSum + warmupIterations);
            double warmupTimeSum =
                tc.hasParam(Constants.WARMUP_TIME_SUM) ?
                    tc.getDoubleParam(Constants.WARMUP_TIME_SUM) : 0L;
            tc.setDoubleParam(Constants.WARMUP_TIME_SUM,
                            warmupTimeSum + duration);
        }        
        
        if (Japex.verbose) {
            System.out.println("               " + 
                Thread.currentThread().getName() + 
                " japex.actualWarmupIterations = " + warmupIterations);        
            System.out.println("               " + 
                Thread.currentThread().getName() + 
                " japex.actualWarmupTime (ms) = " + duration);        
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
        
        double millis, startTime, duration, delayedEndTime;
        
        // Initialize iteration delay and iterations
        long runIterations = 0;
        long runIterationDelay = tc.hasParam(Constants.RUN_ITERATION_DELAY) ?
            tc.getLongParam(Constants.RUN_ITERATION_DELAY) : 0L;
        
        if (tc.hasParam(Constants.RUN_TIME)) {
            startTime = Util.currentTimeMillis();
            
            // Run phase
            do {
                // Sleep for japex.runIterationDelay
                if (runIterationDelay > 0) {
                    try {
                        if (Japex.verbose) {
                            System.out.println("               " + 
                                Thread.currentThread().getName() + " sleeping for " +
                                    runIterationDelay + " ms"); 
                        }
                        Thread.sleep(runIterationDelay);
                    }
                    catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                
                // Call run() in driver and get current time
                run(tc);
                millis = Util.currentTimeMillis();
                
                // Update iterations and calculate delayed end time
                runIterations++;
                delayedEndTime = _endTime + runIterations * runIterationDelay;
            } while (delayedEndTime >= millis);
            
            // Calculate duration excluding delayed time
            duration = millis - startTime - runIterations * runIterationDelay;            
        }
        else {
            runIterations = tc.getLongParam(Constants.RUN_ITERATIONS);

            // Run phase
            startTime = Util.currentTimeMillis();
            for (long i = 0; i < runIterations; i++) {
                // Sleep for japex.runIterationDelay
                if (runIterationDelay > 0) {
                    try {
                        if (Japex.verbose) {
                            System.out.println("               " + 
                                Thread.currentThread().getName() + " sleeping for " +
                                    runIterationDelay + " ms"); 
                        }
                        Thread.sleep(runIterationDelay);
                    }
                    catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                
                // Call run() in driver
                run(tc);
            }
            
            // Calculate duration excluding delayed time
            duration = Util.currentTimeMillis() - startTime 
                - runIterations * runIterationDelay;            
        }
        
        // Accumulate number of iterations and duration
        synchronized (tc) {
            long runIterationsSum =  
                tc.hasParam(Constants.RUN_ITERATIONS_SUM) ? 
                    tc.getLongParam(Constants.RUN_ITERATIONS_SUM) : 0L;
            tc.setLongParam(Constants.RUN_ITERATIONS_SUM, 
                            runIterationsSum + runIterations);
            double runTimeSum =
                tc.hasParam(Constants.RUN_TIME_SUM) ?
                    tc.getDoubleParam(Constants.RUN_TIME_SUM) : 0L;
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
