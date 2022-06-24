/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.0  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2020 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */

package org.openbravo.erpCommon.ad_forms;

import org.openbravo.model.ad.ui.Menu;

/**
 * Class to centralize the logic for getting a reduced translation version.
 * 
 * The way it works is by adding a specific where clause for each translatable entity to filter out
 * the records that are not directly or indirectly linked to a menu entry set as Included in Reduced
 * Translation ({@link Menu#PROPERTY_TRANSLATIONSTRATEGY}
 */
class ReducedTranslationHelper {

  // Suppresses default constructor, ensuring non-instantiability.
  private ReducedTranslationHelper() {
  }

  private static final String EXCLUDE_FROM_REDUCED_TRL = "EXCLUDE_FROM_REDUCED_TRL";

  /*
   * Process and Process Definition have the same logic, it only varies the columns involved. This
   * enum helps to encapsulate this logic.
   */
  private enum ProcessParamType {
    AD_PROCESS_PARA("AD_PROCESS_ID", "AD_PROCESS_ID"),
    OBUIAPP_PARAMETER("EM_OBUIAPP_PROCESS_ID", "OBUIAPP_PROCESS_ID");

    final String linkToMenuColum;
    final String processPrimaryKeyColumn;

    ProcessParamType(String linkToMenuColum, String processPrimaryKeyColumn) {
      this.linkToMenuColum = linkToMenuColum;
      this.processPrimaryKeyColumn = processPrimaryKeyColumn;
    }

  }

  static String getReducedTranslationClause(final String table) {
    // Skip translation table records with isActive = No
    String sql = " and o.IsActive='Y'";
    // Skip parent table records with isActive = No
    sql += " and t.IsActive='Y'";

    //@formatter:off
    switch (table) {
      case "AD_TEXTINTERFACES":
        sql += " and (o.FILENAME IS NULL OR o.FILENAME NOT LIKE '%jrxml%')";
        break;
      case "AD_ELEMENT":
               // Window directly or indirectly linked to menu entry with ad_element
        sql += " AND (EXISTS " + getLinkToWindowsAvailableForReducedTranslationFromADColumn("o.AD_ELEMENT_ID", "AD_ELEMENT_ID", "")
               // Process Definition Param with an ad_element
             + "    OR EXISTS " + getLinkToProcessesAvailableForReducedTranslationFromProcessParam(ProcessParamType.OBUIAPP_PARAMETER, "o.AD_ELEMENT_ID", "AD_ELEMENT_ID")
               // Process Param with an ad_element
             + "    OR EXISTS " + getLinkToProcessesAvailableForReducedTranslationFromProcessParam(ProcessParamType.AD_PROCESS_PARA, "o.AD_ELEMENT_ID", "AD_ELEMENT_ID")
             + " )";
        break;
      case "AD_FIELD":
               // Window directly or indirectly linked to menu entry with ad_field
        sql += " AND EXISTS (SELECT 1 "
             + "             FROM AD_TAB t "
             + "             WHERE t.AD_TAB_ID = o.AD_TAB_ID "
             + "             AND "+getRecordsInWindowsAvailableForReducedTranslation("t") +")";
        break;
      case "AD_PROCESS":
      case "AD_PROCESS_PARA":
               // Direct menu entry
        sql += " AND ( EXISTS (SELECT 1 "
             + "                FROM AD_MENU m "
             + "                WHERE o.AD_Process_ID = m.AD_Process_ID "
             + "                AND COALESCE(m.translation_Strategy,'.') <> '"+EXCLUDE_FROM_REDUCED_TRL+"') "
                // Indirect menu entry through a process in another window
             + "      OR EXISTS " + getLinkToWindowsAvailableForReducedTranslationFromADColumn("o.AD_Process_ID", "AD_Process_ID", "")
             + "     ) ";
        break;
      case "OBUIAPP_PROCESS":
      case "OBUIAPP_PARAMETER":
               // Direct menu entry
        sql += " AND ( EXISTS (SELECT 1 "
             + "                FROM AD_MENU m "
             + "                WHERE o.OBUIAPP_PROCESS_ID = m.EM_OBUIAPP_PROCESS_ID "
             + "                AND COALESCE(m.translation_Strategy, '.') <> '"+EXCLUDE_FROM_REDUCED_TRL+"') "
             // Indirect menu entry through a process definition in another window
             + "      OR EXISTS " + getLinkToWindowsAvailableForReducedTranslationFromADColumn("o.OBUIAPP_PROCESS_ID", "EM_OBUIAPP_PROCESS_ID", "")
             + "     ) ";
        break;
      case "AD_MENU":
        sql += " AND COALESCE(o.translation_Strategy, '.') <> '"+EXCLUDE_FROM_REDUCED_TRL+"'";
        break;
      case "AD_WINDOW":
      case "AD_TAB":
        sql += " AND " + getRecordsInWindowsAvailableForReducedTranslation("o");
        break;
      case "AD_FIELDGROUP":
                      // In Windows
        sql += " AND (EXISTS (SELECT 1 "
            + "               FROM AD_FIELD f "
            + "               JOIN AD_TAB t ON (t.ad_tab_id = f.ad_tab_id) "
            + "               WHERE f.ad_fieldgroup_id = o.ad_fieldgroup_id "
            + "               AND "+ getRecordsInWindowsAvailableForReducedTranslation("t") +" ) "
                      // In Process Definitions
            + "    OR EXISTS " + getLinkToProcessesAvailableForReducedTranslationFromProcessParam(ProcessParamType.OBUIAPP_PARAMETER, "o.AD_FIELDGROUP_ID", "AD_FIELDGROUP_ID")
            + "       )";
        break;
      case "AD_REF_LIST":
                       // In Windows
        sql += " AND ( EXISTS " + getLinkToWindowsAvailableForReducedTranslationFromADColumn("o.AD_REFERENCE_ID", "AD_REFERENCE_VALUE_ID", "AND c.AD_REFERENCE_ID = '17'")
                       // In Process Definition Param
             + "       OR EXISTS " + getLinkToProcessesAvailableForReducedTranslationFromProcessParam(ProcessParamType.OBUIAPP_PARAMETER, "o.AD_REFERENCE_ID", "AD_REFERENCE_VALUE_ID")
                       // In Process Param
             + "       OR EXISTS " + getLinkToProcessesAvailableForReducedTranslationFromProcessParam(ProcessParamType.AD_PROCESS_PARA, "o.AD_REFERENCE_ID", "AD_REFERENCE_VALUE_ID")
             + "     ) ";
        break;
      // Exclude these tables completely as they don't add useful translation for end user
      case "AD_REFERENCE":
      case "OBUISEL_SELECTOR":
        sql += " and 1=2 ";
        break;
      default:
        sql += "";
      //@formatter:on
    }
    return sql;
  }

  /*
   * "Windows with direct entry in Menu with reduced translation enabled" or "Process Definition
   * Windows indirectly linked to a window with entry in Menu with reduced translation enabled"
   */
  private static String getRecordsInWindowsAvailableForReducedTranslation(final String tableAlias) {
    //@formatter:off
    return   "  ( EXISTS (SELECT 1 " 
           + "                FROM AD_MENU m "
           + "                WHERE m.AD_WINDOW_ID = "+tableAlias+".AD_WINDOW_ID "
           + "                AND COALESCE(m.translation_Strategy, '.') <> '"+EXCLUDE_FROM_REDUCED_TRL+"') "
           + "        OR EXISTS (SELECT 1 "
           + "                   FROM OBUIAPP_Ref_Window rw, " 
           + "                        OBUIAPP_PARAMETER pp, " 
           + "                        OBUIAPP_Process pr, " 
           + "                        AD_COLUMN rwc, " 
           + "                        AD_FIELD rwf, " 
           + "                        AD_TAB rwt, " 
           + "                        AD_WINDOW rww, "
           + "                        AD_MENU m "
           + "                    WHERE rw.AD_WINDOW_ID = "+tableAlias+".AD_WINDOW_ID "
           + "                      AND rw.AD_REFERENCE_ID = pp.AD_REFERENCE_VALUE_ID " 
           + "                      AND pp.OBUIAPP_Process_ID = pr.OBUIAPP_Process_ID " 
           + "                      AND pr.OBUIAPP_Process_ID = rwc.EM_OBUIAPP_Process_ID " 
           + "                      AND rwc.AD_COLUMN_ID = rwf.AD_COLUMN_ID " 
           + "                      AND rwf.AD_TAB_ID = rwt.AD_TAB_ID "
           + "                      AND rwt.AD_WINDOW_ID = rww.AD_WINDOW_ID "
           + "                      AND m.AD_WINDOW_ID = rww.AD_WINDOW_ID " 
           + "                      AND COALESCE(m.translation_Strategy, '.') <> '"+EXCLUDE_FROM_REDUCED_TRL+"') "
           + "   ) ";
    //@formatter:on
  }

  /*
   * Utility method that tries to find a (direct or indirect) window available for reduced
   * translation from Column -> Field -> Tab -> Window.
   */
  private static String getLinkToWindowsAvailableForReducedTranslationFromADColumn(
      final String referencedColumnToExternalQuery, final String linkThroughADColumn,
      final String extraWhereClause) {
    //@formatter:off
    return " (SELECT 1 "
         + "  FROM AD_COLUMN c, "
         + "  AD_FIELD f, "
         + "  AD_TAB t "
         + "  WHERE c.AD_COLUMN_ID = f.AD_COLUMN_ID "
         + "  AND f.AD_TAB_ID = t.AD_TAB_ID "
         + "  AND " + referencedColumnToExternalQuery + " = c." + linkThroughADColumn 
         + "  AND " + getRecordsInWindowsAvailableForReducedTranslation("t")
         +   extraWhereClause
         + " ) ";
    //@formatter:on
  }

  /*
   * Utility method that tries to find a (direct or indirect) process/process definition for reduced
   * translation from any process/process definition param.
   */
  private static String getLinkToProcessesAvailableForReducedTranslationFromProcessParam(
      final ProcessParamType processParamType, final String referencedColumnToExternalQuery,
      final String linkThroughProcessParameterTable) {
    //@formatter:off
    return " (SELECT 1 "
         + "  FROM " + processParamType + " pp "
         + "  WHERE pp." + linkThroughProcessParameterTable + " = " + referencedColumnToExternalQuery
                   // ...directly linked to menu entry
         + "  AND (   EXISTS (SELECT 1 "
         + "                  FROM AD_MENU m "
         + "                  WHERE m." + processParamType.linkToMenuColum + " = pp." + processParamType.processPrimaryKeyColumn
         + "                  AND COALESCE(m.translation_Strategy, '.') <> '"+EXCLUDE_FROM_REDUCED_TRL+"') "
                   // ...indirectly linked to a window menu entry
         + "       OR EXISTS " + getLinkToWindowsAvailableForReducedTranslationFromADColumn("pp." + processParamType.processPrimaryKeyColumn, processParamType.linkToMenuColum, "")
         + "      ) "
         + " ) ";
    //@formatter:on
  }

}
