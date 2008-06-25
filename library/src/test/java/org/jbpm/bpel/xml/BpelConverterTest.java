/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the JBPM BPEL PUBLIC LICENSE AGREEMENT as
 * published by JBoss Inc.; either version 1.0 of the License, or
 * (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */
package org.jbpm.bpel.xml;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Iterator;

import javax.xml.namespace.QName;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import junit.framework.TestCase;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.ibm.wsdl.util.xml.DOMUtils;

import org.jbpm.bpel.xml.util.DatatypeUtil;
import org.jbpm.bpel.xml.util.XmlUtil;

/**
 * @author Juan Cantú
 * @version $Revision$ $Date: 2007/09/04 06:42:26 $
 */
public class BpelConverterTest extends TestCase {

  public void testMultipleNamespacePrefixes_noPrefix() throws Exception {
    String xml = "<process name='rodents' targetNamespace='http://rodents.net'"
        + " xmlns:rats='http://rodents.net' xmlns:mice='http://rodents.net'"
        + " xmlns:bpel='"
        + BpelConstants.NS_BPEL_1_1
        + "'"
        + " xmlns='"
        + BpelConstants.NS_BPEL_1_1
        + "'>"
        + " <variables>"
        + "  <variable name='rat' element='rats:rat'/>"
        + "  <variable name='mouse' element='mice:mouse'/>"
        + " </variables>"
        + " <empty/>"
        + "</process>";
    Element process = transformNoWrap(xml);
    // variables
    Element variables = XmlUtil.getElement(process, BpelConstants.NS_BPEL,
        BpelConstants.ELEM_VARIABLES);
    Iterator variableIt = XmlUtil.getElements(variables, BpelConstants.NS_BPEL,
        BpelConstants.ELEM_VARIABLE);
    // rat
    Element variable = (Element) variableIt.next();
    QName element = XmlUtil.getQNameValue(variable.getAttributeNode(BpelConstants.ATTR_ELEMENT));
    assertEquals("rat", element.getLocalPart());
    assertEquals("http://rodents.net", element.getNamespaceURI());
    // mouse
    variable = (Element) variableIt.next();
    element = XmlUtil.getQNameValue(variable.getAttributeNode(BpelConstants.ATTR_ELEMENT));
    assertEquals("mouse", element.getLocalPart());
    assertEquals("http://rodents.net", element.getNamespaceURI());
  }

  public void testMultipleNamespacePrefixes_bpelPrefix() throws Exception {
    String xml = "<bpel:process name='rodents' targetNamespace='http://rodents.net'"
        + " xmlns:rats='http://rodents.net' xmlns:mice='http://rodents.net' "
        + " xmlns:bpel='"
        + BpelConstants.NS_BPEL_1_1
        + "'"
        + " xmlns=''>"
        + " <bpel:variables>"
        + "  <bpel:variable name='rat' element='rats:rat'/>"
        + "  <bpel:variable name='mouse' element='mice:mouse'/>"
        + " </bpel:variables>"
        + " <empty/>"
        + "</bpel:process>";
    Element process = transformNoWrap(xml);
    // variables
    Element variables = XmlUtil.getElement(process, BpelConstants.NS_BPEL,
        BpelConstants.ELEM_VARIABLES);
    Iterator variableIt = XmlUtil.getElements(variables, BpelConstants.NS_BPEL,
        BpelConstants.ELEM_VARIABLE);
    // rat
    Element variable = (Element) variableIt.next();
    QName element = XmlUtil.getQNameValue(variable.getAttributeNode(BpelConstants.ATTR_ELEMENT));
    assertEquals("rat", element.getLocalPart());
    assertEquals("http://rodents.net", element.getNamespaceURI());
    // mouse
    variable = (Element) variableIt.next();
    element = XmlUtil.getQNameValue(variable.getAttributeNode(BpelConstants.ATTR_ELEMENT));
    assertEquals("mouse", element.getLocalPart());
    assertEquals("http://rodents.net", element.getNamespaceURI());
  }

  // ///////////////////// Renamed Elements

  public void testProcessDefaultQueryLanguage() throws Exception {
    String xml = "<process queryLanguage='http://www.w3.org/TR/1999/REC-xpath-19991116'/>";
    Element scope = transform(xml);
    assertEquals(BpelConstants.URN_XPATH_1_0, XmlUtil.getAttribute(scope, "queryLanguage"));
  }

  public void testProcessDefaultExpressionLanguage() throws Exception {
    String xml = "<process expressionLanguage='http://www.w3.org/TR/1999/REC-xpath-19991116'/>";
    Element scope = transform(xml);
    assertEquals(BpelConstants.URN_XPATH_1_0, XmlUtil.getAttribute(scope, "expressionLanguage"));
  }

  public void testScopeVariableAccessSerializable() throws Exception {
    String xml = "<scope variableAccessSerializable='yes'/>";
    Element scope = transform(xml);
    assertNull(XmlUtil.getAttribute(scope, "variableAccessSerializable"));
    assertEquals("yes", XmlUtil.getAttribute(scope, "isolated"));
  }

  public void testScopeOnMessage() throws Exception {
    String xml = "<scope>"
        + " <eventHandlers>"
        + "  <onMessage/>"
        + "  <onMessage/>"
        + " </eventHandlers>"
        + "</scope>";
    Element scope = transform(xml);
    Element events = XmlUtil.getElement(scope, BpelConstants.NS_BPEL, "eventHandlers");
    assertNotNull(events);
    assertEquals(2, events.getElementsByTagNameNS(BpelConstants.NS_BPEL, "onEvent").getLength());
  }

  public void testPickOnMessage() throws Exception {
    String xml = "<pick>" + " <onMessage/>" + " <onMessage/>" + "</pick>";
    Element pick = transform(xml);
    assertEquals(2, pick.getElementsByTagNameNS(BpelConstants.NS_BPEL, "onMessage").getLength());
  }

  public void testTerminate() throws Exception {
    String xml = "<terminate/>";
    Element exit = transform(xml);
    // element name
    assertEquals("exit", exit.getLocalName());
    assertEquals(BpelConstants.NS_BPEL, exit.getNamespaceURI());
  }

  // ///////////////////// Attributes that changed into elements

  public void testOnAlarmFor() throws Exception {
    String xml = "<onAlarm for='f'><empty/></onAlarm>";
    Element onAlarm = transform(xml);
    Element _for = XmlUtil.getElement(onAlarm, BpelConstants.NS_BPEL, "for");
    assertNotNull(_for);
    assertEquals("f", DatatypeUtil.toString(_for));
  }

  public void testOnAlarmUntil() throws Exception {
    String xml = "<onAlarm until='u'><empty/></onAlarm>";
    Element onAlarm = transform(xml);
    Element until = XmlUtil.getElement(onAlarm, BpelConstants.NS_BPEL, "until");
    assertNotNull(until);
    assertEquals("u", DatatypeUtil.toString(until));
  }

  public void testWaitFor() throws Exception {
    String xml = "<wait for='f'/>";
    Element wait = transform(xml);
    Element _for = XmlUtil.getElement(wait, BpelConstants.NS_BPEL, "for");
    assertEquals("f", DatatypeUtil.toString(_for));
    // namespaces
    assertEquals(BpelConstants.NS_BPEL, _for.getNamespaceURI());
    assertEquals(BpelConstants.NS_BPEL_1_1, DOMUtils.getNamespaceURIFromPrefix(_for, "bpws"));
  }

  public void testWaitUntil() throws Exception {
    String xml = "<wait until='u'/>";
    Element wait = transform(xml);
    Element until = XmlUtil.getElement(wait, BpelConstants.NS_BPEL, "until");
    assertEquals("u", DatatypeUtil.toString(until));
    // namespaces
    assertEquals(BpelConstants.NS_BPEL, until.getNamespaceURI());
    assertEquals(BpelConstants.NS_BPEL_1_1, DOMUtils.getNamespaceURIFromPrefix(until, "bpws"));
  }

  public void testWhileCondition_noPrefix() throws Exception {
    String xml = "<while condition='c'><empty/></while>";
    Element whileElem = transform(xml);
    Element condition = XmlUtil.getElement(whileElem, BpelConstants.NS_BPEL, "condition");
    assertNotNull(condition);
    assertEquals("c", DatatypeUtil.toString(condition));
    // namespaces
    assertEquals(BpelConstants.NS_BPEL_1_1, DOMUtils.getNamespaceURIFromPrefix(condition, "bpws"));
  }

  public void testWhileCondition_bpelPrefix() throws Exception {
    String xml = "<bpws:while condition='c'><bpws:empty/></bpws:while>";
    Element _while = transform(xml);
    Element condition = XmlUtil.getElement(_while, BpelConstants.NS_BPEL, "condition");
    assertNotNull(condition);
    assertEquals("c", DatatypeUtil.toString(condition));
    // namespaces
    assertEquals(BpelConstants.NS_BPEL_1_1, DOMUtils.getNamespaceURIFromPrefix(condition, "bpws"));
  }

  public void testIf_noPrefix() throws Exception {
    String xml = "<switch><case condition='c'><empty name='a'/></case></switch>";
    Element _if = transform(xml);
    // element name
    assertEquals("if", _if.getLocalName());
    // condition
    Element condition = XmlUtil.getElement(_if, BpelConstants.NS_BPEL, "condition");
    assertEquals("c", DatatypeUtil.toString(condition));
    // activity
    Element activity = XmlUtil.getElement(_if, BpelConstants.NS_BPEL, "empty");
    assertEquals("a", activity.getAttribute("name"));
    // namespaces
    assertEquals(BpelConstants.NS_BPEL_1_1, DOMUtils.getNamespaceURIFromPrefix(condition, "bpws"));
  }

  public void testIf_bpelPrefix() throws Exception {
    String xml = "<bpws:switch><bpws:case condition='c'><bpws:empty name='a'/></bpws:case></bpws:switch>";
    Element _if = transform(xml);
    // element name
    assertEquals("if", _if.getLocalName());
    // condition
    Element condition = XmlUtil.getElement(_if, BpelConstants.NS_BPEL, "condition");
    assertEquals("c", DatatypeUtil.toString(condition));
    // activity
    Element activity = XmlUtil.getElement(_if, BpelConstants.NS_BPEL, "empty");
    assertEquals("a", activity.getAttribute("name"));
    // namespaces
    assertEquals(BpelConstants.NS_BPEL_1_1, DOMUtils.getNamespaceURIFromPrefix(condition, "bpws"));
  }

  public void testIf_elseif() throws Exception {
    String xml = "<switch>"
        + " <case condition='c1'><empty name='a1'/></case>"
        + " <case condition='c2'><empty name='a2'/></case>"
        + "</switch>";
    Element _if = transform(xml);
    // element name
    assertEquals("if", _if.getLocalName());
    // first branch
    // condition
    Element condition = XmlUtil.getElement(_if, BpelConstants.NS_BPEL, "condition");
    assertEquals("c1", DatatypeUtil.toString(condition));
    // activity
    Element activity = XmlUtil.getElement(_if, BpelConstants.NS_BPEL, "empty");
    assertEquals("a1", activity.getAttribute("name"));
    // second branch
    Element elseif = XmlUtil.getElement(_if, BpelConstants.NS_BPEL, "elseif");
    // condition
    condition = XmlUtil.getElement(elseif, BpelConstants.NS_BPEL, "condition");
    assertEquals("c2", DatatypeUtil.toString(condition));
    // activity
    activity = XmlUtil.getElement(elseif, BpelConstants.NS_BPEL, "empty");
    assertEquals("a2", activity.getAttribute("name"));
    // default branch
    Element elseElem = XmlUtil.getElement(_if, BpelConstants.NS_BPEL, "else");
    assertNull(elseElem);
  }

  public void testIf_else() throws Exception {
    String xml = "<switch>"
        + " <case condition='c'><empty name='a1'/></case>"
        + " <otherwise><empty name='a2'/></otherwise>"
        + "</switch>";
    Element _if = transform(xml);
    // element name
    assertEquals("if", _if.getLocalName());
    // first branch
    // condition
    Element condition = XmlUtil.getElement(_if, BpelConstants.NS_BPEL, "condition");
    assertEquals("c", DatatypeUtil.toString(condition));
    // activity
    Element activity = XmlUtil.getElement(_if, BpelConstants.NS_BPEL, "empty");
    assertEquals("a1", activity.getAttribute("name"));
    // default branch
    Element elseElem = XmlUtil.getElement(_if, BpelConstants.NS_BPEL, "else");
    activity = XmlUtil.getElement(elseElem, BpelConstants.NS_BPEL, "empty");
    assertEquals("a2", activity.getAttribute("name"));
  }

  public void testIf_elseif_else() throws Exception {
    String xml = "<switch>"
        + " <case condition='c1'><empty name='a1'/></case>"
        + " <case condition='c2'><empty name='a2'/></case>"
        + " <otherwise><empty name='a3'/></otherwise>"
        + "</switch>";
    Element _if = transform(xml);
    // element name
    assertEquals("if", _if.getLocalName());
    // first branch
    // condition
    Element condition = XmlUtil.getElement(_if, BpelConstants.NS_BPEL, "condition");
    assertEquals("c1", DatatypeUtil.toString(condition));
    // activity
    Element activity = XmlUtil.getElement(_if, BpelConstants.NS_BPEL, "empty");
    assertEquals("a1", activity.getAttribute("name"));
    // second branch
    Element elseif = XmlUtil.getElement(_if, BpelConstants.NS_BPEL, "elseif");
    // condition
    condition = XmlUtil.getElement(elseif, BpelConstants.NS_BPEL, "condition");
    assertEquals("c2", DatatypeUtil.toString(condition));
    // activity
    activity = XmlUtil.getElement(elseif, BpelConstants.NS_BPEL, "empty");
    assertEquals("a2", activity.getAttribute("name"));
    // default branch
    Element elseElem = XmlUtil.getElement(_if, BpelConstants.NS_BPEL, "else");
    activity = XmlUtil.getElement(elseElem, BpelConstants.NS_BPEL, "empty");
    assertEquals("a3", activity.getAttribute("name"));
  }

  public void testCompensate_noScope() throws Exception {
    String xml = "<compensate />";
    Element compensate = transform(xml);
    // element name
    assertEquals(BpelConstants.ELEM_COMPENSATE, compensate.getLocalName());
  }

  public void testCompensate_scope() throws Exception {
    String xml = "<compensate scope='inner' />";
    Element compensateScope = transform(xml);
    // element name
    assertEquals(BpelConstants.ELEM_COMPENSATE_SCOPE, compensateScope.getLocalName());
    // target
    assertEquals("inner", compensateScope.getAttribute(BpelConstants.ATTR_TARGET));
  }

  public void testCorrelation_request() throws Exception {
    String xml = "<correlation set='cs' pattern='out' />";
    Element correlation = transform(xml);
    // element name
    assertEquals(BpelConstants.ELEM_CORRELATION, correlation.getLocalName());
    // pattern
    assertEquals("request", correlation.getAttribute(BpelConstants.ATTR_PATTERN));
  }

  public void testCorrelation_response() throws Exception {
    String xml = "<correlation set='cs' pattern='in' />";
    Element correlation = transform(xml);
    // element name
    assertEquals(BpelConstants.ELEM_CORRELATION, correlation.getLocalName());
    // pattern
    assertEquals("response", correlation.getAttribute(BpelConstants.ATTR_PATTERN));
  }

  public void testCorrelation_request_response() throws Exception {
    String xml = "<correlation set='cs' pattern='out-in' />";
    Element correlation = transform(xml);
    // element name
    assertEquals(BpelConstants.ELEM_CORRELATION, correlation.getLocalName());
    // pattern
    assertEquals("request-response", correlation.getAttribute(BpelConstants.ATTR_PATTERN));
  }

  public void testCorrelation_noPattern() throws Exception {
    String xml = "<correlation set='cs' />";
    Element correlation = transform(xml);
    // element name
    assertEquals(BpelConstants.ELEM_CORRELATION, correlation.getLocalName());
    // pattern
    assertFalse(correlation.hasAttribute(BpelConstants.ATTR_PATTERN));
  }

  public void testCorrelation_bogusPattern() throws Exception {
    String xml = "<correlation set='cs' pattern='bogus' />";
    Element correlation = transform(xml);
    // element name
    assertEquals(BpelConstants.ELEM_CORRELATION, correlation.getLocalName());
    // pattern
    assertEquals("bogus", correlation.getAttribute(BpelConstants.ATTR_PATTERN));
  }

  // ///////////////////// Copy

  public void testFromExpression_noPrefix() throws Exception {
    String xml = "<from expression=\"bpws:getVariableData('price')*0.15\" />";
    Element from = transform(xml);
    // expression
    assertEquals("bpws:getVariableData('price')*0.15", DatatypeUtil.toString(from));
    assertNull(XmlUtil.getAttribute(from, "expression"));
    // namespaces
    assertEquals(BpelConstants.NS_BPEL, from.getNamespaceURI());
    assertEquals(BpelConstants.NS_BPEL_1_1, DOMUtils.getNamespaceURIFromPrefix(from, "bpws"));
  }

  public void testFromExpression_bpelPrefix() throws Exception {
    String xml = "<bpws:from expression=\"bpws:getVariableData('price')*0.15\" />";
    Element from = transform(xml);
    // expression
    assertEquals("bpws:getVariableData('price')*0.15", DatatypeUtil.toString(from));
    assertNull(XmlUtil.getAttribute(from, "expression"));
    // namespaces
    assertEquals(BpelConstants.NS_BPEL, from.getNamespaceURI());
    assertEquals(BpelConstants.NS_BPEL_1_1, DOMUtils.getNamespaceURIFromPrefix(from, "bpws"));
  }

  public void testFromVariable() throws Exception {
    String xml = "<from variable='v'/>";
    Element from = transform(xml);
    assertEquals("v", from.getAttribute(BpelConstants.ATTR_VARIABLE));
    assertFalse(from.hasAttribute(BpelConstants.ATTR_PART));
    assertNull(XmlUtil.getElement(from, BpelConstants.NS_BPEL, BpelConstants.ELEM_QUERY));
  }

  public void testFromVariablePart() throws Exception {
    String xml = "<from variable='v' part='p'/>";
    Element from = transform(xml);
    assertEquals("v", from.getAttribute(BpelConstants.ATTR_VARIABLE));
    assertEquals("p", from.getAttribute(BpelConstants.ATTR_PART));
    assertNull(XmlUtil.getElement(from, BpelConstants.NS_BPEL, BpelConstants.ELEM_QUERY));
  }

  public void testFromVariableQuery() throws Exception {
    String xml = "<from variable='v' part='p' query='/p/q'/>";
    Element from = transform(xml);
    assertEquals("v", from.getAttribute(BpelConstants.ATTR_VARIABLE));
    assertEquals("p", from.getAttribute(BpelConstants.ATTR_PART));
    Element query = XmlUtil.getElement(from, BpelConstants.NS_BPEL, BpelConstants.ELEM_QUERY);
    assertEquals("/p/q", DatatypeUtil.toString(query));
  }

  public void testFromVariableProperty() throws Exception {
    String xml = "<from variable='v' property='p' />";
    Element from = transform(xml);
    assertEquals("v", from.getAttribute(BpelConstants.ATTR_VARIABLE));
    assertEquals("p", from.getAttribute(BpelConstants.ATTR_PROPERTY));
  }

  // test for BPEL-167: literal values containing embedded text fail to deploy
  public void testFromLiteral_element() throws Exception {
    String xml = "<from xmlns:pur='http://www.manufacture.com'>\n"
        + " <pur:order name='o1' pur:ref='r1'>\n"
        + "  <amount xmlns=''>10</amount>\n"
        + "  <pur:item>metal</pur:item>\n"
        + "  only the best metal!!"
        + " </pur:order>"
        + "</from>";
    Element from = transform(xml);
    assertEquals("http://www.manufacture.com", DOMUtils.getNamespaceURIFromPrefix(from, "pur"));
    Element literal = XmlUtil.getElement(from, BpelConstants.NS_BPEL, BpelConstants.ELEM_LITERAL);
    Element order = XmlUtil.getElement(literal, "http://www.manufacture.com", "order");
    // local attribute
    assertEquals("o1", order.getAttribute("name"));
    // qualified attribute
    assertEquals("r1", order.getAttributeNS("http://www.manufacture.com", "ref"));
    // local child element
    assertEquals("10", DatatypeUtil.toString(XmlUtil.getElement(order, "amount")));
    // qualified child element
    assertEquals("metal", DatatypeUtil.toString(XmlUtil.getElement(order,
        "http://www.manufacture.com", "item")));
    // child text node
    assertEquals("\n  only the best metal!! ", order.getChildNodes().item(4).getNodeValue());
  }

  public void testFromLiteral_text() throws Exception {
    String xml = "<from>\n free text!!!\n</from>";
    Element from = transform(xml);
    Element literal = XmlUtil.getElement(from, BpelConstants.NS_BPEL, BpelConstants.ELEM_LITERAL);
    // text content is lost because the bpel4ws 1.1 schema does not allow it
    assertEquals("", DatatypeUtil.toString(literal));
  }

  public void testToVariable() throws Exception {
    String xml = "<to variable='v'/>";
    Element to = transform(xml);
    assertEquals("v", to.getAttribute(BpelConstants.ATTR_VARIABLE));
    assertFalse(to.hasAttribute(BpelConstants.ATTR_PART));
    assertNull(XmlUtil.getElement(to, BpelConstants.NS_BPEL, BpelConstants.ELEM_QUERY));
  }

  public void testToVariablePart() throws Exception {
    String xml = "<to variable='v' part='p'/>";
    Element to = transform(xml);
    assertEquals("v", to.getAttribute("variable"));
    assertEquals("p", to.getAttribute("part"));
    assertNull(XmlUtil.getElement(to, BpelConstants.NS_BPEL, BpelConstants.ELEM_QUERY));
  }

  public void testToVariableQuery() throws Exception {
    String xml = "<to variable='v' part='p' query='/p/q'/>";
    Element to = transform(xml);
    assertEquals("v", to.getAttribute("variable"));
    assertEquals("p", to.getAttribute("part"));
    Element query = XmlUtil.getElement(to, BpelConstants.NS_BPEL, BpelConstants.ELEM_QUERY);
    assertEquals("/p/q", DatatypeUtil.toString(query));
  }

  public void testToVariableProperty() throws Exception {
    String xml = "<to variable='v' property='p' />";
    Element to = transform(xml);
    assertEquals("v", to.getAttribute(BpelConstants.ATTR_VARIABLE));
    assertEquals("p", to.getAttribute(BpelConstants.ATTR_PROPERTY));
  }

  // ///////////////////// Extensions

  public void testExtensionElement() throws Exception {
    String xml = "<empty>"
        + " <jbpm:extension xmlns:jbpm='"
        + BpelConstants.NS_VENDOR
        + "'>"
        + "  <jbpm:extensionElement/>"
        + " </jbpm:extension>"
        + "</empty>";
    Element activity = transform(xml);
    Element extension = XmlUtil.getElement(activity, BpelConstants.NS_VENDOR, "extension");
    assertNotNull(extension);
  }

  public void testExtensionAttribute() throws Exception {
    String xml = "<empty xmlns:jbpm='http://jbpm.org/bpel' jbpm:extension='bla'/>";
    Element activity = transform(xml);
    assertNotNull(activity.getAttributeNS(BpelConstants.NS_VENDOR, "extension"));
  }

  // ///////////////////// Activity source and targets

  public void testSources_noPrefix() throws Exception {
    String xml = "<empty><source linkName='s1' transitionCondition='tc'/><source linkName='s2'/></empty>";
    Element oldVersion = transform(xml);
    Element sources = XmlUtil.getElement(oldVersion, BpelConstants.NS_BPEL, "sources");
    assertNotNull(sources);
    NodeList sourceList = sources.getElementsByTagNameNS(BpelConstants.NS_BPEL, "source");
    assertEquals(2, sourceList.getLength());
    Element transitionCondition = XmlUtil.getElement(sourceList.item(0), BpelConstants.NS_BPEL,
        "transitionCondition");
    assertNotNull(transitionCondition);
    assertEquals("tc", DatatypeUtil.toString(transitionCondition));
    // namespaces
    assertEquals(BpelConstants.NS_BPEL, transitionCondition.getNamespaceURI());
    assertEquals(BpelConstants.NS_BPEL_1_1, DOMUtils.getNamespaceURIFromPrefix(transitionCondition,
        "bpws"));
  }

  public void testSources_bpelPrefix() throws Exception {
    String xml = "<bpws:empty>"
        + " <bpws:source linkName='s1' transitionCondition='tc'/>"
        + " <bpws:source linkName='s2'/>"
        + "</bpws:empty>";
    Element oldVersion = transform(xml);
    Element sources = XmlUtil.getElement(oldVersion, BpelConstants.NS_BPEL, "sources");
    assertNotNull(sources);
    NodeList sourceList = sources.getElementsByTagNameNS(BpelConstants.NS_BPEL, "source");
    assertEquals(2, sourceList.getLength());
    Element transitionCondition = XmlUtil.getElement(sourceList.item(0), BpelConstants.NS_BPEL,
        "transitionCondition");
    assertNotNull(transitionCondition);
    assertEquals("tc", DatatypeUtil.toString(transitionCondition));
    // namespaces
    assertEquals(BpelConstants.NS_BPEL, transitionCondition.getNamespaceURI());
    assertEquals(BpelConstants.NS_BPEL_1_1, DOMUtils.getNamespaceURIFromPrefix(transitionCondition,
        "bpws"));
  }

  public void testTargets_noPrefix() throws Exception {
    String xml = "<empty joinCondition='jc'><target linkName='t1'/><target linkName='t2'/></empty>";
    Element oldVersion = transform(xml);
    Element targets = XmlUtil.getElement(oldVersion, BpelConstants.NS_BPEL, "targets");
    assertNotNull(targets);
    assertEquals(2, targets.getElementsByTagNameNS(BpelConstants.NS_BPEL, "target").getLength());
    Element joinCondition = XmlUtil.getElement(targets, BpelConstants.NS_BPEL, "joinCondition");
    assertNotNull(joinCondition);
    assertEquals("jc", DatatypeUtil.toString(joinCondition));
    // namespaces
    assertEquals(BpelConstants.NS_BPEL, joinCondition.getNamespaceURI());
    assertEquals(BpelConstants.NS_BPEL_1_1, DOMUtils.getNamespaceURIFromPrefix(joinCondition,
        "bpws"));
  }

  public void testTargets_bpelPrefix() throws Exception {
    String xml = "<bpws:empty joinCondition='jc'>"
        + " <bpws:target linkName='t1'/>"
        + " <bpws:target linkName='t2'/>"
        + "</bpws:empty>";
    Element oldVersion = transform(xml);
    Element targets = XmlUtil.getElement(oldVersion, BpelConstants.NS_BPEL, "targets");
    assertNotNull(targets);
    assertEquals(2, targets.getElementsByTagNameNS(BpelConstants.NS_BPEL, "target").getLength());
    Element joinCondition = XmlUtil.getElement(targets, BpelConstants.NS_BPEL, "joinCondition");
    assertNotNull(joinCondition);
    assertEquals("jc", DatatypeUtil.toString(joinCondition));
    // namespaces
    assertEquals(BpelConstants.NS_BPEL, joinCondition.getNamespaceURI());
    assertEquals(BpelConstants.NS_BPEL_1_1, DOMUtils.getNamespaceURIFromPrefix(joinCondition,
        "bpws"));
  }

  public static Element transform(String xmlText) throws TransformerException, SAXException {
    String wrappedText = "<parent xmlns='"
        + BpelConstants.NS_BPEL_1_1
        + "' xmlns:bpws='"
        + BpelConstants.NS_BPEL_1_1
        + "'>"
        + xmlText
        + "</parent>";
    return XmlUtil.getElement(transformNoWrap(wrappedText));
  }

  public static Element transformNoWrap(String xmlText) throws TransformerException, SAXException {
    StringWriter sink = new StringWriter();
    BpelReader.getBpelUpgradeTemplates().newTransformer().transform(
        new StreamSource(new StringReader(xmlText)), new StreamResult(sink));
    String textResult = sink.toString();
    System.out.println(textResult);
    return XmlUtil.parseText(textResult);
  }
}
