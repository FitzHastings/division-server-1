<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet 
  version="1.1" 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
  xmlns:fo="http://www.w3.org/1999/XSL/Format" 
  xmlns:barcode="org.krysalis.barcode4j.xalan.BarcodeExt" 
  xmlns:common="http://exslt.org/common" 
  xmlns:xalan="http://xml.apache.org" 
  exclude-result-prefixes="barcode common xalan">
  <xsl:output method="xml" version="1.0" omit-xml-declaration="no" indent="yes"/>
  
  <xsl:template match="/document">
    <fo:root xmlns:fo="http://www.w3.org/1999/XSL/Format">
      <fo:layout-master-set>
        <fo:simple-page-master
          master-name="A4"
          page-height="29.7cm"
          page-width="21cm"
          margin-left="1.5cm"
          margin-right="1.5cm"
          margin-top="1cm"
          margin-bottom="1cm">
          <fo:region-body margin-top="2cm" margin-bottom="1.5cm"/>
          <fo:region-before extent="2cm"/>
          <fo:region-after extent="1cm"/>
        </fo:simple-page-master>
        
        <fo:simple-page-master
          master-name   ="A4_albom"
          page-width    ="29.7cm"
          page-height   ="21cm"
          margin-left   ="1.5cm"
          margin-right  ="1.5cm"
          margin-top    ="1cm"
          margin-bottom ="1cm">
          <fo:region-body margin-top="2cm" margin-bottom="1.5cm"/>
          <fo:region-before extent="2cm"/>
          <fo:region-after extent="1cm"/>
        </fo:simple-page-master>
        
        <xsl:apply-templates select="/document/page-type"/>
      </fo:layout-master-set>
      <xsl:apply-templates select="/document/pages"/>
    </fo:root>
  </xsl:template>
  
  <!--Тип страницы-->
  
  <xsl:template match="page-type">
    <fo:simple-page-master
      master-name   = "{@name}"
      page-width    = "{@width}"
      page-height   = "{@height}">
      
      <xsl:if test="(@margin)">
        <xsl:attribute name="margin-left">
          <xsl:value-of select="@margin"/>
        </xsl:attribute>
        <xsl:attribute name="margin-top">
          <xsl:value-of select="@margin"/>
        </xsl:attribute>
        <xsl:attribute name="margin-right">
          <xsl:value-of select="@margin"/>
        </xsl:attribute>
        <xsl:attribute name="margin-bottom">
          <xsl:value-of select="@margin"/>
        </xsl:attribute>
      </xsl:if>

      <xsl:if test="(@margin-left)">
        <xsl:attribute name="margin-left">
          <xsl:value-of select="@margin-left"/>
        </xsl:attribute>
      </xsl:if>
      <xsl:if test="(@margin-top)">
        <xsl:attribute name="margin-top">
          <xsl:value-of select="@margin-top"/>
        </xsl:attribute>
      </xsl:if>
      <xsl:if test="(@margin-right)">
        <xsl:attribute name="margin-right">
          <xsl:value-of select="@margin-right"/>
        </xsl:attribute>
      </xsl:if>
      <xsl:if test="(@margin-bottom)">
        <xsl:attribute name="margin-bottom">
          <xsl:value-of select="@margin-bottom"/>
        </xsl:attribute>
      </xsl:if>
      
      <fo:region-body/>
      <fo:region-before/>
      <fo:region-after/>      
    </fo:simple-page-master>
  </xsl:template>
  <!--#####################-->
  
  <!--Страницы одного типа-->
  <xsl:template match="pages">
    <fo:page-sequence master-reference="{@type}">
      <!--Верхний колонтитул-->
      <fo:static-content flow-name="xsl-region-before">
        <fo:block>
          <xsl:apply-templates select="./header"/>
        </fo:block>
      </fo:static-content>
      <!--Нижний колонтитул-->
      <fo:static-content flow-name="xsl-region-after">
        <fo:block>
          <xsl:apply-templates select="./footer"/>
        </fo:block>
      </fo:static-content>
      <!--Тело-->
      <fo:flow flow-name="xsl-region-body">
        <xsl:apply-templates select="./body"/>
      </fo:flow>
    </fo:page-sequence>
  </xsl:template>
  <!--#####################-->
  
  <xsl:template match="@*">
     <xsl:attribute name="{name()}">
        <xsl:value-of select="."/>
     </xsl:attribute>
  </xsl:template>

  <xsl:template match="@family">
     <xsl:attribute name="font-family">
        <xsl:value-of select="."/>
     </xsl:attribute>
  </xsl:template>

  <xsl:template match="@size">
     <xsl:attribute name="font-size">
        <xsl:value-of select="."/>
     </xsl:attribute>
  </xsl:template>

  <xsl:template match="@style">
     <xsl:attribute name="font-style">
        <xsl:value-of select="."/>
     </xsl:attribute>
  </xsl:template>

  <xsl:template match="@weight">
     <xsl:attribute name="font-weight">
        <xsl:value-of select="."/>
     </xsl:attribute>
  </xsl:template>

  <xsl:template match="@align">
     <xsl:attribute name="text-align">
        <xsl:value-of select="."/>
     </xsl:attribute>
  </xsl:template>
  
  <xsl:template match="@border">
     <xsl:attribute name="border-width">
        <xsl:value-of select="."/>
     </xsl:attribute>
  </xsl:template>

  <xsl:template match="@border-left">
     <xsl:attribute name="border-left-width">
        <xsl:value-of select="."/>
     </xsl:attribute>
  </xsl:template>
  
  <xsl:template match="@border-top">
     <xsl:attribute name="border-top-width">
        <xsl:value-of select="."/>
     </xsl:attribute>
  </xsl:template>
  
  <xsl:template match="@border-right">
     <xsl:attribute name="border-right-width">
        <xsl:value-of select="."/>
     </xsl:attribute>
  </xsl:template>
  
  <xsl:template match="@border-bottom">
     <xsl:attribute name="border-bottom-width">
        <xsl:value-of select="."/>
     </xsl:attribute>
  </xsl:template>
  
  <xsl:template match="@colspan">
     <xsl:attribute name="number-columns-spanned">
        <xsl:value-of select="."/>
     </xsl:attribute>
  </xsl:template>
  
  <xsl:template match="@rowspan">
     <xsl:attribute name="number-rows-spanned">
        <xsl:value-of select="."/>
     </xsl:attribute>
  </xsl:template>
  
  <xsl:template match="@valign">
     <xsl:attribute name="display-align">
        <xsl:value-of select="."/>
     </xsl:attribute>
  </xsl:template>
  
  <xsl:template match="img/@width">
    <xsl:attribute name="content-width">
       <xsl:value-of select="."/>
    </xsl:attribute>
  </xsl:template>
  
  <xsl:template match="img/@height">
    <xsl:attribute name="content-height">
       <xsl:value-of select="."/>
    </xsl:attribute>
  </xsl:template>
  
    
  
  
  
  
  
  
  
  <xsl:template match="body|header|footer">
    <fo:block>
      <xsl:apply-templates select="@*"/>
      <xsl:apply-templates/>
    </fo:block>
  </xsl:template>

  <xsl:template match="page_number">
    <fo:page-number/>
  </xsl:template>
  
  <xsl:template match="page_count">
    <fo:page-number-citation ref-id="{@ref-id}"/>
  </xsl:template>
  
  <xsl:template match="up">
    <fo:inline baseline-shift="super" font-size="75%">
      <xsl:apply-templates/>
    </fo:inline>
  </xsl:template>
  
  <xsl:template match="down">
    <fo:inline baseline-shift="sub" font-size="75%">
      <xsl:apply-templates/>
    </fo:inline>
  </xsl:template>

  <xsl:template match="i">
    <fo:inline font-style="italic">
      <xsl:apply-templates/>
    </fo:inline>
  </xsl:template>

  <xsl:template match="u">
    <fo:inline text-decoration="underline">
      <xsl:apply-templates/>
    </fo:inline>
  </xsl:template>

  <xsl:template match="b">
    <fo:inline font-weight="bold">
      <xsl:apply-templates/>
    </fo:inline>
  </xsl:template>

  <xsl:template match="br">
    <fo:leader leader-length="0mm"/>
    <fo:block/>
  </xsl:template>

  <xsl:template match="n">
    <fo:leader/>
  </xsl:template>

  <xsl:template match="center">
    <fo:block text-align="center">
      <xsl:apply-templates/>
    </fo:block>
  </xsl:template>

  <xsl:template match="pre">
    <fo:block font-family="monospace" white-space-collapse="false" wrap-option="no-wrap">
      <xsl:apply-templates select="*|text()"/>
    </fo:block>
  </xsl:template>

  <xsl:template match="nobr">
    <fo:inline wrap-option="no-wrap">
      <xsl:apply-templates select="*|text()"/>
    </fo:inline>
  </xsl:template>
  

  <xsl:template match="img">
    <fo:external-graphic src="@src">
      <xsl:apply-templates select="@*"/>
    </fo:external-graphic>
  </xsl:template>
  
  
  
  
  
  
  
<xsl:template name="for">
  <xsl:param name="nextpage" select="false"/>
  <xsl:param name="i" select="0"/>
  <xsl:param name="n"/>
  <xsl:if test="$i &lt; $n">
    <xsl:apply-templates/>
    <xsl:if test="$nextpage = 'true'">
      <xsl:if test="$i != $n - 1">
        <fo:block break-before="page"/>
      </xsl:if>
    </xsl:if>
    <xsl:call-template name="for">
      <xsl:with-param name="i" select="$i + 1"/>
      <xsl:with-param name="n" select="$n"/>
      <xsl:with-param name="nextpage" select="$nextpage"/>
    </xsl:call-template>
  </xsl:if>
</xsl:template>  
  
  
  
  <xsl:template match="hide">
  </xsl:template>
  
  <xsl:template match="copy">
    <xsl:choose>
      <xsl:when test="not(@count)">
        <xsl:call-template name="for">
          <xsl:with-param name="n" select="2"/>
          <xsl:with-param name="nextpage" select="@nextpage"/>
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <xsl:call-template name="for">
          <xsl:with-param name="n" select="@count"/>
          <xsl:with-param name="nextpage" select="@nextpage"/>
        </xsl:call-template>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  
  
  <xsl:template match="p">
    <fo:block>
      <xsl:apply-templates select="@*"/>
      <xsl:apply-templates/>
    </fo:block>
  </xsl:template>
  
  <xsl:template match="div">
    <fo:block-container>
      <xsl:apply-templates select="@*"/>
      <fo:block>
        <xsl:apply-templates/>
      </fo:block>
    </fo:block-container>
  </xsl:template>

  <xsl:template match="font">
    <fo:inline>
      <xsl:apply-templates select="@*"/>
      <xsl:apply-templates/>
    </fo:inline>
  </xsl:template>

  <xsl:template match="table">
    <fo:table>
      <xsl:apply-templates select="@*"/>
      
      <xsl:if test="not(@border)">
        <xsl:attribute name="border-width">
          <xsl:text>0</xsl:text>
        </xsl:attribute>
      </xsl:if>
      
      <xsl:if test="not(@border-style)">
        <xsl:attribute name="border-style">
          <xsl:text>solid</xsl:text>
        </xsl:attribute>
      </xsl:if>
      
      <fo:table-body>
        <xsl:apply-templates select="tr"/>
      </fo:table-body>
    </fo:table>
  </xsl:template>

  <xsl:template match="tr">
    <fo:table-row>
      <xsl:apply-templates select="td"/>
    </fo:table-row>
  </xsl:template>

  <xsl:template match="td">
    <fo:table-cell>
      <xsl:apply-templates select="@*"/>
      
      <xsl:if test="not(@padding)">
        <xsl:attribute name="padding">
          <xsl:choose>
            <xsl:when test="not(../../@padding)">
              <xsl:text>0</xsl:text>
            </xsl:when>
            <xsl:otherwise>
              <xsl:value-of select="../../@padding"/>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:attribute>
      </xsl:if>
      
      <xsl:if test="not(@border)">
        <xsl:attribute name="border-width">
          <xsl:choose>
            <xsl:when test="not(../../@border)">
              <xsl:text>0</xsl:text>
            </xsl:when>
            <xsl:otherwise>
              <xsl:value-of select="../../@border"/>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:attribute>
      </xsl:if>
      
      <xsl:if test="not(@border-style)">
        <xsl:attribute name="border-style">
          <xsl:choose>
            <xsl:when test="not(../../@border-style)">
              <xsl:text>solid</xsl:text>
            </xsl:when>
            <xsl:otherwise>
              <xsl:value-of select="../../@border-style"/>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:attribute>
      </xsl:if>
      
      <xsl:if test="not(@border-color)">
        <xsl:attribute name="border-color">
          <xsl:value-of select="../../@border-color"/>
        </xsl:attribute>
      </xsl:if>
      
      <fo:block hyphenate="true" language="ru">
        <xsl:apply-templates/>
      </fo:block>
    </fo:table-cell>
  </xsl:template>
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  <xsl:template match="list">
    <fo:list-block>
      <xsl:apply-templates select="item|li"/>
    </fo:list-block>
  </xsl:template>

  <!--<xsl:template match="list">
    <fo:list-block provisional-distance-between-starts="{@numberSize}" provisional-label-separation="{@numberSize}">
      <xsl:apply-templates select="item|li"/>
    </fo:list-block>
  </xsl:template>-->

  <xsl:template match="item">
    <fo:list-item>

      <fo:list-item-label text-align="end" wrap-option="no-wrap" end-indent="label-end()">
        <fo:block>
          <xsl:number level="multiple" format="{@type}"/>
        </fo:block>
      </fo:list-item-label>

      <fo:list-item-body start-indent="body-start()">
        <fo:block>
          <xsl:apply-templates/>
        </fo:block>
      </fo:list-item-body>

    </fo:list-item>
  </xsl:template>

  <xsl:template match="li">
    <fo:list-item>
      <fo:list-item-label end-indent="label-end()">
        <fo:block>&#x2022;</fo:block>
      </fo:list-item-label>
      <fo:list-item-body start-indent="body-start()">
        <fo:block>
          <xsl:apply-templates select="*|text()"/>
        </fo:block>
      </fo:list-item-body>
    </fo:list-item>
  </xsl:template>


  <xsl:template match="section">
    <fo:block break-before="page">
    </fo:block>
  </xsl:template>

  <xsl:template match="canvas">
    <fo:instream-foreign-object>
      <svg xmlns="http://www.w3.org/2000/svg">
        <xsl:if test="@width">
          <xsl:attribute name="width">
            <xsl:value-of select="@width"/>
          </xsl:attribute>
        </xsl:if>

        <xsl:if test="@height">
          <xsl:attribute name="height">
            <xsl:value-of select="@height"/>
          </xsl:attribute>
        </xsl:if>
        <xsl:apply-templates/>
      </svg>
    </fo:instream-foreign-object>
  </xsl:template>

  <xsl:template match="canvas/line">
    <line
    xmlns="http://www.w3.org/2000/svg"
    x1="{@x1}"
    y1="{@y1}"
    x2="{@x2}"
    y2="{@y2}"
    width="{@width}"
    stroke="{@color}"/>
  </xsl:template>
  
  <xsl:template match="barcode">
    <fo:instream-foreign-object>
      <xsl:variable name="bc" select="barcode:generate(., msg)"/>
      <svg:svg xmlns:svg="http://www.w3.org/2000/svg">
        <xsl:attribute name="width">
          <xsl:value-of select="$bc/svg:svg/@width"/>
        </xsl:attribute>
        <xsl:attribute name="height">
          <xsl:value-of select="$bc/svg:svg/@height"/>
        </xsl:attribute>
        <svg:rect x="0mm" y="0mm" fill="white">
          <xsl:attribute name="width">
            <xsl:value-of select="$bc/svg:svg/@width"/>
          </xsl:attribute>
          <xsl:attribute name="height">
            <xsl:value-of select="$bc/svg:svg/@height"/>
          </xsl:attribute>
        </svg:rect>
        <xsl:copy-of select="$bc"/>
      </svg:svg>
    </fo:instream-foreign-object>
  </xsl:template>

</xsl:stylesheet>
