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

package com.sun.japex.jdsl.xml.parsing.stax;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import com.sun.japex.TestCase;
import com.sun.japex.jdsl.xml.BaseParserDriver;
import com.sun.japex.jdsl.xml.DriverConstants;

public class JAXPStAXDriver extends BaseParserDriver {    
    protected XMLInputFactory _factory;
    protected XMLStreamReader _reader;
    
    protected boolean _nextOnly = false;
    
    public void initializeDriver() {
        super.initializeDriver();
        
        try {
            _factory = XMLInputFactory.newInstance();
            if (hasParam(DriverConstants.STAX_NEXTONLY)) {
                _nextOnly = this.getBooleanParam(DriverConstants.STAX_NEXTONLY);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }   
        
    public void run(TestCase testCase) {
        try {
            _inputStream.reset();
            _reader = _factory.createXMLStreamReader(_xmlFile, _inputStream);
            
            char[] text;
            int acount, ncount;
            String local, prefix, uri;
            
            while (_reader.hasNext()) {
                int event = _reader.next();
                
                // If nextOnly set, don't call any getters
                if (_nextOnly) continue;
                
                _reader.getEventType();
                switch (event) {
                    case XMLStreamReader.START_ELEMENT:
                        local = _reader.getLocalName();
                        prefix = _reader.getPrefix();
                        uri = _reader.getNamespaceURI();
                        acount = _reader.getAttributeCount();
                        ncount = _reader.getNamespaceCount();
                        break;
                    case XMLStreamReader.END_ELEMENT:
                        local = _reader.getLocalName();
                        prefix = _reader.getPrefix();
                        uri = _reader.getNamespaceURI();
                        break;
                    case XMLStreamReader.CHARACTERS:
                    case XMLStreamReader.SPACE:
                        text = _reader.getTextCharacters();
                        break;
                    default:
                        break;
                }
            }
            _reader.close();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }    
}
