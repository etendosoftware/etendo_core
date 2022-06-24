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
 * All portions are Copyright (C) 2012-2017 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.erpCommon.ad_callouts;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.openbravo.advpaymentmngt.utility.FIN_Utility;
import org.openbravo.base.filter.IsIDFilter;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.pricing.pricelist.PriceList;

public class SL_Requisition_BPartner_PriceList extends SimpleCallout {

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    String strChanged = info.getLastFieldChanged();
    if (log4j.isDebugEnabled()) {
      log4j.debug("CHANGED: " + strChanged);
    }

    // Parameters
    String strBPartner = info.getStringParameter("inpcBpartnerId", IsIDFilter.instance);
    String strPriceList = info.getStringParameter("inpmPricelistId", IsIDFilter.instance);

    if (StringUtils.equals(strChanged, "inpcBpartnerId")) {
      if (StringUtils.isEmpty(strBPartner)) {
        // Remove C_BPartner_ID from session
        info.vars.removeSessionValue(info.getWindowId() + "|C_BPartner_ID");
      } else {
        OBContext.setAdminMode(true);
        try {
          BusinessPartner bPartner = OBDal.getInstance().get(BusinessPartner.class, strBPartner);
          // If the Business Partner is blocked for this document, show an information message.
          if (FIN_Utility.isBlockedBusinessPartner(strBPartner, false, 1)) {
            info.showMessage(
                OBMessageUtils.messageBD("ThebusinessPartner") + " " + bPartner.getIdentifier()
                    + " " + OBMessageUtils.messageBD("BusinessPartnerBlocked"));
          }
          // Set Price List and Currency for business partner in Requisition or RequisitionLine
          if (bPartner.getPurchasePricelist() != null) {
            info.addResult("inpmPricelistId", bPartner.getPurchasePricelist().getId());
            info.addResult("inpcCurrencyId", bPartner.getPurchasePricelist().getCurrency().getId());
          }
        } finally {
          OBContext.restorePreviousMode();
        }
      }
    }

    else if (StringUtils.equals(strChanged, "inpmPricelistId")) {
      if (StringUtils.isEmpty(strPriceList)) {
        // Remove M_PriceList_ID from session
        info.vars.removeSessionValue(info.getWindowId() + "|M_PriceList_ID");
      } else {
        // Set Price List currency in Requisition or RequisitionLine
        OBContext.setAdminMode(true);
        try {
          PriceList priceList = OBDal.getInstance().get(PriceList.class, strPriceList);
          info.addResult("inpcCurrencyId", priceList.getCurrency().getId());
        } finally {
          OBContext.restorePreviousMode();
        }
      }
    }
  }
}
