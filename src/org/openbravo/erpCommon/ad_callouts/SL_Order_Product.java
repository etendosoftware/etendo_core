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
import java.math.RoundingMode;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.openbravo.base.filter.IsIDFilter;
import org.openbravo.base.filter.ValueListFilter;
import org.openbravo.dal.service.OBDal;
import org.openbravo.data.FieldProvider;
import org.openbravo.erpCommon.businessUtility.PAttributeSet;
import org.openbravo.erpCommon.businessUtility.PAttributeSetData;
import org.openbravo.erpCommon.businessUtility.PriceAdjustment;
import org.openbravo.erpCommon.businessUtility.Tax;
import org.openbravo.erpCommon.utility.ComboTableData;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.materialmgmt.UOMUtil;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.plm.Product;
import org.openbravo.utils.FormatUtilities;

public class SL_Order_Product extends SimpleCallout {

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    String strChanged = info.getLastFieldChanged();
    if (log4j.isDebugEnabled()) {
      log4j.debug("CHANGED: " + strChanged);
    }

    // Parameters
    String strUOM = info.getStringParameter("inpmProductId_UOM", IsIDFilter.instance);
    String strCurrency = info.getStringParameter("inpmProductId_CURR", IsIDFilter.instance);
    String strMProductID = info.getStringParameter("inpmProductId", IsIDFilter.instance);
    String strCBPartnerLocationID = info.getStringParameter("inpcBpartnerLocationId",
        IsIDFilter.instance);
    String strADOrgID = info.getStringParameter("inpadOrgId", IsIDFilter.instance);
    String strMWarehouseID = info.getStringParameter("inpmWarehouseId", IsIDFilter.instance);
    String strCOrderId = info.getStringParameter("inpcOrderId", IsIDFilter.instance);
    String strIsSOTrx = Utility.getContext(this, info.vars, "isSOTrx", info.getWindowId());
    String cancelPriceAd = info.getStringParameter("inpcancelpricead",
        new ValueListFilter("Y", "N"));
    String strUOMProduct = info.getStringParameter("inpmProductUomId", IsIDFilter.instance);

    BigDecimal qtyOrdered = info.getBigDecimalParameter("inpqtyordered");
    BigDecimal priceList = info.getBigDecimalParameter("inpmProductId_PLIST");
    BigDecimal priceStd = info.getBigDecimalParameter("inpmProductId_PSTD");
    BigDecimal priceLimit = info.getBigDecimalParameter("inpmProductId_PLIM");
    BigDecimal netPriceList = priceList;
    BigDecimal grossPriceList = priceList;
    BigDecimal grossBaseUnitPrice = priceStd;

    // UOM and Prices
    BigDecimal priceActual = BigDecimal.ZERO;
    Order order = OBDal.getInstance().get(Order.class, strCOrderId);
    boolean isTaxIncludedPriceList = order.getPriceList().isPriceIncludesTax();

    if (StringUtils.isNotEmpty(strMProductID)) {
      Product product = OBDal.getInstance().get(Product.class, strMProductID);
      if (!StringUtils.equals(cancelPriceAd, "Y")) {
        if (isTaxIncludedPriceList) {
          priceActual = PriceAdjustment.calculatePriceActual(order, product, qtyOrdered,
              grossBaseUnitPrice);
          netPriceList = BigDecimal.ZERO;
        } else {
          priceActual = PriceAdjustment.calculatePriceActual(order, product, qtyOrdered, priceStd);
          grossPriceList = BigDecimal.ZERO;
        }
      } else {
        priceActual = isTaxIncludedPriceList ? grossBaseUnitPrice : priceList;
      }
    } else {
      strUOM = "";
      netPriceList = grossPriceList = priceLimit = priceStd = BigDecimal.ZERO;
    }

    // UOM
    info.addResult("inpcUomId", strUOM);

    // Prices
    if (isTaxIncludedPriceList) {
      info.addResult("inpgrossUnitPrice", priceActual);
      info.addResult("inpgrosspricelist", grossPriceList);
      info.addResult("inpgrosspricestd", grossBaseUnitPrice);
    } else {
      info.addResult("inppricelist", netPriceList);
      info.addResult("inppricelimit", priceLimit);
      info.addResult("inppricestd", priceStd);
      info.addResult("inppriceactual", priceActual);
    }

    // Currency
    if (StringUtils.isNotEmpty(strCurrency)) {
      info.addResult("inpcCurrencyId", strCurrency);
    }

    // Discount
    BigDecimal discount = BigDecimal.ZERO;
    BigDecimal price = isTaxIncludedPriceList ? grossPriceList : netPriceList;
    BigDecimal priceToSubtract = isTaxIncludedPriceList ? grossBaseUnitPrice : priceStd;
    if (price.compareTo(BigDecimal.ZERO) != 0) {
      discount = price.subtract(priceToSubtract)
          .multiply(new BigDecimal("100"))
          .divide(price, 2, RoundingMode.HALF_UP);
    }
    info.addResult("inpdiscount", discount);

    // Attribute Set Instance
    if (StringUtils.isNotEmpty(strMProductID)) {
      PAttributeSetData[] dataPAttr = PAttributeSetData.selectProductAttr(this, strMProductID);
      if (dataPAttr != null && dataPAttr.length > 0
          && StringUtils.equals(dataPAttr[0].attrsetvaluetype, "D")) {
        PAttributeSetData[] data2 = PAttributeSetData.select(this, dataPAttr[0].mAttributesetId);
        if (PAttributeSet.isInstanceAttributeSet(data2)) {
          info.addResult("inpmAttributesetinstanceId", "");
        } else {
          info.addResult("inpmAttributesetinstanceId", dataPAttr[0].mAttributesetinstanceId);
        }
        info.addResult("inpattributeset", FormatUtilities.replaceJS(dataPAttr[0].mAttributesetId));
        info.addResult("inpattrsetvaluetype",
            FormatUtilities.replaceJS(dataPAttr[0].attrsetvaluetype));
      } else {
        info.addResult("inpmAttributesetinstanceId", "");
        info.addResult("inpattributeset", "");
        info.addResult("inpattrsetvaluetype", "");
      }
    }

    // Organization Location and Tax
    String orgLocationID = SLOrderProductData.getOrgLocationId(this,
        Utility.getContext(this, info.vars, "#User_Client", "SLOrderProduct"),
        "'" + strADOrgID + "'");
    if (StringUtils.isEmpty(orgLocationID)) {
      info.showMessage(FormatUtilities.replaceJS(
          Utility.messageBD(this, "NoLocationNoTaxCalculated", info.vars.getLanguage())));
    } else {
      SLOrderTaxData[] data = SLOrderTaxData.select(this, strCOrderId);
      if (data != null && data.length > 0) {
        try {
          String strCTaxID = Tax.get(this, strMProductID, data[0].dateordered, strADOrgID,
              strMWarehouseID,
              (StringUtils.isEmpty(data[0].billtoId) ? strCBPartnerLocationID : data[0].billtoId),
              strCBPartnerLocationID, data[0].cProjectId, StringUtils.equals(strIsSOTrx, "Y"),
              StringUtils.equals(data[0].iscashvat, "Y"));
          info.addResult("inpcTaxId", strCTaxID);
        } catch (Exception e) {
          log4j.error(e.getMessage());
          throw new ServletException(e.getMessage());
        }
      }
    }

    // Has Second UOM
    String strHasSecondaryUOM = SLOrderProductData.hasSecondaryUOM(this, strMProductID);
    info.addResult("inphasseconduom", Integer.parseInt(strHasSecondaryUOM));

    // Set AUM based on default
    if (UOMUtil.isUomManagementEnabled() && StringUtils.isEmpty(strUOMProduct)) {
      String finalAUM = UOMUtil.getDefaultAUMForDocument(strMProductID,
          order.getTransactionDocument().getId());
      if (StringUtils.isNotEmpty(finalAUM)) {
        info.addResult("inpcAum", finalAUM);
      }
    }

    // Load Product UOM if product has Second UOM
    if (StringUtils.equals(strHasSecondaryUOM, "1") && (!UOMUtil.isUomManagementEnabled()
        || (UOMUtil.isUomManagementEnabled() && StringUtils.isNotEmpty(strUOMProduct)))) {
      FieldProvider[] tld = null;
      try {
        ComboTableData comboTableData = new ComboTableData(info.vars, this, "TABLE", "",
            "M_Product_UOM", "",
            Utility.getContext(this, info.vars, "#AccessibleOrgTree", "SLOrderProduct"),
            Utility.getContext(this, info.vars, "#User_Client", "SLOrderProduct"), 0);
        Utility.fillSQLParameters(this, info.vars, null, comboTableData, "SLOrderProduct", "");
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

    // Move cursor to quantity field
    info.addResult("CURSOR_FIELD", "inpqtyordered");
    if (!StringUtils.equals(strHasSecondaryUOM, "0")) {
      info.addResult("CURSOR_FIELD", "inpquantityorder");
    }
  }
}
