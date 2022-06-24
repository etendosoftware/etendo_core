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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.database.ConnectionProvider;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * SAX Handler for parsing Translation
 * 
 * @author Jorg Janke
 * @version $Id: TranslationHandler.java,v 1.5 2003/10/04 03:52:36 jjanke Exp $
 */
class TranslationHandler extends DefaultHandler {
  /**
   * Translation Handler
   */

  public TranslationHandler(ConnectionProvider cDB) {
    m_AD_Client_ID = 0;
    DB = cDB;
    parameters = new ArrayList<>();
  }

  public TranslationHandler(int AD_Client_ID, ConnectionProvider cDB, Connection con) {

    m_AD_Client_ID = AD_Client_ID;
    DB = cDB;
    this.con = con;
    parameters = new ArrayList<>();

  } // TranslationHandler

  private ConnectionProvider DB;
  private Connection con;

  /** Client */
  private int m_AD_Client_ID = -1;
  /** Language */
  private String m_AD_Language = null;

  /** Table */
  private String m_TableName = null;
  /** Update SQL */
  private String m_updateSQL = null;
  /** Current ID */
  private String m_curID = null;
  /** Current ColumnName */
  private String m_curColumnName = null;
  /** Current Value */
  private String m_curValue = null;
  /** Original Value */
  private String m_oriValue = null;
  /** SQL */
  private String m_sql = null;

  private int m_updateCount = 0;

  private String m_Translated = null;

  private List<String> parameters;

  static Logger log4j = LogManager.getLogger();

  /*************************************************************************/

  /**
   * Receive notification of the start of an element.
   * 
   * @param uri
   *          namespace
   * @param localName
   *          simple name
   * @param qName
   *          qualified name
   * @param attributes
   *          attributes
   * @throws org.xml.sax.SAXException
   */
  @Override
  public void startElement(String uri, String localName, String qName, Attributes attributes)
      throws org.xml.sax.SAXException {
    if (qName.equals(TranslationManager.XML_TAG)) {
      m_AD_Language = attributes.getValue(TranslationManager.XML_ATTRIBUTE_LANGUAGE);

      m_TableName = attributes.getValue(TranslationManager.XML_ATTRIBUTE_TABLE);
      m_updateSQL = "update " + m_TableName;

      m_updateSQL += "_Trl";
      m_updateSQL += " set ";
      if (log4j.isDebugEnabled()) {
        log4j.debug("AD_Language=" + m_AD_Language + ", TableName=" + m_TableName);
      }
    } else if (qName.equals(TranslationManager.XML_ROW_TAG)) {
      m_curID = attributes.getValue(TranslationManager.XML_ROW_ATTRIBUTE_ID);
      m_Translated = attributes.getValue(TranslationManager.XML_ROW_ATTRIBUTE_TRANSLATED);
      m_sql = "";
    } else if (qName.equals(TranslationManager.XML_VALUE_TAG)) {
      m_curColumnName = attributes.getValue(TranslationManager.XML_VALUE_ATTRIBUTE_COLUMN);
      m_oriValue = attributes.getValue(TranslationManager.XML_VALUE_ATTRIBUTE_ORIGINAL);
    } else if (qName.equals(TranslationManager.XML_CONTRIB)) {
      m_AD_Language = attributes.getValue(TranslationManager.XML_ATTRIBUTE_LANGUAGE);
    } else {
      log4j.error("startElement - UNKNOWN TAG: " + qName);
    }
    m_curValue = "";
  } // startElement

  /**
   * Receive notification of character data inside an element.
   * 
   * @param ch
   *          buffer
   * @param start
   *          start
   * @param length
   *          length
   * @throws SAXException
   */
  @Override
  public void characters(char[] ch, int start, int length) throws SAXException {
    m_curValue += new String(ch, start, length);
  } // characters

  /**
   * Receive notification of the end of an element.
   * 
   * @param uri
   *          namespace
   * @param localName
   *          simple name
   * @param qName
   *          qualified name
   * @throws SAXException
   */
  @Override
  public void endElement(String uri, String localName, String qName) throws SAXException {
    // Log.trace(Log.l6_Database+1, "TranslationHandler.endElement", qName);
    if (log4j.isDebugEnabled()) {
      log4j.debug("endelement " + qName);
    }
    if (qName.equals(TranslationManager.XML_TAG)) {
    } else if (qName.equals(TranslationManager.XML_ROW_TAG)) {
      // Set section
      if (m_sql.length() > 0) {
        m_sql += ",";
      }
      m_sql += "Updated=now() ,IsTranslated=?";
      parameters.add(m_Translated);
      // Where section
      //@formatter:off
      m_sql += 
              " where " + m_TableName + "_ID=?" + 
              "   and AD_Language=?";
      //@formatter:on
      parameters.add(m_curID);
      parameters.add(m_AD_Language);
      if (m_AD_Client_ID >= 0) {
        m_sql += "  and AD_Client_ID=?";
        parameters.add(Integer.toString(m_AD_Client_ID));
      }
      // Update section
      m_sql = m_updateSQL + m_sql;
      if (log4j.isDebugEnabled()) {
        log4j.debug("{} with parameters {}", m_sql, parameters);
      }
      // Execute
      int no = 0;
      //
      PreparedStatement st = null;
      try {
        st = DB.getPreparedStatement(con, m_sql);
        int paramCount = 1;
        for (String parameter : parameters) {
          st.setString(paramCount, parameter);
          paramCount++;
        }

        no = st.executeUpdate();
        if (no == 1) {
          log4j.debug(m_sql);
          m_updateCount++;
        } else if (no == 0) {
          log4j.info("Not found translatable element - Table:{}, {}_ID={}, AD_Language={}",
              m_TableName, m_TableName, m_curID, m_AD_Language);
        } else {
          log4j.error("Update Rows={} (Should be 1) - {} with parameters {}", no, m_sql,
              parameters);
        }
      } catch (Exception e) {
        log4j.error("Failed query importing translation: {} with parameters {}. Exception: {}",
            m_sql, parameters, e.getMessage());
        try {
          // Rollback last query so next queries can be executed
          if (con != null && !con.isClosed()) {
            con.rollback();
          }
        } catch (SQLException ex) {
          log4j.error("Failed to rollback transaction.");
        }
      } finally {
        try {
          DB.releaseTransactionalPreparedStatement(st);
        } catch (SQLException e) {
          // This exception is ignored.
        }
        parameters.clear();
      }
    } else if (qName.equals(TranslationManager.XML_VALUE_TAG)) {
      String value = "";
      if (StringUtils.isNotEmpty(m_curValue)) {
        value = TO_STRING(m_curValue);
      } else if (StringUtils.isNotEmpty(m_oriValue)) {
        value = TO_STRING(m_oriValue);
      }
      if (StringUtils.isNotEmpty(value)) {
        if (m_sql.length() > 0) {
          m_sql += ", ";
        }
        m_sql += m_curColumnName + "=?";
        parameters.add(value);
      }
    } else if (qName.equals(TranslationManager.XML_CONTRIB)) {
      if (log4j.isDebugEnabled()) {
        log4j.debug("Contibutors:" + TO_STRING(m_curValue));
      }
      try {
        TranslationData.insertContrib(DB, m_curValue, m_AD_Language);
      } catch (Exception e) {
        log4j.error(e.toString());
      }
    }
  } // endElement

  /**
   * Get Number of updates
   * 
   * @return update count
   */
  public int getUpdateCount() {
    return m_updateCount;
  } // getUpdateCount

  private String TO_STRING(String txt) {
    return TO_STRING(txt, 0);
  } // TO_STRING

  /**
   * Package Strings for SQL command.
   * 
   * Because we are using prepared statements we don't have to escape single quotes, so this method
   * only gets a substring of maxLength from the string provided
   * 
   * @param txt
   *          String with text
   * @param maxLength
   *          Maximum Length of content or 0 to ignore
   * @return escaped string for insert statement (null if null or empty)
   */
  private String TO_STRING(String txt, int maxLength) {
    if (txt == null || txt.isEmpty()) {
      return null;
    }

    // Length
    String text = txt;
    if (maxLength != 0 && text.length() > maxLength) {
      text = txt.substring(0, maxLength);
    }

    return text;
  } // TO_STRING

} // TranslationHandler
