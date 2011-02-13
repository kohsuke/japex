package com.sun.japex;


/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.IOException;
import java.io.Writer;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.codehaus.plexus.configuration.PlexusConfiguration;

/**
 * Write a plexus configuration to a stream
 * Note: This class was originally copied from plexus-container-default.  It is duplicated here
 * to maintain compatibility with both Maven 2.x and Maven 3.x.
 * Note that the config begins with the japexConfig element, which does NOT want to be part of the 
 * results.
 *
 */
class XmlConfigurationWriter {
	private final static XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();
	
	XmlConfigurationWriter() {
	}

    public void write( PlexusConfiguration configuration, Writer writer )
        throws IOException, XMLStreamException {
    	XMLStreamWriter staxWriter = xmlOutputFactory.createXMLStreamWriter(writer);
    	staxWriter.writeStartDocument();
    	// fish out the top level element that we actually want.
    	if (configuration.getChildCount() != 1) {
    		// some day perhaps allow multiple suites turned into multiple files!
    		throw new JapexException("There must be one <testSuite/> child element of japexConfig");
    	}
    	PlexusConfiguration suiteConfig = configuration.getChild(0);
    	write(suiteConfig, staxWriter, 0);
    	staxWriter.writeEndDocument();
    }

    private void write(PlexusConfiguration c, XMLStreamWriter w, int depth) throws IOException, XMLStreamException {
        int count = c.getChildCount();

        if (count == 0) {
            writeTag(c, w, depth);
        } else {
            w.writeStartElement(c.getName());
            writeAttributes(c, w);

            for (int i = 0; i < count; i++ ) {
                PlexusConfiguration child = c.getChild( i );
                write( child, w, depth + 1 );
            }
            w.writeEndElement();
        }
    }

    private void writeTag(PlexusConfiguration c, XMLStreamWriter w, int depth) throws IOException, XMLStreamException {
    	w.writeStartElement(c.getName());
        writeAttributes(c, w);
        
        String value = c.getValue( null );
        if ( value != null )
        {
        	w.writeCharacters(value);
        }

        w.writeEndElement();
    }

    private void writeAttributes(PlexusConfiguration c, XMLStreamWriter w) throws IOException, XMLStreamException {
        String[] names = c.getAttributeNames();

        for ( int i = 0; i < names.length; i++ ) {
        	w.writeAttribute(names[i], c.getAttribute(names[i], null));
        }
    }

}

