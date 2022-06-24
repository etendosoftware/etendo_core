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
import org.openbravo.advpaymentmngt.utility.FIN_Utility;
import org.openbravo.base.filter.IsIDFilter;
import org.openbravo.dal.service.OBDal;
import org.openbravo.data.FieldProvider;
import org.openbravo.erpCommon.businessUtility.BpartnerMiscData;
import org.openbravo.erpCommon.utility.ComboTableData;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.utils.FormatUtilities;
import org.openbravo.utils.Replace;

public class SL_InOut_BPartner extends SimpleCallout {

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    String strChanged = info.getLastFieldChanged();
    if (log4j.isDebugEnabled()) {
      log4j.debug("CHANGED: " + strChanged);
    }

    // Parameters
    String strBPartner = info.getStringParameter("inpcBpartnerId", IsIDFilter.instance);
    String strLocation = info.getStringParameter("inpcBpartnerId_LOC", IsIDFilter.instance);
    String strContact = info.getStringParameter("inpcBpartnerId_CON", IsIDFilter.instance);
    String strOrgId = info.getStringParameter("inpadOrgId", IsIDFilter.instance);
    String strDeliveryTerms = info.getStringParameter("inpdeliveryrule");
    String strDeliveryMethod = info.getStringParameter("inpdeliveryviarule");
    String strIsSOTrx = Utility.getContext(this, info.vars, "isSOTrx", info.getWindowId());

    BpartnerMiscData[] data = BpartnerMiscData.select(this, strBPartner);
    String strUserRep = "";
    if (data != null && data.length > 0) {
      strUserRep = SEOrderBPartnerData.userIdSalesRep(this, data[0].salesrepId);
      if (StringUtils.isEmpty(strUserRep)) {
        strUserRep = info.vars.getUser();
      }
    }

    // Business Partner Location
    FieldProvider[] tdv = null;
    try {
      ComboTableData comboTableData = new ComboTableData(info.vars, this, "TABLEDIR",
          "C_BPartner_Location_ID", "", "C_BPartner Location - Ship To",
          Utility.getContext(this, info.vars, "#AccessibleOrgTree", info.getWindowId()),
          Utility.getContext(this, info.vars, "#User_Client", info.getWindowId()), 0);
      Utility.fillSQLParameters(this, info.vars, null, comboTableData, info.getWindowId(), "");
      tdv = comboTableData.select(false);
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }
    if (tdv != null && tdv.length > 0) {
      info.addSelect("inpcBpartnerLocationId");
      for (int i = 0; i < tdv.length; i++) {
        // If a location is provided it is selected, else the first one is selected
        boolean selected = (StringUtils.isEmpty(strLocation) && i == 0)
            || (StringUtils.isNotEmpty(strLocation)
                && StringUtils.equalsIgnoreCase(tdv[i].getField("id"), strLocation));
        info.addSelectResult(tdv[i].getField("id"),
            FormatUtilities.replaceJS(Replace.replace(tdv[i].getField("name"), "\"", "\\\"")),
            selected);
      }
      info.endSelect();
    } else {
      info.addResult("inpcBpartnerLocationId", "");
    }

    // Sales Representative
    FieldProvider[] tld = null;
    try {
      ComboTableData comboTableData = new ComboTableData(info.vars, this, "TABLE", "SalesRep_ID",
          "AD_User SalesRep", "", Utility.getReferenceableOrg(info.vars, strOrgId),
          Utility.getContext(this, info.vars, "#User_Client", info.getWindowId()), 0);
      Utility.fillSQLParameters(this, info.vars, null, comboTableData, info.getWindowId(), "");
      tld = comboTableData.select(false);
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }
    if (tld != null && tld.length > 0) {
      info.addSelect("inpsalesrepId");
      for (int i = 0; i < tld.length; i++) {
        info.addSelectResult(tld[i].getField("id"),
            FormatUtilities.replaceJS(tld[i].getField("name")),
            StringUtils.equalsIgnoreCase(tld[i].getField("id"), strUserRep));
      }
      info.endSelect();
    } else {
      info.addResult("inpsalesrepId", "");
    }

    // Project
    info.addResult("inpcProjectId", "");

    // Business Partner Contact
    try {
      ComboTableData comboTableData = new ComboTableData(info.vars, this, "TABLEDIR", "AD_User_ID",
          "", "AD_User C_BPartner User/Contacts", Utility.getReferenceableOrg(info.vars, strOrgId),
          Utility.getContext(this, info.vars, "#User_Client", info.getWindowId()), 0);
      Utility.fillSQLParameters(this, info.vars, null, comboTableData, info.getWindowId(), "");
      tdv = comboTableData.select(false);
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }
    if (tdv != null && tdv.length > 0) {
      info.addSelect("inpadUserId");
      for (int i = 0; i < tdv.length; i++) {
        // If no contact is provided it is selected, else the first one is selected
        boolean selected = (StringUtils.isEmpty(strContact) && i == 0)
            || (StringUtils.isNotEmpty(strContact)
                && StringUtils.equalsIgnoreCase(tdv[i].getField("id"), strContact));
        info.addSelectResult(tdv[i].getField("id"),
            FormatUtilities.replaceJS(Replace.replace(tdv[i].getField("name"), "\"", "\\\"")),
            selected);
      }
      info.endSelect();
    } else {
      info.addResult("inpadUserId", "");
    }

    // Delivery Rule
    BusinessPartner bpartner = OBDal.getInstance().get(BusinessPartner.class, strBPartner);
    if (bpartner != null) {
      if (StringUtils.isNotEmpty(bpartner.getDeliveryTerms())
          && !StringUtils.equals(strDeliveryTerms, bpartner.getDeliveryTerms())) {
        info.addResult("inpdeliveryrule", bpartner.getDeliveryTerms());
      }

      // Delivery Via Rule
      if (StringUtils.isNotEmpty(bpartner.getDeliveryMethod())
          && !StringUtils.equals(strDeliveryMethod, bpartner.getDeliveryMethod())) {
        info.addResult("inpdeliveryviarule", bpartner.getDeliveryMethod());
      }

      // If the Business Partner is blocked for this document, show an information message
      final String rtvendorship = "273673D2ED914C399A6C51DB758BE0F9";
      final String rMatReceipt = "123271B9AD60469BAE8A924841456B63";
      final boolean isSOTrx = StringUtils.equals(strIsSOTrx, "Y");
      String message = "";
      if (!StringUtils.equals(info.getWindowId(), rtvendorship)
          && !StringUtils.equals(info.getWindowId(), rMatReceipt)
          && FIN_Utility.isBlockedBusinessPartner(strBPartner, isSOTrx, 2)) {
        message = OBMessageUtils.messageBD("ThebusinessPartner") + " " + bpartner.getIdentifier()
            + " " + OBMessageUtils.messageBD("BusinessPartnerBlocked");
      }

      // If the Business Partner has negative credit available, show an information message
      if (data != null && data.length > 0 && isSOTrx
          && new BigDecimal(data[0].creditavailable).compareTo(BigDecimal.ZERO) < 0) {
        String creditLimitExceed = String.valueOf(Double.parseDouble(data[0].creditavailable) * -1);
        if (StringUtils.isNotEmpty(message)) {
          message = message + "<br>";
        }
        message = message + Utility.messageBD(this, "CreditLimitOver", info.vars.getLanguage())
            + creditLimitExceed;
      }

      if (StringUtils.isNotEmpty(message)) {
        info.showMessage(message);
      }
    }

  }
}
