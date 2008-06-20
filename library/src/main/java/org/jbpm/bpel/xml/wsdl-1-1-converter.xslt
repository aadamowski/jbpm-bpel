<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:wsdl="http://schemas.xmlsoap.org/wsdl/"
  xmlns:plt11="http://schemas.xmlsoap.org/ws/2003/05/partner-link/"
  xmlns:vprop11="http://schemas.xmlsoap.org/ws/2003/03/business-process/"
  xmlns:exslt="http://exslt.org/common">
  
  <xsl:output method="xml" />

  <xsl:variable name="plt-old-uri" select="'http://schemas.xmlsoap.org/ws/2003/05/partner-link/'" />
  <xsl:variable name="plt-new-uri" select="'http://docs.oasis-open.org/wsbpel/2.0/plnktype'" />
  <xsl:variable name="vprop-old-uri" select="'http://schemas.xmlsoap.org/ws/2003/03/business-process/'" />
  <xsl:variable name="vprop-new-uri" select="'http://docs.oasis-open.org/wsbpel/2.0/varprop'" />

  <!--partnerLinkType role-->
  <xsl:template match="plt11:role">
    <xsl:element name="plt2:role" namespace="{$plt-new-uri}">
      <!-- namespaces -->
      <xsl:call-template name="copyNamespaces" />
      <!-- attributes -->
      <xsl:copy-of select="@*" />
      <xsl:attribute name="portType">
        <xsl:value-of select="plt11:portType/@name" />
      </xsl:attribute>
      <!-- child elements -->
      <xsl:apply-templates select="*[namespace-uri() != $plt-old-uri or local-name() != 'portType']" />
    </xsl:element>
  </xsl:template>

  <!--propertyAlias-->
  <xsl:template match="vprop11:propertyAlias">
    <xsl:element name="vprop2:propertyAlias" namespace="{$vprop-new-uri}">
      <!-- namespaces -->
      <xsl:call-template name="copyNamespaces" />
      <!-- attributes -->
      <xsl:copy-of select="@*[name() != 'query']" />
      <xsl:if test="@query">
        <!-- write a non-prefixed element, so that the old namespace
           - declaration keeps the prefix occurring in the XML source -->
        <xsl:element name="query" namespace="{$vprop-new-uri}">
          <!-- exclude the default namespace declaration, which affects a non-prefixed element -->
          <xsl:call-template name="copyNonDefaultNamespaces" />
          <xsl:value-of select="@query" />
        </xsl:element>
      </xsl:if>
      <!-- child elements -->
      <xsl:apply-templates select="*" />
    </xsl:element>
  </xsl:template>

  <!--definitions-->
  <xsl:template match="wsdl:definitions">
    <xsl:element name="{name()}" namespace="{namespace-uri()}">
      <!-- namespaces -->
      <xsl:call-template name="copyNamespaces" />
      <!-- attributes -->
      <xsl:copy-of select="@*" />
      <!-- child elements -->
      <xsl:apply-templates select="*" />
    </xsl:element>
  </xsl:template>

  <!-- other plt elements -->
  <xsl:template match="plt11:*">
    <xsl:element name="plt2:{local-name()}" namespace="{$plt-new-uri}">
      <!-- namespaces -->
      <xsl:call-template name="copyNamespaces" />
      <!-- attributes -->    
      <xsl:copy-of select="@*" />
      <!-- child elements -->
      <xsl:apply-templates select="*" />
    </xsl:element>
  </xsl:template>

  <!--other bpel elements-->
  <xsl:template match="vprop11:*">
    <xsl:element name="vprop2:{local-name()}" namespace="{$vprop-new-uri}">
      <!-- namespaces -->
      <xsl:call-template name="copyNamespaces" />
      <!-- attributes -->    
      <xsl:copy-of select="@*" />
      <!-- child elements -->
      <xsl:apply-templates select="*" />
    </xsl:element>
  </xsl:template>
  
  <!-- other elements -->
  <xsl:template match="*">
    <xsl:copy-of select="." />
  </xsl:template>

  <!--copies namespace declarations excluding the default, the old vprop and plt namespaces -->
  <xsl:template name="copyNamespaces">
    <xsl:for-each select="namespace::*">
      <xsl:variable name="namespace" select="string(.)" />
      <!-- excluding default namespace declarations is required to prevent the transformer
         - from using the default namespace to qualify non-prefixed elements, even if they
         - were assigned a namespace of their own -->
      <!-- we can safely omit declarations of the old vprop and partner link namespaces,
         - as the presence of extension elements will cause the transformer to declare them -->
      <xsl:if test="name(.) and $namespace != $vprop-old-uri and $namespace != $plt-old-uri">
        <xsl:copy/>
      </xsl:if>
    </xsl:for-each>
  </xsl:template>
  
  <!-- copies namespace declarations excluding the default -->
  <xsl:template name="copyNonDefaultNamespaces">
    <xsl:for-each select="namespace::*">
      <xsl:if test="name(.)">
        <xsl:copy/>
      </xsl:if>
    </xsl:for-each>
  </xsl:template>  

</xsl:stylesheet>