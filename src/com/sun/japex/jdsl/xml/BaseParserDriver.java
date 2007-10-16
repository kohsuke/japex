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

package com.sun.japex.jdsl.xml;

import com.sun.japex.Constants;
import com.sun.japex.JapexDriverBase;
import com.sun.japex.TestCase;
import com.sun.xml.fastinfoset.sax.SAXDocumentSerializer;
import com.sun.xml.fastinfoset.tools.VocabularyGenerator;
import com.sun.xml.fastinfoset.vocab.ParserVocabulary;
import com.sun.xml.fastinfoset.vocab.SerializerVocabulary;
import org.jvnet.fastinfoset.FastInfosetParser;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.w3c.dom.Document;

public abstract class BaseParserDriver extends JapexDriverBase {
    public static final String TESTCASE_NORMALIZE = "normalizeTestCaseData";
    
    protected SAXDocumentSerializer _saxSerializer = null;
    protected ByteArrayInputStream _inputStream;    
    protected ByteArrayOutputStream _outputStream;
    protected SerializerVocabulary _initialVocabulary;
    protected HashMap _externalVocabularyMap;
    protected DocumentBuilder _builder;
    
    protected String _xmlFile;
            
    public void prepare(TestCase testCase) {
        _xmlFile = TestCaseUtil.getXmlFile(testCase);

        try {
            FileInputStream fis = new FileInputStream(new File(_xmlFile));
            _outputStream = new ByteArrayOutputStream();
            
            if (this instanceof FastInfosetParserDriver) {
                ByteArrayInputStream bais = new ByteArrayInputStream(com.sun.japex.Util.streamToByteArray(fis));
                prepareFI(bais, _xmlFile);
            } else {
                BufferedInputStream bis = new BufferedInputStream(fis);
                prepareXML(bis);
            }
            
            fis.close();                      
        }  catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }        
    }    
    
    public void prepareXML(InputStream in) throws Exception {
        // Load file into byte array to factor out IO
        byte[] xmlFileByteArray = com.sun.japex.Util.streamToByteArray(in);
        _inputStream = new ByteArrayInputStream(xmlFileByteArray);
    }    
    
    public void prepareFI(ByteArrayInputStream in, String name) throws Exception {        
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setNamespaceAware(true);
        SAXParser parser = spf.newSAXParser();
        
        _saxSerializer = new SAXDocumentSerializer();
        FastInfosetParamSetter.setParams(_saxSerializer, this);
        
        _saxSerializer.setOutputStream(_outputStream);
        
        if (getBooleanParam(DriverConstants.EXTERNAL_VOCABULARY_PROPERTY)) {
            SerializerVocabulary externalSerializeVocabulary = new SerializerVocabulary();
            ParserVocabulary externalParserVocabulary = new ParserVocabulary();
            VocabularyGenerator vocabularyGenerator = new VocabularyGenerator(externalSerializeVocabulary, externalParserVocabulary);
            vocabularyGenerator.setCharacterContentChunkSizeLimit(0);
            vocabularyGenerator.setAttributeValueSizeLimit(0);
            parser.parse(in, vocabularyGenerator);
            in.reset();
            
            String externalVocabularyURI = "file:///" + name; 
            _initialVocabulary = new SerializerVocabulary();
            _initialVocabulary.setExternalVocabulary(externalVocabularyURI,
                    externalSerializeVocabulary, false);
            _saxSerializer.setVocabulary(_initialVocabulary);
            
            FastInfosetParser fps = ((FastInfosetParserDriver)this).getParser();
            _externalVocabularyMap = new HashMap();
            _externalVocabularyMap.put(externalVocabularyURI, externalParserVocabulary);
            fps.setExternalVocabularies(_externalVocabularyMap);
        }
        
        parser.setProperty("http://xml.org/sax/properties/lexical-handler", _saxSerializer);
        parser.parse(in, _saxSerializer);
        _inputStream = new ByteArrayInputStream(_outputStream.toByteArray());
    }
    
    public Document createDocument() throws Exception {
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        builderFactory.setNamespaceAware(true);
        _builder = builderFactory.newDocumentBuilder();
        Document d = _builder.parse(_inputStream);
        _inputStream.reset();
        
        return d;
    }
    
    public void finish(TestCase testCase) {
        super.finish(testCase);
        
        /*
         * Add the size of the encoded data as a point on the X axis.
         * Such data will be used when constructing scatter charts.
         */
        _inputStream.reset();
        
        if (getBooleanParam(DriverConstants.DO_NOT_REPORT_SIZE) == false) {
            testCase.setDoubleParam(Constants.RESULT_VALUE_X,
                                    _inputStream.available() / 1024.0);
            getTestSuite().setParam(Constants.RESULT_UNIT_X, "KBytes");
        }
    }

    
}

