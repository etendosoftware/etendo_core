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

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.openbravo.base.filter.IsIDFilter;
import org.openbravo.data.FieldProvider;
import org.openbravo.erpCommon.businessUtility.BpartnerMiscData;
import org.openbravo.erpCommon.utility.ComboTableData;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.utils.FormatUtilities;

public class SE_Project_BPartner extends SimpleCallout {

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
    String strSalesRep = info.getStringParameter("inpsalesrepId", IsIDFilter.instance);
    String strOrgId = info.getStringParameter("inpadOrgId", IsIDFilter.instance);
    String strIsSOTrx = Utility.getContext(this, info.vars, "isSOTrx", info.getWindowId());
    boolean isSales = StringUtils.equals(strIsSOTrx, "Y");

    // Business Partner Data
    BpartnerMiscData[] data = BpartnerMiscData.select(this, strBPartner);
    String strPaymentrule = "", strPaymentterm = "", strPricelist = "", strPaymentMethod = "",
        strUserRep = "";
    if (data != null && data.length > 0) {
      strPaymentrule = isSales ? data[0].paymentrule : data[0].paymentrulepo;
      strPaymentterm = isSales ? data[0].cPaymenttermId : data[0].poPaymenttermId;
      strPricelist = isSales ? data[0].mPricelistId : data[0].poPricelistId;
      strPaymentMethod = isSales ? data[0].finPaymentmethodId : data[0].poPaymentmethodId;
      strUserRep = SEOrderBPartnerData.userIdSalesRep(this, data[0].salesrepId);
      strUserRep = StringUtils.isEmpty(strUserRep) ? strSalesRep : strUserRep;
    }

    // Business Partner - Ship To Address
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
      // If location is provided it is selected, else the first one is selected
      for (int i = 0; i < tdv.length; i++) {
        info.addSelectResult(tdv[i].getField("id"),
            FormatUtilities.replaceJS(tdv[i].getField("name")),
            (StringUtils.isEmpty(strLocation) && i == 0) || (StringUtils.isNotEmpty(strLocation)
                && StringUtils.equalsIgnoreCase(tdv[i].getField("id"), strLocation)));
      }
      info.endSelect();
    } else {
      info.addResult("inpcBpartnerLocationId", "");
    }

    // Sales Representative
    try {
      ComboTableData comboTableData = new ComboTableData(info.vars, this, "TABLE", "",
          "AD_User SalesRep", "", Utility.getReferenceableOrg(info.vars, strOrgId),
          Utility.getContext(this, info.vars, "#User_Client", info.getWindowId()), 0);
      Utility.fillSQLParameters(this, info.vars, null, comboTableData, info.getWindowId(), "");
      tdv = comboTableData.select(false);
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }
    if (tdv != null && tdv.length > 0) {
      info.addSelect("inpsalesrepId");
      for (int i = 0; i < tdv.length; i++) {
        info.addSelectResult(tdv[i].getField("id"),
            FormatUtilities.replaceJS(tdv[i].getField("name")),
            StringUtils.equalsIgnoreCase(tdv[i].getField("id"), strUserRep));
      }
      info.endSelect();
    } else {
      info.addResult("inpsalesrepId", "");
    }

    // Business Partner - User/Contacts
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
      // If contact is provided it is selected, else the first one is selected
      for (int i = 0; i < tdv.length; i++) {
        info.addSelectResult(tdv[i].getField("id"),
            FormatUtilities.replaceJS(tdv[i].getField("name")),
            (StringUtils.isEmpty(strContact) && i == 0) || (StringUtils.isNotEmpty(strContact)
                && StringUtils.equalsIgnoreCase(tdv[i].getField("id"), strContact)));
      }
      info.endSelect();
    } else {
      info.addResult("inpadUserId", "");
    }

    // Business Partner - Bill To Address
    FieldProvider[] tlv = null;
    try {
      ComboTableData comboTableData = new ComboTableData(info.vars, this, "TABLE", "",
          "C_BPartner Location", "C_BPartner Location - Bill To",
          Utility.getContext(this, info.vars, "#AccessibleOrgTree", info.getWindowId()),
          Utility.getContext(this, info.vars, "#User_Client", info.getWindowId()), 0);
      Utility.fillSQLParameters(this, info.vars, null, comboTableData, info.getWindowId(), "");
      tlv = comboTableData.select(false);
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }
    if (tlv != null && tlv.length > 0) {
      info.addSelect("inpbilltoId");
      // If location is provided it is selected, else the first one is selected
      for (int i = 0; i < tlv.length; i++) {
        info.addSelectResult(tlv[i].getField("id"),
            FormatUtilities.replaceJS(tlv[i].getField("name")),
            (StringUtils.isEmpty(strLocation) && i == 0) || (StringUtils.isNotEmpty(strLocation)
                && StringUtils.equalsIgnoreCase(tdv[i].getField("id"), strLocation)));
      }
      info.endSelect();
    } else {
      info.addResult("inpbilltoId", "");
    }

    // Payment Rule, Payment Term, Price List, Payment Method
    info.addResult("inppaymentrule", strPaymentrule);
    info.addResult("inpcPaymenttermId", strPaymentterm);
    info.addResult("inpmPricelistId", strPricelist);
    info.addResult("inpfinPaymentmethodId", strPaymentMethod);
  }
}
