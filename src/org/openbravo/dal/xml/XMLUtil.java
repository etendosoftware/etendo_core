/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html 
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License. 
 * The Original Code is Openbravo ERP. 
 * The Initial Developer of the Original Code is Openbravo SLU 
 * All portions are Copyright (C) 2008-2020 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.dal.xml;

import java.io.InputStream;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.provider.OBSingleton;
import org.xml.sax.SAXException;

/**
 * Utility class for XML processing.
 * 
 * @see XMLEntityConverter
 * @see EntityXMLConverter
 * @see org.openbravo.service.rest.DalWebService
 * 
 * @author mtaal
 */

public class XMLUtil implements OBSingleton {

  private static final Logger log = LogManager.getLogger();
  private static XMLUtil instance;

  public static synchronized XMLUtil getInstance() {
    if (instance == null) {
      instance = OBProvider.getInstance().get(XMLUtil.class);
    }
    return instance;
  }

  public static synchronized void setInstance(XMLUtil instance) {
    XMLUtil.instance = instance;
  }

  /** @return a new Dom4j Document */
  public Document createDomDocument() {
    return DocumentHelper.createDocument();
  }

  /** @return a new secure {@link DocumentBuilder} */
  public DocumentBuilder newDocumentBuilder() throws ParserConfigurationException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
    factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
    factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
    factory.setXIncludeAware(false);
    factory.setExpandEntityReferences(false);
    return factory.newDocumentBuilder();
  }

  /** @return a new secure {@link TransformerHandler} */
  public TransformerHandler newSAXTransformerHandler() throws TransformerConfigurationException {
    final SAXTransformerFactory tf = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
    setAttribute(tf, "http://javax.xml.XMLConstants/property/accessExternalDTD", "");
    setAttribute(tf, "http://javax.xml.XMLConstants/property/accessExternalStylesheet", "");

    return tf.newTransformerHandler();
  }

  /** @return a new secure {@link SAXReader} */
  public SAXReader newSAXReader() throws SAXException {
    final SAXReader reader = new SAXReader();
    reader.setFeature("http://xml.org/sax/features/external-general-entities", false);
    reader.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
    reader.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    return reader;
  }

  /**
   * Parses provided InputStream into XML and extracts root element
   * 
   * @param in
   *          InputStream XML
   * @return Element root element
   */
  public Element getRootElement(InputStream in) {
    Element rootElement = null;
    try {
      SAXReader reader = newSAXReader();
      // Most SVG image files come with <Doctype> declaration. This option is set to false to allow
      // its usage and be able to parse those SVGs
      reader.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false);
      Document doc = reader.read(in);
      rootElement = doc.getRootElement();
    } catch (DocumentException | SAXException ex) {
      log.error("Failed to parse XML input for extracting root element.", ex);
    }
    return rootElement;
  }

  /** @return a new secure {@link TransformerFactory} */
  public TransformerFactory newTransformerFactory() {
    TransformerFactory factory = TransformerFactory.newInstance();
    setAttribute(factory, "http://javax.xml.XMLConstants/property/accessExternalDTD", "");
    setAttribute(factory, "http://javax.xml.XMLConstants/property/accessExternalStylesheet", "");
    return factory;
  }

  private void setAttribute(TransformerFactory factory, String attribute, Object value) {
    try {
      factory.setAttribute(attribute, value);
    } catch (IllegalArgumentException ex) {
      log.warn("TransformerFactory implementation {} doesn't recognize the attribute {}",
          factory.getClass().getName(), attribute);
    }
  }

  /**
   * Creates a standard Openbravo root element for a xml document and set ths namespace. Ads the new
   * root element to the Dom4j document.
   * 
   * @param doc
   *          the Dom4j document to set the root element
   * @param elementName
   *          the name of the root element
   * @return the new root element
   */
  public Element addRootElement(Document doc, String elementName) {
    final Namespace ns = new Namespace("ob", "http://www.openbravo.com");
    final QName qName = new QName(elementName, ns);
    final Element root = doc.addElement(qName);
    root.addNamespace("ob", "http://www.openbravo.com");
    return root;
  }

  /**
   * Converts a Dom4j document to a string. A number of specific settings: 1) output encoding is
   * UTF-8, 2) text nodes are not trimmed
   * 
   * @param document
   *          the Dom4j to convert to a XML string
   * @return the XML representation
   */
  public String toString(Document document) {
    try {
      final OutputFormat format = OutputFormat.createPrettyPrint();
      format.setEncoding("UTF-8");
      format.setTrimText(false);
      final StringWriter out = new StringWriter();
      final XMLWriter writer = new XMLWriter(out, format);
      writer.write(document);
      writer.close();
      return out.toString();
    } catch (final Exception e) {
      throw new OBException(e);
    }
  }

}
