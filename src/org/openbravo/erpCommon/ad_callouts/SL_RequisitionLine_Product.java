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
 * All portions are Copyright (C) 2008-2018 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.ad_callouts;

import java.math.BigDecimal;
import java.math.RoundingMode;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.openbravo.base.filter.IsIDFilter;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.data.FieldProvider;
import org.openbravo.erpCommon.utility.ComboTableData;
import org.openbravo.erpCommon.utility.DateTimeData;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.materialmgmt.UOMUtil;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.pricing.pricelist.PriceList;
import org.openbravo.utils.FormatUtilities;

public class SL_RequisitionLine_Product extends SimpleCallout {

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    String strChanged = info.getLastFieldChanged();
    if (log4j.isDebugEnabled()) {
      log4j.debug("CHANGED: " + strChanged);
    }

    // Parameters
    String strMProductID = info.getStringParameter("inpmProductId", IsIDFilter.instance);
    String strRequisitionId = info.getStringParameter("inpmRequisitionId", IsIDFilter.instance);
    String strPriceListId = info.getStringParameter("inpmPricelistId", IsIDFilter.instance);
    String strAttributeSetInstanceId = info.getStringParameter("inpmProductId_ATR",
        IsIDFilter.instance);
    String strUOMId = info.getStringParameter("inpmProductId_UOM", IsIDFilter.instance);
    String strUOMProductId = info.getStringParameter("inpmProductUomId", IsIDFilter.instance);
    String strDueDate = info.vars.getStringParameter("inpneedbydate", DateTimeData.today(this));

    // Currency
    OBContext.setAdminMode(true);
    try {
      PriceList pList = OBDal.getInstance().get(PriceList.class, strPriceListId);
      if (pList != null) {
        info.addResult("inpcCurrencyId", pList.getCurrency().getId());
      }
    } finally {
      OBContext.restorePreviousMode();
    }

    if (StringUtils.isNotEmpty(strMProductID)) {

      if (StringUtils.isEmpty(strPriceListId)) {
        strPriceListId = SLRequisitionLineProductData.selectPriceList(this, strRequisitionId);
      }

      if (StringUtils.isNotEmpty(strPriceListId)) {

        // Price List
        PriceList priceListObj = OBDal.getInstance().get(PriceList.class, strPriceListId);

        // Set Auxiliary Input parameter Gross Price
        info.addResult("inpgrossprice", priceListObj.isPriceIncludesTax() ? "Y" : "N");

        // Discount, Price List, Gross Unit Price, Price Actual
        String strMessage = "";
        String strPriceListVersion = SLRequisitionLineProductData.selectPriceListVersion(this,
            strPriceListId, strDueDate);
        if (StringUtils.isNotEmpty(strPriceListVersion)) {
          SLRequisitionLineProductData[] prices = SLRequisitionLineProductData.getPrices(this,
              strMProductID, strPriceListVersion);
          if (prices != null && prices.length > 0 && (!StringUtils.equals(prices[0].pricelist, "0")
              || !StringUtils.equals(prices[0].pricestd, "0"))) {
            BigDecimal priceList = StringUtils.isEmpty(prices[0].pricelist) ? BigDecimal.ZERO
                : new BigDecimal(prices[0].pricelist);
            BigDecimal priceActual = StringUtils.isEmpty(prices[0].pricestd) ? BigDecimal.ZERO
                : new BigDecimal(prices[0].pricestd);
            BigDecimal discount = BigDecimal.ZERO;
            if (priceList.compareTo(BigDecimal.ZERO) != 0) {
              discount = (((priceList.subtract(priceActual)).divide(priceList, 12,
                  RoundingMode.HALF_EVEN)).multiply(new BigDecimal("100"))).setScale(2,
                      RoundingMode.HALF_UP);
            }
            info.addResult("inpdiscount", discount);
            info.addResult("inppricelist", priceList);
            info.addResult(
                priceListObj.isPriceIncludesTax() ? "inpgrossUnitPrice" : "inppriceactual",
                priceActual);
          } else {
            strMessage = "PriceNotFound";
          }
        } else {
          strMessage = "PriceListVersionNotFound";
        }

        // Set Message either PriceListVersionNotFound or PriceNotFound if any
        if (StringUtils.isNotEmpty(strMessage)) {
          info.showMessage(FormatUtilities
              .replaceJS(Utility.messageBD(this, strMessage, info.vars.getLanguage())));
        }
      } else {
        info.addResult("inpgrossprice", "");
      }
    }

    // Set AUM based on default
    if (UOMUtil.isUomManagementEnabled() && StringUtils.isEmpty(strUOMProductId)
        && StringUtils.isNotEmpty(strMProductID)) {
      String finalAUM = UOMUtil.getDefaultAUMForPurchase(strMProductID);
      if (finalAUM != null) {
        info.addResult("inpcAum", finalAUM);
      }
    }

    // UOM, AttributeSetInstance, AttributeSet, AttributeSetValueType
    if (StringUtils.equals(strChanged, "inpmProductId")) {

      info.addResult("inpcUomId", strUOMId);
      info.addResult("inpmAttributesetinstanceId", strAttributeSetInstanceId);

      OBContext.setAdminMode();
      try {
        final Product product = OBDal.getInstance().get(Product.class, strMProductID);
        if (product != null) {
          info.addResult("inpattributeset",
              product.getAttributeSet() != null ? product.getAttributeSet().getId() : "");
          info.addResult("inpattrsetvaluetype",
              FormatUtilities.replaceJS(product.getUseAttributeSetValueAs()));
        }
      } finally {
        OBContext.restorePreviousMode();
      }

      // Has Second UOM
      String strHasSecondaryUOM = SLRequisitionLineProductData.hasSecondaryUOM(this, strMProductID);
      info.addResult("inphasseconduom", Integer.parseInt(strHasSecondaryUOM));

      // Product UOM List
      if (StringUtils.equals(strHasSecondaryUOM, "1") && (!UOMUtil.isUomManagementEnabled()
          || (UOMUtil.isUomManagementEnabled() && StringUtils.isNotEmpty(strUOMProductId)))) {
        FieldProvider[] tld = null;
        try {
          ComboTableData comboTableData = new ComboTableData(info.vars, this, "TABLEDIR",
              "M_Product_UOM_ID", "", "M_Product_UOM_ID",
              Utility.getContext(this, info.vars, "#AccessibleOrgTree", "SLRequisitionLineProduct"),
              Utility.getContext(this, info.vars, "#User_Client", "SLRequisitionLineProduct"), 0);
          Utility.fillSQLParameters(this, info.vars, null, comboTableData, info.getTabId(), "");
          tld = comboTableData.select(false);
          comboTableData = null;
        } catch (Exception ex) {
          throw new ServletException(ex);
        }
        if (tld != null && tld.length > 0) {
          info.addSelect("inpmProductUomId");
          for (int i = 0; i < tld.length; i++) {
            info.addSelectResult(tld[i].getField("id"),
                FormatUtilities.replaceJS(tld[i].getField("name")), false);
          }
          info.endSelect();
        } else {
          info.addResult("inpmProductUomId", "");
        }
      }

      // To set the cursor focus in the amount field
      if (StringUtils.isNotEmpty(strMProductID)) {
        info.addResult("CURSOR_FIELD", "inpqty");
      }
    }

  }
}
