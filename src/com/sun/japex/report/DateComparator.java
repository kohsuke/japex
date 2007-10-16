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
import java.util.Comparator;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.text.ParseException;
import java.text.SimpleDateFormat;

/**
 * Date comparator for timestamped reports.
 *
 * @author Joe.Wang@sun.com
 * @author Santiago.PericasGeertsen@sun.com
 */
public class DateComparator implements Comparator {
    
    public static final int ORDER_DESC = 1;
    public static final int ORDER_ASC = 2;
    
    private int order;
    
    
    public DateComparator() {
        order = ORDER_DESC;
    }
    
    public DateComparator(int order) {
        this.order = order;
    }
    
    public int compare(Object o1, Object o2) {
        if ((o1 instanceof File) && (o2 instanceof File)) {
            long lm1, lm2;
            lm1 = convertDate((File)o1);
            lm2 = convertDate((File)o2);
            
            if (lm1 < lm2) {
                return (order == ORDER_DESC) ? -1 : 1;
            } else if (lm1 > lm2) {
                return (order == ORDER_DESC) ? 1 : -1;
            } else {
                return 0;
            }
        } else if ((o1 instanceof Comparable) && (o2 instanceof Comparable)) {
            return ((Comparable)o1).compareTo( ((Comparable)o2) );
        } else {
            return -1;
        }
    }
    
    public boolean equals(Object obj) {
        return (obj instanceof DateComparator);
    }
    
    long convertDate(File file) {
        long time = 0;
        if (file != null) {
            Calendar d0 = parseReportDirectory(file);
            if (d0 != null) {
                time = d0.getTimeInMillis();
            }
        }
        return time;
    }
    
    Calendar parseReportDirectory(File file) {
        Calendar result = null;
        
        if (file != null) {
            try {
                SimpleDateFormat formatter =
                        new SimpleDateFormat(ReportConstants.REPORT_DIRECTORY_FORMAT, Locale.ENGLISH);
                Date date = formatter.parse(file.getName());
                result = Calendar.getInstance();
                result.setTime(date);
            } 
            catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }
        return result;
    }    
    
}
