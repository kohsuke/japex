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

import java.io.*;
import java.net.URL;
import javax.xml.transform.*;
import javax.xml.transform.stream.*;
import java.util.Enumeration;

/**
 * <p>This class implements a simple preprocessor for Japex configuration 
 * files. It uses a helper stylesheet that looks for elements with 
 * japex:if and japex:unless attributes, eliminating them from the output
 * if the conditions are false. For simplicity, a condition is simply 
 * checking if a Java system property is defined.</p> 
 * 
 * <p>For example, it is possible to do conditional inclusion using
 * XInclude based on the value of a property (typically set on the command 
 * line using
 * -D) as follows:
 *
 *  <xi:include href="myDriver.xml" japex:if="this-prop-is-defined"/>
 *
 * <p>The attributes japex:if and japex:unless can be set on any 
 * element, resulting in the subtree rooted at that element to
 * be excluded if the condition does not hold.</p>
 *
 * @author Santiago.PericasGeertsen@sun.com
 */
public class ConditionalProcessor {
    
    static final TransformerFactory tf = TransformerFactory.newInstance();
    
    public ConditionalProcessor() {
    }
  
    public Reader process(String fileName) {
        try {
            // Convert the system props to the string " name1 name2 ... nameN "
            StringBuffer propertyList = new StringBuffer();
            Enumeration<?> names = System.getProperties().propertyNames();
            while (names.hasMoreElements()) {
                propertyList.append(' ');
                propertyList.append((String) names.nextElement());
            }
            propertyList.append(' ');
            
            URL stylesheet = getClass().getResource("/resources/conditional-inclusion.xsl");
            assert stylesheet != null;
            
            // By default, parser use by transformer does not process XInclude
            Transformer transformer = tf.newTransformer(
                    new StreamSource(stylesheet.toExternalForm()));
            
            transformer.setParameter("property-list", propertyList.toString());
            
            StringWriter writer = new StringWriter(2 * 1024);
            StreamResult result = new StreamResult(writer);
            StreamSource source = new StreamSource(new FileReader(fileName));
            source.setSystemId(fileName);
            transformer.transform(source, result);
            
            return new StringReader(writer.getBuffer().toString());
        } 
        catch (RuntimeException e) {
            throw e;
        } 
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
