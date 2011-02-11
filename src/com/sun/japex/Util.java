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

package com.sun.japex;

import java.io.*;
import java.util.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.net.URL;
import java.net.URLClassLoader;
import javax.xml.parsers.*;
import org.xml.sax.XMLReader;

public class Util {
    
    static final int KB = 1024;
    static final String spaces = "                                        ";
    
    static String getSpaces(int length) {
        return spaces.substring(0, length);
    }
    
    static public byte[] streamToByteArray(InputStream is) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(8 * KB);
        int c;
        try {
            while ((c = is.read()) != -1) {
                bos.write(c);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return bos.toByteArray();
    }
    
    static public ByteArrayInputStream streamToByteArrayInputStream(InputStream is) {
        return new ByteArrayInputStream(streamToByteArray(is));
    }
    
    public static long parseDuration(String duration) {
        try {
            int length = duration.length();
            switch (length) {
                case 1:
                case 2:
                    // S?S
                    return Integer.parseInt(duration.substring(0, length))
                    * 1000;
                case 5:
                    if (duration.charAt(2) == ':') {
                        // MM:SS
                        return Integer.parseInt(duration.substring(0, 2))
                        * 60 * 1000 +
                                Integer.parseInt(duration.substring(3, 5))
                                * 1000;
                    }
                    break;
                case 8:
                    // HH:MM:SS
                    if (duration.charAt(2) == ':' && duration.charAt(5) == ':') {
                        return Integer.parseInt(duration.substring(0, 2))
                        * 60 * 60 * 1000 +
                                Integer.parseInt(duration.substring(3, 5))
                                * 60 * 1000 +
                                Integer.parseInt(duration.substring(6, 8))
                                * 1000;
                    }
                    break;
            }
        } catch (NumberFormatException e) {
            // Falls through
        }
        throw new RuntimeException("Duration '" + duration
                + "' does not conform to pattern '((HH:)?MM:)?S?S'");
    }
    
    public static long currentTimeNanos() {
        return System.nanoTime();
    }
    
    public static double currentTimeMillis() {
        return nanosToMillis(System.nanoTime());
    }
    
    public static long millisToNanos(long millis) {
        return millis * 1000000L;
    }
    
    public static double nanosToMillis(long nanos) {
        return nanos / 1000000.0;
    }
    
    public static double arithmeticMean(double[] sample) {
        return arithmeticMean(sample, 0);
    }
    
    public static double arithmeticMean(double[] sample, int start) {
        double mean = 0.0;
        for (int i = start; i < sample.length; i++) {
            mean += sample[i];
        }
        return (mean / (sample.length - start));
    }
    
    public static double standardDev(double[] sample) {
        return standardDev(sample, 0);
    }
    
    public static double standardDev(double[] sample, int start) {
        double mean = arithmeticMean(sample, start);
        
        // Compute biased variance
        double variance = 0.0;
        for (int i = start; i < sample.length; i++) {
            variance += (sample[i] - mean) * (sample[i] - mean);
        }
        variance /= (sample.length - start);
        
        // Return standard deviation
        return Math.sqrt(variance);
    }
    
    public static double arithmeticMean(long[] sample) {
        return arithmeticMean(sample, 0);
    }
    
    public static double arithmeticMean(long[] sample, int start) {
        double mean = 0.0;
        for (int i = start; i < sample.length; i++) {
            mean += sample[i];
        }
        return (mean / (sample.length - start));
    }
    
    public static double standardDev(long[] sample) {
        return standardDev(sample, 0);
    }
    
    public static double standardDev(long[] sample, int start) {
        double mean = arithmeticMean(sample, start);
        
        // Compute biased variance
        double variance = 0.0;
        for (int i = start; i < sample.length; i++) {
            variance += (sample[i] - mean) * (sample[i] - mean);
        }
        variance /= (sample.length - start);
        
        // Return standard deviation
        return Math.sqrt(variance);
    }
    
    /**
     * Create an instance of <code>DecimalFormat</code> to format numbers
     * as xsd:decimal. That is, using '.' as decimal separator and without
     * using ',' for grouping.
     */
    static DecimalFormat _decimalFormat;
    static {
        DecimalFormatSymbols dfs = new DecimalFormatSymbols(Locale.US);
        dfs.setDecimalSeparator('.');   // should be redundant
        _decimalFormat = new DecimalFormat("0.###", dfs);
        _decimalFormat.setGroupingSize(Byte.MAX_VALUE);
    }
    
    public static String formatDouble(double value) {
        return Double.isNaN(value) ? "NaN" : _decimalFormat.format(value);
    }
    
    public static String getManifestAsString(URLClassLoader cl, String jarBaseName) {
        try {
            Enumeration<URL> e = ((URLClassLoader) cl).findResources("META-INF/MANIFEST.MF");
            
            while (e.hasMoreElements()) {
                URL url = e.nextElement();
                String urlString = url.toString();
                
                // Have we found the right jar?
                if (urlString.indexOf(jarBaseName) > 0) {
                    StringBuilder sb = new StringBuilder();
                    int c;
                    InputStream is = url.openStream();
                    while ((c = is.read()) != -1) {
                        char ch = (char) c;
                        sb.append(Character.isWhitespace(ch) ? ' ' : ch);
                    }
                    return sb.toString();
                }
            }
            return "";
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    
    /**
     * Calculate group sizes for tests to avoid a very small final group.
     * For example, calculateGroupSizes(21, 5) return { 5,5,5,3,3 } instead
     * of { 5,5,5,5,1 }.
     */
    public static int[] calculateGroupSizes(int nOfTests, int maxGroupSize) {
        if (nOfTests <= maxGroupSize) {
            return new int[] { nOfTests };
        }
        
        int[] result = new int[nOfTests / maxGroupSize +
                ((nOfTests % maxGroupSize > 0) ? 1 : 0)];
        
        // Var m1 represents the number of groups of size maxGroupSize
        int m1 = (nOfTests - maxGroupSize) / maxGroupSize;
        for (int i = 0; i < m1; i++) {
            result[i] = maxGroupSize;
        }
        
        // Var m2 represents the number of tests not allocated into groups
        int m2 = nOfTests - m1 * maxGroupSize;
        if (m2 <= maxGroupSize) {
            result[result.length - 1] = m2;
        } else {
            // Allocate last two groups
            result[result.length - 2] = (int) Math.ceil(m2 / 2.0);
            result[result.length - 1] = m2 - result[result.length - 2];
        }
        return result;
    }
    
    //return a legal filename from a testcase title
    public static String getFilename(String testcase) {
        StringBuffer filename = new StringBuffer();
        for (int i=0; i<testcase.length(); i++) {
            char achar = testcase.charAt(i);
            if (achar == 46) { //.
                char nchar = testcase.charAt(i+1);
                filename.append("_");
                if (nchar == 47 || nchar == 92) {  //./
                    i++;
                }
            } else if (achar == 47 || achar == 92 || achar == 32) { // / \ and space
                filename.append("_");
            } else {
                filename.append(achar);
            }
        }
        return filename.toString();
    }
    
    private static SAXParserFactory saxParserFactory;
    
    static {
        saxParserFactory = SAXParserFactory.newInstance();
        saxParserFactory.setNamespaceAware(true);
        try {
            saxParserFactory.setXIncludeAware(true);
        } catch (UnsupportedOperationException e) {
            System.err.print("Warning: Available SAX parser factory does not support XInclude");
        }
    }
    
    public static XMLReader getXIncludeXMLReader() {
        try {
            return saxParserFactory.newSAXParser().getXMLReader();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    public static void copyFile(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);
        
        int len;
        byte[] buf = new byte[1024];
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }
    
    public static void copyDirectory(File srcDir, File dstDir) throws IOException {
        if (srcDir.isDirectory()) {
            if (!dstDir.exists()) {
                dstDir.mkdir();
            }
            
            String[] children = srcDir.list();
            for (int i = 0; i < children.length; i++) {
                copyDirectory(new File(srcDir, children[i]),
                              new File(dstDir, children[i]));
            }
        } 
        else {
            copyFile(srcDir, dstDir);
        }
    }
    
    public static void copyResource(String basename, String outputDir, String fileSep) {
        InputStream is;
        OutputStream os;
        
        try {
            int c;
            URL css = Util.class.getResource("/resources/" + basename);
            if (css != null) {
                is = css.openStream();
                os = new BufferedOutputStream(new FileOutputStream(
                        new File(outputDir + fileSep + basename)));

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
