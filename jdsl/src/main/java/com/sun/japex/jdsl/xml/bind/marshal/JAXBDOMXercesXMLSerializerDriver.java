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

package com.sun.japex.jdsl.xml.bind.marshal;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.xml.serialize.XMLSerializer;
import org.w3c.dom.Document;

import com.sun.japex.TestCase;

@SuppressWarnings("deprecation")
public class JAXBDOMXercesXMLSerializerDriver extends BaseMarshallerDriver {
	XMLSerializer _xmlSerializer;
    DocumentBuilder _builder;
    
    public void initializeDriver() {
        super.initializeDriver();
        
        try {
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            builderFactory.setNamespaceAware(true);
            _builder = builderFactory.newDocumentBuilder();
        } 
        catch (Exception e) {
            e.printStackTrace();
        }
        
        _xmlSerializer = new XMLSerializer();
    }
    
    public void prepare(TestCase testCase) {
        super.prepare(testCase);
        
        _xmlSerializer.setOutputByteStream(_outputStream);
    }
    
    public void run(TestCase testCase) {
        try {
            _outputStream.reset();
            Document d = _builder.newDocument();
            _marshaller.marshal(_bean, d);
            _xmlSerializer.serialize(d);
        } 
        catch (Exception e) {
            e.printStackTrace();
        }
    }    
}
