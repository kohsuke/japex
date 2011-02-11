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
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.Date;
import java.util.Locale;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import static com.sun.japex.report.TrendReport.FILE_SEP;

/**
 * Report page generator.
 *
 * @author Joe.Wang@sun.com
 * @author Santiago.PericasGeertsen@sun.com
 */
public class IndexPage {

    static final String REPORT_TITLE = "<!--{title}-->";
    static final String REPORT_NEWINDEX = "<!--{new index}-->";
    static final String REPORT_NEWROW = "<!--{new row}-->";
    
    TrendReportParams _params;
    
    String _chartName;
   
    StringBuffer _content;
    
    public IndexPage(TrendReportParams params, String chartName) {
        _params = params;
        _chartName = chartName;
    }
    
    public IndexPage(TrendReportParams params, boolean openfile) {
        _params = params;
        if (openfile) {
            try {
                _content = new StringBuffer();
               _content.append(getTemplate());
            } 
            catch (RuntimeException e) {
                throw e;
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
    
    public void updateContent(String chartName) {     
        int start = 0;
        int end = 0;
        StringBuffer newindex = new StringBuffer();
        newindex.append("<option value=\"#" + _params.title() + "\">" + 
                _params.title() + "</option>");
        newindex.append(REPORT_NEWINDEX);
        
        StringBuffer newrow = new StringBuffer();
        newrow.append("<table id=\"" + _params.title() + "\" ");
        newrow.append("width=\"100%\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\"><tbody>");
        newrow.append("<tr valign=\"top\"><td width=\"90%\"><font color=\"005A9C\" size=\"5\">");
        newrow.append("</font></td><td align=\"right\"><a href=\"#top\"><font size=\"3\">[Top]</font></a></td></tr>");
        newrow.append("</tbody></table>\n");
        newrow.append("<table width=\"100%\" border=\"0\">");
        newrow.append("<tr><td colspan=\"2\" align=\"center\"><img src=\"" + 
                chartName + "\"></td></tr>");
        newrow.append("</table><br>");
        newrow.append(REPORT_NEWROW);
        
        start = _content.indexOf(REPORT_TITLE);
        if (start > 0) {
            end = start + REPORT_TITLE.length();
            _content.replace(start, end, _params.pageTitle());
        }
        
        start = _content.indexOf(REPORT_NEWINDEX);
        end = start + REPORT_NEWINDEX.length();
        _content.replace(start, end, newindex.toString());
        
        start = _content.indexOf(REPORT_NEWROW);
        end = start + REPORT_NEWROW.length();
        _content.replace(start, end, newrow.toString());
    }
    
    public void writeContent() {
        try {
            String filename = _params.outputPath() + FILE_SEP + "index.html";
            File file = new File(filename);
            OutputStreamWriter osr = new OutputStreamWriter(new FileOutputStream(file));
            osr.write(_content.toString());
            osr.close();
        }             
        catch (RuntimeException e) {
            throw e;
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    private String getTemplate() {
        StringBuffer template = new StringBuffer();
        template.append("<html>\n<link href=\"report.css\" type=\"text/css\" rel=\"stylesheet\"/>\n");
        template.append("<head><script>"
                + "function jump(menu){window.location.hash=menu.choice.options[menu.choice.selectedIndex].value;}"
                + "</script></head>");   
        template.append("<body><table border=\"0\" cellpadding=\"2\"><tr>" +
            "<td valign=\"middle\" width=\"90\"><p>" +
            "<a href=\"https://japex.dev.java.net\">" +
            "<img src=\"small_japex.gif\" align=\"middle\" border=\"0\"/>" +
            "</a></p></td><td valign=\"middle\">" +
            "<h1>Japex Trend Report: " + REPORT_TITLE + "</h1></td></tr></table>");
        template.append("<h2>Global Parameters</h2>");
        template.append("<ul>\n<li>Report Path: " + _params.reportPath() +  "</li>\n");
        template.append("<li>Output Path: " + _params.outputPath() + "</li>\n");
        DateFormat df = new SimpleDateFormat("dd MMM yyyy/HH:mm:ss z", Locale.ENGLISH);
        template.append("<li>Report Period: " + _params.dateFrom().getTime() + " - " 
                + _params.dateTo().getTime() + "</li>\n");
        template.append("<li>Timestamp: " + df.format(new Date()));
        template.append("</li>\n</ul>\n");
        template.append("<h2>Results</h2>");
        template.append("<form action=\"dummy\" method=\"post\">"
                + "<select name=\"choice\" size=\"1\" onChange=\"jump(this.form)\">");
        template.append(REPORT_NEWINDEX);
        template.append("</select></form><br>");
        template.append(REPORT_NEWROW);
        
        template.append("<br><br><small><hr/><font size=\"-2\">Generated using " +
                "<a href=\"https://japex.dev.java.net\">Japex Trend Report</a> version " +
                ReportConstants.VERSION + "</font></small>");        
        template.append("</body>\n</html>");
        return template.toString();
    }
    
}
