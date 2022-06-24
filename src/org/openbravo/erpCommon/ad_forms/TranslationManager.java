/******************************************************************************
 * The contents of this file are subject to the   Compiere License  Version 1.1
 * ("License"); You may not use this file except in compliance with the License
 * You may obtain a copy of the License at http://www.compiere.org/license.html
 * Software distributed under the License is distributed on an  "AS IS"  basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 * The Original Code is                  Compiere  ERP & CRM  Business Solution
 * The Initial Developer of the Original Code is Jorg Janke  and ComPiere, Inc.
 * Portions created by Jorg Janke are Copyright (C) 1999-2001 Jorg Janke, parts
 * created by ComPiere are Copyright (C) ComPiere, Inc.;   All Rights Reserved.
 * Contributor(s): Openbravo SLU
 * Contributions are Copyright (C) 2001-2020 Openbravo S.L.U.
 ******************************************************************************/
package org.openbravo.erpCommon.ad_forms;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.xml.XMLUtil;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.BasicUtility;
import org.openbravo.erpCommon.utility.OBError;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Class for import/export languages.
 * 
 * The tree of languages is:
 * 
 * {attachmentsDir} {laguageFolder} {moduleFolder}
 * 
 * Example: /opt/attachments/ en_US/ &lt;trl tables from core&gt; module1/ &lt;trl tables from
 * module1&gt;
 * 
 */
public class TranslationManager {

  /** XML Element Tag */
  static final String XML_TAG = "compiereTrl";
  /** XML Attribute Table */
  static final String XML_ATTRIBUTE_TABLE = "table";
  /** XML Attribute Language */
  static final String XML_ATTRIBUTE_LANGUAGE = "language";
  /** XML row attribute original language */
  static final String XML_ATTRIBUTE_BASE_LANGUAGE = "baseLanguage";
  /** XML Attribute Version */
  static final String XML_ATTRIBUTE_VERSION = "version";
  /** XML Row Tag */

  static final String XML_ROW_TAG = "row";
  /** XML Row Attribute ID */
  static final String XML_ROW_ATTRIBUTE_ID = "id";
  /** XML Row Attribute Translated */
  static final String XML_ROW_ATTRIBUTE_TRANSLATED = "trl";

  /** XML Value Tag */
  static final String XML_VALUE_TAG = "value";
  /** XML Value Column */
  static final String XML_VALUE_ATTRIBUTE_COLUMN = "column";
  /** XML Value Original */
  static final String XML_VALUE_ATTRIBUTE_ORIGINAL = "original";
  /** XML Value Original */
  static final String XML_VALUE_ATTRIBUTE_ISTRL = "isTrl";

  public static final String CONTRIBUTORS_FILENAME = "CONTRIBUTORS";
  static final String XML_CONTRIB = "Contributors";

  private static final Logger log4j = LogManager.getLogger();

  /**
   * Export all the trl tables that refers to tables with ad_module_id column or trl tables that
   * refers to tables with a parent table with ad_module_id column
   * 
   * For example: If a record from ad_process is in module "core", the records from ad_process_trl
   * and ad_process_para_trl are exported in "core" module
   * 
   * @param exportDirectory
   *          Directory for trl's xml files
   * @param strLang
   *          Language to export.
   * @param strClient
   *          Client to export.
   * @param uiLanguage
   *          Language to be used for translating error messages
   * @return Message with the error or with the success
   */
  public static OBError exportTrl(ConnectionProvider conn, String exportDirectory, String strLang,
      String strClient, String uiLanguage) {
    return exportTrl(conn, exportDirectory, strLang, strClient, uiLanguage, false);
  }

  /**
   * Export all the trl tables that refers to tables with ad_module_id column or trl tables that
   * refers to tables with a parent table with ad_module_id column
   * 
   * For example: If a record from ad_process is in module "core", the records from ad_process_trl
   * and ad_process_para_trl are exported in "core" module
   * 
   * @param exportDirectory
   *          Directory for trl's xml files
   * @param strLang
   *          Language to export.
   * @param strClient
   *          Client to export.
   * @param uiLanguage
   *          Language to be used for translating error messages
   * @param isReducedVersion
   *          If true then the export will only take into account elements that are related with a
   *          Menu entry that is checked to be translated. If false, everything will be exported to
   *          the translation
   * @return Message with the error or with the success
   */
  public static OBError exportTrl(ConnectionProvider conn, String exportDirectory, String strLang,
      String strClient, String uiLanguage, boolean isReducedVersion) {
    final String AD_Language = strLang;
    OBError myMessage = null;

    myMessage = new OBError();
    myMessage.setTitle("");
    final String AD_Client_ID = strClient;

    final String strFTPDirectory = exportDirectory;

    if (new File(strFTPDirectory).canWrite()) {
      if (log4j.isDebugEnabled()) {
        log4j.debug("can write...");
      }
    } else {
      log4j.error("Can't write on directory: " + strFTPDirectory);
      myMessage.setType("Error");
      myMessage.setMessage(
          BasicUtility.messageBD(conn, "CannotWriteDirectory", uiLanguage) + " " + strFTPDirectory);
      return myMessage;
    }

    (new File(strFTPDirectory + "/lang")).mkdir();
    final String rootDirectory = strFTPDirectory + "/lang/";
    final String directory = strFTPDirectory + "/lang/" + AD_Language + "/";
    (new File(directory)).mkdir();

    if (log4j.isDebugEnabled()) {
      log4j.debug("directory " + directory);
    }

    try {
      final TranslationData[] modulesTables = TranslationData.trlModulesTables(conn);

      for (int i = 0; i < modulesTables.length; i++) {
        exportModulesTrl(conn, rootDirectory, AD_Client_ID, AD_Language, modulesTables[i].c,
            isReducedVersion);
      }
      // We need to also export translations for some tables which are considered reference data
      // and are imported using datasets (such as Masterdata: UOMs, Currencies, ...)
      exportReferenceData(conn, rootDirectory, AD_Language, isReducedVersion);

      exportContibutors(conn, directory, AD_Language);
    } catch (final Exception e) {
      log4j.error(e);
      myMessage.setType("Error");
      myMessage.setMessage(BasicUtility.messageBD(conn, "Error", uiLanguage));
      return myMessage;
    }
    myMessage.setType("Success");
    myMessage.setMessage(BasicUtility.messageBD(conn, "Success", uiLanguage));
    return myMessage;
  }

  /**
   * 
   * The import process insert in database all the translations found in the folder of the defined
   * language RECURSIVELY. It don't take into account if a module is marked o no as isInDevelopment.
   * Only search for trl's xml files corresponding with trl's tables in database.
   * 
   * 
   * @param directory
   *          Directory for trl's xml files
   * @param strLang
   *          Language to import
   * @param strClient
   *          Client to import
   * @param uiLanguage
   *          Language to be used for translating error messages
   * @return Message with the error or with the success
   */
  public static OBError importTrlDirectory(ConnectionProvider cp, String directory, String strLang,
      String strClient, String uiLanguage) {
    final String AD_Language = strLang;

    OBError myMessage = null;
    myMessage = new OBError();
    myMessage.setTitle("");

    final String UILanguage = uiLanguage == null ? "en_US" : uiLanguage;

    if ((new File(directory).exists()) && (new File(directory).canRead())) {
      log4j.debug("can read " + directory);
    } else {
      log4j.error("Can't read on directory: " + directory);
      myMessage.setType("Error");
      myMessage.setMessage(
          BasicUtility.messageBD(cp, "CannotReadDirectory", UILanguage) + " " + directory);
      return myMessage;
    }

    final int AD_Client_ID = Integer.valueOf(strClient);
    try {
      final TranslationData[] tables = TranslationData.trlTables(cp);
      for (int i = 0; i < tables.length; i++) {
        importTrlFile(cp, directory, AD_Client_ID, AD_Language, tables[i].c);
      }
      importContributors(cp, directory, AD_Language);
    } catch (OBException e) {
      myMessage.setType("Error");
      String message = String.format(BasicUtility.messageBD(cp, "ERROR_PARSE_FILE", UILanguage),
          e.getMessage());
      myMessage.setMessage(BasicUtility.messageBD(cp, message, UILanguage));
      return myMessage;
    } catch (Exception e) {
      log4j.error(e.toString());
      myMessage.setType("Error");
      myMessage.setMessage(BasicUtility.messageBD(cp, "Error", UILanguage));
      return myMessage;
    }

    final File file = new File(directory);
    final File[] list = file.listFiles();
    for (int f = 0; f < list.length; f++) {
      if (list[f].isDirectory()) {
        final OBError subDirError = importTrlDirectory(cp, list[f].toString() + "/", strLang,
            strClient, UILanguage);
        if (!"Success".equals(subDirError.getType())) {
          return subDirError;
        }
      }
    }

    myMessage.setType("Success");
    myMessage.setMessage(BasicUtility.messageBD(cp, "Success", UILanguage));
    return myMessage;
  }

  private static void exportContibutors(ConnectionProvider conn, String directory,
      String AD_Language) {
    final File out = new File(directory, CONTRIBUTORS_FILENAME + "_" + AD_Language + ".xml");
    try {
      final Document document = newDocument();
      final Element root = document.createElement(XML_CONTRIB);
      root.setAttribute(XML_ATTRIBUTE_LANGUAGE, AD_Language);
      document.appendChild(root);
      root.appendChild(
          document.createTextNode(TranslationData.selectContributors(conn, AD_Language)));
      final DOMSource source = new DOMSource(document);

      // Output
      out.createNewFile();
      final StreamResult result = new StreamResult(out);
      // Transform
      newTransformer().transform(source, result);
    } catch (final Exception e) {
      log4j.error("exportTrl", e);
    }
  }

  private static Document newDocument() throws ParserConfigurationException {
    return XMLUtil.getInstance().newDocumentBuilder().newDocument();
  }

  private static Transformer newTransformer() throws TransformerConfigurationException {
    TransformerFactory tFactory = XMLUtil.getInstance().newTransformerFactory();
    tFactory.setAttribute("indent-number", 2);

    Transformer transformer = tFactory.newTransformer();
    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
    transformer.setOutputProperty(OutputKeys.INDENT, "yes");

    return transformer;
  }

  private static void exportReferenceData(ConnectionProvider conn, String rootDirectory,
      String AD_Language, final boolean isReducedVersion) {
    try {
      // Export translations for reference data (do not take into account
      // client data, only system)
      final TranslationData[] referenceTrlData = TranslationData.referenceDataTrl(conn);
      for (final TranslationData refTrl : referenceTrlData) {
        exportTable(conn, AD_Language, true, refTrl.isindevelopment.equals("Y"),
            refTrl.tablename.toUpperCase(), refTrl.adTableId, rootDirectory, refTrl.adModuleId,
            refTrl.adLanguage, refTrl.value, true, isReducedVersion);
      }
    } catch (final Exception e) {
      e.printStackTrace();
    }
  }

  private static String exportModulesTrl(ConnectionProvider conn, String rootDirectory,
      String AD_Client_ID, String AD_Language, String Trl_Table, final boolean isReducedVersion) {
    try {
      final TranslationData[] modules = TranslationData.modules(conn);
      for (int mod = 0; mod < modules.length; mod++) {
        final String moduleLanguage = TranslationData.selectModuleLang(conn,
            modules[mod].adModuleId);
        if (moduleLanguage != null && !moduleLanguage.equals("")) {
          // only
          // languages
          // different
          // than the
          // modules's
          // one

          final String tableName = Trl_Table;
          final int pos = tableName.indexOf("_TRL");
          final String Base_Table = Trl_Table.substring(0, pos);
          boolean trl = true;

          if (moduleLanguage.equals(AD_Language)) {
            trl = false;
          }
          exportTable(conn, AD_Language, false, false, Base_Table, "0", rootDirectory,
              modules[mod].adModuleId, moduleLanguage, modules[mod].value, trl, isReducedVersion);

        } // translate or not (if)
      }
    } catch (final Exception e) {
      log4j.error("exportTrl", e);
    }
    return "";
  } // exportModulesTrl

  /**
   * Exports a single trl table in a xml file
   * 
   * @param AD_Language
   *          Language to export
   * @param exportReferenceData
   *          Defines whether exporting reference data
   * @param exportAll
   *          In case it is reference data if it should be exported all data or just imported
   * @param table
   *          Base table
   * @param tableID
   *          Base table id
   * @param rootDirectory
   *          Root directory to the the exportation
   * @param moduleId
   *          Id for the module to export to
   * @param moduleLanguage
   *          Base language for the module
   * @param javaPackage
   *          Java package for the module
   * @param isReducedVersion
   *          If true then the export will only take into account elements that are related with a
   *          Menu entry that is checked to be translated. If false, everything will be exported to
   *          the translation
   */
  private static void exportTable(ConnectionProvider cp, String AD_Language,
      boolean exportReferenceData, boolean exportAll, String table, String tableID,
      String rootDirectory, String moduleId, String moduleLanguage, String javaPackage, boolean trl,
      final boolean isReducedVersion) {

    PreparedStatement st = null;
    String sql = null;
    List<String> parameters = new ArrayList<>();
    try {
      String trlTable = table;
      if (trl && !table.endsWith("_TRL")) {
        trlTable = table + "_TRL";
      }
      final TranslationData[] trlColumns = getTrlColumns(cp, table, isReducedVersion);
      final String keyColumn = table + "_ID";

      boolean m_IsCentrallyMaintained = false;
      try {
        m_IsCentrallyMaintained = !(TranslationData.centrallyMaintained(cp, table).equals("0"));
        if (m_IsCentrallyMaintained) {
          log4j.debug("table:" + table + " IS centrally maintained");
        } else {
          log4j.debug("table:" + table + " is NOT centrally maintained");
        }
      } catch (final Exception e) {
        log4j.error("getTrlColumns (IsCentrallyMaintained)", e);
      }

      // Prepare query to retrieve translated rows
      sql = "select ";
      if (trl) {
        sql += "t.IsTranslated,";
      } else {
        sql += "'N', ";
      }
      sql += "t." + keyColumn;

      for (int i = 0; i < trlColumns.length; i++) {
        sql += ", t." + trlColumns[i].c + ", o." + trlColumns[i].c + " AS " + trlColumns[i].c + "O";
      }

      sql += " from " + trlTable + " t, " + table + " o";

      if (exportReferenceData && !exportAll) {
        sql += ", AD_REF_DATA_LOADED DL";
      }

      sql += " where ";
      if (trl) {
        sql += "t.AD_Language=? AND ";
        parameters.add(AD_Language);
      }
      sql += "o." + keyColumn + "= t." + keyColumn;

      if (m_IsCentrallyMaintained) {
        sql += " and o.IsCentrallyMaintained='N'";
      }

      // AdClient !=0 not supported
      sql += " and o.AD_Client_ID='0' ";

      if (!exportReferenceData) {
        String tempTrlTableName = trlTable;
        if (!tempTrlTableName.toLowerCase().endsWith("_trl")) {
          tempTrlTableName = tempTrlTableName + "_Trl";
        }
        final TranslationData[] parentTable = TranslationData.parentTable(cp, tempTrlTableName);

        if (parentTable.length == 0) {
          sql += " and o.ad_module_id=?";
          parameters.add(moduleId);
        } else {
          /** Search for ad_module_id in the parent table */
          if (StringUtils.isEmpty(parentTable[0].grandparent)) {
            String strParentTable = parentTable[0].tablename;
            //@formatter:off
            sql += "  and exists ( " +
                   "   select 1 " +
                   "     from " + strParentTable + " p " + 
                   "    where p." + strParentTable + "_ID = o." + strParentTable + "_ID " +
                   "      and p.ad_module_id=?)";
            //@formatter:on
            parameters.add(moduleId);
          } else {
            String strParentTable = parentTable[0].tablename;
            String strGandParentTable = parentTable[0].grandparent;

            //@formatter:off
            sql += "  and exists (" +
                   "   select 1 " +
                   "     from " + strGandParentTable + " gp, " + strParentTable + " p " +
                   "    where p." + strParentTable + "_ID = o." + strParentTable + "_ID " +
                   "      and p." + strGandParentTable + "_ID = gp." + strGandParentTable + "_ID " +
                   "      and gp.ad_module_id = ?)";
            //@formatter:on
            parameters.add(moduleId);
          }
        }
      }
      if (exportReferenceData && !exportAll) {
        //@formatter:off
        sql += 
               " and DL.GENERIC_ID = o." + keyColumn + 
               " and DL.AD_TABLE_ID = ?" +
               " and DL.AD_MODULE_ID = ?";
        //@formatter:on
        parameters.add(tableID);
        parameters.add(moduleId);
      }

      if (isReducedVersion) {
        sql += ReducedTranslationHelper.getReducedTranslationClause(table);
      }

      sql += " order by t." + keyColumn;
      //

      if (log4j.isDebugEnabled()) {
        log4j.debug("SQL:" + sql);
      }
      st = cp.getPreparedStatement(sql);
      if (log4j.isDebugEnabled()) {
        log4j.debug("st");
      }
      int paramCounter = 1;
      for (String parameter : parameters) {
        st.setString(paramCounter, parameter);
        paramCounter++;
      }

      final ResultSet rs = st.executeQuery();
      if (log4j.isDebugEnabled()) {
        log4j.debug("rs");
      }
      int rows = 0;
      boolean hasRows = false;

      Document document = null;
      Element root = null;
      File out = null;

      // Create xml file

      String directory = "";

      document = newDocument();
      // Root
      root = document.createElement(XML_TAG);
      root.setAttribute(XML_ATTRIBUTE_LANGUAGE, AD_Language);
      root.setAttribute(XML_ATTRIBUTE_TABLE, table);
      root.setAttribute(XML_ATTRIBUTE_BASE_LANGUAGE, moduleLanguage);
      root.setAttribute(XML_ATTRIBUTE_VERSION, TranslationData.version(cp));
      document.appendChild(root);

      if (moduleId.equals("0")) {
        directory = rootDirectory + AD_Language + "/";
      } else {
        directory = rootDirectory + AD_Language + "/" + javaPackage + "/";
      }
      if (!new File(directory).exists()) {
        (new File(directory)).mkdir();
      }

      String fileName = directory + trlTable + "_" + AD_Language + ".xml";
      log4j.info("exportTrl - " + fileName);
      out = new File(fileName);

      while (rs.next()) {
        if (!hasRows && !exportReferenceData) { // Create file only in
          // case it has contents
          // or it is not rd
          hasRows = true;

          document = newDocument();
          // Root
          root = document.createElement(XML_TAG);
          root.setAttribute(XML_ATTRIBUTE_LANGUAGE, AD_Language);
          root.setAttribute(XML_ATTRIBUTE_TABLE, table);
          root.setAttribute(XML_ATTRIBUTE_BASE_LANGUAGE, moduleLanguage);
          root.setAttribute(XML_ATTRIBUTE_VERSION, TranslationData.version(cp));
          document.appendChild(root);

          if (moduleId.equals("0")) {
            directory = rootDirectory + AD_Language + "/";
          } else {
            directory = rootDirectory + AD_Language + "/" + javaPackage + "/";
          }
          if (!new File(directory).exists()) {
            (new File(directory)).mkdir();
          }

          fileName = directory + trlTable + "_" + AD_Language + ".xml";
          log4j.info("exportTrl - " + fileName);
          out = new File(fileName);
        }

        final Element row = document.createElement(XML_ROW_TAG);
        row.setAttribute(XML_ROW_ATTRIBUTE_ID, String.valueOf(rs.getString(2))); // KeyColumn
        row.setAttribute(XML_ROW_ATTRIBUTE_TRANSLATED, rs.getString(1)); // IsTranslated
        for (int i = 0; i < trlColumns.length; i++) {
          final Element value = document.createElement(XML_VALUE_TAG);
          value.setAttribute(XML_VALUE_ATTRIBUTE_COLUMN, trlColumns[i].c);
          String origString = rs.getString(trlColumns[i].c + "O"); // Original
          String isTrlString = "Y";
          // Value
          if (origString == null) {
            origString = "";
            isTrlString = "N";
          }
          String valueString = rs.getString(trlColumns[i].c); // Value
          if (valueString == null) {
            valueString = "";
            isTrlString = "N";
          }
          if (origString.equals(valueString)) {
            isTrlString = "N";
          }
          value.setAttribute(XML_VALUE_ATTRIBUTE_ISTRL, isTrlString);
          value.setAttribute(XML_VALUE_ATTRIBUTE_ORIGINAL, origString);
          value.appendChild(document.createTextNode(valueString));
          row.appendChild(value);
        }
        root.appendChild(row);
        rows++;
      }
      rs.close();

      log4j.info("exportTrl - Records=" + rows + ", DTD=" + document.getDoctype());

      final DOMSource source = new DOMSource(document);
      // Output
      out.createNewFile();
      // Transform
      final OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(out), "UTF-8");
      newTransformer().transform(source, new StreamResult(osw));
      osw.close();
    } catch (final Exception e) {
      log4j.error("Error exporting translation for table " + table + "\n" + sql, e);
    } finally {
      try {
        if (st != null) {
          cp.releasePreparedStatement(st);
        }
      } catch (final Exception ignored) {
      }
    }

  }

  private static String importContributors(ConnectionProvider cp, String directory,
      String AD_Language) {
    final String fileName = directory + File.separator + CONTRIBUTORS_FILENAME + "_" + AD_Language
        + ".xml";
    final File in = new File(fileName);
    if (!in.exists()) {
      final String msg = "File does not exist: " + fileName;
      log4j.debug(msg);
      return msg;
    }
    try {
      final TranslationHandler handler = new TranslationHandler(cp);
      final SAXParserFactory factory = SAXParserFactory.newInstance();
      final SAXParser parser = factory.newSAXParser();
      parser.parse(in, handler);
      return "";
    } catch (final Exception e) {
      log4j.error("importContrib", e);
      return e.toString();
    }
  }

  private static String importTrlFile(ConnectionProvider conn, String directory, int AD_Client_ID,
      String AD_Language, String Trl_Table) {
    final String fileName = directory + File.separator + Trl_Table + "_" + AD_Language + ".xml";
    log4j.debug("importTrl - " + fileName);
    final File in = new File(fileName);
    if (!in.exists()) {
      final String msg = "File does not exist: " + fileName;
      log4j.debug("importTrl - " + msg);
      return msg;
    }

    Connection con = null;
    try {
      con = conn.getTransactionConnection();
      final TranslationHandler handler = new TranslationHandler(AD_Client_ID, conn, con);
      final SAXParserFactory factory = SAXParserFactory.newInstance();
      // factory.setValidating(true);
      final SAXParser parser = factory.newSAXParser();
      parser.parse(in, handler);
      conn.releaseCommitConnection(con);
      log4j
          .info("importTrl - Updated=" + handler.getUpdateCount() + " - from file " + in.getName());
      return "";
    } catch (final Exception e) {
      log4j.error("importTrlFile - error parsing file: " + fileName, e);
      try {
        conn.releaseRollbackConnection(con);
      } catch (SQLException e1) {
        log4j.error("Error on releaseRollbackConnection", e1);
      }
      throw new OBException(fileName);
    }
  }

  private static TranslationData[] getTrlColumns(ConnectionProvider cp, String Base_Table,
      boolean isReducedVersion) {

    TranslationData[] list = null;

    try {
      list = TranslationData.trlColumns(cp, Base_Table + "_TRL",
          isReducedVersion ? "isReducedVersion" : "");
    } catch (final Exception e) {
      log4j.error("getTrlColumns", e);
    }
    return list;
  }

}
