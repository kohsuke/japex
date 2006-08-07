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

import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.HashMap;

public class ParseReports {
    
    ArrayList _reports = new ArrayList();
    
    ArrayList _dates = new ArrayList();
    
    boolean hasReport = false;
    
    public ParseReports(TrendReportParams params) {
        File cwd = new File(params.reportPath());
        ReportFilter filter =new ReportFilter(params.dateFrom(), params.dateTo());
        File[] reportDirs = cwd.listFiles(filter);
        if (reportDirs == null) {
            System.out.println("No report found between " + params.dateFrom() +
                    " and " + params.dateTo() + ". exit.");
            return;
            
        }
        Arrays.sort(reportDirs, new DateComparator(params.reportByName));
        
        String separator = System.getProperty("file.separator");
        ReportDataParser handler = null;
        SAXParserFactory factory = SAXParserFactory.newInstance();
        
        try {
            // Parse the input
            SAXParser saxParser = factory.newSAXParser();
            GregorianCalendar cal = new GregorianCalendar();
            
            for (int i = 0; i < reportDirs.length; i++) {
//System.out.println("report "+(i+1)+": "+reportDirs[i].getName());
                File file = new File(reportDirs[i].getAbsolutePath()+separator+"report.xml");
                if (file.exists()) {
                    Date date;
                    if (params.reportByName) {
                        date = TrendReportParams.parseReportDirectory(reportDirs[i]);
                    } else {
                        date = new Date(reportDirs[i].lastModified());
                    }
                    cal.setTime(date);
                    handler = new ReportDataParser(params);
                    saxParser.parse(file, handler);
                    Map report = (Map)handler.getReports();
                    if (report != null) {
                        _reports.add(report);
                        _dates.add(date);
                        hasReport = true;
                    }
                }
            }            
        }
        catch (RuntimeException e) {
            throw e;
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }        
    }
    
    public Map[] getReports() {
        if (!hasReport) return null;
        Map[] reports = new HashMap[_reports.size()];
        reports = (Map[])_reports.toArray(reports);
        return reports;
    }
    
    public Date[] getDates() {
        if (!hasReport) return null;
        Date[] dates = new Date[_reports.size()];
        dates = (Date[])_dates.toArray(dates);
        return dates;
    }
}