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

import java.util.*;

import static com.sun.japex.Constants.*;

public class DriverImpl extends ParamsImpl implements Driver, Cloneable {
    
    /**
     * This driver's name.
     */
    String _name;
    
    /**
     * Name of the driver that this driver extends.
     */
    String _baseName = null;
    
    /**
     * True if all other results should be normalized based
     * on this driver's results.
     */
    boolean _isNormal = false;
    
    /**
     * True means are already computed (avoids unnecessary 
     * re-computation).
     */
    boolean _computeMeans = true;
     
    /**
     * Java class implementing this driver.
     */
    Class<? extends JapexDriverBase> _class = null;
    
    /**
     * Array of tests cases for this driver.
     */     
    ArrayList<ArrayList<TestCaseImpl>> _testCases;
    
    /**
     * Aggregate results for this driver.
     */
    ArrayList<TestCaseImpl> _aggregateTestCases;
    
    /**
     * Store serialized description for this driver.
     */
    String _description;
    
    public DriverImpl(String name, boolean isNormal, ParamsImpl params) {
        super(params);
        _name = name;
        _isNormal = isNormal;
    }
    
    public Object clone() {
        return super.clone();
    }
    
    public void setDescription(String description) {
        _description = description;
    }
    
    public void setTestCases(ArrayList<TestCaseImpl> testCases) {
        int runsPerDriver = getIntParam(RUNS_PER_DRIVER);
        int warmupsPerDriver = getIntParam(WARMUPS_PER_DRIVER);
        
        int actualRuns = runsPerDriver + warmupsPerDriver;
        _testCases = new ArrayList<ArrayList<TestCaseImpl>>(actualRuns);
        for (int i = 0; i < actualRuns; i++) {
            _testCases.add(new ArrayList<TestCaseImpl>(testCases));
        }        
        _aggregateTestCases = new ArrayList<TestCaseImpl>(testCases);
    }
        
    private void computeMeans() {
        int runsPerDriver = getIntParam(RUNS_PER_DRIVER);        
        int warmupsPerDriver = getIntParam(WARMUPS_PER_DRIVER);        
                
        // Define start run and actual runs 
        int startRun = warmupsPerDriver;    // skip warmups
        int actualRuns = runsPerDriver + warmupsPerDriver;
        
        // Avoid re-computing the driver's aggregates
        if (_computeMeans) {
            final int nOfTests = _testCases.get(0).size();

            for (int n = 0; n < nOfTests; n++) {

                double[] results = new double[actualRuns];
                double[] resultsX = new double[actualRuns];
                long[] resultIterations = new long[actualRuns];
                double[] resultTime = new double[actualRuns];
                
                // Set hasResultValueX - should be the same for all runs
                TestCaseImpl startRunTc = (TestCaseImpl) _testCases.get(startRun).get(n);
                boolean hasResultValueX = startRunTc.hasParam(RESULT_VALUE_X);
                
                // Collect vertical results for this test
                for (int i = startRun; i < actualRuns; i++) {            
                    TestCaseImpl tc = (TestCaseImpl) _testCases.get(i).get(n);
                    results[i] = tc.getDoubleParam(RESULT_VALUE);
                    resultTime[i] = tc.getDoubleParam(ACTUAL_RUN_TIME);
                    resultIterations[i] = tc.getLongParam(RUN_ITERATIONS_SUM);
                    if (hasResultValueX) {
                        resultsX[i] = tc.getDoubleParam(RESULT_VALUE_X);
                    }
                }
                
                // Compute vertical average and stddev for this test
                TestCaseImpl tc = (TestCaseImpl) _aggregateTestCases.get(n);
                tc.setDoubleParam(RESULT_VALUE, 
                                  Util.arithmeticMean(results, startRun));
                tc.setDoubleParam(RESULT_TIME, 
                                  Util.arithmeticMean(resultTime, startRun) / 1000);
                tc.setDoubleParam(RESULT_ITERATIONS,
                                  Util.arithmeticMean(resultIterations, startRun));
                if (hasResultValueX) {
                    tc.setDoubleParam(RESULT_VALUE_X, 
                                      Util.arithmeticMean(resultsX, startRun));                    
                }                
                if (actualRuns - startRun > 1) {
                    tc.setDoubleParam(RESULT_VALUE_STDDEV, 
                                      Util.standardDev(results, startRun));
                    if (hasResultValueX) {
                        tc.setDoubleParam(RESULT_VALUE_X_STDDEV, 
                                          Util.standardDev(resultsX, startRun));                        
                    }
                }
            }
            
            // geometric mean = (sum{i,n} x_i) / n
            double geomMeanresult = 1.0;
            // arithmetic mean = (prod{i,n} x_i)^(1/n)
            double aritMeanresult = 0.0;
            // harmonic mean inverse = sum{i,n} 1/(n * x_i)
            double harmMeanresultInverse = 0.0;
            
            // geometric mean = (sum{i,n} x_i) / n
            double geomMeanresultX = 1.0;
            // arithmetic mean = (prod{i,n} x_i)^(1/n)
            double aritMeanresultX = 0.0;
            // harmonic mean inverse = sum{i,n} 1/(n * x_i)
            double harmMeanresultXInverse = 0.0;
            
            boolean setMeansAxisX = false;
            
            // Compute horizontal means based on vertical means
            Iterator<TestCaseImpl> tci = _aggregateTestCases.iterator();
            while (tci.hasNext()) {
                TestCaseImpl tc = tci.next();       
                
                // Compute running means 
                double result = tc.getDoubleParam(RESULT_VALUE);
                aritMeanresult += result / nOfTests;
                geomMeanresult *= Math.pow(result, 1.0 / nOfTests);
                harmMeanresultInverse += 1.0 / (nOfTests * result);                
                if (tc.hasParam(RESULT_VALUE_X)) {
                    double resultX = tc.getDoubleParam(RESULT_VALUE_X);
                    aritMeanresultX += resultX / nOfTests;
                    geomMeanresultX *= Math.pow(resultX, 1.0 / nOfTests);
                    harmMeanresultXInverse += 1.0 / (nOfTests * resultX);
                    setMeansAxisX = true;
                }
            }
            
            // Set driver-specific params
            setDoubleParam(RESULT_ARIT_MEAN, aritMeanresult);
            setDoubleParam(RESULT_GEOM_MEAN, geomMeanresult);
            setDoubleParam(RESULT_HARM_MEAN, 1.0 / harmMeanresultInverse);      
            if (setMeansAxisX) {
                setDoubleParam(RESULT_ARIT_MEAN_X, aritMeanresultX);
                setDoubleParam(RESULT_GEOM_MEAN_X, geomMeanresultX);
                setDoubleParam(RESULT_HARM_MEAN_X, 1.0 / harmMeanresultXInverse);                      
            }
            
            // Avoid re-computing these means
            _computeMeans = false;
            
            // If number of runs is just 1, we're done
            if (actualRuns - startRun == 1) {
                return;
            }
            
            // geometric mean = (sum{i,n} x_i) / n
            geomMeanresult = 1.0;
            // arithmetic mean = (prod{i,n} x_i)^(1/n)
            aritMeanresult = 0.0;
            // harmonic mean inverse = sum{i,n} 1/(n * x_i)
            harmMeanresultInverse = 0.0;
            
            // geometric mean = (sum{i,n} x_i) / n
            geomMeanresultX = 1.0;
            // arithmetic mean = (prod{i,n} x_i)^(1/n)
            aritMeanresultX = 0.0;
            // harmonic mean inverse = sum{i,n} 1/(n * x_i)
            harmMeanresultXInverse = 0.0;
            
            boolean setStddevsAxisX = false;
            
            // Compute horizontal stddevs based on vertical stddevs
            tci = _aggregateTestCases.iterator();
            while (tci.hasNext()) {
                TestCaseImpl tc = (TestCaseImpl) tci.next();       
                
                // Compute running means 
                double result = tc.getDoubleParam(RESULT_VALUE_STDDEV);
                aritMeanresult += result / nOfTests;
                geomMeanresult *= Math.pow(result, 1.0 / nOfTests);
                harmMeanresultInverse += 1.0 / (nOfTests * result);
                if (tc.hasParam(RESULT_VALUE_X_STDDEV)) {
                    double resultX = tc.getDoubleParam(RESULT_VALUE_X_STDDEV);
                    aritMeanresultX += resultX / nOfTests;
                    geomMeanresultX *= Math.pow(resultX, 1.0 / nOfTests);
                    harmMeanresultXInverse += 1.0 / (nOfTests * resultX);
                    setStddevsAxisX = true;
                }
            }
            
            // Set driver-specific params
            setDoubleParam(RESULT_ARIT_MEAN_STDDEV, aritMeanresult);
            setDoubleParam(RESULT_GEOM_MEAN_STDDEV, geomMeanresult);
            setDoubleParam(RESULT_HARM_MEAN_STDDEV, 1.0 / harmMeanresultInverse);            
            if (setStddevsAxisX) {
                setDoubleParam(RESULT_ARIT_MEAN_X_STDDEV, aritMeanresultX);
                setDoubleParam(RESULT_GEOM_MEAN_X_STDDEV, geomMeanresultX);
                setDoubleParam(RESULT_HARM_MEAN_X_STDDEV, 1.0 / harmMeanresultXInverse);                        
            }
        }        
    }
    
    public List<TestCaseImpl> getTestCases(int driverRun) {
        return _testCases.get(driverRun);
    }
    
    public List<TestCaseImpl> getAggregateTestCases() {
        computeMeans();  
        return _aggregateTestCases;
    }
    
    public String getName() {
        return _name;    
    }    
    
    public void setName(String name) {
        _name = name;
    }
    
    public String getBaseName() {
        return _baseName;    
    }    
    
    public void setBaseName(String name) {
        _baseName = name;
    }
    
    public boolean isNormal() {
        return _isNormal;
    }
    
    public void setNormal(boolean b) {
        _isNormal = b;
    }
    
    /**
     * Compute a parameter closure before serializing each test case. By 
     * doing so, it is guaranteed that all test cases will define the 
     * same set of test case parameters which make it easy to display them
     * in table form in the HTML report.
     *
     * Calling getAggregateTestCases() forces call to computeMeans(). This
     * is necessary before serializing driver params.
     */
    public void serialize(StringBuffer report, int spaces) {
        report.append(Util.getSpaces(spaces) 
            + "<driver name=\"" + _name + "\""
            + (_isNormal ? " normal=\"true\">\n" : ">\n"));

        // Serialize description
        if (_description != null) {
            report.append(_description);
        }
                
        // Called before serializing driver params
        List<TestCaseImpl> aggregateTestCases = getAggregateTestCases();
       
        // Serialize driver params
        super.serialize(report, spaces + 2);

        // Compute a parameter closure for all test cases
        Iterator<TestCaseImpl> tci = aggregateTestCases.iterator();
        Set<String> paramsClosure = new HashSet<String>();
        while (tci.hasNext()) {
            Set<String> testCaseParams = 
                    ((TestCaseImpl) tci.next()).getLocalParams();
            for (String name : testCaseParams) {
                paramsClosure.add(name);
            }
        }
        
        // Close the set of test case parameters in all test cases
        tci = aggregateTestCases.iterator();
        while (tci.hasNext()) {
            TestCaseImpl tc = (TestCaseImpl) tci.next();
            for (String name : paramsClosure) {
                if (!tc.hasLocalParam(name)) {
                    tc.setParam(name, "n/a");
                }
            }
        }
        
        // Serialize each test case
        tci = aggregateTestCases.iterator();
        while (tci.hasNext()) {
            TestCaseImpl tc = (TestCaseImpl) tci.next();
            tc.serialize(report, spaces + 2);
        }            

        report.append(Util.getSpaces(spaces) + "</driver>\n");       
    }
    
}
