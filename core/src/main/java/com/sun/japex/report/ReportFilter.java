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

package com.sun.japex.report;

import java.io.File;
import java.io.FileFilter;
import java.util.Date;
import java.util.Calendar;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * Report filter based on user options.
 *
 * @author Joe.Wang@sun.com
 * @author Santiago.PericasGeertsen@sun.com
 */
public class ReportFilter implements FileFilter {
    
    Calendar _from, _to;
    
    public ReportFilter(Calendar from, Calendar to) {
        _from = from;
        _to = to;
    }
    
    public boolean accept(File pathname) {
        if (pathname.isDirectory()) {
            Calendar d0 = parseDate(pathname);
            if (d0 != null) {
                if (d0.compareTo(_from) >= 0 && d0.compareTo(_to) <= 0) {
                    return true;
                }
            }
        }
        return false;
    }
    
    static Calendar parseDate(File f) {
        Date d0 = null;
        if (f.getName().length() < 10) {
            return null;
        }
        
        String s = f.getName().substring(0, 10);
        DateFormat df = new SimpleDateFormat("yyyy_MM_dd");
        try {
            d0 = df.parse(s);
        } 
        catch (Exception e) {
            // falls through
        }
        
        if (d0 != null) {
            Calendar result = Calendar.getInstance();
            result.setTime(d0);
            return result;
        }
        return null;
    }
}
