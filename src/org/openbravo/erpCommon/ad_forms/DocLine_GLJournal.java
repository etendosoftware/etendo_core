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
 * All portions are Copyright (C) 2014-2015 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */
package org.openbravo.erpCommon.ad_forms;

import javax.servlet.ServletException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.database.ConnectionProvider;

public class DocLine_GLJournal extends DocLine {
  static Logger log4jDocLine_GLJournal = LogManager.getLogger();

  public DocLine_GLJournal(String DocumentType, String TrxHeader_ID, String TrxLine_ID) {
    super(DocumentType, TrxHeader_ID, TrxLine_ID);
  }

  /**
   * Line Account from Product (or Charge).
   * 
   * @param AcctType
   *          see ProoductInfo.ACCTTYPE_* (0..3)
   * @param as
   *          Accounting schema
   * @return Requested Product Account
   */
  public Account getAccount(String AcctType, AcctSchema as, ConnectionProvider conn) {

    // GL Item directly from GLJournal Line
    if (m_C_Glitem_ID != null && !m_C_Glitem_ID.equals("")) {
      try {
        DocLineGLJournalData[] data = null;
        data = DocLineGLJournalData.selectGlitem(conn, m_C_Glitem_ID, as.getC_AcctSchema_ID());
        String Account_ID = "";
        if (data == null || data.length == 0) {
          return null;
        }
        if (data.length > 0) {
          switch (Integer.parseInt(AcctType)) {
            case 1:
              // It is similar to ProductInfo.ACCTTYPE_P_Revenue
              Account_ID = data[0].glitemDebitAcct;
              break;
            case 2:
              // It is similar to ProductInfo.ACCTTYPE_P_Expense
              Account_ID = data[0].glitemCreditAcct;
              break;
          }
        }
        // No account
        if (Account_ID.equals("")) {
          log4jDocLine_GLJournal.warn("getAccount - NO account for m_C_Glitem_ID=" + m_C_Glitem_ID);
          return null;
        }
        // Return Account
        return Account.getAccount(conn, Account_ID);

      } catch (ServletException e) {
        log4jDocLine_GLJournal.warn(e);
      }
    } else {
      return this.m_account;
    }
    return null;
  } // getAccount
}
