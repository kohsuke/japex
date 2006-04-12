<?xml version='1.0'?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
    xmlns:rep="http://www.sun.com/japex/testSuiteReport"
    xmlns:reg="http://www.sun.com/japex/regressionReport"
    version='1.0'
    exclude-result-prefixes="rep">

    <xsl:output method="xml" indent="yes"/>
    
    <xsl:param name="lastReport"/>
    <xsl:param name="nextReport"/>

    <xsl:template match="/">
        <reg:regressionReport>
            <xsl:variable name="lastReportDoc" select="document($lastReport)/rep:testSuiteReport"/>
            <xsl:variable name="nextReportDoc" select="document($nextReport)/rep:testSuiteReport"/>
            
            <reg:lastReport><xsl:value-of select="$lastReport"/></reg:lastReport>
            <reg:nextReport><xsl:value-of select="$nextReport"/></reg:nextReport>
            <reg:resultUnit><xsl:value-of select="$nextReportDoc/rep:resultUnit"/></reg:resultUnit>
            <reg:version><xsl:value-of select="$nextReportDoc/rep:version"/></reg:version>
            
            <xsl:for-each select="$lastReportDoc/rep:driver">
                <xsl:variable name="driverName" select="@name"/>
                
                <xsl:variable name="resultAritMeanDiff"
                select="$nextReportDoc/rep:driver[@name = $driverName]/rep:resultAritMean - rep:resultAritMean"/>
                <xsl:variable name="resultGeomMeanDiff"
                select="$nextReportDoc/rep:driver[@name = $driverName]/rep:resultGeomMean - rep:resultGeomMean"/>
                <xsl:variable name="resultHarmMeanDiff"
                select="$nextReportDoc/rep:driver[@name = $driverName]/rep:resultHarmMean - rep:resultHarmMean"/>
                              
                <reg:driver name="{$driverName}">
                    <reg:resultAritMeanDiff>
                        <xsl:value-of select="format-number($resultAritMeanDiff, '0.000')"/>
                    </reg:resultAritMeanDiff>
                    <reg:resultAritMeanDiffAsPercentage>
                        <xsl:value-of select="format-number($resultAritMeanDiff div rep:resultAritMean * 100, '0.000')"/>
                    </reg:resultAritMeanDiffAsPercentage>
                    <reg:resultGeomMeanDiff>
                        <xsl:value-of select="format-number($resultGeomMeanDiff, '0.000')"/>
                    </reg:resultGeomMeanDiff>
                    <reg:resultGeomMeanDiffAsPercentage>
                        <xsl:value-of select="format-number($resultGeomMeanDiff div rep:resultGeomMean * 100, '0.000')"/>
                    </reg:resultGeomMeanDiffAsPercentage>
                    <reg:resultHarmMeanDiff>
                        <xsl:value-of select="format-number($resultHarmMeanDiff, '0.000')"/>
                    </reg:resultHarmMeanDiff>
                    <reg:resultHarmMeanDiffAsPercentage>
                        <xsl:value-of select="format-number($resultHarmMeanDiff div rep:resultHarmMean * 100, '0.000')"/>
                    </reg:resultHarmMeanDiffAsPercentage>
                </reg:driver>
            </xsl:for-each>                
        </reg:regressionReport>
    </xsl:template>

</xsl:stylesheet>
