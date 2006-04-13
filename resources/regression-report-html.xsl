<?xml version='1.0'?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
    xmlns:rep="http://www.sun.com/japex/testSuiteReport"
    xmlns:reg="http://www.sun.com/japex/regressionReport"
    version='1.0'>

    <xsl:output method="xml" indent="yes"/>
     
    <xsl:template match="reg:regressionReport">
        <html xmlns="http://www.w3.org/1999/xhtml">
            <link href="report.css" type="text/css" rel="stylesheet"/>

            <body>
            <table border="0" cellpadding="2">
                <tr>
                    <td valign="middle" width="90"><p>
                        <a href="https://japex.dev.java.net">
                            <img src="https://japex.dev.java.net/images/small_japex.gif" align="middle" border="0"/>
                        </a>
                    </p></td>
                    <td valign="middle"><h1>Japex Regression Report</h1></td>  
                </tr>
            </table>
   
            <h2>Parameters</h2>
            <ul>
                <li>Last report: <xsl:value-of select="reg:lastReport"/></li>
                <li>Next report: <xsl:value-of select="reg:nextReport"/></li>
                <li>Result unit: <xsl:value-of select="reg:resultUnit"/></li>
                <li>Threshold: <xsl:value-of select="reg:threshold"/></li>
            </ul>

            <table width="80%" border="1">
                <thead>
                    <tr>
                        <th><b>Driver</b></th>
                        <th><b>Arithmetic Mean</b></th>
                        <th><b>Geometric Mean</b></th>
                        <th><b>Harmonic Mean</b></th>
                    </tr>
                </thead>
                <tbody>
                    <xsl:for-each select="reg:driver">
                        <tr>
                            <td align="left"><xsl:value-of select="@name"/></td>
                            
                            <xsl:choose>
                                <xsl:when test ="reg:resultAritMeanDiffAsPercentage >= 0">
                                    <td align="center" bgcolor="green">
                                        <font color="white">
                                            <xsl:value-of select="reg:resultAritMeanDiffAsPercentage"/>
                                            <xsl:text>% (</xsl:text>
                                            <xsl:value-of select="reg:resultAritMeanDiff"/>
                                            <xsl:text>)</xsl:text>
                                        </font>
                                    </td>
                                </xsl:when>
                                <xsl:otherwise>                                    
                                    <td align="center" bgcolor="red">
                                        <font color="white">
                                            <xsl:value-of select="reg:resultAritMeanDiffAsPercentage"/>
                                            <xsl:text>% (</xsl:text>
                                            <xsl:value-of select="reg:resultAritMeanDiff"/>
                                            <xsl:text>)</xsl:text>
                                        </font>
                                    </td>
                                </xsl:otherwise>
                            </xsl:choose>
                            
                            <xsl:choose>
                                <xsl:when test ="reg:resultGeomMeanDiffAsPercentage >= 0">
                                    <td align="center" bgcolor="green">
                                        <font color="white">
                                            <xsl:value-of select="reg:resultGeomMeanDiffAsPercentage"/>
                                            <xsl:text>% (</xsl:text>
                                            <xsl:value-of select="reg:resultGeomMeanDiff"/>
                                            <xsl:text>)</xsl:text>
                                        </font>
                                    </td>
                                </xsl:when>
                                <xsl:otherwise>                                    
                                    <td align="center" bgcolor="red">
                                        <font color="white">
                                            <xsl:value-of select="reg:resultGeomMeanDiffAsPercentage"/>
                                            <xsl:text>% (</xsl:text>
                                            <xsl:value-of select="reg:resultGeomMeanDiff"/>
                                            <xsl:text>)</xsl:text>
                                        </font>
                                    </td>
                                </xsl:otherwise>
                            </xsl:choose>
                            
                            <xsl:choose>
                                <xsl:when test ="reg:resultHarmMeanDiffAsPercentage >= 0">
                                    <td align="center" bgcolor="green">
                                        <font color="white">
                                            <xsl:value-of select="reg:resultHarmMeanDiffAsPercentage"/>
                                            <xsl:text>% (</xsl:text>
                                            <xsl:value-of select="reg:resultHarmMeanDiff"/>
                                            <xsl:text>)</xsl:text>
                                        </font>
                                    </td>
                                </xsl:when>
                                <xsl:otherwise>                                    
                                    <td align="center" bgcolor="red">
                                        <font color="white">
                                            <xsl:value-of select="reg:resultHarmMeanDiffAsPercentage"/>
                                            <xsl:text>% (</xsl:text>
                                            <xsl:value-of select="reg:resultHarmMeanDiff"/>
                                            <xsl:text>)</xsl:text>
                                        </font>
                                    </td>
                                </xsl:otherwise>
                            </xsl:choose>                            
                        </tr>
                    </xsl:for-each>
                </tbody>
            </table>
            <br/>

            <small>
                <hr/>
                <font size="-2">
                    Generated using <a href="https://japex.dev.java.net">Japex</a> version 
                    <xsl:value-of select="reg:version"/>
                </font>
            </small>
            </body>   
        </html>
    </xsl:template>

    <xsl:template match="text()"/>

</xsl:stylesheet>
