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

import java.util.*;
import java.text.*;
import java.net.*;
import java.io.File;
import java.util.concurrent.*;
import java.lang.management.*;

public class Engine {
    
    /**
     * The test suite being executed by this engine.
     */
    TestSuiteImpl _testSuite;

    /**
     * Thread pool used for the test's execution.
     */
    ThreadPoolExecutor _threadPool;
    
    /**
     * Matrix of driver instances of size nOfThreads * runsPerDriver.
     */
    JapexDriverBase _drivers[][];
    
    /**
     * Current driver being executed.
     */
    DriverImpl _driverImpl;
    
    /**
     * Current driver run being executed.
     */
    int _driverRun;    
    
    /**
     * Used to check if all drivers (or no driver) compute a result.
     */
    Boolean _computeResult = null;
    
    /**
     * Running geometric mean = (sum{i,n} x_i) / n
     */
    double _geomMeanresult = 1.0;
    
    /**
     * Running arithmetic mean = (prod{i,n} x_i)^(1/n)
     */
    double _aritMeanresult = 0.0;
    
    /**
     * Harmonic mean inverse = sum{i,n} 1/(n * x_i)
     */
    double _harmMeanresultInverse = 0.0;

    /**
     * List of GC beans to estimate percentage of GC time (%gctime unit)
     */
    protected List<GarbageCollectorMXBean> _gCCollectors;
    
    /**
     * GC time in millis during measurement period
     */
    protected long _gCTime;

    /**
     * Used to compute per driver heap memory usage
     */
    long _beforeHeapMemoryUsage;
    
    public Engine() {
        _gCCollectors = ManagementFactory.getGarbageCollectorMXBeans();                 
    }
    
    public TestSuiteImpl start(String configFile) {
        try { 
            // Load config file
            ConfigFileLoader cfl = new ConfigFileLoader(configFile);
            _testSuite = cfl.getTestSuite();
            
            if (Japex.test) {
                System.out.println("Running in test mode without generating reports ...");
            }

            // Print estimated running time
            if (_testSuite.hasParam(Constants.WARMUP_TIME) && 
                    _testSuite.hasParam(Constants.RUN_TIME)) 
            {
                int[] hms = estimateRunningTime(_testSuite);
                System.out.println("Estimated warmup time + run time is " +
                    (hms[0] > 0 ? (hms[0] + " hours ") : "") +
                    (hms[1] > 0 ? (hms[1] + " minutes ") : "") +
                    (hms[2] > 0 ? (hms[2] + " seconds ") : ""));                    
            }

            forEachDriver();                  
        }
        catch (RuntimeException e) {
            throw e;
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }

        return _testSuite;
    }        

    private void forEachDriver() {
        try {
            // Iterate through each driver
            Iterator jdi = _testSuite.getDriverInfoList().iterator();
            while (jdi.hasNext()) {                               
                _driverImpl = (DriverImpl) jdi.next();

                int nOfCpus = _driverImpl.getIntParam(Constants.NUMBER_OF_CPUS);
                int nOfThreads = _driverImpl.getIntParam(Constants.NUMBER_OF_THREADS);
                int runsPerDriver = _driverImpl.getIntParam(Constants.RUNS_PER_DRIVER);
                int warmupsPerDriver = _driverImpl.getIntParam(Constants.WARMUPS_PER_DRIVER);

                // Create a Japex class loader for this driver
                JapexClassLoader jcLoader = 
                    new JapexClassLoader(_driverImpl.getParam(Constants.CLASS_PATH));
		Thread.currentThread().setContextClassLoader(jcLoader);
 
                System.out.print("  " + _driverImpl.getName() + " using " 
                    + nOfThreads + " thread(s) on " + nOfCpus + " cpu(s)");
                
                // Allocate a matrix of nOfThreads * actualRuns size and initialize each instance
                int actualRuns = warmupsPerDriver + runsPerDriver;
                _drivers = new JapexDriverBase[nOfThreads][actualRuns];
                for (int i = 0; i < nOfThreads; i++) {
                    for (int j = 0; j < actualRuns; j++) {
                        _drivers[i][j] = 
                            jcLoader.getJapexDriver(
                                _driverImpl.getParam(Constants.DRIVER_CLASS));   // returns fresh copy
                        _drivers[i][j].setDriver(_driverImpl);
                        _drivers[i][j].setTestSuite(_testSuite);
                        _drivers[i][j].initializeDriver();
                    }
                }

		// Created thread pool of nOfThreads size and pre-start threads                
		if (nOfThreads > 1) {
		    _threadPool = new ThreadPoolExecutor(nOfThreads, nOfThreads, 0L,
			TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(),
			new JapexThreadFactory(jcLoader));      // Use Japex thread factory
		    _threadPool.prestartAllCoreThreads();
		}

                // Reset memory usage before starting runs
                resetPeakMemoryUsage();
                        
                // Display driver's name
                forEachRun();
                
                // Set memory usage param and display info
                setPeakMemoryUsage(_driverImpl);                
                System.out.println("    Peak heap usage: "
                    + _driverImpl.getParam(Constants.PEAK_HEAP_USAGE)
                    + " KB");
                    
                // Call terminate on all driver instances
                for (int i = 0; i < nOfThreads; i++) {
                    for (int j = 0; j < actualRuns; j++) {
                        _drivers[i][j].terminateDriver();
                    }
                }                
                
                // Shutdown thread pool
                if (nOfThreads > 1) {
		    _threadPool.shutdown();
                }                
            }   
        }
        catch (RuntimeException e) {
            throw e;
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    private void forEachRun() {
        try {
            int runsPerDriver = _driverImpl.getIntParam(Constants.RUNS_PER_DRIVER);
            int warmupsPerDriver = _driverImpl.getIntParam(Constants.WARMUPS_PER_DRIVER);

            int actualRuns = warmupsPerDriver + runsPerDriver;
            for (_driverRun = 0; _driverRun < actualRuns; _driverRun++) {
                if (_driverRun < warmupsPerDriver) {
                    System.out.print("\n    Warmup " + (_driverRun + 1) + ": ");
                }
                else {
                    System.out.print("\n    Run " + (_driverRun - warmupsPerDriver + 1) + ": ");                        
                }
                
                if (Japex.resultPerLine) {
                    System.out.println("");
                }
                
                // geometric mean = (sum{i,n} x_i) / n
                _geomMeanresult = 1.0;
                // arithmetic mean = (prod{i,n} x_i)^(1/n)
                _aritMeanresult = 0.0;
                // harmonic mean inverse = sum{i,n} 1/(n * x_i)
                _harmMeanresultInverse = 0.0;

                forEachTestCase();

                if (Japex.resultPerLine) {
                    System.out.print(
                            "      aritmean," + Util.formatDouble(_aritMeanresult) +
                            ",\n      geommean," + Util.formatDouble(_geomMeanresult) +
                            ",\n      harmmean," + Util.formatDouble(1.0 / _harmMeanresultInverse));
                } else {
                    System.out.print(
                            "aritmean," + Util.formatDouble(_aritMeanresult) +
                            ",geommean," + Util.formatDouble(_geomMeanresult) +
                            ",harmmean," + Util.formatDouble(1.0 / _harmMeanresultInverse));
                }
            }

            int startRun = warmupsPerDriver;
            if (actualRuns - startRun > 1) {
                // Print average for all runs
                System.out.print("\n     Avgs: ");
                Iterator tci = _driverImpl.getAggregateTestCases().iterator();
                while (tci.hasNext()) {
                    TestCaseImpl tc = (TestCaseImpl) tci.next();
                    System.out.print(tc.getName() + ",");                        
                    System.out.print(
                        Util.formatDouble(tc.getDoubleParam(Constants.RESULT_VALUE)) 
                        + ",");
                }
                System.out.print(
                    "aritmean," +
                    _driverImpl.getParam(Constants.RESULT_ARIT_MEAN) + 
                    ",geommean," +
                    _driverImpl.getParam(Constants.RESULT_GEOM_MEAN) + 
                    ",harmmean," +
                    _driverImpl.getParam(Constants.RESULT_HARM_MEAN));   

                // Print standardDevs for all runs
                System.out.print("\n    Stdev: ");
                tci = _driverImpl.getAggregateTestCases().iterator();
                while (tci.hasNext()) {
                    TestCaseImpl tc = (TestCaseImpl) tci.next();
                    System.out.print(tc.getName() + ",");                        
                    System.out.print(
                        Util.formatDouble(tc.getDoubleParam(Constants.RESULT_VALUE_STDDEV)) 
                        + ",");
                }
                System.out.println(
                    "aritmean," +
                    _driverImpl.getParam(Constants.RESULT_ARIT_MEAN_STDDEV) + 
                    ",geommean," +
                    _driverImpl.getParam(Constants.RESULT_GEOM_MEAN_STDDEV) + 
                    ",harmmean," +
                    _driverImpl.getParam(Constants.RESULT_HARM_MEAN_STDDEV));   
            }
            else {
                System.out.println("");
            }
        }
        catch (RuntimeException e) {
            throw e;
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    private void forEachTestCase() {
        try {
            double endTime;
            int nOfCpus = _driverImpl.getIntParam(Constants.NUMBER_OF_CPUS);
            int nOfThreads = _driverImpl.getIntParam(Constants.NUMBER_OF_THREADS);
            
            // Get list of tests
            List tcList = _driverImpl.getTestCases(_driverRun);
            int nOfTests = tcList.size();
            
            // Iterate through list of test cases
            Iterator tci = tcList.iterator();
            while (tci.hasNext()) {
                long runTime = 0L;
                TestCaseImpl tc = (TestCaseImpl) tci.next();
                
                if (Japex.verbose) {
                    System.out.println(tc.getName());
                } 
                else if (Japex.resultPerLine) {
                    System.out.print("      " + tc.getName() + ",");
                }
                else {
                    System.out.print(tc.getName() + ",");
                }
                
                Future<?>[] futures = null;
                List<Long> gCStartTimes = null;
                
                try {
                    // If nOfThreads == 1, re-use this thread
                    if (nOfThreads == 1) {
                        // -- Prepare phase --------------------------------------
                        
                        _drivers[0][_driverRun].setTestCase(tc);     // tc is shared!
                        _drivers[0][_driverRun].prepare();
                        
                        // -- Warmup phase ---------------------------------------
                        
                        endTime = tc.hasParam(Constants.WARMUP_TIME) ?
                            Util.currentTimeMillis() +
                                Util.parseDuration(tc.getParam(Constants.WARMUP_TIME)) : 0L;
                        
                        // First time call does warmup
                        _drivers[0][_driverRun].setEndTime(endTime);
                        _drivers[0][_driverRun].call();
                        
                        // Set actual warmup time using sum if just one thread
                        tc.setDoubleParam(Constants.ACTUAL_WARMUP_TIME,
                            tc.getDoubleParam(Constants.WARMUP_TIME_SUM));
                        
                        // -- Run phase -------------------------------------------
                        
                        endTime = tc.hasParam(Constants.RUN_TIME) ?
                            Util.currentTimeMillis() +
                                Util.parseDuration(tc.getParam(Constants.RUN_TIME)) : 0L;
 
                        // Run GC and reset GC start times
                        System.gc();                       
                        gCStartTimes = getGCAbsoluteTimes();
                        
                        // Second time call does run
                        _drivers[0][_driverRun].setEndTime(endTime);
                        _drivers[0][_driverRun].call();
                        
                        // Set actual run time using sum if there's one thread
                        tc.setDoubleParam(Constants.ACTUAL_RUN_TIME,
                            tc.getDoubleParam(Constants.RUN_TIME_SUM));
                    } 
                    else {  // nOfThreads > 1
                        
                        // -- Prepare phase --------------------------------------
                        
                        // Initialize driver instance with test case object do prepare
                        for (int i = 0; i < nOfThreads; i++) {
                            _drivers[i][_driverRun].setTestCase(tc);     // tc is shared!
                            _drivers[i][_driverRun].prepare();
                        }
                        
                        // -- Warmup phase ---------------------------------------
                        
                        // Fork all threads -- first time drivers will warmup
                        futures = new Future<?>[nOfThreads];
                        
                        endTime = tc.hasParam(Constants.WARMUP_TIME) ?
                            Util.currentTimeMillis() +
                                Util.parseDuration(tc.getParam(Constants.WARMUP_TIME)) : 0L;
                        
                        for (int i = 0; i < nOfThreads; i++) {
                            _drivers[i][_driverRun].setEndTime(endTime);
                            futures[i] = _threadPool.submit(_drivers[i][_driverRun]);
                        }
                        
                        // Wait for all threads to finish
                        for (int i = 0; i < nOfThreads; i++) {
                            futures[i].get();
                        }
                        
                        // Set actual warmup time using average over threads
                        tc.setDoubleParam(Constants.ACTUAL_WARMUP_TIME,
                            tc.getDoubleParam(Constants.WARMUP_TIME_SUM) / nOfThreads);
                        
                        // -- Run phase -------------------------------------------
                        
                        endTime = tc.hasParam(Constants.RUN_TIME) ?
                            Util.currentTimeMillis() +
                                Util.parseDuration(tc.getParam(Constants.RUN_TIME)) : 0L;
                        
                        // Run GC and reset GC start times
                        System.gc();                       
                        gCStartTimes = getGCAbsoluteTimes();
                        
                        // Fork all threads -- second time drivers will run
                        for (int i = 0; i < nOfThreads; i++) {
                            _drivers[i][_driverRun].setEndTime(endTime);
                            futures[i] = _threadPool.submit(_drivers[i][_driverRun]);
                        }
                        
                        // Wait for all threads to finish
                        for (int i = 0; i < nOfThreads; i++) {
                            futures[i].get();
                        }
                        
                        // Set actual run time using average over threads
                        tc.setDoubleParam(Constants.ACTUAL_RUN_TIME,
                            tc.getDoubleParam(Constants.RUN_TIME_SUM) / nOfThreads);                        
                    }
                    
                    // Get the total time take for GC over the measurement period
                    _gCTime = getGCRelativeTotalTime(gCStartTimes);
                    
                    // Finish phase
                    for (int i = 0; i < nOfThreads; i++) {
                        _drivers[i][_driverRun].finish();
                    }
                } 
                catch (Exception e) {
                    // Set japex.resultValue to Not-A-Number
                    tc.setDoubleParam(Constants.RESULT_VALUE, Double.NaN);
                } 
                finally {
                    if (futures != null) {
                        // Cancel all remaining threads
                        for (int i = 0; i < nOfThreads; i++) {
                            futures[i].cancel(true);
                        }
                    }
                }
                
                double result;
                if (tc.hasParam(Constants.RESULT_VALUE)) {
                    result = tc.getDoubleParam(Constants.RESULT_VALUE);
                } 
                else {
                    result = computeResultValue(tc, nOfThreads, nOfCpus);
                    tc.setDoubleParam(Constants.RESULT_VALUE, result);
                }
                
                // Compute running means
                _aritMeanresult += result / nOfTests;
                _geomMeanresult *= Math.pow(result, 1.0 / nOfTests);
                _harmMeanresultInverse += 1.0 / (nOfTests * result);
                
                // Display results for this test
                if (Japex.verbose) {
                    System.out.println("           " + tc.getParam(Constants.RESULT_VALUE));
                    System.out.print("           ");
                } 
                else if (Japex.resultPerLine) {
                    System.out.println(tc.getParam(Constants.RESULT_VALUE) + ",");
                }
                else {
                    System.out.print(tc.getParam(Constants.RESULT_VALUE) + ",");
                    System.out.flush();
                }
            }
        } 
        catch (RuntimeException e) {
            throw e;
        } 
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void resetPeakMemoryUsage() {
        // Force GC before collecting current usage (from JLS 4th)
        Runtime rt = Runtime.getRuntime();
        long wasFree, isFree = rt.freeMemory();
        do {
            wasFree = isFree;
            rt.runFinalization();
            rt.gc();
            isFree = rt.freeMemory();
        } while (isFree > wasFree);
                
        // Accumulate usage from all heap-type pools
        _beforeHeapMemoryUsage = 0L;
        for (MemoryPoolMXBean b : ManagementFactory.getMemoryPoolMXBeans()) {
            b.resetPeakUsage();     // Sets it to current usage
            if (b.getType() == MemoryType.HEAP) {
                _beforeHeapMemoryUsage += b.getPeakUsage().getUsed();
            }
        }
    }
    
    private void setPeakMemoryUsage(DriverImpl driver) {
        long afterHeapMemoryUsage = 0L;
        
        // Accumulate usage from all heap-type pools
        for (MemoryPoolMXBean b : ManagementFactory.getMemoryPoolMXBeans()) {
            if (b.getType() == MemoryType.HEAP) {
                afterHeapMemoryUsage += b.getPeakUsage().getUsed();
            }
        }

        // Set output parameter
        driver.setDoubleParam(Constants.PEAK_HEAP_USAGE,
                (afterHeapMemoryUsage - _beforeHeapMemoryUsage) / 1024.0);
    }
    
    private List<Long> getGCAbsoluteTimes() {
        List<Long> gCTimes = new ArrayList();
        for (GarbageCollectorMXBean gcc : _gCCollectors) {
            gCTimes.add(gcc.getCollectionTime());
        }        
        return gCTimes;
    }
    
    private long getGCRelativeTotalTime(List<Long> start) {
        List<Long> end = getGCAbsoluteTimes();        
        long time = 0;
        for (int i = 0; i < start.size(); i++) {
            time += end.get(i) - start.get(i);
        }        
        return time;
    }

    /**
     * Compute Tx, L and Mbps
     * 
     * T = test duration in seconds
     * N = number of threads 
     * C = number of CPUs available on the system
     * I = number of iterations
     */        
    private double computeResultValue(TestCase tc, int nOfThreads, int nOfCpus) {
        String resultUnit = _testSuite.getParam(Constants.RESULT_UNIT);
        
        if (Japex.verbose) {
            System.out.println("             " + 
                Thread.currentThread().getName() + 
                    " japex.runIterationsSum = " +
                        tc.getLongParam(Constants.RUN_ITERATIONS_SUM)); 
            System.out.println("             " + 
                Thread.currentThread().getName() + 
                    " japex.runTimeSum (ms) = " +
                        tc.getDoubleParam(Constants.RUN_TIME_SUM));
        }

        // Get actual run time
        double actualTime = tc.getDoubleParam(Constants.ACTUAL_RUN_TIME);

        // Tx = sum(I_k) / T for k in 1..N
        double tps = tc.getLongParam(Constants.RUN_ITERATIONS_SUM) /
              (actualTime / 1000.0);
        
        // Compute latency as L = (min(C, N) / Tx) * 1000
        double l = Math.min(nOfCpus, nOfThreads) / tps * 1000.0;
        
        if (resultUnit == null || resultUnit.equalsIgnoreCase("tps")) {
            return tps;
        }
        else if (resultUnit.equalsIgnoreCase("ms")) {
            return l;
        }
        // Mbps = size-in-mbits * Tx 
        else if (resultUnit.equalsIgnoreCase("mbps")) {
            // Check if japex.inputFile was defined
            String inputFile = tc.getParam(Constants.INPUT_FILE);            
            if (inputFile == null) {
                throw new RuntimeException("Unable to compute japex.resultValue " + 
                    " because japex.inputFile is not defined or refers to an illegal path.");
            }            
            return new File(inputFile).length() * 0.000008d * tps;
        }     
        else if (resultUnit.equalsIgnoreCase("%GCTIME")) {      // EXPERIMENTAL
            // Calculate % of GC relative to the run time
            double gctime = (_gCTime / actualTime) * 100.0;
            
            // Report latency on the X axis
            _testSuite.setParam(Constants.RESULT_UNIT_X, "ms");
            tc.setDoubleParam(Constants.RESULT_VALUE_X, l);
            return gctime;
        }
        else {
            throw new RuntimeException("Unknown value '" + 
                resultUnit + "' for global param japex.resultUnit.");
        }
    }
    
    /**
     * Calculates the time of the warmup and run phases. Returns an array 
     * of size 3 with hours, minutes and seconds. Note: if japex.runsPerDriver
     * is redefined by any driver, this estimate will be off.
     */
    private int[] estimateRunningTime(TestSuiteImpl testSuite) {        
        int nOfDrivers = testSuite.getDriverInfoList().size();
        int nOfTests = ((DriverImpl) testSuite.getDriverInfoList().get(0)).getTestCases(0).size();
    
        String runTime = testSuite.getParam(Constants.RUN_TIME);
        String warmupTime = testSuite.getParam(Constants.WARMUP_TIME);
        int actualRuns = testSuite.getIntParam(Constants.RUNS_PER_DRIVER) +
            testSuite.getIntParam(Constants.WARMUPS_PER_DRIVER);
        
        long seconds = (long)
            (nOfDrivers * nOfTests * (Util.parseDuration(warmupTime) / 1000.0) +
            nOfDrivers * nOfTests * (Util.parseDuration(runTime) / 1000.0)) *
            actualRuns;     
        
        int[] hms = new int[3];
        hms[0] = (int) (seconds / 60 / 60);
        hms[1] = (int) ((seconds / 60) % 60);
        hms[2] = (int) (seconds % 60);
        return hms;
    }
}
