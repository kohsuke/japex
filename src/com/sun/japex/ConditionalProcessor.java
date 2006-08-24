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
            Enumeration names = System.getProperties().propertyNames();
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
