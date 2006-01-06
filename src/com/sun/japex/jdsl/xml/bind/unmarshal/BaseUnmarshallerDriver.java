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

package com.sun.japex.jdsl.xml.bind.unmarshal;

import java.net.URLClassLoader;

import com.sun.japex.TestCase;
import com.sun.japex.jdsl.xml.BaseParserDriver;
import com.sun.japex.jdsl.xml.TestCaseUtil;
import com.sun.japex.jdsl.xml.DriverConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import static com.sun.japex.Util.*;
import com.sun.japex.jdsl.xml.FastInfosetParserDriver;
import com.sun.xml.fastinfoset.stax.StAXDocumentParser;
import com.sun.xml.fastinfoset.stax.StAXDocumentSerializer;
import org.jvnet.fastinfoset.FastInfosetParser;
import org.jvnet.fastinfoset.FastInfosetSource;
import java.io.FileInputStream;
import java.io.ByteArrayInputStream;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;


public abstract class BaseUnmarshallerDriver extends BaseParserDriver {
    protected JAXBContext _jc;
    protected Unmarshaller _unmarshaller;
    protected Object _bean;
        
    protected static boolean printManifest = true;
    
    public void initializeDriver() {
        if (printManifest) {
            getTestSuite().setParam("jaxb-impl.manifest",
                getManifestAsString((URLClassLoader) getClass().getClassLoader(), 
                                    "jaxb-impl.jar"));
            printManifest = false;
        }
    }
    
    public void prepare(TestCase testCase) {
        super.prepare(testCase);

        try {            
            // Get JAXB unmarshaller
            _jc = JAXBContext.newInstance(testCase.getParam("contextPath"));
            _unmarshaller = _jc.createUnmarshaller();
            
        } 
        catch (Exception e) {
            e.printStackTrace();
        }
    }    

    public void JAXBRoundTrip_SJSXP(TestCase testCase) {
        super.prepare(testCase);
        try {            
            _bean = null;
            XMLInputFactory inputFactory = new com.sun.xml.stream.ZephyrParserFactory();
            XMLStreamReader reader = inputFactory.createXMLStreamReader(_inputStream);
            Unmarshaller unmarshaller = _jc.createUnmarshaller();
            _bean = unmarshaller.unmarshal(reader);
            reader.close();

            //mashalling:            
            Marshaller marshaller = _jc.createMarshaller();
            
            XMLOutputFactory outputFactory = new com.sun.xml.stream.ZephyrWriterFactory();
            XMLStreamWriter writer = outputFactory.createXMLStreamWriter(_outputStream);
            marshaller.marshal(_bean, writer);
            writer.close();
        } 
        catch (Exception e) {
            e.printStackTrace();
        }

        //convert outputstream to input
        _inputStream = new ByteArrayInputStream(_outputStream.toByteArray());
        
    }            
    
    // Perform JAXB roundtrip to eliminate BASE64 conversion for certain testcases
    public void JAXBRoundTrip(TestCase testCase, boolean SAX, FastInfosetSource fis1) {
        StAXDocumentSerializer staxSerializer = new StAXDocumentSerializer();
        FastInfosetParser fps = ((FastInfosetParserDriver)this).getParser();
        if (getBooleanParam(DriverConstants.EXTERNAL_VOCABULARY_PROPERTY)) {
            fps.setExternalVocabularies(_externalVocabularyMap);
            staxSerializer.setVocabulary(_initialVocabulary);
        }
        
        try {            
            JAXBContext jc = JAXBContext.newInstance(testCase.getParam("contextPath"));
            FastInfosetSource fis = new FastInfosetSource(_inputStream);
            _bean = null;
            _bean = _unmarshaller.unmarshal(fis);
            /*
            if (SAX) {
                _bean = _unmarshaller.unmarshal(fis);
            } else {
                _bean = _unmarshaller.unmarshal((StAXDocumentParser)fps);            
            }
             */
            //mashalling:            
            _outputStream.reset();
            staxSerializer.setOutputStream(_outputStream);
            Marshaller marshaller;
            marshaller = jc.createMarshaller();
            marshaller.marshal(_bean, (XMLStreamWriter)staxSerializer); 
        } 
        catch (Exception e) {
            e.printStackTrace();
        }

        //convert outputstream to input
        _inputStream = new ByteArrayInputStream(_outputStream.toByteArray());
    }
}
