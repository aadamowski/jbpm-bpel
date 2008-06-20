<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:exslt="http://exslt.org/common"
  xmlns:bpel11="http://schemas.xmlsoap.org/ws/2003/03/business-process/">
  <xsl:output method="xml" />

  <xsl:variable name="old-uri" select="'http://schemas.xmlsoap.org/ws/2003/03/business-process/'" />
  <xsl:variable name="new-uri" select="'http://docs.oasis-open.org/wsbpel/2.0/process/executable'" />

  <xsl:variable name="old-xpath-lang" select="'http://www.w3.org/TR/1999/REC-xpath-19991116'" />
  <xsl:variable name="new-xpath-lang" select="'urn:oasis:names:tc:wsbpel:2.0:sublang:xpath1.0'" />

  <!-- matching templates ////////////////////////// -->

  <!-- terminate: rename to exit -->
  <xsl:template match="bpel11:terminate">
    <xsl:element name="exit" namespace="{$new-uri}">
      <!-- namespaces -->
      <xsl:call-template name="copyNonBpelNamespaces" />
      <!-- attributes  -->
      <xsl:copy-of select="@*[name() != 'joinCondition']" />
      <!-- child elements -->
      <xsl:call-template name="copyActivityElements" />
    </xsl:element>
  </xsl:template>

  <!-- switch: rename to if -->
  <xsl:template match="bpel11:switch">
    <xsl:element name="if" namespace="{$new-uri}">
      <!-- namespaces -->
      <xsl:call-template name="copyNonBpelNamespaces" />
      <!-- attributes  -->
      <xsl:copy-of select="@*[name() != 'joinCondition']" />
      <!-- child elements -->
      <xsl:call-template name="copyStandardElements" />
      <!-- first case -->
      <xsl:for-each select="bpel11:case[1]">
        <xsl:call-template name="copyCaseContents" />
      </xsl:for-each>
      <!-- remaining cases -->
      <xsl:for-each select="bpel11:case[position() &gt; 1]">
        <xsl:element name="elseif" namespace="{$new-uri}">
          <xsl:call-template name="copyCaseContents" />
        </xsl:element>
      </xsl:for-each>
      <!-- otherwise -->
      <xsl:for-each select="bpel11:otherwise">
        <xsl:element name="else" namespace="{$new-uri}">
          <xsl:call-template name="copyBpelElementContents" />
        </xsl:element>
      </xsl:for-each>
    </xsl:element>
  </xsl:template>

  <!-- wait: move time expressions -->
  <xsl:template match="bpel11:wait">
    <xsl:element name="wait" namespace="{$new-uri}">
      <!-- namespaces -->
      <xsl:call-template name="copyNonBpelNamespaces" />
      <!-- attributes -->
      <xsl:copy-of select="@*[name() != 'for' and name() != 'until' and name() != 'joinCondition']" />
      <xsl:choose>
        <xsl:when test="@for">
          <xsl:element name="for" namespace="{$new-uri}">
            <xsl:call-template name="copyNonDefaultNamespaces" />
            <xsl:value-of select="@for" />
          </xsl:element>
        </xsl:when>
        <xsl:when test="@until">
          <xsl:element name="until" namespace="{$new-uri}">
            <xsl:call-template name="copyNonDefaultNamespaces" />
            <xsl:value-of select="@until" />
          </xsl:element>
        </xsl:when>
      </xsl:choose>
      <!-- child elements -->
      <xsl:call-template name="copyActivityElements" />
    </xsl:element>
  </xsl:template>

  <!-- while: move condition -->
  <xsl:template match="bpel11:while">
    <xsl:element name="while" namespace="{$new-uri}">
      <!-- namespaces -->
      <xsl:call-template name="copyNonBpelNamespaces" />
      <!-- attributes -->
      <xsl:copy-of select="@*[name() != 'condition' and name() != 'joinCondition']" />
      <xsl:call-template name="moveCondition" />
      <!-- child elements -->
      <xsl:call-template name="copyActivityElements" />
    </xsl:element>
  </xsl:template>

  <!-- compensate: rename to compensateScope -->
  <xsl:template match="bpel11:compensate">
    <xsl:choose>
      <xsl:when test="@scope">
        <xsl:element name="compensateScope" namespace="{$new-uri}">
          <!-- namespaces -->
          <xsl:call-template name="copyNonBpelNamespaces" />
          <!-- attributes -->
          <xsl:copy-of select="@*[name() != 'scope' and name() != 'joinCondition']" />
          <xsl:attribute name="target">
            <xsl:value-of select="@scope" />
          </xsl:attribute>
        </xsl:element>
        <!-- child elements -->
        <xsl:call-template name="copyActivityElements" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:element name="compensate" namespace="{$new-uri}">
          <!-- namespaces -->
          <xsl:call-template name="copyNonBpelNamespaces" />
          <!-- attributes -->
          <xsl:copy-of select="@*[name() != 'joinCondition']" />
          <!-- child elements -->
          <xsl:call-template name="copyActivityElements" />
        </xsl:element>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!-- scope: rename variableAccessSerializable -->
  <xsl:template match="bpel11:scope">
    <xsl:element name="scope" namespace="{$new-uri}">
      <!-- namespaces -->
      <xsl:call-template name="copyNonBpelNamespaces" />
      <!-- attributes -->
      <xsl:copy-of
        select="@*[name() != 'joinCondition' and name() != 'variableAccessSerializable']" />
      <xsl:if test="@variableAccessSerializable">
        <xsl:attribute name="isolated">
          <xsl:value-of select="@variableAccessSerializable" />
        </xsl:attribute>
      </xsl:if>
      <!-- child elements -->
      <xsl:call-template name="copyActivityElements" />
    </xsl:element>
  </xsl:template>

  <!-- other bpel 1.1 activities -->
  <xsl:template
    match="bpel11:*[local-name() = 'receive'
    or local-name() = 'reply'
    or local-name() = 'invoke'
    or local-name() = 'assign'
    or local-name() = 'empty'
    or local-name() = 'throw'
    or local-name() = 'sequence'
    or local-name() = 'pick'
    or local-name() = 'flow']">
    <xsl:element name="{local-name()}" namespace="{$new-uri}">
      <!-- namespaces -->
      <xsl:call-template name="copyNonBpelNamespaces" />
      <!-- attributes -->
      <xsl:copy-of select="@*[local-name() != 'joinCondition']" />
      <!-- child nodes -->
      <xsl:call-template name="copyActivityElements" />
    </xsl:element>
  </xsl:template>

  <!-- process: update the xpath language uri -->
  <xsl:template match="bpel11:process">
    <xsl:element name="process" namespace="{$new-uri}">
      <!-- namespaces -->
      <xsl:call-template name="copyNonBpelNamespaces" />
      <!-- attributes -->
      <xsl:copy-of select="@*[name() != 'expressionLanguage' and name() != 'queryLanguage']" />
      <xsl:if test="@expressionLanguage">
        <xsl:attribute name="expressionLanguage">
          <xsl:choose>
            <xsl:when test="@expressionLanguage = $old-xpath-lang">
              <xsl:value-of select="$new-xpath-lang" />
            </xsl:when>
            <xsl:otherwise>
              <xsl:value-of select="@expressionLanguage" />
            </xsl:otherwise>
          </xsl:choose>
        </xsl:attribute>
      </xsl:if>
      <xsl:if test="@queryLanguage">
        <xsl:attribute name="queryLanguage">
          <xsl:choose>
            <xsl:when test="@queryLanguage = $old-xpath-lang">
              <xsl:value-of select="$new-xpath-lang" />
            </xsl:when>
            <xsl:otherwise>
              <xsl:value-of select="@queryLanguage" />
            </xsl:otherwise>
          </xsl:choose>
        </xsl:attribute>
      </xsl:if>
      <!-- child elements -->
      <xsl:apply-templates select="*" />
    </xsl:element>
  </xsl:template>

  <!-- onMessage: rename to onEvent when used as event handler -->
  <xsl:template match="bpel11:onMessage">
    <!--rename to onEvent if parent is eventHandlers-->
    <xsl:variable name="name">
      <xsl:choose>
        <xsl:when test="local-name(..) = 'eventHandlers'">
          <xsl:value-of select="'onEvent'" />
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="'onMessage'" />
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:element name="{$name}" namespace="{$new-uri}">
      <xsl:call-template name="copyBpelElementContents" />
    </xsl:element>
  </xsl:template>

  <!-- onAlarm: move time expressions -->
  <xsl:template match="bpel11:onAlarm">
    <xsl:element name="onAlarm" namespace="{$new-uri}">
      <!-- namespaces -->
      <xsl:call-template name="copyNonBpelNamespaces" />
      <!-- attributes -->
      <xsl:copy-of select="@*[name() != 'for' and name() != 'until']" />
      <xsl:choose>
        <xsl:when test="@for">
          <xsl:element name="for" namespace="{$new-uri}">
            <xsl:call-template name="copyNonDefaultNamespaces" />
            <xsl:value-of select="@for" />
          </xsl:element>
        </xsl:when>
        <xsl:when test="@until">
          <xsl:element name="until" namespace="{$new-uri}">
            <xsl:call-template name="copyNonDefaultNamespaces" />
            <xsl:value-of select="@until" />
          </xsl:element>
        </xsl:when>
      </xsl:choose>
      <!-- child elements -->
      <xsl:apply-templates select="*" />
    </xsl:element>
  </xsl:template>

  <!-- source: move transition condition -->
  <xsl:template match="bpel11:source">
    <xsl:element name="source" namespace="{$new-uri}">
      <!-- namespaces -->
      <xsl:call-template name="copyNonBpelNamespaces" />
      <!-- attributes -->
      <xsl:copy-of select="@*[name() != 'transitionCondition']" />
      <xsl:if test="@transitionCondition">
        <xsl:element name="transitionCondition" namespace="{$new-uri}">
          <xsl:call-template name="copyNonDefaultNamespaces" />
          <xsl:value-of select="@transitionCondition" />
        </xsl:element>
      </xsl:if>
      <!-- child elements -->
      <xsl:apply-templates select="*" />
    </xsl:element>
  </xsl:template>

  <!-- from: move expression, query and literal -->
  <xsl:template match="bpel11:from">
    <xsl:choose>
      <!-- expression -->
      <xsl:when test="@expression">
        <xsl:element name="from" namespace="{$new-uri}">
          <!-- namespaces -->
          <xsl:call-template name="copyNonDefaultNamespaces" />
          <!-- attributes -->
          <xsl:copy-of select="@*[name() != 'expression']" />
          <xsl:value-of select="@expression" />
          <!-- child elements -->
          <xsl:apply-templates select="*" />
        </xsl:element>
      </xsl:when>
      <!-- variable/property, partnerLink -->
      <xsl:when test="@property or @partnerLink">
        <xsl:call-template name="copyBpelElement" />
      </xsl:when>
      <!-- variable/part/query -->
      <xsl:when test="@variable">
        <xsl:element name="from" namespace="{$new-uri}">
          <!-- namespaces -->
          <xsl:call-template name="copyNonDefaultNamespaces" />
          <!-- attributes -->
          <xsl:copy-of select="@*[name() != 'query']" />
          <xsl:if test="@query">
            <xsl:element name="query" namespace="{$new-uri}">
              <xsl:value-of select="@query" />
            </xsl:element>
          </xsl:if>
          <!-- child elements -->
          <xsl:apply-templates select="*" />
        </xsl:element>
      </xsl:when>
      <!--literal-->
      <xsl:otherwise>
        <xsl:element name="from" namespace="{$new-uri}">
          <!-- namespaces -->
          <xsl:call-template name="copyNonBpelNamespaces" />
          <!-- attributes -->
          <xsl:copy-of select="@*" />
          <!-- child elements -->
          <xsl:element name="literal" namespace="{$new-uri}">
            <!-- first child element only -->
            <xsl:copy-of select="*[1]" />
          </xsl:element>
        </xsl:element>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!-- to: move query -->
  <xsl:template match="bpel11:to">
    <xsl:choose>
      <!-- variable/part/query -->
      <xsl:when test="@variable and not(@property)">
        <xsl:element name="to" namespace="{$new-uri}">
          <!-- namespaces -->
          <xsl:call-template name="copyNonDefaultNamespaces" />
          <!-- attributes -->
          <xsl:copy-of select="@*[name() != 'query']" />
          <xsl:if test="@query">
            <xsl:element name="query" namespace="{$new-uri}">
              <xsl:value-of select="@query" />
            </xsl:element>
          </xsl:if>
          <!-- child elements -->
          <xsl:apply-templates select="*" />
        </xsl:element>
      </xsl:when>
      <!-- variable/property, partnerLink -->
      <xsl:otherwise>
        <xsl:call-template name="copyBpelElement" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!-- correlation: rename pattern -->
  <xsl:template match="bpel11:correlation">
    <xsl:element name="correlation" namespace="{$new-uri}">
      <!-- namespaces -->
      <xsl:call-template name="copyNonBpelNamespaces" />
      <!-- attributes -->
      <xsl:copy-of select="@*[name() != 'pattern']" />
      <xsl:if test="@pattern">
        <xsl:attribute name="pattern">
          <xsl:choose>
            <xsl:when test="@pattern = 'out'">request</xsl:when>
            <xsl:when test="@pattern = 'in'">response</xsl:when>
            <xsl:when test="@pattern = 'out-in'">request-response</xsl:when>
            <!-- pass value through for semantic validation -->
            <xsl:otherwise>
              <xsl:value-of select="@pattern" />
            </xsl:otherwise>
          </xsl:choose>
        </xsl:attribute>
      </xsl:if>
      <!-- child elements -->
      <xsl:apply-templates select="*" />
    </xsl:element>
  </xsl:template>

  <!--other bpel 1.1 elements-->
  <xsl:template name="copyBpelElement" match="bpel11:*">
    <xsl:element name="{local-name()}" namespace="{$new-uri}">
      <xsl:call-template name="copyBpelElementContents" />
    </xsl:element>
  </xsl:template>

  <!-- other elements -->
  <xsl:template match="*">
    <xsl:copy-of select="." />
  </xsl:template>

  <!-- named templates ////////////////////////// -->

  <!-- copies the contents of a case element -->
  <xsl:template name="copyCaseContents">
    <!-- namespaces -->
    <xsl:call-template name="copyNonBpelNamespaces" />
    <!-- attributes -->
    <xsl:copy-of select="@*[name() != 'condition']" />
    <xsl:call-template name="moveCondition" />
    <!-- child elements -->
    <xsl:apply-templates select="*" />
  </xsl:template>

  <!-- moves the condition attribute value to a child element -->
  <xsl:template name="moveCondition">
    <xsl:element name="condition" namespace="{$new-uri}">
      <xsl:call-template name="copyNonDefaultNamespaces" />
      <xsl:value-of select="@condition" />
    </xsl:element>
  </xsl:template>

  <!-- copies the contents of an element in the old bpel namespace -->
  <xsl:template name="copyBpelElementContents">
    <!-- namespaces -->
    <xsl:call-template name="copyNonBpelNamespaces" />
    <!-- attributes -->
    <xsl:copy-of select="@*" />
    <!-- child elements -->
    <xsl:apply-templates select="*" />
  </xsl:template>

  <!-- copies the nested elements of activities  -->
  <xsl:template name="copyActivityElements">
    <xsl:call-template name="copyStandardElements" />
    <!--recurse on the other nested elements-->
    <xsl:apply-templates
      select="*[namespace-uri() != $old-uri or 
      (local-name() != 'target' and local-name() != 'source')]" />
  </xsl:template>

  <!-- copies source and target elements-->
  <xsl:template name="copyStandardElements">
    <!-- groups <source> elements in a <sources> element  -->
    <xsl:variable name="sources" select="bpel11:source" />
    <xsl:if test="$sources">
      <xsl:element name="sources" namespace="{$new-uri}">
        <xsl:apply-templates select="$sources" />
      </xsl:element>
    </xsl:if>
    <!-- groups <target> elements in  a <targets> element -->
    <xsl:variable name="targets" select="bpel11:target" />
    <xsl:if test="$targets">
      <xsl:element name="targets" namespace="{$new-uri}">
        <xsl:if test="@joinCondition">
          <xsl:element name="joinCondition" namespace="{$new-uri}">
            <xsl:call-template name="copyNonDefaultNamespaces" />
            <xsl:value-of select="@joinCondition" />
          </xsl:element>
        </xsl:if>
        <xsl:apply-templates select="$targets" />
      </xsl:element>
    </xsl:if>
  </xsl:template>

  <!-- copies namespace declarations excluding the default and the old bpel namespaces -->
  <xsl:template name="copyNonBpelNamespaces">
    <xsl:for-each select="namespace::*">
      <!-- excluding default namespace declarations prevents the transformer
        - from using the default namespace to qualify non-prefixed elements, 
        - even if they were assigned a namespace of their own -->
      <!-- we can safely omit declarations of the old bpel namespace,
        - as the presence of extension elements will cause the transformer to declare it -->
      <xsl:if test="name(.) and string(.) != $old-uri">
        <xsl:copy />
      </xsl:if>
    </xsl:for-each>
  </xsl:template>

  <!-- copies namespace declarations excluding the default -->
  <xsl:template name="copyNonDefaultNamespaces">
    <xsl:for-each select="namespace::*">
      <!-- excluding default namespace declarations prevents the transformer
        - from using the default namespace to qualify non-prefixed elements, 
        - even if they were assigned a namespace of their own -->
      <xsl:if test="name(.)">
        <xsl:copy />
      </xsl:if>
    </xsl:for-each>
  </xsl:template>

</xsl:stylesheet>