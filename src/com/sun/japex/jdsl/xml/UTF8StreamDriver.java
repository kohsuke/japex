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

import java.io.*;
import java.nio.charset.Charset;
import com.sun.japex.Util;
import com.sun.japex.JapexDriverBase;
import com.sun.japex.TestCase;
import com.sun.japex.Constants;

import com.sun.xml.fastinfoset.DecoderStateTables;

/**
* <p>This class can be used to estimate the time needed to consume an 
 * XML input stream decoding UTF-8 bytes. This driver is useful to get 
 * a lower bound on how fast an XML parser can run when parsing UTF-8 
 * encoded streams.</p>
 *
 * @author Santiago.PericasGeertsen@sun.com
 */
public class UTF8StreamDriver extends ByteStreamDriver {
    
    protected char[] _charBuffer = new char[_buffer.length];
    
    @Override
    public void run(TestCase testCase) {
        int bytesRead, offset = 0;
        _inputStream.reset();
        
        while ((bytesRead = _inputStream.read(_buffer, offset, _buffer.length - offset)) > 0) {
            int left = decodeUTF8ByteBuffer(_buffer, bytesRead, _charBuffer);
            // Copy undecoded bytes to the beginning of array
            for (int i = 0; i < left; i++) {
                _buffer[i] = _buffer[bytesRead - left + i];
            }
            offset = left;
        }
    }
    
    int decodeUTF8ByteBuffer(byte[] buffer, int bytes, char[] charBuffer) {
        int charBufferIndex = 0;
        for (int j = 0, i = 0; i < bytes; i++) {
            final byte b1 = buffer[i];
            
            switch(DecoderStateTables.UTF8[b1]) {
                case DecoderStateTables.UTF8_ONE_BYTE:
                    charBuffer[j++] = (char) b1;
                break;
                case DecoderStateTables.UTF8_TWO_BYTES:
                {
                    if (i + 1 >= bytes) {
                        return 1;
                    }
                    final int b2 = buffer[i++] & 0xFF;
                    if ((b2 & 0xC0) != 0x80) {
                        throw new RuntimeException("Illegal 2-char UTF-8 sequence");
                    }
                    
                    // Character guaranteed to be in [0x20, 0xD7FF] range
                    // since a character encoded in two bytes will be in the
                    // range [0x80, 0x1FFF]
                    charBuffer[j++] = (char) (((b1 & 0x1F) << 6) | (b2 & 0x3F));
                    break;
                }
                case DecoderStateTables.UTF8_THREE_BYTES:
                {
                    if (i + 2 >= bytes) {
                        return 2;
                    }
                    final int b2 = buffer[i++] & 0xFF;
                    if ((b2 & 0xC0) != 0x80
                            || (b1 == 0xED && b2 >= 0xA0)
                            || ((b1 & 0x0F) == 0 && (b2 & 0x20) == 0)) {
                        throw new RuntimeException("Illegal 3-char UTF-8 sequence");
                    }
                    
                    final int b3 = buffer[i++] & 0xFF;
                    if ((b3 & 0xC0) != 0x80) {
                        throw new RuntimeException("Illegal 3-char UTF-8 sequence");
                    }
                    
                    return (char) ((b1 & 0x0F) << 12 | (b2 & 0x3F) << 6
                            | (b3 & 0x3F));
                }
                case DecoderStateTables.UTF8_FOUR_BYTES:
                {
                    throw new RuntimeException("High/low surrogates not supported");
                }
                default:
                    throw new RuntimeException("Error decoding UTF-8 char stream");
            }
        }
        
        return 0;
    }
    
}

