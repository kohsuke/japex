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

import java.util.Date;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

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
 * @author Santiago.PericasGeertsen@sun.com
 * @author Joe.Wang@sun.com
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
    
}
