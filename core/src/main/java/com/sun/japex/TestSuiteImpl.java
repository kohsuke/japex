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
import java.text.*;
import java.io.StringWriter;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import com.sun.japex.testsuite.*;

import static com.sun.japex.ConfigFileLoader.context;

public class TestSuiteImpl extends ParamsImpl implements TestSuite {

    final static String PATH_SEPARATOR = System.getProperty("path.separator");

    /**
     * The JAXB test suite element bean from which this class
     * is created.
     */
    TestSuiteElement _testSuiteElement;

    /**
     * This test suite's name.
     */
    String _name;

    /**
     * Final list of drivers that resulted from parsing the 
     * configuration file. Note that base drivers that are used
     * for extension are omitted from the final list.
     */
    List<DriverImpl> _driverList = new ArrayList<DriverImpl>();

    /*
    * This is a temporary list of base drivers that are used
    * to extend others. Drivers in this list will eventually
    * be removed from <code>_driverList</code>.
    */
    List<DriverImpl> _baseDriversUsed = new ArrayList<DriverImpl>();
    
    String _description;

    /**
     * Creates a new instance of TestSuiteImpl from a JAXB-generated
     * object. In essence, this constructor implements a mapping
     * between the JAXB object model and the internal object model
     * used in Japex.
     */
    public TestSuiteImpl(TestSuiteElement ts) {
        _testSuiteElement = ts;
        
        _name = ts.getName();

        // Serialize description and store in variable
        _description = marshalDescription(_testSuiteElement.getDescription());
        
        // Set global properties by traversing JAXB's model
        List<ParamElement> params = createParamList(ts.getParamOrParamGroup());

        if (params != null) {
            for (ParamElement pt : params) {
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

        // Check plot drivers flag and set default if necessary
        if (!hasParam(Constants.PLOT_DRIVERS)) {
            setParam(Constants.PLOT_DRIVERS, Constants.DEFAULT_PLOT_DRIVERS);
        }

        // Check plot drivers flag and set default if necessary
        if (!hasParam(Constants.PLOT_GROUP_SIZE)) {
            setParam(Constants.PLOT_GROUP_SIZE, Constants.DEFAULT_PLOT_GROUP_SIZE);
        }

        // Check single class loader flag and set default if necessary
        if (!hasParam(Constants.SINGLE_CLASS_LOADER)) {
            setParam(Constants.SINGLE_CLASS_LOADER, 
                     Constants.DEFAULT_SINGLE_CLASS_LOADER);
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

        // Report error for deprecated parameter but continue
        if (hasParam("japex.includeWarmupRun")) {
            System.err.print("Warning: Parameter 'japex.includeWarmupRun' is deprecated, " +
                "use 'japex.warmupsPerDriver' instead.");
        }

        // Set default japex.warmupsPerDriver based on japex.runsPerDriver
        if (!hasParam(Constants.WARMUPS_PER_DRIVER)) {
            setIntParam(Constants.WARMUPS_PER_DRIVER, (runsPerDriver > 1) ? 1 : 0);
        }

        // Set other global params
        setParam(Constants.VERSION, Constants.VERSION_VALUE);
        setParam(Constants.OS_NAME, System.getProperty("os.name"));
        setParam(Constants.OS_ARCHITECTURE, System.getProperty("os.arch"));
        DateFormat df = new SimpleDateFormat(Constants.DEFAULT_DATE_TIME_FORMAT,Locale.ENGLISH);
        setParam(Constants.DATE_TIME, df.format(Japex.TODAY));
        setParam(Constants.VM_INFO,
            System.getProperty("java.vendor") + " " +
            System.getProperty("java.vm.version"));
        setIntParam(Constants.NUMBER_OF_CPUS,
            Runtime.getRuntime().availableProcessors());
        String hostName = "localhost";
        try {
            hostName = java.net.InetAddress.getLocalHost().getHostName();
        }
        catch (java.net.UnknownHostException e) {
            // falls through
        }
        setParam(Constants.HOST_NAME, hostName);

        // Create and populate list of drivers and base drivers used
        _driverList = createDriverList(ts.getDriverOrDriverGroup(), this);

        // Remove base drivers in use so that they are ignored
        for (DriverImpl driverInfo : _baseDriversUsed) {
            _driverList.remove(driverInfo);
        }

        // Create and populate list of test cases
        TestCaseArrayList testCases = createTestCaseList(
            ts.getTestCaseOrTestCaseGroup(), this);

        // If running in test mode just do one iteration
        if (Japex.test) {
            setIntParam(Constants.WARMUPS_PER_DRIVER, 0);
            setIntParam(Constants.RUNS_PER_DRIVER, 1);
            removeParam(Constants.WARMUP_TIME);
            removeParam(Constants.RUN_TIME);

            for (TestCaseImpl tc : testCases) {
                tc.removeParam(Constants.WARMUP_TIME);
                tc.removeParam(Constants.RUN_TIME);
                tc.setIntParam(Constants.WARMUP_ITERATIONS, 0);
                tc.setIntParam(Constants.RUN_ITERATIONS, 1);
            }
        }

        // Set list of test cases on each driver
        for (DriverImpl di: _driverList) {
            di.setTestCases(testCases);
        }
    }

    /**
     * Returns a flat list of test cases by recursively traversing test
     * case groups, if necessary. The initial value for <code>defaults</code>
     * should be the list of globally defined params.
     */
    private TestCaseArrayList createTestCaseList(List<Object> testCaseOrTestGroup,
    		ParamsImpl defaults)
    {
        TestCaseArrayList result = new TestCaseArrayList();

        for (Object o : testCaseOrTestGroup) {
            if (o instanceof TestCaseElement) {
                TestCaseElement tc = (TestCaseElement) o;

                // Create new TestCaseImpl
                TestCaseImpl testCase = new TestCaseImpl(tc.getName(), defaults);

                // Copy params from JAXB object to Japex object
                for (ParamElement pt : createParamList(tc.getParamOrParamGroup())) {
                    testCase.setParam(pt.getName(), pt.getValue());
                }

                // Append to result list
                result.add(testCase);
            }
            else {
                TestCaseGroupElement testCaseGroup = (TestCaseGroupElement) o;

                // Create a new scope for this group
                ParamsImpl groupScope = new ParamsImpl(defaults);
                for (ParamElement pt : createParamList(testCaseGroup.getParamOrParamGroup())) {
                    groupScope.setParam(pt.getName(), pt.getValue());
                }

                // Recurse and append to result list
                result.addAll(
                    createTestCaseList(testCaseGroup.getTestCaseOrTestCaseGroup(),
                                       groupScope));
            }
        }

        return result;
    }

    /**
     * Returns a flat list of drivers by recursively traversing driver
     * groups, if necessary. The initial value for <code>defaults</code>
     * should be the list of globally defined params.
     */
    private List<DriverImpl> createDriverList(List<Object> driverOrDriverGroup,
                                              ParamsImpl defaults)
    {
        List<DriverImpl> result = new ArrayList<DriverImpl>();

        for (Object o : driverOrDriverGroup) {
            DriverImpl driverInfo = null;

            // Single driver or driver group?
            if (o instanceof DriverElement) {
                DriverElement dt = (DriverElement) o;
                driverInfo = createDriverImpl(dt, defaults, result);

                // If japex.driverClass not specified, use the driver's name
                if (!driverInfo.hasParam(Constants.DRIVER_CLASS)) {
                    driverInfo.setParam(Constants.DRIVER_CLASS, dt.getName());
                }

                // Append driver to result list
                result.add(driverInfo);
            }
            else {
                DriverGroupElement driverGroup = (DriverGroupElement) o;

                // Create group's scope using testsuite params as default
                ParamsImpl groupScope = new ParamsImpl(defaults);
                for (ParamElement pt :
                     createParamList(driverGroup.getParamOrParamGroup()))
                {
                    String name = pt.getName();
                    String value = pt.getValue();
                    String oldValue = groupScope.getParam(name);

                    // If japex.classPath, append to existing value
                    groupScope.setParam(name,
                        name.equals(Constants.CLASS_PATH) && oldValue != null ?
                        (oldValue + PATH_SEPARATOR + value) : value);
                }

                // Recurse and append result to list
                result.addAll(
                    createDriverList(driverGroup.getDriverOrDriverGroup(), groupScope));
            }
        }

        return result;
    }

    /**
     * Create a driver implementation either by cloning a base driver (when
     * the extends attribute is specified) or by directly allocating a new
     * instance.
     */
    private DriverImpl createDriverImpl(DriverElement dt, ParamsImpl inScope,
                                        List<DriverImpl> driverList)
    {
        DriverImpl driverInfo = null;

        // Check if this driver extends another
        String baseDriver = dt.getExtends();

        if (baseDriver != null) {
            boolean baseDriverFound = false;
            for (DriverImpl base : driverList) {
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
                    for (String name : inScope.getLocalParams()) {
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
        
         // Serialize description and store in driver
        driverInfo.setDescription(marshalDescription(dt.getDescription()));

        // Copy params from JAXB object to Japex object
        for (ParamElement pt : createParamList(dt.getParamOrParamGroup())) {
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

    /**
     * Returns a flat list of params by recursively traversing param
     * groups, if necessary. 
     */
    private List<ParamElement> createParamList(List<Object> paramOrParamGroup) {
        List<ParamElement> result = new ArrayList<ParamElement>();

        for (Object o : paramOrParamGroup) {
            if (o instanceof ParamElement) {
                result.add((ParamElement) o);
            }
            else {
                ParamGroupElement p = (ParamGroupElement) o;
                result.addAll(createParamList(p.getParamOrParamGroup()));
            }
        }

        return result;
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
     * or set global params of the same name from the config file. If the 
     * value of the system property is "", then it is ignored.
     */
    private void readAndSetSystemProperties() {
        Properties sysProps = System.getProperties();

        for (Iterator<Object> i = sysProps.keySet().iterator(); i.hasNext();) {
            String name = (String) i.next();
            if (name.startsWith("japex.")) {
                String value = sysProps.getProperty(name);
                if (value.length() > 0) {
                    setParam(name, value);
                }
            }
        }
    }

    public TestSuiteElement getTestSuiteElement() {
        return _testSuiteElement;
    }

    public String getName() {
        return _name;
    }

    public List<DriverImpl> getDriverInfoList() {
        return _driverList;
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
        
        // Serialize description
        if (_description != null) {
            report.append(_description);
        }
        
        // Serialize global parameters
        serialize(report, 2);

        // Compute a parameter closure for all drivers
        Set<String> paramsClosure = new HashSet<String>();
        for (DriverImpl di : _driverList) {
            for (String name : di.getLocalParams()) {
                paramsClosure.add(name);
            }
        }

        // Close the set of driver params in all drivers
        for (DriverImpl di : _driverList) {
            for (String name : paramsClosure) {
                if (!di.hasLocalParam(name)) {
                    di.setParam(name, "n/a");
                }
            }
        }

        // Iterate through each class (aka driver)
        for (DriverImpl di : _driverList) {
            di.serialize(report, 2);
        }

        report.append("</testSuiteReport>\n");
    }
    
    private String marshalDescription(DescriptionElement desc) {
        if (desc != null) {
            try {
                StringWriter writer = new StringWriter();
                Marshaller m = context.createMarshaller();
                m.setProperty("jaxb.fragment", Boolean.TRUE);
                m.setProperty("jaxb.formatted.output", Boolean.TRUE);
                m.marshal(desc, writer);
                return writer.toString();
            } 
            catch (JAXBException e) {
                System.err.println("Warning: Unable to serialize 'description' element - ignoring.");
                
            }
        }       
        return null;
    }

}
