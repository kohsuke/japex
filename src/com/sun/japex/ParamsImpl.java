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

import org.apache.tools.ant.taskdefs.Execute;

public class ParamsImpl implements Params {
    
    final static int OUT_EXPR = 0;
    final static int IN_EXPR  = 1;
    final static String DELIMITER = "\uFFFE";      // A Unicode nonchar
    
    /**
     * Mapping between strings and values. Values could be of types: 
     * String, Long, Double, Boolean.
     */
    Map<String, Object> _mapping = new HashMap<String, Object>();
    
    /**
     * Default mapping used when a parameter is not defined in this
     * mapping.
     */
    ParamsImpl _defaults = null;
    
    public ParamsImpl() {
    }
    
    public ParamsImpl(Properties props) {
        for (Iterator i = props.keySet().iterator(); i.hasNext(); ) {
            String key = (String) i.next();
            convertAndPut(key, props.getProperty(key));
        }
    }
    
    public ParamsImpl(ParamsImpl defaults) {
        _defaults = defaults;
    }
    
    /**
     * Indicates if this is a set of global parameters. All instances of
     * this class will have a set of default parameters, expect the set
     * for <code>TestSuiteImpl</code> which contains global params.
     */
    public boolean isGlobal() {
        return _defaults == null;
    }

    private void convertAndPut(String key, String value) {
        if (value.equals("true") || value.equals("false")) {
            _mapping.put(key, new Boolean(value));
        }
        else {
            try {
                long l = Long.parseLong(value);
                _mapping.put(key, new Long(l));
            }
            catch (NumberFormatException e1) {
                try {
                    double d = Double.parseDouble(value);
                    _mapping.put(key, new Double(d));
                }
                catch (NumberFormatException e2) {
                    _mapping.put(key, value);                    
                }
            }     
        }
    }
    
    public Object clone() {
        try {
            // Start with a shallow copy of the object
            ParamsImpl clone = (ParamsImpl) super.clone();

            // Make a deep copy of _mapping
            clone._mapping = new HashMap();
            for (Iterator i = _mapping.keySet().iterator(); i.hasNext(); ) {
                String key = (String) i.next();
                clone._mapping.put(key, _mapping.get(key));
            }
            
            return clone;
        }
        catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    /** 
     * Returns the set of parameter names defined in this class
     * ignoring params defined in <code>_defaults</code>.
     */
    public Set<String> nameSet() {
        return _mapping.keySet();
    }
    
    private Object getParamOrDefault(String name) {
        Object value = _mapping.get(name);
        if (value == null && _defaults != null) {
            value = _defaults.getParamOrDefault(name);
        }
        return value;
    }
    
    public synchronized boolean hasParam(String name) {
        return getParamOrDefault(name) != null;
    }

    /**
     * Returns true if this param is defined locally. A param is local
     * if it is defined in this set or in any enclosing set that is 
     * not global. In particular, for <code>TestCaseImpl</code> and
     * <code>DriverTestImpl</code> any group params will be local.
     */
    public synchronized boolean hasLocalParam(String name) {
        ParamsImpl params = this;
        
        do {
            if (params._mapping.get(name) != null) {
                return true;
            }
            params = params._defaults;
        } while (!params.isGlobal()); 
            
        return false;
    }
    
    public synchronized void removeParam(String name) {
        if (hasParam(name)) {
            _mapping.remove(name);
        }
        else if (_defaults != null) {
            _defaults.removeParam(name);
        }
    }
    
    // -- String params --------------------------------------------------
    
    public synchronized void setParam(String name, String value) {
        convertAndPut(name, evaluate(name, value));
    }
    
    public synchronized String getParam(String name) {
        Object value = getParamOrDefault(name);
        if (value instanceof Long) {
            value = ((Long) value).toString();
        }
        else if (value instanceof Double) {
            value = Util.formatDouble(((Double) value).doubleValue());
        }
        else if (value instanceof Boolean) {
            value = ((Boolean) value).toString();
        }
        return (String) value;
    }
    
    // -- Boolean params --------------------------------------------------
    
    public synchronized void setBooleanParam(String name, boolean value) {
        _mapping.put(name, new Boolean(value));
    }
    
    public synchronized boolean getBooleanParam(String name) {
        Object value = getParamOrDefault(name);
        if (value instanceof Boolean) {
            return ((Boolean) value).booleanValue();
        }
        else {
            return Boolean.valueOf((String) value).booleanValue();
        }
    }
    
    // -- Int params -----------------------------------------------------
    
    public synchronized void setIntParam(String name, int value) {
        _mapping.put(name, new Long(value));
    }
    
    public synchronized int getIntParam(String name) {
        Object value = getParamOrDefault(name);
        if (value instanceof Long) {
            return ((Long) value).intValue();
        }
        else if (value instanceof Double) {
            return ((Double) value).intValue();
        }
        else {
            try {
                return Integer.parseInt((String) value);
            }
            catch (NumberFormatException e) {
                throw new RuntimeException(e);
            }
        }
    }
    
    // -- Long params ----------------------------------------------------
    
    public synchronized void setLongParam(String name, long value) {
        _mapping.put(name, new Long(value));
    }
    
    public synchronized long getLongParam(String name) {
        Object value = getParamOrDefault(name);
        if (value instanceof Long) {
            return ((Long) value).longValue();
        }
        else if (value instanceof Double) {
            return ((Double) value).longValue();
        }
        else {
            return Long.parseLong((String) value);
        }
    }
    
    // -- Double params --------------------------------------------------
    
    public synchronized void setDoubleParam(String name, double value) {
        _mapping.put(name, new Double(value));
    }
    
    public synchronized double getDoubleParam(String name) {
        Object value = getParamOrDefault(name);
        if (value instanceof Long) {
            return ((Long) value).doubleValue();
        }
        else if (value instanceof Double) {
            return ((Double) value).doubleValue();
        }
        else {
            return Double.parseDouble((String) value);
        }
    }

    /**
     * Same as <code>getDoubleParam(String)</code> but returning
     * 0.0 instead of NaN.
     */
    public synchronized double getDoubleParamNoNaN(String name) {
        double result = getDoubleParam(name);
        return Double.isNaN(result) ? 0.0 : result;
    }
    
    // -- Other methods --------------------------------------------------
    
    /**
     * This method returns a list of all the parameters that are visible 
     * in this scope, except the global parameters. This is needed to
     * include <testGroup> and <driverGroup> parameters when serializing
     * a <code>TestCaseImpl</code> or <code>DriverImpl</code>.
     *
     * This method works bottom up and ensures that shadowed params are
     * not included in the resulting set.
     *
     * @param groupParams  The list where the collection takes place
     * @param params       The current set of parameters to add
     */
    private static void collectGroupParams(ArrayList<String> groupParams, 
        ParamsImpl params) 
    {
        if (params.isGlobal()) {
            return;
        }
        else {
            ArrayList<String> names = new ArrayList(params._mapping.keySet());
            for (String s : names) {
                if (!groupParams.contains(s)) {
                    groupParams.add(s);
                }
            }
            // Proceed recursively using outer scope
            collectGroupParams(groupParams, params._defaults);
        }                
    }
    
    public void serialize(StringBuffer buffer, int indent) {              
        // Collect a list of all params and group params in scope
        ArrayList<String> paramNames;        
        if (isGlobal()) {
            paramNames = new ArrayList(_mapping.keySet());            
        }
        else {
            paramNames = new ArrayList();
            collectGroupParams(paramNames, this);
        }
        
        // Sort list of parameters before serialization
        Collections.sort(paramNames);
        
        for (String name : paramNames) {
            if (name.startsWith("japex.")) {
                String xmlName = name.substring(name.indexOf('.') + 1);                
                // Replace path.separator by a single space
                if (name.equals(Constants.CLASS_PATH)) {
                    buffer.append(Util.getSpaces(indent) 
                        + "<" + xmlName + ">" 
                        + getParam(name).replaceAll(
                              System.getProperty("path.separator"), "\n")
                        + "</" + xmlName + ">\n");                    
                }
                else {
                    buffer.append(Util.getSpaces(indent) 
                        + "<" + xmlName + ">" 
                        + getParam(name)
                        + "</" + xmlName + ">\n");
                }
            }
        }
        
        // Serialize user-defined params
        for (String name : paramNames) {
            if (!name.startsWith("japex.")) {
                buffer.append(Util.getSpaces(indent) 
                    + "<" + name + " xmlns=\"\">" 
                    + getParam(name) 
                    + "</" + name + ">\n");                
            }
        }
    }

    /**
     * Expand expression of the form ${paramname}
     */
    private String evaluate(String name, String value) {
        StringTokenizer tokenizer = 
            new StringTokenizer(value, "${}", true);
        
        String t = null;
        StringBuffer buffer = new StringBuffer();
        int state = OUT_EXPR;
        
        while (tokenizer.hasMoreTokens()) {            
            t = tokenizer.nextToken();
            
            if (t.length() == 1) {
                switch (t.charAt(0)) {
                    case '$':
                        switch (state) {
                            case OUT_EXPR:
                                t = tokenizer.nextToken();
                                if (t.equals("{")) {
                                    buffer.append(DELIMITER);
                                    state = IN_EXPR;                                    
                                }
                                else {
                                    buffer.append("$" + t);
                                }
                                break;
                            case IN_EXPR:
                                buffer.append('$');
                                break;
                        }                                                
                        break;
                    case '}':
                        switch (state) {
                            case OUT_EXPR:
                                buffer.append('}');
                                break;
                            case IN_EXPR:
                                buffer.append(DELIMITER);
                                state = OUT_EXPR;
                                break;
                        }
                        break;
                    default:
                        buffer.append(t);
                        break;
                }
            }
            else {
                buffer.append(t);
            }
        }

        // Must be in OUT_EXPR at the end of parsing
        if (state != OUT_EXPR) {
            throw new RuntimeException("Error evaluating parameter '"
                + name + "' of value '" + value + "'");
        }
        
        /*
          * Second pass: split up buffer into literal and non-literal expressions.
          */
        tokenizer = new StringTokenizer(buffer.toString(), DELIMITER, true);
        StringBuffer result = new StringBuffer();
        
        while (tokenizer.hasMoreTokens()) {
            t = tokenizer.nextToken();
            
            if (t.equals(DELIMITER)) {
                String paramName = tokenizer.nextToken();
                String paramValue = getParam(paramName);
                if (paramValue != null) {
                    result.append(paramValue);
                }
                else {
                    // If not defined, check OS environment
                    paramValue = getEnvVariable(paramName);
                    if (paramValue != null) {
                        result.append(paramValue);                            
                    }
                    else {
                        throw new RuntimeException("Undefined parameter '"
                          + paramName + "'");        
                    }
                }                    
                
                tokenizer.nextToken();      // consume other delimiter
            }
            else {
                result.append(t);
            }
        }        
        
        return result.toString();
    }

    // Use Ant class to get environment
    private static Vector ENV = Execute.getProcEnvironment();
    
    private String getEnvVariable(String name) {
        for (int i = 0; i < ENV.size(); i++) {
            String def = (String) ENV.get(i);
            int k = def.indexOf('=');
            if (k > 0) {
                if (name.equals(def.substring(0, k))) {
                    return def.substring(k + 1);
                }
            }
        }
        return null;
    }

}
