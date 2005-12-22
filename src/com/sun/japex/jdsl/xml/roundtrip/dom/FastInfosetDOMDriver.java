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

package com.sun.japex.jdsl.xml.roundtrip.dom;

import com.sun.japex.jdsl.xml.TestCaseUtil;
import com.sun.japex.TestCase;
import com.sun.japex.jdsl.xml.DriverConstants;
import com.sun.japex.jdsl.xml.FastInfosetParamSetter;
import com.sun.xml.fastinfoset.dom.DOMDocumentParser;
import com.sun.xml.fastinfoset.dom.DOMDocumentSerializer;
import com.sun.xml.fastinfoset.sax.VocabularyGenerator;
import com.sun.xml.fastinfoset.vocab.SerializerVocabulary;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;


public class FastInfosetDOMDriver extends BaseJAXPDOMDriver {
    protected DOMDocumentParser _ddp;
    protected DOMDocumentSerializer _fiSerializer;
    
    public void initializeDriver() {
        super.initializeDriver();
        
        _fiSerializer = new DOMDocumentSerializer();        
        _ddp = new DOMDocumentParser();
        _ddp.setStringInterning(getBooleanParam(DriverConstants.STRING_INTERNING_PROPERTY));
    }   
    
    public void prepare(TestCase testCase) {
        super.prepare(testCase);        
        
        _fiSerializer.setOutputStream(_outputStream);
    }
        
    public void run(TestCase testCase) {
        try {
            _inputStream.reset();
            _d = _builder.newDocument();
            _ddp.parse(_d, _inputStream);
            _outputStream.reset();
            _fiSerializer.serialize(_d);
        } 
        catch (RuntimeException e) {
            throw e;
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }    
}