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

package com.sun.japex.report;

import java.io.File;
import java.util.Date;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.ArrayList;
import java.util.StringTokenizer;

import static com.sun.japex.report.ReportConstants.*;

/**
 * Parse command line argumens using the following format.
 *
 * Usage: trend-report title reportPath outputPath [date] [offset]
 *   title: Title to be used for this report
 *   reportPath: Directory containing timestamped Japex reports
 *   outputPath: Directory where the trend report will be placed
 *   date: Starting/ending date for this report in the format 'YYYY-MM-DD' (default today)
 *   offset: Positive or negative offset from 'date' in the format '-?[0-9]+(D|W|M|Y)'
 *     where D=days, W=weeks, M=months and Y=years (default -1Y)
 *
 */
public class TrendReportParams {
    
    String _pageTitle;
    
    String _reportPath;
    
    String _outputPath;
    
    Calendar _from, _to;
        
    String _title;
    
    DateFormat _dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                
    public TrendReportParams(String[] args) {
        if (args.length < 3 || args.length > 5) {
            displayUsageAndExit();
        }
        
        _pageTitle = args[0];
        _reportPath = args[1];
        _outputPath = args[2];

        String date = null;
        String offset = null;        
        switch (args.length) {
            case 3:
                date = DEFAULT_STARTDATE;
                offset = DEFAULT_DATEOFFSET;
                break;
            case 4:
                try {
                    date = args[3];
                    offset = DEFAULT_DATEOFFSET;
                    _dateFormat.parse(date);            
                }
                catch (ParseException e1) {
                    // Must be an offset then
                    date = DEFAULT_STARTDATE;
                    offset = args[3];
                }        
                break;
            case 5:
                date = args[3];
                offset = args[4];
                break;
            default:
                assert false;                
        }
        
        parseDates(date, offset);
    }
        
    void parseDates(String date, String offset) {
        try {
            Date date1 = date.equals("today") ? new Date()
                : _dateFormat.parse(date);
            
            int n = Integer.parseInt(offset.substring(0, offset.length() - 1));
            char c = offset.toUpperCase().charAt(offset.length() - 1);
            Calendar cal = Calendar.getInstance();
            cal.setTime(date1);
            
            switch (c) {
                case 'D':
                    cal.add(Calendar.DATE, n);
                    break;
                case 'W':
                    cal.add(Calendar.WEEK_OF_YEAR, n);
                    break;
                case 'M':
                    cal.add(Calendar.MONTH, n);
                    break;
                case 'Y':
                    cal.add(Calendar.YEAR, n);
            }
            
            if (n > 0) {
                _from = Calendar.getInstance();
                _from.setTime(date1);
                _to = cal;
            } 
            else {
                _from = cal;
                _to = Calendar.getInstance();
                _to.setTime(date1);
                _to.add(Calendar.DATE, 1);      // needed?
            }
        } 
        catch (ParseException e) {
            displayUsageAndExit();
        }
    }
    
    public String pageTitle() {
        return _pageTitle;
    }
    
    public String reportPath() {
        return _reportPath;
    }
    
    public String outputPath() {
        return _outputPath;
    }
    
    public Calendar dateFrom() {
        return _from;
    }
    
    public Calendar dateTo() {
        return _to;
    }
    
    public String title() {
        return _title;
    }
    
    public void setTitle(String title) {
        _title = title;
    }
    
    private void displayUsageAndExit() {
        System.err.println("Usage: trend-report title reportPath outputPath [date] [offset]\n"
                + "  title: Title to be used for this report\n" 
                + "  reportPath: Directory containing timestamped Japex reports\n"
                + "  outputPath: Directory where the trend report will be placed\n"
                + "  date: Starting/ending date for this report in the format 'YYYY-MM-DD' (default today)\n"
                + "  offset: Positive or negative offset from 'date' in the format '-?[0-9]+(D|W|M|Y)'\n"
                + "    where D=days, W=weeks, M=months and Y=years (default -1Y)");
        System.exit(1);
    }
    
    // TODO: Why is this method here
    public static Calendar parseReportDirectory(File file) {
        Calendar result = null;
        
        if (file != null) {
            try {
                SimpleDateFormat formatter =
                        new SimpleDateFormat(ReportConstants.REPORT_DIRECTORY_FORMAT);
                Date date = formatter.parse(file.getName());
                result = new GregorianCalendar();
                result.setTime(date);
            } 
            catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }
        return result;
    }
}
