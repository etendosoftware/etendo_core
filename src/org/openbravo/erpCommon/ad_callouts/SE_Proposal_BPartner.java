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
import org.openbravo.erpCommon.utility.ComboTableData;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.utils.FormatUtilities;

public class SE_Proposal_BPartner extends SimpleCallout {

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    String strChanged = info.getLastFieldChanged();
    if (log4j.isDebugEnabled()) {
      log4j.debug("CHANGED: " + strChanged);
    }

    // Parameters
    String strBPartner = info.getStringParameter("inpcBpartnerId", IsIDFilter.instance);
    String strLocation = info.getStringParameter("inpcBpartnerLocationId", IsIDFilter.instance);
    String strContact = info.getStringParameter("inpadUserId", IsIDFilter.instance);
    String strWindowId = info.getWindowId();

    // Update payment rule
    String strPaymentRule = SEProposalBPartnerData.selectPaymentRule(this, strBPartner);
    info.addResult("inppaymentrule", strPaymentRule);

    // Update payment term
    String strPaymentTerm = SEProposalBPartnerData.selectPaymentTerm(this, strBPartner);
    info.addResult("inpcPaymenttermId", strPaymentTerm);

    // If a Location ID has not been specified, the first one is selected. Else if Location ID is
    // provided, it is selected.
    FieldProvider[] tdv = null;
    try {
      ComboTableData comboTableData = new ComboTableData(info.vars, this, "TABLEDIR",
          "C_BPartner_Location_ID", "", "C_BPartner Location - Ship To",
          Utility.getContext(this, info.vars, "#AccessibleOrgTree", strWindowId),
          Utility.getContext(this, info.vars, "#User_Client", strWindowId), 0);
      Utility.fillSQLParameters(this, info.vars, null, comboTableData, strWindowId, "");
      tdv = comboTableData.select(false);
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }
    if (tdv != null && tdv.length > 0) {
      info.addSelect("inpcBpartnerLocationId");
      for (int i = 0; i < tdv.length; i++) {
        boolean selected = (StringUtils.isEmpty(strLocation) && i == 0)
            || StringUtils.equalsIgnoreCase(tdv[i].getField("id"), strLocation);
        info.addSelectResult(tdv[i].getField("id"),
            FormatUtilities.replaceJS(tdv[i].getField("name")), selected);
      }
      info.endSelect();
    }

    // If a contactID has not been specified, the first one is selected. Else if contact ID is
    // provided, it is selected.
    try {
      ComboTableData comboTableData = new ComboTableData(info.vars, this, "TABLEDIR", "AD_User_ID",
          "", "AD_User C_BPartner User/Contacts",
          Utility.getContext(this, info.vars, "#AccessibleOrgTree", strWindowId),
          Utility.getContext(this, info.vars, "#User_Client", strWindowId), 0);
      Utility.fillSQLParameters(this, info.vars, null, comboTableData, strWindowId, "");
      tdv = comboTableData.select(false);
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }
    if (tdv != null && tdv.length > 0) {
      info.addSelect("inpadUserId");
      for (int i = 0; i < tdv.length; i++) {
        boolean selected = (StringUtils.isEmpty(strContact) && i == 0)
            || StringUtils.equalsIgnoreCase(tdv[i].getField("id"), strContact);
        info.addSelectResult(tdv[i].getField("id"),
            FormatUtilities.replaceJS(tdv[i].getField("name")), selected);
      }
      info.endSelect();
    }
  }
}
