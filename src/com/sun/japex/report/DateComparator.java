/*
 * Japex software ("Software")
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

package com.sun.japex.report;

import java.io.File;
import java.util.Comparator;
import java.util.Calendar;
import java.util.Date;
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
                        new SimpleDateFormat(ReportConstants.REPORT_DIRECTORY_FORMAT);
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
