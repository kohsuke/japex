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

package com.sun.japex;

import com.sun.japex.report.*;

import java.io.File;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.BufferedOutputStream;
import java.net.URL;

public class TrendReport {
    
    public static final String FILE_SEP = System.getProperty("file.separator");
    
    public TrendReport() {
    }
    
    public static void main(String[] args) {
        TrendReportParams params = new TrendReportParams(args);
        new TrendReport().run(params);            
        System.exit(0);        
    }
    
    public void run(TrendReportParams params) {       
        try {
            ParseReports testReports = new ParseReports(params);            
            new ReportGenerator(params, testReports).createReport();            
            
            // Copy some resources to output directory
            copyResource("report.css", params.outputPath());
            copyResource("small_japex.gif", params.outputPath());
        }            
        catch (RuntimeException e) {
            throw e;
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void copyResource(String basename, String outputDir) {
        InputStream is;
        OutputStream os;
        
        try {
            int c;
            URL css = getClass().getResource("/resources/" + basename);
            if (css != null) {
                is = css.openStream();
                os = new BufferedOutputStream(new FileOutputStream(
                        new File(outputDir + FILE_SEP + basename)));
                
                while ((c = is.read()) != -1) {
                    os.write(c);
                }
                is.close();
                os.close();
            }
        } 
        catch (RuntimeException e) {
            throw e;
        } 
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
         
}
