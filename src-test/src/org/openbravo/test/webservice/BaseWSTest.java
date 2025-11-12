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
 * All portions are Copyright (C) 2008-2025 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.webservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.io.SAXReader;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.dal.xml.XMLUtil;
import org.openbravo.test.base.OBBaseTest;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

/**
 * Base class for webservice tests. Provides several methods to do HTTP REST requests.
 *
 * @author mtaal
 */
public class BaseWSTest extends OBBaseTest {

  private static final Logger log = LogManager.getLogger();
  private static final String CONTEXT_PROPERTY = "context.url";
  private static String OB_URL = null;
  protected static final String LOGIN = "admin";
  protected static final String PWD = "admin";

  private String xmlSchema = null;

  /**
   * Executes a DELETE HTTP request, the wsPart is appended to the {@link #getOpenbravoURL()}.
   *
   * @param wsPart
   *     the actual webservice part of the url, is appended to the openbravo url (
   *     {@link #getOpenbravoURL()}), includes any query parameters
   * @param expectedResponse
   *     the expected HTTP response code
   */
  protected void doDirectDeleteRequest(String wsPart, int expectedResponse) {
    try {
      final HttpURLConnection hc = createConnection(wsPart, "DELETE");
      hc.connect();
      assertEquals(expectedResponse, hc.getResponseCode());
      assertTrue(hc.getContentType() != null, "Content type not set in delete response");
    } catch (final Exception e) {
      throw new OBException(e);
    }
  }

  /**
   * Execute a REST webservice HTTP request which posts/puts content and returns a XML result. The
   * content is validated against the XML schema retrieved using the /ws/dal/schema webservice call.
   *
   * @param wsPart
   *     the actual webservice part of the url, is appended to the openbravo url (
   *     {@link #getOpenbravoURL()}), includes any query parameters
   * @param content
   *     the content (XML) to post or put
   * @param expectedResponse
   *     the expected HTTP response code
   * @param expectedContent
   *     the system check that the returned content contains this expectedContent
   * @param method
   *     POST or PUT
   * @return the result from the rest request (i.e. the content of the response), most of the time
   *     an xml string
   */
  protected String doContentRequest(String wsPart, String content, int expectedResponse,
      String expectedContent, String method) {
    return doContentRequest(wsPart, content, expectedResponse, expectedContent, method, true);
  }

  /**
   * Execute a REST webservice HTTP request which posts/puts content and returns a result. If
   * validateXML parameter is <code>true</code>, the content is validated against the XML schema
   * retrieved using the /ws/dal/schema webservice call.
   *
   * @param wsPart
   *     the actual webservice part of the url, is appended to the openbravo url (
   *     {@link #getOpenbravoURL()}), includes any query parameters
   * @param content
   *     the content (XML) to post or put
   * @param expectedResponse
   *     the expected HTTP response code
   * @param expectedContent
   *     the system check that the returned content contains this expectedContent
   * @param method
   *     POST or PUT
   * @param validateXML
   *     should response be validated as XML
   * @return the result from the rest request (i.e. the content of the response), most of the time
   *     an xml string
   */
  protected String doContentRequest(String wsPart, String content, int expectedResponse,
      String expectedContent, String method, boolean validateXML) {
    try {
      final HttpURLConnection hc = createConnection(wsPart, method);
      try (OutputStream os = hc.getOutputStream()) {
        os.write(content.getBytes(StandardCharsets.UTF_8));
        os.flush();
      }

      hc.connect();
      assertEquals(expectedResponse, hc.getResponseCode());

      if (expectedResponse == 500) {
        return "";
      }

      String retContent;
      if (validateXML) {
        final SAXReader sr = XMLUtil.getInstance().newSAXReader();
        try (InputStream is = hc.getInputStream()) {
          final Document doc = sr.read(is);
          retContent = XMLUtil.getInstance().toString(doc);
          validateXML(retContent);
        }
      } else {
        try (StringWriter writer = new StringWriter()) {
          IOUtils.copy(hc.getInputStream(), writer, StandardCharsets.UTF_8);
          retContent = writer.toString();
        }
      }

      if (!retContent.contains(expectedContent)) {
        log.debug(retContent);
        fail("WS response does not contain: [" + expectedContent + "]\nActual result:\n" + retContent);
      }

      return retContent;
    } catch (final Exception e) {
      throw new OBException(e);
    }
  }

  /**
   * Convenience method to get a value of a specific XML element without parsing the whole xml
   *
   * @param content
   *     the xml
   * @param tag
   *     the element name
   * @return the value
   */
  protected String getTagValue(String content, String tag) {
    final int index1 = content.indexOf("<" + tag + ">") + ("<" + tag + ">").length();
    if (index1 == -1) {
      return "";
    }
    final int index2 = content.indexOf("</" + tag + ">");
    if (index2 == -1) {
      return "";
    }
    return content.substring(index1, index2);
  }

  /**
   * Executes a GET request and validates the return against the schema. The content is validated
   * against the XML schema retrieved using the /ws/dal/schema webservice call.
   *
   * @param wsPart
   *     the actual webservice part of the url, is appended to the openbravo url (
   *     {@link #getOpenbravoURL()}), includes any query parameters
   * @param testContent
   *     the system check that the returned content contains this testContent. if null is
   *     passed for this parameter then this check is not done.
   * @param responseCode
   *     the expected HTTP response code
   * @return the content returned from the GET request
   */
  protected String doTestGetRequest(String wsPart, String testContent, int responseCode) {
    return doTestGetRequest(wsPart, testContent, responseCode, true);
  }

  protected String doTestGetRequest(String wsPart, String testContent, int responseCode,
      boolean validate) {
    return doTestGetRequest(wsPart, testContent, responseCode, validate, true);
  }

  /**
   * Executes a GET request. The content is validated against the XML schema retrieved using the
   * /ws/dal/schema webservice call.
   *
   * @param wsPart
   *     the actual webservice part of the url, is appended to the openbravo url (
   *     {@link #getOpenbravoURL()}), includes any query parameters
   * @param testContent
   *     the system check that the returned content contains this testContent. if null is
   *     passed for this parameter then this check is not done.
   * @param responseCode
   *     the expected HTTP response code
   * @param validate
   *     if true then the response content is validated against the Openbravo XML Schema
   * @param logException
   *     indicates whether in case of Exception it should be logged, this param should be false
   *     when Exception is expected in order not to pollute the log
   * @return the content returned from the GET request
   */
  protected String doTestGetRequest(String wsPart, String testContent, int responseCode,
      boolean validate, boolean logException) {
    try {
      final HttpURLConnection hc = createConnection(wsPart, "GET");
      hc.connect();
      final SAXReader sr = XMLUtil.getInstance().newSAXReader();
      final StringBuilder sb = new StringBuilder();

      try (InputStream is = hc.getInputStream();
           BufferedReader reader = new BufferedReader(
               new InputStreamReader(is, StandardCharsets.UTF_8))) {
        String line;
        while ((line = reader.readLine()) != null) {
          sb.append(line).append("\n");
        }
      }

      final Document doc = sr.read(new StringReader(sb.toString()));
      final String content = XMLUtil.getInstance().toString(doc);
      if (testContent != null && !content.contains(testContent)) {
        log.debug(content);
        fail("Expected content not found in WS response");
      }

      assertEquals(responseCode, hc.getResponseCode());
      if (validate) {
        validateXML(content);
      }
      return content;
    } catch (final Exception e) {
      throw new OBException("Exception when executing ws: " + wsPart, e, logException);
    }
  }

  /**
   * Creates a HTTP connection.
   *
   * @param wsPart
   * @param method
   *     POST, PUT, GET or DELETE
   * @return the created connection
   * @throws Exception
   */
  protected HttpURLConnection createConnection(String wsPart, String method) throws Exception {
    Authenticator.setDefault(new Authenticator() {
      @Override
      protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(LOGIN, PWD.toCharArray());
      }
    });

    log.debug(method + ": " + getOpenbravoURL() + wsPart);
    final URL url = new URL(getOpenbravoURL() + wsPart);
    final HttpURLConnection hc = (HttpURLConnection) url.openConnection();
    hc.setRequestMethod(method);
    hc.setDoOutput(true);
    hc.setDoInput(true);
    hc.setInstanceFollowRedirects(true);
    hc.setUseCaches(false);
    hc.setRequestProperty("Content-Type", "text/xml");
    return hc;
  }

  /**
   * Returns the url of the Openbravo instance. The default value is: {@link #OB_URL}
   * 
   * @return the url of the Openbravo instance.
   */
  protected String getOpenbravoURL() {
    if (OB_URL != null) {
      return OB_URL;
    }
    Properties props = OBPropertiesProvider.getInstance().getOpenbravoProperties();
    OB_URL = props.getProperty(CONTEXT_PROPERTY);
    if (StringUtils.isEmpty(OB_URL)) {
      throw new OBException(CONTEXT_PROPERTY + " is not set in Openbravo.properties");
    }
    log.debug("got OB context: " + OB_URL);
    return OB_URL;
  }

  /**
   * Returns the login used to login for the webservice. The default value is {@link #LOGIN}.
   *
   * @return the login name used to login for the webservice
   */
  protected String getLogin() {
    return LOGIN;
  }

  /**
   * Returns the password used to login into the webservice server. The default value is
   * {@link #PWD}.
   *
   * @return the password used to login into the webservice, the default is {@link #PWD}
   */
  protected String getPassword() {
    return PWD;
  }

  /**
   * Validates the xml against the generated schema.
   *
   * @param xml
   *     the xml to validate
   */
  protected void validateXML(String xml) {
    final Reader schemaReader = new StringReader(getXMLSchema());
    final Reader xmlReader = new StringReader(xml);
    try {
      SAXParserFactory factory = SAXParserFactory.newInstance();
      factory.setValidating(false);
      factory.setNamespaceAware(true);
      SchemaFactory schemaFactory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
      factory.setSchema(schemaFactory.newSchema(new Source[]{ new StreamSource(schemaReader) }));

      SAXParser parser = factory.newSAXParser();
      XMLReader reader = parser.getXMLReader();
      reader.setErrorHandler(new SimpleErrorHandler());
      reader.parse(new InputSource(xmlReader));
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  private String getXMLSchema() {
    if (xmlSchema != null) {
      return xmlSchema;
    }
    xmlSchema = doTestGetRequest("/ws/dal/schema", "<xs:element name=\"Openbravo\">", 200, false);
    return xmlSchema;
  }

  public class SimpleErrorHandler implements ErrorHandler {
    @Override
    public void warning(SAXParseException e) throws SAXException {
    }

    @Override
    public void error(SAXParseException e) throws SAXException {
      throw e;
    }

    @Override
    public void fatalError(SAXParseException e) throws SAXException {
      throw e;
    }
  }

}
