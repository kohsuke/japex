/*
 * Copyright, 2004-2005 Sun Microsystems, Inc. All Rights Reserved.
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

package com.sun.japex.jdsl.xml.serialize.stax;

import com.sun.japex.jdsl.xml.XMLStreamReaderToXMLStreamWriter;
import java.io.ByteArrayOutputStream;
import javax.xml.stream.*;
import com.sun.japex.jdsl.xml.BaseParserDriver;
import com.sun.japex.TestCase;
import com.sun.xml.stream.buffer.XMLStreamBuffer;

/**
 * This class uses an <code>XMLStreamBuffer</code> to read the input 
 * document into memory and then write to an <code>XMLStreamWriter</code>.
 * A subclass should only need to override method 
 * <code>initializeDriver</code> in order to instantiate the appropriate
 * output factory.
 *
 * @author Santiago.PericasGeertsen@sun.com
 */
public abstract class BaseStAXDriver extends BaseParserDriver {
    
    protected XMLInputFactory _xmlInputFactory;
    protected XMLOutputFactory _xmlOutputFactory;
    
    protected XMLStreamBuffer _xmlStreamBuffer;
    protected ByteArrayOutputStream _outputStream;     
    protected XMLStreamReaderToXMLStreamWriter _xmlReaderToWriter;
    
    public void initializeDriver() {
        try {
            super.initializeDriver();       
            
            _xmlInputFactory = XMLInputFactory.newInstance();
            _xmlOutputFactory = XMLOutputFactory.newInstance();
        } 
        catch (RuntimeException e) {
            throw e;            
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }        
    }
    
    public void prepare(TestCase testCase) {
        try {
            super.prepare(testCase);        // Initializes _inputStream

            _outputStream = new ByteArrayOutputStream();

            // Read input document into an XMLStreamBuffer
            _xmlStreamBuffer = new XMLStreamBuffer();
            _xmlStreamBuffer.createFromXMLStreamReader(
                    _xmlInputFactory.createXMLStreamReader(_inputStream));
            
            _xmlReaderToWriter = new XMLStreamReaderToXMLStreamWriter();
        }
        catch (RuntimeException e) {
            throw e;            
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }        
    }    
    
    public void run(TestCase testCase) {
        try {
            _outputStream.reset();            
            _xmlReaderToWriter.bridge(
                _xmlStreamBuffer.processUsingXMLStreamReader(),
                _xmlOutputFactory.createXMLStreamWriter(_outputStream));
        }
        catch (RuntimeException e) {
            throw e;            
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }        
    }
}
