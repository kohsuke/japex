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

package com.sun.japex.jdsl.xml;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

/**
 * Reads a sub-tree from {@link XMLStreamReader} and writes to {@link XMLStreamWriter}
 * as-is.
 *
 * <p>
 * This class can be sub-classed to implement a simple transformation logic.
 *
 * @author Kohsuke Kawaguchi
 * @author Ryan Shoemaker
 */
public class XMLStreamReaderToXMLStreamWriter {

    protected XMLStreamReader in;
    protected XMLStreamWriter out;

    /**
     * Reads one subtree and writes it out.
     *
     * <p>
     * The {@link XMLStreamWriter} never receives a start/end document event.
     * Those need to be written separately by the caller.
     */
    public void bridge(XMLStreamReader in, XMLStreamWriter out) throws XMLStreamException {
        assert in!=null && out!=null;
        this.in = in;
        this.out = out;

        // remembers the nest level of elements to know when we are done.
        int depth=0;

        // if the parser is at the start tag, proceed to the first element
        int event = in.getEventType();
        if(event == XMLStreamConstants.START_DOCUMENT) {
            // nextTag doesn't correctly handle DTDs
            while( !in.isStartElement() )
                event = in.next();
        }


        if( event!=XMLStreamConstants.START_ELEMENT)
            throw new IllegalStateException("The current event is not START_ELEMENT\n but " + event);

        do {
            // These are all of the events listed in the javadoc for
            // XMLEvent.
            // The spec only really describes 11 of them.
            switch (event) {
                case XMLStreamConstants.START_ELEMENT :
                    depth++;
                    handleStartElement();
                    break;
                case XMLStreamConstants.END_ELEMENT :
                    handleEndElement();
                    depth--;
                    if(depth==0)
                        return;
                    break;
                case XMLStreamConstants.CHARACTERS :
                    handleCharacters();
                    break;
                case XMLStreamConstants.ENTITY_REFERENCE :
                    handleEntityReference();
                    break;
                case XMLStreamConstants.PROCESSING_INSTRUCTION :
                    handlePI();
                    break;
                case XMLStreamConstants.COMMENT :
                    handleComment();
                    break;
                case XMLStreamConstants.DTD :
                    handleDTD();
                    break;
                case XMLStreamConstants.CDATA :
                    handleCDATA();
                    break;
                case XMLStreamConstants.SPACE :
                    handleSpace();
                    break;
                default :
                    throw new InternalError("processing event: " + event);
            }

            event=in.next();
        } while (depth!=0);
    }

    protected void handlePI() throws XMLStreamException {
        out.writeProcessingInstruction(
            in.getPITarget(),
            in.getPIData());
    }

    protected void handleCharacters() throws XMLStreamException {
        out.writeCharacters(
            in.getTextCharacters(),
            in.getTextStart(),
            in.getTextLength() );
    }

    protected void handleEndElement() throws XMLStreamException {
        out.writeEndElement();
    }

    protected void handleStartElement() throws XMLStreamException {
        String nsUri = in.getNamespaceURI();
        if(nsUri==null)
            out.writeStartElement(in.getLocalName());
        else
            out.writeStartElement(
                in.getPrefix(),
                in.getLocalName(),
                nsUri
            );

        // start namespace bindings
        int nsCount = in.getNamespaceCount();
        for (int i = 0; i < nsCount; i++) {
            out.writeNamespace(
                in.getNamespacePrefix(i),
                in.getNamespaceURI(i));
        }

        // write attributes
        int attCount = in.getAttributeCount();
        for (int i = 0; i < attCount; i++) {
            handleAttribute(i);
        }
    }

    /**
     * Writes out the {@code i}-th attribute of the current element.
     *
     * <p>
     * Used from {@link #handleStartElement()}.
     */
    protected void handleAttribute(int i) throws XMLStreamException {
        String nsUri = in.getAttributeNamespace(i);
        if(nsUri==null) {
            out.writeAttribute(
                in.getAttributeLocalName(i),
                in.getAttributeValue(i)
            );
        } else {
            out.writeAttribute(
                in.getAttributePrefix(i),
                nsUri,
                in.getAttributeLocalName(i),
                in.getAttributeValue(i)
            );
        }
    }

    protected void handleDTD() throws XMLStreamException {
        out.writeDTD(in.getText());
    }

    protected void handleComment() throws XMLStreamException {
        out.writeComment(in.getText());
    }

    protected void handleEntityReference() throws XMLStreamException {
        out.writeEntityRef(in.getText());
    }

    protected void handleSpace() throws XMLStreamException {
        handleCharacters();
    }

    protected void handleCDATA() throws XMLStreamException {
        out.writeCData(in.getText());
    }
}
