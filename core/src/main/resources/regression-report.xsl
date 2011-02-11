<?xml version='1.0'?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
    xmlns:rep="http://www.sun.com/japex/testSuiteReport"
    xmlns:reg="http://www.sun.com/japex/regressionReport"
    version='1.0'
    exclude-result-prefixes="rep">

    <xsl:output method="xml" indent="yes"/>
    
    <xsl:param name="lastReport"/>
    <xsl:param name="nextReport"/>
    <xsl:param name="lastReportHref"/>
    <xsl:param name="nextReportHref"/>
    <xsl:param name="threshold" select="-1.0"/>

    <xsl:template match="/">
        <xsl:variable name="lastReportDoc" select="document($lastReport)/rep:testSuiteReport"/>
        <xsl:variable name="nextReportDoc" select="document($nextReport)/rep:testSuiteReport"/>
        
        <xsl:variable name="resultsDelta">
            <xsl:for-each select="$nextReportDoc/rep:driver">
                <xsl:variable name="driverName" select="@name"/>
                
                <!-- Compute deltas between last report and next report -->
                <xsl:variable name="resultAritMeanDiff"
                    select="rep:resultAritMean - $lastReportDoc/rep:driver[@name = $driverName]/rep:resultAritMean"/>
                <xsl:variable name="resultGeomMeanDiff"
                    select="rep:resultGeomMean - $lastReportDoc/rep:driver[@name = $driverName]/rep:resultGeomMean"/>
                <xsl:variable name="resultHarmMeanDiff"
                    select="rep:resultHarmMean - $lastReportDoc/rep:driver[@name = $driverName]/rep:resultHarmMean"/>
                              
                <!-- Compute deltas as a percetange -->
                <xsl:variable name="resultAritMeanDiffAsPercentage"
                    select="$resultAritMeanDiff div rep:resultAritMean * 100"/>
                <xsl:variable name="resultGeomMeanDiffAsPercentage"
                    select="$resultGeomMeanDiff div rep:resultGeomMean * 100"/>
                <xsl:variable name="resultHarmMeanDiffAsPercentage"
                    select="$resultHarmMeanDiff div rep:resultHarmMean * 100"/>
                
                <!-- Calculate conditions to notify regressions -->
                <xsl:variable name="aritThresholdMet"
                    select="$resultAritMeanDiffAsPercentage &gt; $threshold or -$resultAritMeanDiffAsPercentage &gt; $threshold"/>
                
                <!-- 
                   - Include per-driver section in output report. Notify only if arithmetic mean 
                   - difference exceeds the threshold 
                   -->
                <reg:driver name="{$driverName}" notify="{$aritThresholdMet}">
                    <reg:resultAritMeanDiff>
                        <xsl:value-of select="format-number($resultAritMeanDiff, '0.000')"/>
                    </reg:resultAritMeanDiff>
                    <reg:resultAritMeanDiffAsPercentage>
                        <xsl:value-of select="format-number($resultAritMeanDiffAsPercentage, '0.000')"/>
                    </reg:resultAritMeanDiffAsPercentage>
                    <reg:resultGeomMeanDiff>
                        <xsl:value-of select="format-number($resultGeomMeanDiff, '0.000')"/>
                    </reg:resultGeomMeanDiff>
                    <reg:resultGeomMeanDiffAsPercentage>
                        <xsl:value-of select="format-number($resultGeomMeanDiffAsPercentage, '0.000')"/>
                    </reg:resultGeomMeanDiffAsPercentage>
                    <reg:resultHarmMeanDiff>
                        <xsl:value-of select="format-number($resultHarmMeanDiff, '0.000')"/>
                    </reg:resultHarmMeanDiff>
                    <reg:resultHarmMeanDiffAsPercentage>
                        <xsl:value-of select="format-number($resultHarmMeanDiffAsPercentage, '0.000')"/>
                    </reg:resultHarmMeanDiffAsPercentage>
                </reg:driver>
            </xsl:for-each>    
        </xsl:variable>
            
        <reg:regressionReport>
            <!-- Set notify attribute if one or more drivers have set notify to true -->
            <xsl:attribute name="notify">
                <xsl:choose>
                    <xsl:when test="nodeset($resultsDelta)/reg:driver[@notify = 'true']">true</xsl:when>
                    <xsl:otherwise>false</xsl:otherwise>
                </xsl:choose>
            </xsl:attribute>
            
            <reg:lastReport><xsl:value-of select="$lastReport"/></reg:lastReport>
            <reg:nextReport><xsl:value-of select="$nextReport"/></reg:nextReport>
            <xsl:if test="$lastReportHref != ''">
                <reg:lastReportHref><xsl:value-of select="$lastReportHref"/></reg:lastReportHref>
                <reg:nextReportHref><xsl:value-of select="$nextReportHref"/></reg:nextReportHref>
            </xsl:if>
            <reg:resultUnit><xsl:value-of select="$nextReportDoc/rep:resultUnit"/></reg:resultUnit>
            <reg:version><xsl:value-of select="$nextReportDoc/rep:version"/></reg:version>
            <reg:threshold><xsl:value-of select="$threshold"/>%</reg:threshold>
            <xsl:copy-of select="$resultsDelta"/>
        </reg:regressionReport>        
    </xsl:template>

</xsl:stylesheet>
