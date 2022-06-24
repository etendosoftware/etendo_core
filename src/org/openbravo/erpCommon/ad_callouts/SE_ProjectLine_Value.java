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

import java.io.IOException;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.openbravo.base.filter.IsIDFilter;
import org.openbravo.erpCommon.businessUtility.Tax;
import org.openbravo.erpCommon.utility.DateTimeData;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.utils.FormatUtilities;

public class SE_ProjectLine_Value extends SimpleCallout {

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    // Parameters
    String strmProductId = info.getStringParameter("inpmProductId", IsIDFilter.instance);
    String strProjectId = info.getStringParameter("inpcProjectId", IsIDFilter.instance);
    String strPhaseId = info.getStringParameter("inpcProjectphaseId", IsIDFilter.instance);
    String strADOrgID = info.getStringParameter("inpadOrgId", IsIDFilter.instance);
    String strPriceStd = info.vars.getNumericParameter("inpmProductId_PSTD");
    String strCreatePL = info.getStringParameter("inpcreatetemppricelist");
    String strStatus = info.getStringParameter("inpprojectstatus");
    String strPriceListVersion = info.getStringParameter("inpmPricelistVersionId",
        IsIDFilter.instance);

    String strMessage = "";
    if (StringUtils.isNotEmpty(strmProductId)) {
      // Project Data
      if (StringUtils.isNotEmpty(strPhaseId) && StringUtils.isEmpty(strProjectId)) {
        strProjectId = SEProjectLineValueData.selectPhaseProject(this, strPhaseId);
      }
      SEProjectLineValueData[] data1 = SEProjectLineValueData.selectProject(this, strProjectId);
      String strProjCat = data1[0].projcat;

      // Planned Price, Price Actual from Price List Version if any
      if (StringUtils.isNotEmpty(strPriceListVersion)) {
        String plannedprice = SEProjectLineValueData.selectPlannedPrice(this, strPriceListVersion,
            strmProductId, strProjectId);
        if (StringUtils.isNotEmpty(plannedprice)) {
          if (!StringUtils.equals(strProjCat, "S")) {
            info.addResult("inpplannedprice", plannedprice);
          } else {
            info.addResult("inppriceactual", plannedprice);
          }
        } else {
          strMessage = "PriceNotFound";
        }
      }

      // Price from Product Selector applies only in Service Project - Project Line
      info.addResult("inpplannedprice", strPriceStd);

      // Warning message: is not going to add in the pricelist.
      if (StringUtils.equalsIgnoreCase(strStatus, "OR")
          && StringUtils.equalsIgnoreCase(strCreatePL, "Y")) {
        strMessage = OBMessageUtils.messageBD("PriceListNotUpdated");
      }

      // Tax: Service Project - Project Line
      if (!StringUtils.equals(strProjCat, "S")) {
        String strCBPartnerLocationID = data1[0].bplocation;
        String strMWarehouseID = data1[0].warehouse;
        if (StringUtils.isNotEmpty(strCBPartnerLocationID)
            && StringUtils.isNotEmpty(strMWarehouseID)) {
          try {
            String strCTaxID = Tax.get(this, strmProductId, DateTimeData.today(this), strADOrgID,
                strMWarehouseID, strCBPartnerLocationID, strCBPartnerLocationID, strProjectId,
                true);
            if (StringUtils.isNotEmpty(strCTaxID)) {
              info.addResult("inpcTaxId", strCTaxID);
            } else {
              strMessage = "TaxNotFound";
            }
          } catch (IOException e) {
            strMessage = "TaxNotFound";
          }
        }
        SEProjectLineValueData[] data = SEProjectLineValueData.select(this, strmProductId);
        info.addResult("inpproductValue", data[0].value);
        info.addResult("inpproductName", FormatUtilities.replaceJS(data[0].name));
        info.addResult("inpproductDescription", FormatUtilities.replaceJS(data[0].description));
      }
    }

    // Show message if any
    if (StringUtils.isNotEmpty(strMessage)) {
      info.showMessage(
          FormatUtilities.replaceJS(Utility.messageBD(this, strMessage, info.vars.getLanguage())));
    }
  }
}
