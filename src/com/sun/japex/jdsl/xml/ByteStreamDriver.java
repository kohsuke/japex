/*
 * Copyright, 2004-2006 Sun Microsystems, Inc. All Rights Reserved.
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

package com.sun.japex.jdsl.xml;

import java.io.*;
import com.sun.japex.Util;
import com.sun.japex.JapexDriverBase;
import com.sun.japex.TestCase;
import com.sun.japex.Constants;

/**
 * <p>This simple class can be used to estimate the time needed to consume 
 * an XML input stream without actually parsing. This driver is useful 
 * to get a lower bound on how fast an XML parser can run.</p>
 *
 * @author Santiago.PericasGeertsen@sun.com
 */
public class ByteStreamDriver extends JapexDriverBase {
    
    protected String _xmlFile;
    protected ByteArrayInputStream _inputStream;    
    protected byte[] _buffer = new byte[1024];

    @Override
    public void prepare(TestCase testCase) {
        _xmlFile = TestCaseUtil.getXmlFile(testCase);

        try {
            _inputStream = Util.streamToByteArrayInputStream(
                new FileInputStream(new File(_xmlFile)));
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
            _inputStream.reset();
            while (_inputStream.read(_buffer) > 0) {     
                // no op
            }       
        } 
        catch (IOException e) {
            throw new RuntimeException(e);
        }                
    }
    
    @Override
    public void finish(TestCase testCase) {
        /*
         * Add the size of the encoded data as a point on the X axis.
         * Such data will be used when constructing scatter charts.
         */
        _inputStream.reset();
        
        if (getBooleanParam(DriverConstants.DO_NOT_REPORT_SIZE) == false) {
            testCase.setDoubleParam(Constants.RESULT_VALUE_X,
                                    _inputStream.available() / 1024.0);
            getTestSuite().setParam(Constants.RESULT_UNIT_X, "kbs");
        }
    }

}

