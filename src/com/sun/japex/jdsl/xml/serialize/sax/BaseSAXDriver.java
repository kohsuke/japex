/*
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

package com.sun.japex.jdsl.xml.serialize.sax;

import java.io.ByteArrayOutputStream;
import com.sun.japex.jdsl.xml.BaseParserDriver;
import com.sun.japex.TestCase;
import com.sun.xml.stream.buffer.XMLStreamBuffer;
import java.io.OutputStream;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.XMLReader;

/**
 * This class uses an <code>XMLStreamBuffer</code> to read the input 
 * document into memory and then write to an <code>XMLStreamWriter</code>.
 * A subclass should only need to implement the method {@link getContentHandler}.
 *
 * @author Santiago.PericasGeertsen@sun.com
 * @author Paul.Sandoz@sun.com
 */
public abstract class BaseSAXDriver extends BaseParserDriver {
    
    protected XMLStreamBuffer _xmlStreamBuffer;
    protected ByteArrayOutputStream _outputStream;     
    
    protected XMLReader _reader;
    
    protected String _encoding;
    
    public void initializeDriver() {
        try {
            super.initializeDriver();       
    
            SAXParserFactory spf = SAXParserFactory.newInstance();
            spf.setNamespaceAware(true);
            _reader = spf.newSAXParser().getXMLReader();
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
            _xmlStreamBuffer.createFromXMLReader(_reader, _inputStream);
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
            _xmlStreamBuffer.writeTo(getContentHandler(_outputStream));
        }
        catch (RuntimeException e) {
            throw e;            
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }        
    }
    
    protected abstract ContentHandler getContentHandler(OutputStream out);
}
