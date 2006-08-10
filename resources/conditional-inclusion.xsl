<?xml version='1.0'?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
    xmlns:japex="http://www.sun.com/japex/testSuite"
    xmlns:xi="http://www.w3.org/2001/XInclude"    
    version='1.0'>

    <!-- Format of this property list should be ' name1 name2 ... nameN ' -->
    <xsl:param name="property-list" select="''"/>
    
    <xsl:output method="xml" indent="yes"/>

    <xsl:template match="*[@japex:if]">
        <xsl:choose>            
            <!-- Search for ' name ' -->
            <xsl:when test="contains($property-list, concat(' ', @japex:if, ' '))">
                <xsl:call-template name="copy-element"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:message>
                    <xsl:text>  Ignoring: &#10;</xsl:text>
                    <xsl:copy-of select="."/>
                </xsl:message>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
    <xsl:template match="*[@japex:unless]">
        <xsl:choose>            
            <!-- Search for ' name ' -->
            <xsl:when test="not(contains($property-list, concat(' ', @japex:if, ' ')))">
                <xsl:call-template name="copy-element"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:message>
                    <xsl:text>  Ignoring: &#10;</xsl:text>
                    <xsl:copy-of select="."/>
                </xsl:message>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
    <xsl:template match="*">
        <xsl:call-template name="copy-element"/>
    </xsl:template>

    <xsl:template name="copy-element">
        <xsl:copy>
            <xsl:for-each select="@*">
                <xsl:copy/>
            </xsl:for-each>
            <xsl:apply-templates/>
        </xsl:copy>
    </xsl:template>
    
    <xsl:template match="text()">
        <xsl:copy/>
    </xsl:template>

</xsl:stylesheet>
