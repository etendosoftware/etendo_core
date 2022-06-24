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
 * All portions are Copyright (C) 2001-2019 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.wad;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.servlet.ServletException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.data.FieldProvider;
import org.openbravo.data.Sqlc;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.utils.FormatUtilities;
import org.openbravo.wad.controls.WADControl;
import org.openbravo.xmlEngine.XmlDocument;
import org.openbravo.xmlEngine.XmlEngine;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Main class of WAD project. This class manage all the process to generate the sources from the
 * model.
 * 
 * @author Fernando Iriazabal
 */
public class Wad extends DefaultHandler {
  private static final int MAX_SIZE_EDITION_1_COLUMNS = 90;
  private static final int MAX_TEXTBOX_LENGTH = 110;
  private static final String WELD_LISTENER_ID = "3F88D97C7E9E4DD9847A5488771F4AB3";
  private static final String ERROR_CODE_PAGES = "error-code";
  private static final String EXCEPTION_TYPE_PAGES = "exception-type";
  private static final String NONE = "none";
  private XmlEngine xmlEngine;
  private WadConnection pool;
  private String strSystemSeparator;
  private static String jsDateFormat;
  private static String sqlDateFormat;
  private static boolean generateAllClassic250Windows;
  private static boolean excludeCDI;

  private static final Logger log4j = LogManager.getLogger();

  /**
   * Main function, entrusted to launch the process of generation of sources. The list of arguments
   * that it can receive are the following ones:<br>
   * <ol>
   * <li>Path to XmlPool.xml</li>
   * <li>Name of the window to generate (% for all)</li>
   * <li>Path to generate the code</li>
   * <li>Path to generate common objects (reference)</li>
   * <li>Path to generate the web.xml</li>
   * <li>Parameter:
   * <ul>
   * <li>tabs (To generate only the windows and action buttons)</li>
   * <li>web.xml (To generate only the web.xml)</li>
   * <li>all (To generate everything)</li>
   * </ul>
   * <li>Path to generate the action buttons</li>
   * <li>Path to generate the translated objects</li>
   * <li>Base package for the translation objects</li>
   * <li>Path to find the client web.xml file</li>
   * <li>Path to the project root</li>
   * <li>Path to the attached files</li>
   * <li>Url to the static web contents</li>
   * <li>Path to the src</li>
   * <li>Boolean to indicate if it's gonna be made a complete generation or not</li>
   * </ol>
   * 
   * @param argv
   *          Arguments array
   * @throws Exception
   */
  public static void main(String argv[]) throws Exception {
    String strWindowName;
    String module;
    String dirFin;
    String dirReference;
    String dirWebXml;
    String dirActionButton;
    boolean generateWebXml;
    boolean generateTabs;
    String attachPath;
    String webPath;
    boolean complete;
    boolean quick;
    if (argv.length < 1) {
      log4j.error("Usage: java Wad connection.xml [{% || Window} [destinyDir]]");
      return;
    }
    final String strFileConnection = argv[0];
    final Wad wad = new Wad();
    wad.strSystemSeparator = System.getProperty("file.separator");
    wad.createPool(strFileConnection + "/Openbravo.properties");
    wad.createXmlEngine(strFileConnection);
    wad.readProperties(strFileConnection + "/Openbravo.properties");
    try {
      // the second parameter is the tab to be generated
      // if there is none it's * then all them are read
      strWindowName = argv[1];

      // the third parameter is the directory where the tab files are
      // created
      if (argv.length <= 2) {
        dirFin = ".";
      } else {
        dirFin = argv[2];
      }

      // the fourth paramenter is the directory where the references are
      // created
      // (TableList_data.xsql y TableDir_data.xsql)
      if (argv.length <= 3) {
        dirReference = dirFin;
      } else {
        dirReference = argv[3];
      }

      // the fifth parameter is the directory where web.xml is created
      if (argv.length <= 4) {
        dirWebXml = dirFin;
      } else {
        dirWebXml = argv[4];
      }

      // the sixth parementer indicates whether web.xml has to be
      // generated or not
      if (argv.length <= 5) {
        generateWebXml = true;
        generateTabs = true;
      } else if (argv[5].equals("web.xml")) {
        generateWebXml = true;
        generateTabs = false;
      } else if (argv[5].equals("tabs")) {
        generateWebXml = false;
        generateTabs = true;
      } else {
        generateWebXml = true;
        generateTabs = true;
      }

      // Path to generate the action button
      if (argv.length <= 6) {
        dirActionButton = dirFin;
      } else {
        dirActionButton = argv[6];
      }

      // Path to base translation generation
      // was argv[7] no longer used

      // Translate base structure
      // was argv[8] no longer used

      // Path to find the client's web.xml file
      // was argv[9] no longer used

      // Path of the root project
      // was argv[10] no longer used

      // Path of the attach files
      if (argv.length <= 11) {
        attachPath = dirFin;
      } else {
        attachPath = argv[11];
      }

      // Url to the static content
      if (argv.length <= 12) {
        webPath = dirFin;
      } else {
        webPath = argv[12];
      }

      // Path to the src folder
      // was argv[13] no longer used

      // Boolean to indicate if we are doing a complete generation
      if (argv.length <= 14) {
        complete = false;
      } else {
        complete = ((argv[14].equals("true")) ? true : false);
      }

      // Module to compile
      if (argv.length <= 15) {
        module = "%";
      } else {
        module = argv[15].equals("%") ? "%"
            : "'" + argv[15].replace(", ", ",").replace(",", "', '") + "'";
      }

      // Check for quick build
      if (argv.length <= 16) {
        quick = false;
      } else {
        quick = argv[16].equals("quick");
      }

      if (quick) {
        module = "%";
        strWindowName = "xx";
      }

      if (argv.length <= 17) {
        generateAllClassic250Windows = false;
      } else {
        generateAllClassic250Windows = argv[17].equals("true");
      }

      if (argv.length <= 18) {
        excludeCDI = false;
      } else {
        excludeCDI = argv[18].equals("true");
      }

      log4j.info("File connection: " + strFileConnection);
      log4j.info("window: " + strWindowName);
      log4j.info("module: " + module);
      log4j.info("directory destiny: " + dirFin);
      log4j.info("directory reference: " + dirReference + wad.strSystemSeparator + "reference");
      log4j.info("directory web.xml: " + dirWebXml);
      log4j.info("directory ActionButtons: " + dirActionButton);
      log4j.info("generate web.xml: " + generateWebXml);
      log4j.info("generate tabs: " + generateTabs);
      log4j.info("File separator: " + wad.strSystemSeparator);
      log4j.info("Attach path: " + attachPath);
      log4j.info("Web path: " + webPath);
      log4j.info("Quick mode: " + quick);
      log4j.info("Generate all 2.50 windows: " + generateAllClassic250Windows);
      log4j.info("Exclude CDI: " + excludeCDI);

      final File fileFin = new File(dirFin);
      if (!fileFin.exists()) {
        log4j.error("No such directory: " + fileFin.getAbsoluteFile());

        return;
      }

      final File fileFinReloads = new File(dirReference + wad.strSystemSeparator + "ad_callouts");
      if (!fileFinReloads.exists()) {
        log4j.error("No such directory: " + fileFinReloads.getAbsoluteFile());

        return;
      }

      final File fileReference = new File(dirReference + wad.strSystemSeparator + "reference");
      if (!fileReference.exists()) {
        log4j.error("No such directory: " + fileReference.getAbsoluteFile());

        return;
      }

      final File fileWebXml = new File(dirWebXml);
      if (!fileWebXml.exists()) {
        log4j.error("No such directory: " + fileWebXml.getAbsoluteFile());

        return;
      }

      final File fileActionButton = new File(dirActionButton);
      if (!fileActionButton.exists()) {
        log4j.error("No such directory: " + fileActionButton.getAbsoluteFile());

        return;
      }

      // Calculate windows to generate
      String strCurrentWindow;
      final StringTokenizer st = new StringTokenizer(strWindowName, ",", false);
      ArrayList<TabsData> td = new ArrayList<TabsData>();
      while (st.hasMoreTokens()) {
        strCurrentWindow = st.nextToken().trim();
        TabsData tabsDataAux[];
        if (quick) {
          tabsDataAux = TabsData.selectQuick(wad.pool);
        } else if (module.equals("%") || complete) {
          tabsDataAux = TabsData.selectTabs(wad.pool, strCurrentWindow);
        } else {
          tabsDataAux = TabsData.selectTabsinModules(wad.pool, strCurrentWindow, module);
        }
        td.addAll(Arrays.asList(tabsDataAux));
      }
      TabsData[] tabsData = td.toArray(new TabsData[0]);
      log4j.info(tabsData.length + " tabs to compile.");

      // Call to update the table identifiers
      log4j.info("Updating table identifiers");
      WadData.updateIdentifiers(wad.pool, quick ? "Y" : "N");

      // Call to generate audit trail infrastructure
      log4j.info("Re-generating audit trail infrastructure");
      WadData.updateAuditTrail(wad.pool);

      // If generateTabs parameter is true, the action buttons must be
      // generated
      if (generateTabs) {
        if (!quick || ProcessRelationData.generateActionButton(wad.pool)) {
          wad.processProcessComboReloads(fileFinReloads);
          wad.processActionButton(fileReference);
        } else {
          log4j.info("No changes in ActionButton_data.xml");
        }
        if (!quick || FieldsData.buildActionButton(wad.pool)) {
          wad.processActionButtonXml(fileActionButton);
          wad.processActionButtonHtml(fileActionButton);
        } else {
          log4j.info("No changes in Action button for columns");
        }
        if (!quick || ActionButtonRelationData.buildGenerics(wad.pool)) {
          wad.processActionButtonGenerics(fileActionButton);
          wad.processActionButtonXmlGenerics(fileActionButton);
          wad.processActionButtonHtmlGenerics(fileActionButton);
          wad.processActionButtonSQLDefaultGenerics(fileActionButton);
        } else {
          log4j.info("No changes in generic action button responser");
        }

      }

      checkInvalidWindowDefs(wad.pool, tabsData);
      Map<String, Boolean> generateTabMap = calculateTabsToGenerate(wad.pool, tabsData);
      int skip = 0;
      int generate = 0;
      for (Boolean b : generateTabMap.values()) {
        if (b) {
          generate++;
        } else {
          skip++;
        }
      }
      log4j.info("After filtering generating " + generate + " tabs and skipping " + skip);

      // If generateWebXml parameter is true, the web.xml file should be
      // generated
      if (generateWebXml) {

        if (!quick || WadData.genereteWebXml(wad.pool)) {
          wad.processWebXml(fileWebXml, attachPath, webPath);
        } else {
          log4j.info("No changes in web.xml");
        }
      }

      if (tabsData.length == 0) {
        log4j.info("No windows to compile");
      }

      if (generateTabs) {
        for (int i = 0; i < tabsData.length; i++) {
          // don't compile if it is in an unactive branch
          if (wad.allTabParentsActive(tabsData[i].tabid)) {
            boolean tabJavaNeeded = generateAllClassic250Windows
                || generateTabMap.get(tabsData[i].tabid);

            if (tabJavaNeeded) {
              log4j.info("Processing Window: " + tabsData[i].windowname + " - Tab: "
                  + tabsData[i].tabname + " - id: " + tabsData[i].tabid);
              wad.processTab(fileFin, fileFinReloads, tabsData[i]);
            } else {
              log4j.debug("Skipped Window: " + tabsData[i].windowname + " - Tab: "
                  + tabsData[i].tabname + " - id: " + tabsData[i].tabid);
            }
          }
        }
      }
    } finally {
      wad.pool.destroy();
    }
  }

  /** Checks and warns about windows defined in 2.50 style */
  private static void checkInvalidWindowDefs(ConnectionProvider conn, FieldProvider[] tabsData)
      throws ServletException {
    String oldWindowId = null;
    for (FieldProvider tab : tabsData) {
      if (oldWindowId == null || !tab.getField("key").equals(oldWindowId)) {
        // new window -> check all tabs in that window
        boolean res = TabsData.selectShowWindowIn250ClassicMode(conn, tab.getField("key"));
        if (res) {
          log4j.error("Window: " + tab.getField("windowname")
              + " is needed in classic 2.50 mode. This is no longer supported. The module containing the window must be fixed.");
        } else {
          res = TabsData.selectShowWindowIn250ClassicModePreference(conn, tab.getField("key"));
          if (res) {
            log4j.error("Window: " + tab.getField("windowname")
                + " is configured for classic 2.50 mode via preferences. This is no longer supported...");
          }
        }
        oldWindowId = tab.getField("key");
      }
    }
  }

  private static Map<String, Boolean> calculateTabsToGenerate(ConnectionProvider conn,
      FieldProvider[] tabsData) throws ServletException {
    Map<String, Boolean> res = new HashMap<String, Boolean>();

    for (FieldProvider tab : tabsData) {
      // if already calculated before skip
      if (res.get(tab.getField("tabid")) != null) {
        continue;
      }

      boolean needToCompile = tabJavaNeededforActionButtons(conn, tab.getField("tabid"));

      if ("Y".equals(tab.getField("issorttab"))) {
        log4j.warn("2.50 Sort Tab no longer supported (it will be skipped): "
            + tab.getField("tabname") + ",id: " + tab.getField("tabid"));
        needToCompile = false;
      } else if (needToCompile) {
        log4j.debug("Need to generate tab: " + tab.getField("tabname") + ",id: "
            + tab.getField("tabid") + ", level: " + tab.getField("tablevel"));
      }
      res.put(tab.getField("tabid"), needToCompile);
    }

    return res;
  }

  private static boolean tabJavaNeededforActionButtons(ConnectionProvider conn, String tabId)
      throws ServletException {
    ActionButtonRelationData[] actBtns = ActionButtonRelationData.select(conn, tabId);
    ActionButtonRelationData[] actBtnsJava = ActionButtonRelationData.selectJava(conn, tabId);

    if ((actBtns == null || actBtns.length == 0) && (actBtnsJava == null || actBtnsJava.length == 0)
        && FieldsData.hasPostedButton(conn, tabId).equals("0")
        && FieldsData.hasCreateFromButton(conn, tabId).equals("0")) {
      // No action buttons
      return false;
    }

    return true;
  }

  private boolean allTabParentsActive(String tabId) {
    try {
      if (!TabsData.isTabActive(pool, tabId)) {
        return false;
      } else {
        String parentTabId = TabsData.selectParentTab(pool, tabId);
        if (parentTabId != null && !parentTabId.equals("")) {
          return allTabParentsActive(parentTabId);
        }
      }
      return true;
    } catch (Exception e) {
      return true;
    }
  }

  /**
   * Generates the action button's xsql files
   * 
   * @param fileReference
   *          The path where to create the files.
   */
  private void processActionButton(File fileReference) {
    try {
      log4j.info("Processing ActionButton_data.xml");
      final XmlDocument xmlDocumentData = xmlEngine
          .readXmlTemplate("org/openbravo/wad/ActionButton_data")
          .createXmlDocument();
      final ProcessRelationData ard[] = ProcessRelationData.select(pool);

      xmlDocumentData.setData("structure1", ard);
      WadUtility.writeFile(fileReference, "ActionButton_data.xsql",
          "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + xmlDocumentData.print());
    } catch (final ServletException e) {
      log4j.error("Problem of ServletExceptio in process of ActionButtonData", e);
    } catch (final IOException e) {
      log4j.error("Problem of IOExceptio in process of ActionButtonData", e);
    }
  }

  /**
   * Generates the action button's xml files
   * 
   * @param fileReference
   *          The path where to create the files.
   */
  private void processActionButtonXml(File fileReference) {
    try {
      log4j.info("Processing ActionButtonXml");
      final FieldsData fd[] = FieldsData.selectActionButton(pool);
      if (fd != null) {
        for (int i = 0; i < fd.length; i++) {
          final Vector<Object> vecFields = new Vector<Object>();
          WadActionButton.buildXml(pool, xmlEngine, fileReference, fd[i], vecFields,
              MAX_TEXTBOX_LENGTH);
        }
      }
    } catch (final ServletException | IOException e) {
      log4j.error("Problem in process of ActionButtonXml", e);
    }
  }

  /**
   * Generates the action button's html files
   * 
   * @param fileReference
   *          The path where to create the files.
   */
  private void processActionButtonHtml(File fileReference) {
    try {
      log4j.info("Processing ActionButtonHtml");
      final FieldsData fd[] = FieldsData.selectActionButton(pool);
      if (fd != null) {
        for (int i = 0; i < fd.length; i++) {
          final Vector<Object> vecFields = new Vector<Object>();

          // calculate fields that need combo reload
          final FieldsData[] dataReload = FieldsData.selectValidationProcess(pool, fd[i].reference);

          final Vector<Object> vecReloads = new Vector<Object>();
          if (dataReload != null && dataReload.length > 0) {
            for (int z = 0; z < dataReload.length; z++) {
              String code = dataReload[z].whereclause
                  + ((!dataReload[z].whereclause.equals("")
                      && !dataReload[z].referencevalue.equals("")) ? " AND " : "")
                  + dataReload[z].referencevalue;

              if (code.equals("") && dataReload[z].type.equals("R")) {
                code = "@AD_Org_ID@";
              }
              WadUtility.getComboReloadText(code, vecFields, null, vecReloads, "",
                  dataReload[z].columnname);
            }
          }

          // build the html template
          WadActionButton.buildHtml(pool, xmlEngine, fileReference, fd[i], vecFields,
              MAX_TEXTBOX_LENGTH, MAX_SIZE_EDITION_1_COLUMNS, "", false, jsDateFormat, vecReloads);
        }
      }
    } catch (final ServletException | IOException e) {
      log4j.error("Problem in process of ActionButtonHtml", e);
    }
  }

  /**
   * Generates the main file to manage the action buttons (ActionButton_Responser.java). These are
   * the menu's action buttons.
   * 
   * @param fileReference
   *          The path where to create the files.
   */
  private void processActionButtonGenerics(File fileReference) {
    try {
      // Generic action button for jasper and PL
      log4j.info("Processing ActionButton_Responser.xml");
      XmlDocument xmlDocumentData = xmlEngine
          .readXmlTemplate("org/openbravo/wad/ActionButton_Responser")
          .createXmlDocument();

      ActionButtonRelationData[] abrd = WadActionButton.buildActionButtonCallGenerics(pool);
      xmlDocumentData.setData("structure1", abrd);
      xmlDocumentData.setData("structure2", abrd);
      xmlDocumentData.setData("structure3", abrd);
      xmlDocumentData.setData("structure4", abrd);

      WadUtility.writeFile(fileReference, "ActionButton_Responser.java", xmlDocumentData.print());

      // Generic action button for java
      log4j.info("Processing ActionButton_ResponserJava.xml");
      xmlDocumentData = xmlEngine.readXmlTemplate("org/openbravo/wad/ActionButtonJava_Responser")
          .createXmlDocument();
      abrd = WadActionButton.buildActionButtonCallGenericsJava(pool);

      xmlDocumentData.setData("structure1", abrd);
      xmlDocumentData.setData("structure2", abrd);
      xmlDocumentData.setData("structure3", abrd);
      xmlDocumentData.setData("structure4", abrd);

      WadUtility.writeFile(fileReference, "ActionButtonJava_Responser.java",
          xmlDocumentData.print());

    } catch (final IOException e) {
      log4j.error("Problem of IOExceptio in process of ActionButton_Responser", e);
    }
  }

  /**
   * Generates the action button's xsql file for the action buttons called directly from menu. This
   * xsql file contains all the queries needed for SQL default values in generated parameters.
   * 
   * @param fileReference
   *          The path where to create the files.
   */
  private void processActionButtonSQLDefaultGenerics(File fileReference) {
    try {
      log4j.info("Processing ActionButtonDefault_data.xsql");

      ProcessRelationData defaults[] = ProcessRelationData.selectXSQLGenericsParams(pool);
      if (defaults != null && defaults.length > 0) {
        for (int i = 0; i < defaults.length; i++) {
          final Vector<Object> vecParametros = new Vector<Object>();
          defaults[i].reference = defaults[i].adProcessId + "_"
              + FormatUtilities.replace(defaults[i].columnname);
          defaults[i].defaultvalue = WadUtility.getSQLWadContext(defaults[i].defaultvalue,
              vecParametros);
          final StringBuffer parametros = new StringBuffer();
          for (final Enumeration<Object> e = vecParametros.elements(); e.hasMoreElements();) {
            final String paramsElement = WadUtility.getWhereParameter(e.nextElement(), true);
            parametros.append("\n" + paramsElement);
          }
          defaults[i].whereclause = parametros.toString();
        }
        XmlDocument xmlDocumentData = xmlEngine
            .readXmlTemplate("org/openbravo/wad/ActionButtonDefault_data")
            .createXmlDocument();
        xmlDocumentData.setData("structure16", defaults);

        WadUtility.writeFile(fileReference, "ActionButtonSQLDefault_data.xsql",
            xmlDocumentData.print());
      }
    } catch (final Exception e) {
      log4j.error("Error processing sql defaults", e);
    }
  }

  /**
   * Generates the action button's xml files. These are the menu's action buttons.
   * 
   * @param fileReference
   *          The path where to create the files.
   */
  private void processActionButtonXmlGenerics(File fileReference) {
    try {
      log4j.info("Processing ActionButtonXml Generics");
      final FieldsData fd[] = FieldsData.selectActionButtonGenerics(pool);
      if (fd != null) {
        for (int i = 0; i < fd.length; i++) {
          final Vector<Object> vecFields = new Vector<Object>();
          WadActionButton.buildXml(pool, xmlEngine, fileReference, fd[i], vecFields,
              MAX_TEXTBOX_LENGTH);
        }
      }
    } catch (final ServletException | IOException e) {
      log4j.error("Problem in process of ActionButtonXml Generics", e);
    }
  }

  /**
   * Generates the action button's html files. These are the menu's action button
   * 
   * @param fileReference
   *          The path where to create the files.
   */
  private void processActionButtonHtmlGenerics(File fileReference) {
    try {
      log4j.info("Processing ActionButtonHtml for generics");
      final FieldsData fd[] = FieldsData.selectActionButtonGenerics(pool);
      if (fd != null) {
        for (int i = 0; i < fd.length; i++) {
          final Vector<Object> vecFields = new Vector<Object>();

          // calculate fields that need combo reload
          final FieldsData[] dataReload = FieldsData.selectValidationProcess(pool, fd[i].reference);

          final Vector<Object> vecReloads = new Vector<Object>();
          if (dataReload != null && dataReload.length > 0) {
            for (int z = 0; z < dataReload.length; z++) {
              String code = dataReload[z].whereclause
                  + ((!dataReload[z].whereclause.equals("")
                      && !dataReload[z].referencevalue.equals("")) ? " AND " : "")
                  + dataReload[z].referencevalue;

              if (code.equals("") && dataReload[z].type.equals("R")) {
                code = "@AD_Org_ID@";
              }
              WadUtility.getComboReloadText(code, vecFields, null, vecReloads, "",
                  dataReload[z].columnname);
            }
          }

          // build the html template
          WadActionButton.buildHtml(pool, xmlEngine, fileReference, fd[i], vecFields,
              MAX_TEXTBOX_LENGTH, MAX_SIZE_EDITION_1_COLUMNS, "", true, jsDateFormat, vecReloads);
        }
      }
    } catch (final ServletException | IOException e) {
      log4j.error("Problem in process of ActionButtonHtml Generics", e);
    }
  }

  /**
   * Generates the web.xml file
   * 
   * @param fileWebXml
   *          path to generate the new web.xml file.
   * @param attachPath
   *          The path where are the attached files.
   * @param webPath
   *          The url where are the static web content.
   */
  private void processWebXml(File fileWebXml, String attachPath, String webPath)
      throws ServletException, IOException {
    try {
      log4j.info("Processing web.xml");
      final XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/wad/webConf")
          .createXmlDocument();

      String excludeWeldListener = excludeCDI ? WELD_LISTENER_ID : NONE;
      xmlDocument.setData("structureListener", WadData.selectListener(pool, excludeWeldListener));

      xmlDocument.setData("structureResource", WadData.selectResource(pool));
      final WadData[] filters = WadData.selectFilter(pool);
      WadData[][] filterParams = null;
      if (filters != null && filters.length > 0) {
        filterParams = new WadData[filters.length][];
        for (int i = 0; i < filters.length; i++) {
          filterParams[i] = WadData.selectParams(pool, "F", filters[i].classname, filters[i].id);
        }
      } else {
        filterParams = new WadData[0][0];
      }
      xmlDocument.setData("structureFilter", filters);
      xmlDocument.setDataArray("reportFilterParams", "structure1", filterParams);

      WadData[] contextParams = WadData.selectContextParams(pool);
      xmlDocument.setData("structureContextParams", contextParams);

      // obtaining again all the tabs, as previously calculated ones could be partial
      WadData[] allTabs = WadData.selectAllTabs(pool);
      Map<String, Boolean> tabsWithJava;
      if (!generateAllClassic250Windows) {
        tabsWithJava = calculateTabsToGenerate(pool, allTabs);
      } else {
        tabsWithJava = Collections.emptyMap();
      }

      xmlDocument.setData("structureServletTab", getTabServlets(allTabs, tabsWithJava));
      xmlDocument.setData("structureMappingTab", getTabMappings(allTabs, tabsWithJava));

      final WadData[] servlets = WadData.select(pool);
      WadData[][] servletParams = null;
      if (servlets != null && servlets.length > 0) {
        servletParams = new WadData[servlets.length][];
        for (int i = 0; i < servlets.length; i++) {
          if (servlets[i].loadonstartup != null && !servlets[i].loadonstartup.equals("")) {
            servlets[i].loadonstartup = "<load-on-startup>" + servlets[i].loadonstartup
                + "</load-on-startup>";
          }
          servletParams[i] = WadData.selectParams(pool, "S", servlets[i].classname, servlets[i].id);
        }
      } else {
        servletParams = new WadData[0][0];
      }

      WadData[] timeout = WadData.selectSessionTimeOut(pool);
      if (timeout.length == 0) {
        log4j.info("No session timeout found, setting default 60min");
      } else if (timeout.length > 1) {
        log4j.error("Multiple session timeout config found (" + timeout.length
            + "), setting default 60min");
      } else {
        xmlDocument.setParameter("fieldSessionTimeOut", timeout[0].value);
      }

      xmlDocument.setData("structure1", servlets);
      xmlDocument.setDataArray("reportServletParams", "structure1", servletParams);
      xmlDocument.setData("structureFilterMapping", WadData.selectFilterMapping(pool));
      xmlDocument.setData("structure2", WadData.selectMapping(pool));

      String baseDesignFolder = getBaseDesignFolder(contextParams);
      xmlDocument.setData("structureErrorExceptionPage", prepareErrorPageData(
          WadData.selectErrorPages(pool, EXCEPTION_TYPE_PAGES), baseDesignFolder));
      xmlDocument.setData("structureErrorCodePage",
          prepareErrorPageData(WadData.selectErrorPages(pool, ERROR_CODE_PAGES), baseDesignFolder));
      xmlDocument.setData("structureGenericErrorPage",
          prepareErrorPageData(WadData.selectGenericErrorPages(pool), baseDesignFolder));

      String webXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + xmlDocument.print();
      webXml = webXml.replace("${attachPath}", attachPath);
      webXml = webXml.replace("${webPath}", webPath);

      WadUtility.writeFile(fileWebXml, "web.xml", webXml);
    } catch (final IOException e) {
      log4j.error("Problem of IOException in process of Web.xml", e);
    }
  }

  private String getBaseDesignFolder(WadData[] contextParams) {
    String baseDesignPath = findParameterByName("BaseDesignPath", contextParams);
    String defaultDesignPath = findParameterByName("DefaultDesignPath", contextParams);

    return String.format("/%s/%s", baseDesignPath, defaultDesignPath);
  }

  private WadData[] prepareErrorPageData(WadData[] originalData, String baseDesignFolder) {
    List<WadData> appendedData = new ArrayList<>();
    for (WadData data : originalData) {
      if (!validateErrorCode(data.errortype, data.value)) {
        log4j.warn("Error page " + data.name + " has invalid error code: " + data.value);
        continue;
      }
      if (data.location != null && !data.location.isEmpty()) {
        data.location = String.format("%s/%s", baseDesignFolder, data.location);
        appendedData.add(data);
        log4j.debug("Processed error page " + data.name);
      } else {
        log4j.warn("Error page " + data.name + " has no location");
      }
    }

    return appendedData.toArray(new WadData[appendedData.size()]);
  }

  private boolean validateErrorCode(String errorType, String errorCode) {
    if (ERROR_CODE_PAGES.equals(errorType)) {
      try {
        Integer.parseInt(errorCode);
      } catch (NumberFormatException nfe) {
        return false;
      }
    }

    return true;
  }

  private String findParameterByName(String name, WadData[] contextParams) {
    for (WadData param : contextParams) {
      if (param.name.equals(name)) {
        return param.value;
      }
    }
    return "";
  }

  private WadData[] getTabServlets(WadData[] allTabs, Map<String, Boolean> generateTabMap) {
    ArrayList<WadData> servlets = new ArrayList<WadData>();
    for (WadData tab : allTabs) {
      boolean tabJavaNeeded = generateAllClassic250Windows
          || (generateTabMap.get(tab.tabid) == null) || generateTabMap.get(tab.tabid);

      if (!tabJavaNeeded) {
        continue;
      }

      String tabClassName = "org.openbravo.erpWindows."
          + ("0".equals(tab.windowmodule) ? "" : tab.windowpackage + ".") + tab.windowname + "."
          + tab.tabname + ("0".equals(tab.tabmodule) ? "" : tab.tabid);

      WadData servlet = new WadData();
      servlet.displayname = tabClassName;
      servlet.name = "W" + tab.tabid;
      servlet.classname = tabClassName;
      servlets.add(servlet);
    }
    return servlets.toArray(new WadData[servlets.size()]);
  }

  private FieldProvider[] getTabMappings(WadData[] allTabs, Map<String, Boolean> generateTabMap) {
    ArrayList<WadData> mappings = new ArrayList<WadData>();
    for (WadData tab : allTabs) {
      boolean tabJavaNeeded = generateAllClassic250Windows
          || ((generateTabMap.get(tab.tabid) != null) && (generateTabMap.get(tab.tabid)));

      if (!tabJavaNeeded) {
        continue;
      }

      String prefix = "/" + ("0".equals(tab.windowmodule) ? "" : tab.windowpackage) + tab.windowname
          + "/" + tab.tabname + ("0".equals(tab.tabmodule) ? "" : tab.tabid);

      // Keeping mapping to *_Edition.html because it is the mapping used for processes
      WadData mapping2 = new WadData();
      mapping2.name = "W" + tab.tabid;
      mapping2.classname = prefix + "_Edition.html";
      mappings.add(mapping2);
    }
    return mappings.toArray(new WadData[mappings.size()]);
  }

  /**
   * Generates all the windows defined in the dictionary. Also generates the translated files for
   * the defineds languages.
   * 
   * @param fileFin
   *          Path where are gonna be created the sources.
   * @param fileFinReloads
   *          Path where are gonna be created the reloads sources.
   * @param tabsData
   *          An object containing the tabs info.
   * @throws Exception
   */
  private void processTab(File fileFin, File fileFinReloads, TabsData tabsData) throws Exception {
    try {
      final String tabNamePresentation = tabsData.realtabname;
      // tabName contains tab's UUID for non core tabs
      final String tabName = FormatUtilities.replace(tabNamePresentation)
          + (tabsData.tabmodule.equals("0") ? "" : tabsData.tabid);
      final String windowName = FormatUtilities.replace(tabsData.windowname);
      final String tableName = FieldsData.tableName(pool, tabsData.tabid);
      final String isSOTrx = FieldsData.isSOTrx(pool, tabsData.tabid);
      final TabsData[] allTabs = TabsData.selectTabParent(pool, tabsData.key);

      /************************************************
       * The 2 tab lines generation
       *************************************************/
      if (allTabs == null || allTabs.length == 0) {
        throw new Exception("No tabs found for AD_Tab_ID: " + tabsData.tabid + " - key: "
            + tabsData.key + " - level: " + tabsData.tablevel);
      }

      final String javaPackage = (!tabsData.javapackage.equals("")
          ? tabsData.javapackage.replace(".", "/") + "/"
          : "") + windowName; // Take into account
                              // java packages for
                              // modules
      final File fileDir = new File(fileFin, javaPackage);

      String keyColumnName = "";
      final FieldsData[] dataKey = FieldsData.keyColumnName(pool, tabsData.tabid);
      if (dataKey != null && dataKey.length > 0) {
        keyColumnName = dataKey[0].name;
      }
      log4j.debug("KeyColumnName: " + keyColumnName);

      /************************************************
       * JAVA
       *************************************************/
      processTabJava(fileDir, tabsData.tabid, tabName, windowName, keyColumnName, isSOTrx,
          tabsData.key, tabsData.accesslevel, tabsData.tableId, tabsData.javapackage,
          tabsData.tabmodule);

      /************************************************
       * XSQL
       *************************************************/
      processTabXSQL(fileDir, tabsData.tabid, tabName, tableName, windowName, keyColumnName,
          tabsData.javapackage);

    } catch (final Exception e) {
      log4j.error("Problem in file: " + tabsData.tabid, e);
    }
  }

  /**
   * Generates the java files for a normal tab type.
   * 
   * @param allfields
   *          Array with the fields of the tab.
   * @param fileDir
   *          Path where to build the file.
   * @param strTab
   *          The id of the tab.
   * @param tabName
   *          The name of the tab.
   * @param windowName
   *          The name of the window.
   * @param keyColumnName
   *          The name of the key column.
   * @param strTables
   *          String with the from clause.
   * @param isSOTrx
   *          String that indicates if is a Sales Order tab or not (Y | N).
   * @param strWindow
   *          The id of the window.
   * @param accesslevel
   *          The access level.
   * @param tableId
   *          The id of the tab's table.
   * @param tabmodule
   */
  private void processTabJava(File fileDir, String strTab, String tabName, String windowName,
      String keyColumnName, String isSOTrx, String strWindow, String accesslevel, String tableId,
      String javaPackage, String tabmodule) throws ServletException, IOException {
    log4j.debug("Processing java: " + strTab + ", " + tabName);
    XmlDocument xmlDocument;
    final String createFromProcess = FieldsData.hasCreateFromButton(pool, strTab);
    final boolean hasCreateFrom = !createFromProcess.equals("0");
    final String postedProcess = FieldsData.hasPostedButton(pool, strTab);
    final boolean hasPosted = !postedProcess.equals("0");

    final boolean noPInstance = (ActionButtonRelationData.select(pool, strTab).length == 0);
    final boolean noActionButton = FieldsData.hasActionButton(pool, strTab).equals("0");

    // Obtain action buttons processes to be called from tab trough buttons
    final ActionButtonRelationData[] actBtns = WadActionButton.buildActionButtonCall(pool, strTab,
        tabName, keyColumnName, isSOTrx, strWindow);
    final ActionButtonRelationData[] actBtnsJava = WadActionButton.buildActionButtonCallJava(pool,
        strTab, tabName, keyColumnName, isSOTrx, strWindow);

    final String[] discard = { "", "", "", "", "" };

    if (!hasCreateFrom) {
      discard[0] = "sectionCreateFrom";
    }
    if (!hasPosted) {
      discard[1] = "sectionPosted";
    }
    if ((noPInstance) && (noActionButton)) {
      discard[2] = "hasAdPInstance";
    }
    if (noActionButton) {
      discard[3] = "hasAdActionButton";
    }
    if ((actBtns == null || actBtns.length == 0)
        && (actBtnsJava == null || actBtnsJava.length == 0)) {
      // No action buttons, service method is not neccessary
      discard[4] = "discardService";
    }

    xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/wad/javasource", discard)
        .createXmlDocument();

    fileDir.mkdirs();
    xmlDocument.setParameter("class", tabName);
    xmlDocument.setParameter("package",
        (!javaPackage.equals("") ? javaPackage + "." : "") + windowName);
    xmlDocument.setParameter("key", keyColumnName);

    xmlDocument.setParameter("keyData", Sqlc.TransformaNombreColumna(keyColumnName));
    xmlDocument.setParameter("windowId", strWindow);
    xmlDocument.setParameter("accessLevel", accesslevel);
    xmlDocument.setParameter("moduleId", tabmodule);
    xmlDocument.setParameter("tabId", strTab);
    xmlDocument.setParameter("tableId", tableId);
    xmlDocument.setParameter("createFromProcessId",
        ((Integer.valueOf(createFromProcess).intValue() > 0) ? createFromProcess : ""));
    xmlDocument.setParameter("postedProcessId",
        ((Integer.valueOf(postedProcess).intValue() > 0) ? postedProcess : ""));

    // process action buttons
    xmlDocument.setData("structure14", actBtns);
    xmlDocument.setData("structure15", actBtns);
    xmlDocument.setData("structure16", actBtns);
    xmlDocument.setData("structureActionBtnService", actBtns);

    // process standard UI java implemented buttons
    xmlDocument.setData("structure14java", actBtnsJava);
    xmlDocument.setData("structure15java", actBtnsJava);
    xmlDocument.setData("structure16java", actBtnsJava);
    xmlDocument.setData("structureActionBtnServiceJava", actBtnsJava);
    xmlDocument.setData("structureActionBtnServiceJavaSecuredProcess", actBtnsJava);

    xmlDocument.setData("structure38", FieldsData.explicitAccessProcess(pool, strTab));

    WadUtility.writeFile(fileDir, tabName + ".java", xmlDocument.print());
  }

  /**
   * Generates the xsql file for the tab
   * 
   * @param fileDir
   *          Path where the file is gonna be created.
   * @param strTab
   *          Id of the tab.
   * @param tableName
   *          Tab's table name.
   * @param windowName
   *          Window name.
   * @param keyColumnName
   *          Name of the key column.
   * @param strTables
   *          From clause for the tab.
   * @param selCol
   *          Array with the selection columns.
   * @throws ServletException
   * @throws IOException
   */
  private void processTabXSQL(File fileDir, String strTab, String tabName, String tableName,
      String windowName, String keyColumnName, String javaPackage)
      throws ServletException, IOException {
    log4j.debug("Procesig xsql: " + strTab + ", " + tabName);
    XmlDocument xmlDocumentXsql;
    xmlDocumentXsql = xmlEngine.readXmlTemplate("org/openbravo/wad/datasource").createXmlDocument();

    xmlDocumentXsql.ignoreTranslation = true;
    xmlDocumentXsql.setParameter("class", tabName + "Data");
    xmlDocumentXsql.setParameter("package", "org.openbravo.erpWindows."
        + (!javaPackage.equals("") ? javaPackage + "." : "") + windowName);
    xmlDocumentXsql.setParameter("table", tableName);
    xmlDocumentXsql.setParameter("key", tableName + "." + keyColumnName);

    xmlDocumentXsql.setParameter("paramKey", Sqlc.TransformaNombreColumna(keyColumnName));

    boolean hasCode = false;
    {
      // default values for search references in parameter windows for action buttons
      // keep it hardcoded by now
      final ProcessRelationData[] data = ProcessRelationData.selectXSQL(pool, strTab);
      if (data != null) {
        for (int i = 0; i < data.length; i++) {
          hasCode = true;
          String tableN = "";
          if (data[i].adReferenceId.equals("28")) {
            tableN = "C_ValidCombination";
          } else if (data[i].adReferenceId.equals("31")) {
            tableN = "M_Locator";
          } else {
            tableN = data[i].name.substring(0, data[i].searchname.length() - 3);
          }
          String strName = "";
          if (data[i].adReferenceId.equals("28")) {
            strName = "C_ValidCombination_ID";
          } else if (data[i].adReferenceId.equals("31")) {
            strName = "M_Locator_ID";
          } else {
            strName = data[i].searchname;
          }
          final String strColumnName = FieldsData.columnIdentifier(pool, tableN);
          final StringBuffer fields = new StringBuffer();
          fields.append("SELECT " + strColumnName);
          fields.append(" FROM " + tableN);
          fields.append(" WHERE isActive='Y'");
          fields.append(" AND " + strName + " = ? ");
          data[i].whereclause = fields.toString();
          data[i].name = FormatUtilities.replace(data[i].name);
        }
      }
      xmlDocumentXsql.setData("structure12", data);
    }
    // SQLs of the defaultvalue of the parameter of the tab-associated
    // processes
    {
      final ProcessRelationData fieldsAux[] = ProcessRelationData.selectXSQLParams(pool, strTab);
      if (fieldsAux != null && fieldsAux.length > 0) {
        for (int i = 0; i < fieldsAux.length; i++) {
          hasCode = true;
          final Vector<Object> vecParametros = new Vector<Object>();
          fieldsAux[i].reference = fieldsAux[i].adProcessId + "_"
              + FormatUtilities.replace(fieldsAux[i].columnname);
          fieldsAux[i].defaultvalue = WadUtility.getSQLWadContext(fieldsAux[i].defaultvalue,
              vecParametros);
          final StringBuffer parametros = new StringBuffer();
          for (final Enumeration<Object> e = vecParametros.elements(); e.hasMoreElements();) {
            final String paramsElement = WadUtility.getWhereParameter(e.nextElement(), true);
            parametros.append("\n" + paramsElement);
          }
          fieldsAux[i].whereclause = parametros.toString();
        }
      }
      xmlDocumentXsql.setData("structure16", fieldsAux);
    }

    {
      final ActionButtonRelationData[] abrd = WadActionButton.buildActionButtonSQL(pool, strTab);
      hasCode = hasCode || abrd.length > 0;
      xmlDocumentXsql.setData("structure11", abrd);
    }

    if (hasCode) {
      WadUtility.writeFile(fileDir, tabName + "_data.xsql",
          "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + xmlDocumentXsql.print());
    }
  }

  /**
   * Generates combo reloads for all action buttons
   * 
   * @param fileDir
   *          Directory to save the generated java
   * @throws ServletException
   * @throws IOException
   */
  private void processProcessComboReloads(File fileDir) throws ServletException, IOException {
    log4j.info("Processig combo reloads for action buttons ");
    Vector<FieldsData> generatedProcesses = new Vector<FieldsData>();
    Vector<FieldsData[]> processCode = new Vector<FieldsData[]>();
    FieldsData[] processes = FieldsData.selectProcessesWithReloads(pool);

    for (FieldsData process : processes) {

      String processId = process.id;

      final FieldsData[] data = FieldsData.selectValidationProcess(pool, processId);
      if (data == null || data.length == 0) {
        return;
      }

      final boolean hasOrg = FieldsData.processHasOrgParam(pool, processId);

      final Vector<Object> vecReloads = new Vector<Object>();
      final Vector<Object> vecTotal = new Vector<Object>();

      FieldsData[] result = null;

      for (FieldsData param : data) {
        final String code = param.whereclause
            + ((!param.whereclause.equals("") && !param.referencevalue.equals("")) ? " AND " : "")
            + param.referencevalue;
        param.columnname = "inp" + Sqlc.TransformaNombreColumna(param.columnname);
        param.whereclause = WadUtility.getComboReloadText(code, null, null, vecReloads, "inp");
        if (param.whereclause.equals("") && param.type.equals("R")) {
          // Add combo reloads for all combo references in case there is a ad_org parameter, if not
          // only for the params with validation rule
          if (!hasOrg) {
            continue;
          }
          param.whereclause = "\"inpadOrgId\"";
        }
        if (param.reference.equals("17") && param.whereclause.equals("")) {
          param.whereclause = "\"inp" + param.columnname + "\"";
        }
        if (!param.whereclause.equals("") && (param.reference.equals("17")
            || param.reference.equals("18") || param.reference.equals("19"))) {

          param.orgcode = "Utility.getReferenceableOrg(vars, vars.getStringParameter(\"inpadOrgId\"))";

          if (param.reference.equals("18")) { // Table
            final FieldsData[] tables = FieldsData.selectColumnTableProcess(pool, param.id);
            if (tables == null || tables.length == 0) {
              throw new ServletException(
                  "Not found Table reference for parameter with id: " + param.id);
            }
          } else if (param.reference.equals("19")) { // TableDir
            final FieldsData[] tableDir = FieldsData.selectColumnTableDirProcess(pool, param.id);
            if (tableDir == null || tableDir.length == 0) {
              throw new ServletException(
                  "Not found TableDir reference for parameter with id " + param.id);
            }
          }
          vecTotal.addElement(param);
        }
      }
      if (vecTotal != null && vecTotal.size() > 0) {
        result = new FieldsData[vecTotal.size()];
        vecTotal.copyInto(result);
        processCode.add(result);
        generatedProcesses.add(process);
      }

    }
    if (generatedProcesses.size() > 0) {
      // create the helper class, it is a servlet that manages all combo reloads
      XmlDocument xmlDocumentHelper = xmlEngine
          .readXmlTemplate("org/openbravo/wad/ComboReloadsProcessHelper")
          .createXmlDocument();
      FieldsData[] processesGenerated = new FieldsData[generatedProcesses.size()];
      generatedProcesses.copyInto(processesGenerated);
      FieldsData[][] processData = new FieldsData[generatedProcesses.size()][];
      for (int i = 0; i < generatedProcesses.size(); i++) {
        processData[i] = processCode.get(i);
      }

      xmlDocumentHelper.setData("structure1", processesGenerated);
      xmlDocumentHelper.setData("structure2", processesGenerated);
      xmlDocumentHelper.setDataArray("reportComboReloadsProcess", "structure1", processData);
      WadUtility.writeFile(fileDir, "ComboReloadsProcessHelper.java", xmlDocumentHelper.print());
      log4j.debug("created :" + fileDir + "/ComboReloadsProcessHelper.java");
    }
  }

  /**
   * Method to prepare the XmlEngine object, which is the one in charged of the templates.
   * 
   * @param fileConnection
   *          The path to the connection file.
   */
  private void createXmlEngine(String fileConnection) {
    // pass null as connection to running the translation at compile time
    xmlEngine = new XmlEngine(null);
    xmlEngine.isResource = true;
    xmlEngine.fileBaseLocation = new File(".");
    xmlEngine.strReplaceWhat = null;
    xmlEngine.strReplaceWith = null;
    XmlEngine.strTextDividedByZero = "TextDividedByZero";
    xmlEngine.fileXmlEngineFormat = new File(fileConnection, "Format.xml");
    log4j.debug("xmlEngine format file: " + xmlEngine.fileXmlEngineFormat.getAbsoluteFile());
    xmlEngine.initialize();
  }

  /**
   * Creates an instance of the connection's pool.
   * 
   * @param strFileConnection
   *          Path where is allocated the connection file.
   */
  private void createPool(String strFileConnection) {
    pool = new WadConnection(strFileConnection);
    WADControl.setConnection(pool);
  }

  /**
   * Method to read the Openbravo.properties file.
   * 
   * @param strFileProperties
   *          The path of the property file to read.
   */
  private void readProperties(String strFileProperties) {
    // Read properties file.
    final Properties properties = new Properties();
    try {
      log4j.info("strFileProperties: " + strFileProperties);
      properties.load(new FileInputStream(strFileProperties));
      jsDateFormat = properties.getProperty("dateFormat.js");
      log4j.info("jsDateFormat: " + jsDateFormat);
      sqlDateFormat = properties.getProperty("dateFormat.sql");
      WADControl.setDateFormat(sqlDateFormat);
      log4j.info("sqlDateFormat: " + sqlDateFormat);
    } catch (final IOException e) {
      log4j.error("Couldn't read properties", e);
    }
  }
}
