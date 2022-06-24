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
 * All portions are Copyright (C) 2001-2018 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.ad_callouts;

import java.math.BigDecimal;
import java.math.RoundingMode;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.financialmgmt.gl.GLJournal;

public class SL_JournalLineAmt extends SimpleCallout {

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    String strChanged = info.getLastFieldChanged();
    if (log4j.isDebugEnabled()) {
      log4j.debug("CHANGED: " + strChanged);
    }

    // Parameters
    String strGLJournal = info.vars.getRequiredStringParameter("inpglJournalId");
    String strCurrencyRateType = info.vars.getStringParameter("inpcurrencyratetype", "S");
    BigDecimal amtSourceDr = info.getBigDecimalParameter("inpamtsourcedr");
    BigDecimal amtSourceCr = info.getBigDecimalParameter("inpamtsourcecr");

    String strAcctSchema = SLJournalLineAmtData.selectGeneralLedger(this, strGLJournal);
    SLJournalLineAmtData[] data = SLJournalLineAmtData.select(this, strAcctSchema);
    int stdPrecision = 2;
    if (data != null && data.length > 0) {
      stdPrecision = Integer.valueOf(data[0].stdprecision);
    }

    if (StringUtils.equals(strChanged, "inpamtsourcedr")
        && amtSourceDr.compareTo(BigDecimal.ZERO) != 0) {
      amtSourceCr = BigDecimal.ZERO;
    }
    if (StringUtils.equals(strChanged, "inpamtsourcecr")
        && amtSourceCr.compareTo(BigDecimal.ZERO) != 0) {
      amtSourceDr = BigDecimal.ZERO;
    }

    GLJournal gLJournal = OBDal.getInstance().get(GLJournal.class, strGLJournal);
    BigDecimal currencyRate = gLJournal.getRate().setScale(stdPrecision, RoundingMode.HALF_UP);
    BigDecimal amtAcctDr = amtSourceDr.multiply(currencyRate)
        .setScale(stdPrecision, RoundingMode.HALF_UP);
    BigDecimal amtAcctCr = amtSourceCr.multiply(currencyRate)
        .setScale(stdPrecision, RoundingMode.HALF_UP);

    info.addResult("inpamtacctdr", amtAcctDr);
    info.addResult("inpamtacctcr", amtAcctCr);
    info.addResult("inpamtsourcedr", amtSourceDr);
    info.addResult("inpamtsourcecr", amtSourceCr);
    info.addResult("inpcurrencyrate", currencyRate);
    info.addResult("inpcurrencyratetype", strCurrencyRateType);
  }
}
