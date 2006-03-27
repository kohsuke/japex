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

import com.sun.japex.testsuite.*;

import static com.sun.japex.testsuite.DriverType.ParamGroup;
import static com.sun.japex.testsuite.TestSuiteElement.DriverGroup;
import static com.sun.japex.testsuite.TestSuiteElement.TestCaseGroup;

public class TestSuiteImpl extends ParamsImpl implements TestSuite {
    
    final static String PATH_SEPARATOR = System.getProperty("path.separator");
    
    /**
     * This test suite's name.
     */
    String _name;
    
    /**
     * Final list of drivers that resulted from parsing the 
     * configuration file. Note that base drivers that are used
     * for extension are omitted from the final list.
     */
    List<DriverImpl> _driverInfo = new ArrayList<DriverImpl>();
    
    /*
     * This is a temporary list of base drivers that are used
     * to extend others. Drivers in this list will eventually
     * be removed from <code>_driverInfo</code>.
     */
    List<DriverImpl> _baseDriversUsed = new ArrayList<DriverImpl>();
        
    /**
     * Creates a new instance of TestSuiteImpl from a JAXB-generated
     * object. In essence, this constructor implements a mapping
     * between the JAXB object model and the internal object model
     * used in Japex.
     */
    public TestSuiteImpl(TestSuiteElement ts) {
        _name = ts.getName();
        
        // Set global properties by traversing JAXB's model
        List params = flattenParamGroups(ts.getParamGroupOrParam());
        List classPathURLs = new ArrayList();
        
        if (params != null) {
            Iterator it = params.iterator();
            while (it.hasNext()) {
                ParamType pt = (ParamType) it.next();
                String name = pt.getName();
                String value = pt.getValue();
                String oldValue = getParam(name);
                
                // If japex.classPath, append to existing value
                setParam(name, 
                    name.equals(Constants.CLASS_PATH) && oldValue != null ?
                    (oldValue + PATH_SEPARATOR + value) : value);
            }
        }
        
        // Override config props using system properties
        readAndSetSystemProperties();
        
        // Set default global params if necessary
        if (!hasParam(Constants.WARMUP_TIME) && 
            !hasParam(Constants.WARMUP_ITERATIONS))
        {
            setParam(Constants.WARMUP_ITERATIONS, 
                     Constants.DEFAULT_WARMUP_ITERATIONS);    
        }
        if (!hasParam(Constants.RUN_TIME) && 
            !hasParam(Constants.RUN_ITERATIONS))
        {
            setParam(Constants.RUN_ITERATIONS, 
                     Constants.DEFAULT_RUN_ITERATIONS);    
        }        
        
        // Check output directory
        if (!hasParam(Constants.REPORTS_DIRECTORY)) {
            setParam(Constants.REPORTS_DIRECTORY, 
                     Constants.DEFAULT_REPORTS_DIRECTORY);    
        }
        
        // Check chart type
        if (!hasParam(Constants.CHART_TYPE)) {
            setParam(Constants.CHART_TYPE, 
                     Constants.DEFAULT_CHART_TYPE);    
        }
        else {
            String chartType = getParam(Constants.CHART_TYPE);
            if (!chartType.equalsIgnoreCase("barchart") && 
                !chartType.equalsIgnoreCase("scatterchart") &&
                !chartType.equalsIgnoreCase("linechart")) 
            {
                throw new RuntimeException(
                    "Parameter 'japex.chartType' must be set to " +
                    "'barchart', 'scatterchart' or 'linechart'");
            }
        }
        
        // Check result axis
        if (!checkResultAxis(Constants.RESULT_AXIS) ||
            !checkResultAxis(Constants.RESULT_AXIS_X))
        {
                throw new RuntimeException(
                    "Parameter 'japex.resultAxis' and 'japex.resultAxisX' " +
                    "must be set to either 'normal' or 'logarithmic'");            
        }
        
        // Check result unit and set default if necessary 
        if (!hasParam(Constants.RESULT_UNIT)) {
            setParam(Constants.RESULT_UNIT, "TPS");
        }
        
        // Check number of threads 
        if (!hasParam(Constants.NUMBER_OF_THREADS)) {
            setParam(Constants.NUMBER_OF_THREADS, 
                     Constants.DEFAULT_NUMBER_OF_THREADS);    
        }
        else {
            int nOfThreads = getIntParam(Constants.NUMBER_OF_THREADS);
            if (nOfThreads < 1) {
                throw new RuntimeException(
                    "Parameter 'japex.numberOfThreads' must be at least 1");
            }
        }
        
        // Check runs per driver
        if (!hasParam(Constants.RUNS_PER_DRIVER)) {
            setParam(Constants.RUNS_PER_DRIVER, 
                     Constants.DEFAULT_RUNS_PER_DRIVER);    
        }
        int runsPerDriver = getIntParam(Constants.RUNS_PER_DRIVER);
        if (runsPerDriver < 1) {
            throw new RuntimeException(
                "Parameter 'japex.runsPerDriver' must be at least 1");
        }
        
        // Check include warmup run - default true if runsPerDriver > 1
        boolean includeWarmupRun = (runsPerDriver > 1);
        if (!hasParam(Constants.INCLUDE_WARMUP_RUN)) {
            setBooleanParam(Constants.INCLUDE_WARMUP_RUN, includeWarmupRun); 
        }
        else {
            includeWarmupRun = getBooleanParam(Constants.INCLUDE_WARMUP_RUN);
        }
        // Increment runsPerDriver to accomodate warmup run
        if (includeWarmupRun) {
            setIntParam(Constants.RUNS_PER_DRIVER, runsPerDriver + 1);
        }
        
        // Set other global params
        setParam(Constants.VERSION, Constants.VERSION_VALUE);
        setParam(Constants.OS_NAME, System.getProperty("os.name"));
        setParam(Constants.OS_ARCHITECTURE, System.getProperty("os.arch"));
        DateFormat df = new SimpleDateFormat("dd MMM yyyy/HH:mm:ss z");
        setParam(Constants.DATE_TIME, df.format(Japex.TODAY));
        setParam(Constants.VM_INFO,
            System.getProperty("java.vendor") + " " + 
            System.getProperty("java.vm.version"));
        setIntParam(Constants.NUMBER_OF_CPUS, 
            Runtime.getRuntime().availableProcessors());
                
        // Create and populate list of drivers
        for (Object driverGroupOrDriver : ts.getDriverGroupOrDriver()) {
            DriverImpl driverInfo = null;
            
            // Single driver or driver group?
            if (driverGroupOrDriver instanceof DriverType) {
                driverInfo = createDriverImpl((DriverType) driverGroupOrDriver, this);
                _driverInfo.add(driverInfo);
            }
            else {
                DriverGroup driverGroup = (DriverGroup) driverGroupOrDriver;
                
                // Create group's scope using testsuite params as default
                ParamsImpl groupScope = new ParamsImpl(this);
                for (ParamType pt : 
                     flattenParamGroups(driverGroup.getParamGroupOrParam())) 
                {
                    String name = pt.getName();
                    String value = pt.getValue();
                    String oldValue = groupScope.getParam(name);
                    
                    // If japex.classPath, append to existing value
                    groupScope.setParam(name, 
                        name.equals(Constants.CLASS_PATH) && oldValue != null ?
                        (oldValue + PATH_SEPARATOR + value) : value);
                }            
                    
                // Create each driver and then override using group params
                for (DriverType dt : driverGroup.getDriver()) {
                    driverInfo = createDriverImpl(dt, groupScope);
                    
                    // If japex.driverClass not specified, use the driver's name
                    if (!driverInfo.hasParam(Constants.DRIVER_CLASS)) {
                        driverInfo.setParam(Constants.DRIVER_CLASS, dt.getName());
                    }          
        
                    _driverInfo.add(driverInfo);
                }                
            }            
        }
        
        // Remove base drivers in use so that they are ignored
        for (DriverImpl driverInfo : _baseDriversUsed) {
            _driverInfo.remove(driverInfo);
        }

        // Create and populate list of test cases
        TestCaseArrayList testCases = new TestCaseArrayList();
        
        for (Object testCaseOrTestGroup : ts.getTestCaseGroupOrTestCase()) {
            // Is this a test group?
            if (testCaseOrTestGroup instanceof TestCaseGroup) {
                TestCaseGroup testCaseGroup = 
                    (TestCaseGroup) testCaseOrTestGroup;

                // Create group's scope using testsuite params as default
                ParamsImpl groupScope = new ParamsImpl(this);
                for (ParamType pt : 
                    flattenParamGroups(testCaseGroup.getParamGroupOrParam())) {
                    groupScope.setParam(pt.getName(), pt.getValue());
                }            
                            
                for (TestCaseType tc : testCaseGroup.getTestCase()) {                    
                    // Create new TestCaseImpl using group parameters
                    TestCaseImpl testCase = new TestCaseImpl(tc.getName(), groupScope);

                    // Copy params from JAXB object to Japex object
                    for (ParamType pt : tc.getParam()) {
                        testCase.setParam(pt.getName(), pt.getValue());
                    }                                
                    
                    // Add to the list of test cases
                    testCases.add(testCase);
                }                
            }
            else {
                // Must be an instance of TestCase
                TestCaseType tc = (TestCaseType) testCaseOrTestGroup;

                // Create new TestCaseImpl
                TestCaseImpl testCase = new TestCaseImpl(tc.getName(), this);

                // Copy params from JAXB object to Japex object
                for (ParamType pt : tc.getParam()) {
                    testCase.setParam(pt.getName(), pt.getValue());
                }            
                
                // Add to the list of test cases
                testCases.add(testCase);
            }
        }
                
        // Set list of test cases and number of runs on each driver
        for (DriverImpl di: _driverInfo) {
            di.setTestCases(testCases);
        }
    }
    
    private DriverImpl createDriverImpl(DriverType dt, ParamsImpl inScope) {
        DriverImpl driverInfo = null;
        
        // Check if this driver extends another
        String baseDriver = dt.getExtends();
        
        if (baseDriver != null) {
            boolean baseDriverFound = false;
            for (DriverImpl base : _driverInfo) {
                if (base.getName().equals(baseDriver)) {
                    // Cloning works in depth for parameters
                    driverInfo = (DriverImpl) base.clone();  

                    // Set name and normal attribute
                    driverInfo.setNormal(dt.isNormal());
                    driverInfo.setBaseName(driverInfo.getName());
                    driverInfo.setName(dt.getName());

                    // Add base driver so that it is removed 
                    if (!_baseDriversUsed.contains(base)) {
                        _baseDriversUsed.add(base);
                    }

                    // Copy in-scope params not defined in original driver
                    for (String name : inScope.nameSet()) {
                        if (!driverInfo.hasParam(name)) {
                            driverInfo.setParam(name, inScope.getParam(name));
                        }
                    }
                    
                    baseDriverFound = true;
                    break;
                }
            }

            // Report an error if base driver has not been defined yet
            if (!baseDriverFound) {
                throw new RuntimeException("Base driver '" + baseDriver + 
                    "' used to extend '" + dt.getName() + "' not found");
            }
        }
        else {
            // Create new DriverImpl
            driverInfo = new DriverImpl(dt.getName(), dt.isNormal(), inScope);
        }

        // Copy params from JAXB object to Japex object
        for (ParamType pt : flattenParamGroups(dt.getParamGroupOrParam())) {
            String name = pt.getName();
            String value = pt.getValue();
            String oldValue = driverInfo.getParam(name);

            /*
             * If japex.classPath, append to existing value. Note that 
             * this prevents fully redefining a class path when extending
             * another driver. May need to revise this later.
             */
            driverInfo.setParam(name, 
                name.equals(Constants.CLASS_PATH) && oldValue != null ?
                (oldValue + PATH_SEPARATOR + value) : value);
        }

        return driverInfo;
    }
    
    private boolean checkResultAxis(String paramName) {
        if (hasParam(paramName)) {
            String value = getParam(paramName);
            return value.equalsIgnoreCase("normal") ||
                   value.equalsIgnoreCase("logarithmic");
        }
        else {
            setParam(paramName, Constants.DEFAULT_RESULT_AXIS);
        }
        return true;
    }
    
    /**
     * System properties that start with "japex." can be used to override
     * global params of the same name from the config file. If the value
     * of the system property is "", then it is ignored.
     */
    private void readAndSetSystemProperties() {
        Properties sysProps = System.getProperties();
        
        for (Iterator i = sysProps.keySet().iterator(); i.hasNext();) {
            String name = (String) i.next();
            if (name.startsWith("japex.")) {
                String value = sysProps.getProperty(name);
                if (value.length() > 0) {
                    setParam(name, value);
                }
            }
        }
    }
    
    public String getName() {
        return _name;        
    }
    
    public List getDriverInfoList() {
        return _driverInfo;
    }
    
    /**
     * Compute a parameter closure before serializing each driver. By 
     * doing so, it is guaranteed that all drivers will define the 
     * same set of driver parameters which make it easy to display them
     * in table form in the HTML report.
     */
    public void serialize(StringBuffer report) {
        report.append("<testSuiteReport name=\"" + _name 
            + "\" xmlns=\"http://www.sun.com/japex/testSuiteReport\">\n");      

        // Serialize global parameters
        serialize(report, 2);
        
        // Compute a parameter closure for all drivers
        Set<String> paramsClosure = new HashSet<String>();
        for (DriverImpl di : _driverInfo) {
            for (String name : di.nameSet()) {
                paramsClosure.add(name);
            }
        }

        // Close the set of driver params in all drivers
        for (DriverImpl di : _driverInfo) {
            for (String name : paramsClosure) {
                if (!di.hasLocalParam(name)) {
                    di.setParam(name, "n/a");
                }
            }
        }        
        
        // Iterate through each class (aka driver)
        Iterator jdi = _driverInfo.iterator();
        while (jdi.hasNext()) {
            DriverImpl di = (DriverImpl) jdi.next();
            di.serialize(report, 2);
        }
                    
        report.append("</testSuiteReport>\n");
    }
    
    /**
     * A list of params may contain one or more <paramGroup>. This method 
     * flattens this list and returns a list of <param>. The element 
     * <paramGroup> is only used for grouping (it has no associated
     * semantics) and cannot be nested.
     */
    static private List<ParamType> flattenParamGroups(
        List<Object> paramGroupOrParams) 
    {
        List<ParamType> result = new ArrayList<ParamType>();
        for (Object o : paramGroupOrParams) {
            if (o instanceof ParamType) {
                result.add((ParamType) o);
            }
            else {
                ParamGroup paramGroup = (ParamGroup) o;
                for (ParamType p : paramGroup.getParam()) {
                    result.add(p);
                }
            }
        }
        return result;
    }    
    
}
