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

import com.sun.japex.Params;
import org.jvnet.fastinfoset.FastInfosetParser;
import org.jvnet.fastinfoset.FastInfosetSerializer;

public class FastInfosetParamSetter {
    public static void setParams(FastInfosetSerializer s, Params p) {
        String indexedContent = p.getParam(DriverConstants.INDEXED_CONTENT_PROPERTY);
        indexedContent = (indexedContent == null) ? "" : indexedContent.intern();
        if (indexedContent == DriverConstants.INDEXED_CONTENT_PROPERTY_VALUE_FULL) {
            s.setMaxCharacterContentChunkSize(Integer.MAX_VALUE);
            s.setCharacterContentChunkMapMemoryLimit(Integer.MAX_VALUE);
        } else if (indexedContent == DriverConstants.INDEXED_CONTENT_PROPERTY_VALUE_NONE) {
            s.setMaxCharacterContentChunkSize(0);
            s.setCharacterContentChunkMapMemoryLimit(0);
        } else if (indexedContent == DriverConstants.INDEXED_CONTENT_PROPERTY_VALUE_DEFAULT) {
        } else {
            try {
                long v = p.getLongParam(DriverConstants.CHARACTER_CONTENT_CHUNK_SIZE_LIMIY_PROPERTY);
                if (v > 0) {
                    s.setMaxCharacterContentChunkSize((int)v);
                }
            } catch (NumberFormatException e){
            }
            
            try {
                long v = p.getLongParam(DriverConstants.ATTRIBUTE_VALUE_SIZE_LIMIT_PROPERTY);
                if (v > 0) {
                    s.setMaxAttributeValueSize((int)v);
                }
            } catch (NumberFormatException e){
            }
        }
    }
    
    public static void setParams(FastInfosetParser s, Params p) {
    }
}
