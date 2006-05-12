<?xml version='1.0'?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
    xmlns:rep="http://www.sun.com/japex/testSuiteReport"
    xmlns:extrep="http://www.sun.com/japex/extendedTestSuiteReport"
    version='1.0'>

    <xsl:output method="xml" indent="yes"/>
     
    <xsl:template match="rep:testSuiteReport">
        <html xmlns="http://www.w3.org/1999/xhtml">
            <link href="report.css" type="text/css" rel="stylesheet"/>

            <body>
                <table border="0" cellpadding="2">
                    <tr>
                        <td valign="middle" width="90"><p>
                            <a href="https://japex.dev.java.net">
                                <img src="small_japex.gif" align="middle" border="0"/>
                            </a>
                        </p></td>
                        <td valign="middle"><h1>Japex Report: <xsl:value-of select="@name"/></h1></td>  
                    </tr>
                </table>
   
                <h2>Global Parameters</h2>
                <ul>
                    <!-- Use not(@name) to omit sibling drivers -->
                    <xsl:for-each select="*[not(@name)]">
                        <li><xsl:value-of select="name()"/>
                        <xsl:text>: </xsl:text>
                        <xsl:value-of select="."/></li>
                    </xsl:for-each>
                </ul>

                <!-- Generate result summary section -->
                <xsl:call-template name="resultsSummary"/>
      
                <br/><br/>
                <center><img src="{/*/extrep:resultChart}"/></center>
                <br/><br/>
      
                <!-- Generate detailed result per driver -->
                <xsl:for-each select="rep:driver">
                    <xsl:call-template name="resultsPerDriver"/>
                </xsl:for-each>

                <xsl:choose>
                    <xsl:when test="rep:plotDrivers = 'true'">
                        <h2>Results Per Driver</h2>
                    </xsl:when>
                    <xsl:otherwise>
                        <h2>Results Per Test</h2>
                    </xsl:otherwise>
                </xsl:choose>      
                <br/>
                <xsl:for-each select="/*/extrep:testCaseChart">
                    <center><img src="{.}"/></center>
                    <br/><br/>
                </xsl:for-each>
      
                <br/>
                <small>
                    <hr/>
                    <font size="-2">
                        Generated using <a href="https://japex.dev.java.net">Japex</a> version 
                        <xsl:value-of select="rep:version"/>
                    </font>
                </small>
            </body>   
        </html>
    </xsl:template>

    <xsl:template name="resultsSummary">
        <h2>Result Summary 
        (<xsl:value-of select="/*/rep:testSuiteReport/rep:resultUnit"/>)</h2>
    
        <!-- 
        - Use an HTML table to list all the Japex driver params except
        - classPath and driverClass. User-defined params are also ignored here.
        -->    
        <table width="80%" border="1">
            <thead>
                <tr><th width="15%"><b>driver</b></th>
                    <xsl:for-each select="rep:driver[1]/*[not(@name) and namespace-uri(.)!='']">
                        <!-- Ignore classPath and driverClass here to keep the table narrow -->
                        <xsl:if test="name() != 'classPath' and name() != 'driverClass'">
                            <th><b><xsl:value-of select="name()"/></b></th>
                        </xsl:if>
                    </xsl:for-each>
                </tr>
            </thead>
            <tbody>
                <xsl:for-each select="rep:driver">
                    <tr><td align="right"><xsl:value-of select="@name"/></td>
                        <xsl:for-each select="*[not(@name) and namespace-uri(.)!='']">
                            <!-- Ignore classPath and driverClass here to keep the table narrow -->
                            <xsl:if test="name() != 'classPath' and name() != 'driverClass'">
                                <td align="right">
                                    <nobr>
                                        <xsl:variable name="value" select="."/>
                                        <xsl:choose>
                                            <xsl:when test="$value = 'NaN'">
                                                <font color="red"><xsl:value-of select="$value"/></font>
                                            </xsl:when>
                                            <xsl:otherwise>
                                                <xsl:value-of select="$value"/>
                                            </xsl:otherwise>
                                        </xsl:choose>
                                    </nobr>
                                </td>
                            </xsl:if>
                        </xsl:for-each>
                    </tr>
                </xsl:for-each>
            </tbody>
        </table>
    </xsl:template>

    <xsl:template name="resultsPerDriver">
        <h2>Driver: <xsl:value-of select="@name"/></h2>
    
        <!-- List classPath, driverClass and any user-define parameter here -->
        <p>
            <ul>
                <xsl:for-each select="rep:driverClass | rep:classPath | *[not(@name) and namespace-uri(.)='']">
                    <li><xsl:value-of select="name()"/>
                    <xsl:text>: </xsl:text>
                    <xsl:value-of select="."/></li>
                </xsl:for-each>
            </ul>
        </p>
      
        <!--
        - Use an HTML table to list all test case params, regardless of whether they
        - are user-defined or not. List all Japex params first, though.
        -->
        <table width="80%" border="1">
            <thead>
                <tr><th><b>testCase</b></th>
                    <xsl:for-each select="rep:testCase[1]/*[namespace-uri(.)!='']">
                        <th><b><xsl:value-of select="name()"/></b></th>
                    </xsl:for-each>
                    <xsl:for-each select="rep:testCase[1]/*[namespace-uri(.)='']">
                        <th><b><xsl:value-of select="name()"/></b></th>
                    </xsl:for-each>
                </tr>
            </thead>
            <tbody>
                <xsl:for-each select="rep:testCase">
                    <tr><td align="right">
                        <xsl:value-of select="@name"/></td>
                        <xsl:for-each select="*[namespace-uri(.)!='']">
                            <td align="right"><nobr>                          
                                <xsl:variable name="value" select="."/>
                                <xsl:choose>
                                    <xsl:when test="$value = 'NaN'">
                                        <font color="red"><xsl:value-of select="$value"/></font>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:value-of select="$value"/>
                                    </xsl:otherwise>
                                </xsl:choose>
                            </nobr></td>
                        </xsl:for-each>
                        <xsl:for-each select="*[namespace-uri(.)='']">
                            <td align="left"><nobr><xsl:value-of select="."/></nobr></td>
                        </xsl:for-each>
                    </tr>
                </xsl:for-each>
            </tbody>
        </table>
        <br/>
    </xsl:template>

    <xsl:template match="text()"/>

</xsl:stylesheet>
