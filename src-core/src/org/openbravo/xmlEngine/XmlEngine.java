/*
 ************************************************************************************
 * Copyright (C) 2001-2019 Openbravo S.L.U.
 * Licensed under the Apache Software License version 2.0
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to  in writing,  software  distributed
 * under the License is distributed  on  an  "AS IS"  BASIS,  WITHOUT  WARRANTIES  OR
 * CONDITIONS OF ANY KIND, either  express  or  implied.  See  the  License  for  the
 * specific language governing permissions and limitations under the License.
 ************************************************************************************
 */
package org.openbravo.xmlEngine;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Hashtable;
import java.util.Stack;
import java.util.Vector;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.xerces.parsers.SAXParser;
import org.openbravo.database.ConnectionProvider;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

public class XmlEngine {
  private XMLReader xmlParser;
  private XMLReader htmlParser;
  private Hashtable<String, XmlTemplate> hasXmlTemplate;
  private Stack<XmlTemplate> stcRead;
  Hashtable<String, FormatCouple> formatHashtable;
  Hashtable<String, Vector<ReplaceElement>> replaceHashtable;
  private String strDriverDefault;
  private String strUrlDefault;
  private String strFormatFile;
  public File fileXmlEngineFormat;
  public File fileBaseLocation;
  public String sessionLanguage;
  public String strReplaceWhat;
  public String strReplaceWith;
  public boolean isResource = false;

  public static String strTextDividedByZero;

  private static final Logger log4jXmlEngine = LogManager.getLogger();
  private static final Logger log4jReloadXml = LogManager.getLogger("reloadXml");

  ConnectionProvider connProvider;

  public XmlEngine(ConnectionProvider connProvider) {
    this.connProvider = connProvider;
  }

  public XmlEngine() {
  }

  private void loadParams() {
    replaceHashtable = new Hashtable<>();
    Vector<ReplaceElement> htmlReplaceVector = new Vector<>();
    htmlReplaceVector.addElement(new ReplaceElement("&", "&amp;")); // this
    htmlReplaceVector.addElement(new ReplaceElement("\"", "&quot;"));
    htmlReplaceVector.addElement(new ReplaceElement("\n", " "));
    htmlReplaceVector.addElement(new ReplaceElement("\r", " "));
    htmlReplaceVector.addElement(new ReplaceElement("<", "&lt;"));
    htmlReplaceVector.addElement(new ReplaceElement(">", "&gt;"));
    htmlReplaceVector.addElement(new ReplaceElement("®", "&reg;"));
    htmlReplaceVector.addElement(new ReplaceElement("€", "&euro;"));
    htmlReplaceVector.addElement(new ReplaceElement("ñ", "&ntilde;"));
    htmlReplaceVector.addElement(new ReplaceElement("Ñ", "&Ntilde;"));
    replaceHashtable.put("html", htmlReplaceVector);

    Vector<ReplaceElement> foReplaceVector = new Vector<>();
    foReplaceVector.addElement(new ReplaceElement("&", "&#38;"));
    foReplaceVector.addElement(new ReplaceElement("<", "&#60;"));
    foReplaceVector.addElement(new ReplaceElement(">", "&#62;"));
    foReplaceVector.addElement(new ReplaceElement("\\", "&#92;"));
    foReplaceVector.addElement(new ReplaceElement("º", "&#186;"));
    foReplaceVector.addElement(new ReplaceElement("ª", "&#170;"));
    foReplaceVector.addElement(new ReplaceElement("®", "&#174;"));
    foReplaceVector.addElement(new ReplaceElement("€", "&#8364;"));
    foReplaceVector.addElement(new ReplaceElement("\n", "&#10;"));
    replaceHashtable.put("fo", foReplaceVector);

    Vector<ReplaceElement> htmlPreformatedReplaceVector = new Vector<>();
    htmlPreformatedReplaceVector.addElement(new ReplaceElement("&", "&amp;"));
    htmlPreformatedReplaceVector.addElement(new ReplaceElement("\"", "&quot;"));
    htmlPreformatedReplaceVector.addElement(new ReplaceElement("<", "&lt;"));
    htmlPreformatedReplaceVector.addElement(new ReplaceElement(">", "&gt;"));
    htmlPreformatedReplaceVector.addElement(new ReplaceElement("\n", "<BR>"));
    htmlPreformatedReplaceVector.addElement(new ReplaceElement("\r", " "));
    htmlPreformatedReplaceVector.addElement(new ReplaceElement("®", "&reg;"));
    replaceHashtable.put("htmlPreformated", htmlPreformatedReplaceVector);

    Vector<ReplaceElement> htmlHelpReplaceVector = new Vector<>();
    htmlHelpReplaceVector.addElement(new ReplaceElement("\n", "<BR>"));
    htmlHelpReplaceVector.addElement(new ReplaceElement("\r", ""));
    replaceHashtable.put("htmlHelp", htmlHelpReplaceVector);

    Vector<ReplaceElement> htmlPreformatedTextareaReplaceVector = new Vector<>();
    htmlPreformatedTextareaReplaceVector.addElement(new ReplaceElement("&", "&amp;"));
    htmlPreformatedTextareaReplaceVector.addElement(new ReplaceElement("\"", "&quot;"));
    htmlPreformatedTextareaReplaceVector.addElement(new ReplaceElement("<", "&lt;"));
    htmlPreformatedTextareaReplaceVector.addElement(new ReplaceElement(">", "&gt;"));
    htmlPreformatedTextareaReplaceVector.addElement(new ReplaceElement("®", "&reg;"));
    replaceHashtable.put("htmlPreformatedTextarea", htmlPreformatedTextareaReplaceVector);

    Vector<ReplaceElement> htmlJavaScriptReplaceVector = new Vector<>();
    htmlJavaScriptReplaceVector.addElement(new ReplaceElement("'", "\\'"));
    htmlJavaScriptReplaceVector.addElement(new ReplaceElement("\"", "&quot;"));
    htmlJavaScriptReplaceVector.addElement(new ReplaceElement("\n", "\\n"));
    replaceHashtable.put("htmlJavaScript", htmlJavaScriptReplaceVector);
  }

  public void initialize() {
    hasXmlTemplate = new Hashtable<>();
    stcRead = new Stack<>(); // stack of XmlTemplates not read
    formatHashtable = new Hashtable<>();
    XMLReader xmlParserFormat = new SAXParser();
    xmlParserFormat.setContentHandler(new FormatRead(formatHashtable));

    log4jXmlEngine.debug("XmlEngine file formats: {}", strFormatFile);
    log4jXmlEngine.debug("fileXmlEngineFormat: {}", fileXmlEngineFormat);
    try {
      xmlParserFormat.parse(new InputSource(
          new InputStreamReader(new FileInputStream(fileXmlEngineFormat), "UTF-8")));
    } catch (Exception e) {
      log4jXmlEngine.error("Exception in fileXmlEngineFormat: " + fileXmlEngineFormat, e);
      return;
    }
    loadParams();
  }

  /**
   * this function reads a file that defines a XmlTemplate without any discard
   * 
   * @param strXmlTemplateFile
   *          A configuration file of the XmlTemplate in XML format
   */
  public XmlTemplate readXmlTemplate(String strXmlTemplateFile) {
    return readXmlTemplate(strXmlTemplateFile, new String[0]);
  }

  /**
   * this function reads a file that defines a XmlTemplate with a vector of discard
   * 
   * @param strXmlTemplateFile
   *          A configuration file of the XmlTemplate in XML format
   * @param discard
   *          A vector of Strings with the names of the discards in the template file. The elements
   *          with a id equal to a discard are not readed
   */
  public synchronized XmlTemplate readXmlTemplate(String strXmlTemplateFile, String[] discard) {
    String xmlTemplateName = strXmlTemplateFile;
    xmlTemplateName = fileBaseLocation.getName() + xmlTemplateName;
    for (int i = 0; i < discard.length; i++) {
      xmlTemplateName = xmlTemplateName + "?" + discard[i];
    }
    if (log4jReloadXml.isDebugEnabled()) {
      initialize();
      log4jReloadXml.debug("XmlEngine Initialized");
    }
    return readAllXmlTemplates(xmlTemplateName, strXmlTemplateFile, discard);
  }

  /**
   * this function add the XmlTemplate to the list of XmlTemplates and read all the XmlTemplates
   * that there are in the Stack. The XmlTemplates are added to the Stack in the addXmlTemplate
   * function or in the readFile function
   * 
   * @param strXmlTemplateName
   *          The name that identifies the XmlTemplate
   * @param strXmlTemplateFile
   *          The configuration file of the XmlTemplate
   * @param discard
   *          A vector of Strings with the names of the discards in the template
   */
  XmlTemplate readAllXmlTemplates(String strXmlTemplateName, String strXmlTemplateFile,
      String[] discard) {
    XmlTemplate xmlTemplate = addXmlTemplate(strXmlTemplateName, strXmlTemplateFile, discard);
    while (!stcRead.empty()) {
      XmlTemplate xmlTemplateRead = stcRead.pop();
      readFile(xmlTemplateRead);
    }
    return xmlTemplate;
  }

  /**
   * this function add the XmlTemplate to the list of XmlTemplates or return an existing XmlTemplate
   * if it was found in the list
   * 
   * @param strXmlTemplateName
   *          The name that identifies the XmlTemplate
   * @param strXmlTemplateFile
   *          The configuration file of the XmlTemplate
   * @param discard
   *          A vector of Strings with the names of the discards in the template
   */
  private XmlTemplate addXmlTemplate(String strXmlTemplateName, String strXmlTemplateFile,
      String[] discard) {
    log4jXmlEngine.debug("Adding: {}", strXmlTemplateName);
    XmlTemplate xmlTemplate = hasXmlTemplate.get(strXmlTemplateName);
    if (xmlTemplate != null) {
      return xmlTemplate;
    }

    xmlTemplate = new XmlTemplate(strXmlTemplateName, strXmlTemplateFile, discard, this);
    xmlTemplate.configuration.strDriverDefault = strDriverDefault;
    xmlTemplate.configuration.strUrlDefault = strUrlDefault;
    hasXmlTemplate.put(strXmlTemplateName, xmlTemplate);
    stcRead.push(xmlTemplate);
    return xmlTemplate;
  }

  /**
   * this function read the XmlTemplate
   * 
   * @param xmlTemplate
   *          The XmlTemplate object
   */
  private void readFile(XmlTemplate xmlTemplate) {
    xmlParser = new SAXParser();
    htmlParser = new org.cyberneko.html.parsers.SAXParser();

    // parser of the configuration file
    xmlParser.setContentHandler(xmlTemplate.configuration);
    String strFile = xmlTemplate.fileConfiguration() + ".xml";
    if (log4jXmlEngine.isDebugEnabled()) {
      log4jXmlEngine.debug("XmlEngine name: " + strFile);
    }
    File fileXmlEngineConfiguration = null;
    if (!isResource) {
      fileXmlEngineConfiguration = new File(fileBaseLocation, strFile);
      log4jXmlEngine.debug("fileXmlEngineConfiguration: {} - parent: {}",
          fileXmlEngineConfiguration.toString(), fileXmlEngineConfiguration.getParent());
    }
    xmlTemplate.clear();
    try {
      if (!isResource) {
        xmlParser.parse(new InputSource(
            new InputStreamReader(new FileInputStream(fileXmlEngineConfiguration), "UTF-8")));
      } else {
        xmlParser.parse(new InputSource(ClassLoader.getSystemResourceAsStream(strFile)));
      }
    } catch (Exception e) {
      if (!isResource) {
        log4jXmlEngine.error("Exception in fileXmlEngineConfiguration: {}",
            fileXmlEngineConfiguration, e);
      } else {
        log4jXmlEngine.error("Exception in fileXmlEngineConfiguration: {}", strFile, e);
      }
      return;
    }

    // parser of the template file
    int posExtension = xmlTemplate.configuration.strTemplate.lastIndexOf('.');
    XMLReader templateParser;
    if (xmlTemplate.configuration.strTemplate.substring(posExtension).equals(".html")) {
      if (log4jXmlEngine.isDebugEnabled()) {
        log4jXmlEngine.debug("Html file: {}",
            xmlTemplate.configuration.strTemplate.substring(posExtension));
      }
      templateParser = htmlParser;
    } else {
      if (log4jXmlEngine.isDebugEnabled()) {
        log4jXmlEngine.debug("Xml file: {}",
            xmlTemplate.configuration.strTemplate.substring(posExtension));
      }
      templateParser = xmlParser;
    }
    templateParser.setContentHandler(xmlTemplate);
    log4jXmlEngine.debug("XmlEngine file template: {}", xmlTemplate.configuration.strTemplate);

    File fileXmlEngineTemplate = null;
    String strPath = "";
    if (!isResource) {
      fileXmlEngineTemplate = new File(fileXmlEngineConfiguration.getParent(),
          xmlTemplate.configuration.strTemplate);
      log4jXmlEngine.debug("fileXmlEngineTemplate: {}", fileXmlEngineTemplate);
    } else {
      int finPath = -1;
      if ((finPath = strFile.lastIndexOf('/')) != -1) {
        strPath = strFile.substring(0, finPath);
        if (!strPath.endsWith("/")) {
          strPath += "/";
        }
      }
    }
    try {
      if (!isResource) {
        templateParser.parse(new InputSource(
            new InputStreamReader(new FileInputStream(fileXmlEngineTemplate), "UTF-8")));
      } else {
        templateParser.parse(new InputSource(ClassLoader
            .getSystemResourceAsStream(strPath + xmlTemplate.configuration.strTemplate)));
      }
    } catch (Exception e) {
      if (!isResource) {
        log4jXmlEngine.error("Exception in fileXmlEngineTemplate: {}", fileXmlEngineTemplate, e);
      } else {
        log4jXmlEngine.error("Exception in fileXmlEngineTemplate: {}",
            strPath + xmlTemplate.configuration.strTemplate, e);
      }
    }
  }
}
