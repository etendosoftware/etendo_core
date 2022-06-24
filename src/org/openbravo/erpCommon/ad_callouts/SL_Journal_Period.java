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
 * All portions are Copyright (C) 2001-2017 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.ad_callouts;

import java.math.BigDecimal;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.openbravo.base.filter.IsIDFilter;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.DateTimeData;
import org.openbravo.erpCommon.utility.OBCurrencyUtils;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.financialmgmt.accounting.coa.AcctSchema;

public class SL_Journal_Period extends SimpleCallout {

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    String strChanged = info.getLastFieldChanged();
    if (log4j.isDebugEnabled()) {
      log4j.debug("CHANGED: " + strChanged);
    }

    // Parameters
    String strOrgId = info.getStringParameter("inpadOrgId", IsIDFilter.instance);
    String strDateAcct = info.getStringParameter("inpdateacct");
    String strDateDoc = info.getStringParameter("inpdatedoc");
    String strcPeriodId = info.getStringParameter("inpcPeriodId", IsIDFilter.instance);
    String strCurrencyId = info.getStringParameter("inpcCurrencyId", IsIDFilter.instance);
    String strAcctSchemaId = info.getStringParameter("inpcAcctschemaId", IsIDFilter.instance);
    String strCurrencyRateType = info.vars.getStringParameter("inpcurrencyratetype", "S");
    String stradClientId = info.vars.getClient();

    // When organization is changed, update currency
    String currency = null;
    if (StringUtils.equals(strChanged, "inpadOrgId")) {
      currency = OBCurrencyUtils.getOrgCurrency(strOrgId);
    }

    // When DateDoc is changed, update DateAcct
    if (StringUtils.equals(strChanged, "inpdatedoc")) {
      strDateAcct = strDateDoc;
      strChanged = "inpdateacct";
    }

    // When DateAcct is changed, set C_Period_ID
    if (StringUtils.equals(strChanged, "inpdateacct")) {
      strcPeriodId = SLJournalPeriodData.period(this, stradClientId, strOrgId, strDateAcct);
      if (StringUtils.isEmpty(strcPeriodId)) {
        info.showError(OBMessageUtils.messageBD(this, "PeriodNotValid", info.vars.getLanguage()));
      }
    }

    // When C_Period_ID is changed, check if in DateAcct range and set to end date if not
    boolean isStandardPeriod = true;
    if (StringUtils.equals(strChanged, "inpcPeriodId") && StringUtils.isNotEmpty(strcPeriodId)) {
      SLJournalPeriodData[] data = SLJournalPeriodData.select(this, strcPeriodId);
      String periodType = data[0].periodtype;
      String startDate = data[0].startdate;
      String endDate = data[0].enddate;
      // Standard Periods
      if (StringUtils.equals(periodType, "S")) {
        // Out of range, set to last day
        if (StringUtils.equals(DateTimeData.compare(this, startDate, strDateAcct), "1")
            || StringUtils.equals(DateTimeData.compare(this, endDate, strDateAcct), "-1")) {
          strDateAcct = endDate;
        }
      } else {
        isStandardPeriod = false;
        strDateAcct = endDate;
      }
    }

    // Update Date acct
    info.addResult("inpdateacct", strDateAcct);

    // Update currency
    if (currency != null) {
      info.addResult("inpcCurrencyId", currency);
    }

    // Update date doc
    if (!isStandardPeriod) {
      info.addResult("inpdatedoc", strDateAcct);
    }

    // Update period
    info.addResult("inpcPeriodId", strcPeriodId);

    // Update currency rate
    if (StringUtils.isNotEmpty(strAcctSchemaId)) {
      AcctSchema acctSchema = OBDal.getInstance().get(AcctSchema.class, strAcctSchemaId);
      String currencyRate = null;
      try {
        currencyRate = SLJournalPeriodData.getCurrencyRate(this, strCurrencyId,
            acctSchema.getCurrency().getId(), strDateAcct, strCurrencyRateType, stradClientId,
            strOrgId, strAcctSchemaId);
      } catch (Exception e) {
        OBDal.getInstance().rollbackAndClose();
        log4j.warn("No currency conversion exists.");
        info.showMessage(
            OBMessageUtils.messageBD(this, "NoCurrencyConversion", info.vars.getLanguage()));
      }
      info.addResult("inpcurrencyrate",
          StringUtils.isNotEmpty(currencyRate) ? new BigDecimal(currencyRate) : BigDecimal.ONE);
    }

  }
}
