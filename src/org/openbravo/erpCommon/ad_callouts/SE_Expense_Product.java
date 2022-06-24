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
import org.openbravo.dal.security.OrganizationStructureProvider;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.DateTimeData;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.timeandexpense.Sheet;

public class SE_Expense_Product extends SimpleCallout {

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    String strChanged = info.getLastFieldChanged();
    if (log4j.isDebugEnabled()) {
      log4j.debug("CHANGED: " + strChanged);
    }

    // Parameters
    String strDateexpense = info.getStringParameter("inpdateexpense");
    String strmProductId = info.getStringParameter("inpmProductId", IsIDFilter.instance);
    String strsTimeexpenseId = info.getStringParameter("inpsTimeexpenseId", IsIDFilter.instance);
    String strcCurrencyId = info.getStringParameter("inpcCurrencyId", IsIDFilter.instance);
    String strInvPrice = info.vars.getNumericParameter("inpinvoiceprice");
    String strWindowId = info.vars.getStringParameter("inpwindowId", IsIDFilter.instance);
    String strlastNetUnitPrice = info.vars.getNumericGlobalVariable("inplastNetUnitPrice",
        strWindowId + "|lastNetUnitPrice", "0");
    BigDecimal qty = info.getBigDecimalParameter("inpqty");

    if (StringUtils.equals(strlastNetUnitPrice, "0")) {
      strlastNetUnitPrice = strInvPrice;
    }

    BigDecimal invPrice = StringUtils.isNotEmpty(strInvPrice) ? new BigDecimal(strInvPrice)
        : BigDecimal.ZERO;
    BigDecimal lastNetUnitPrice = StringUtils.isNotEmpty(strlastNetUnitPrice)
        ? new BigDecimal(strlastNetUnitPrice)
        : BigDecimal.ZERO;
    String strmPricelistId = SEExpenseProductData.priceList(this, strsTimeexpenseId);
    SEExpenseProductData[] data = SEExpenseProductData.select(this, strmProductId, strmPricelistId);
    if (StringUtils.isEmpty(strDateexpense)) {
      strDateexpense = StringUtils.isEmpty(
          SEExpenseProductData.selectReportDate(this, strsTimeexpenseId)) ? DateTimeData.today(this)
              : SEExpenseProductData.selectReportDate(this, strsTimeexpenseId);
    }

    // Search for price
    boolean noPrice = true;
    String priceActual = "";
    String cCurrencyID = "";
    if (StringUtils.isEmpty(strInvPrice) || (StringUtils.equals(strChanged, "inpmProductId")
        && invPrice.compareTo(lastNetUnitPrice) == 0)) {
      for (int i = 0; data != null && i < data.length && noPrice; i++) {
        if (StringUtils.isEmpty(data[i].validfrom) || !StringUtils
            .equals(DateTimeData.compare(this, strDateexpense, data[i].validfrom), "-1")) {
          noPrice = false;
          // Price
          priceActual = data[i].pricestd;
          if (StringUtils.isEmpty(priceActual)) {
            priceActual = StringUtils.isNotEmpty(data[i].pricelist) ? data[i].pricelist
                : data[i].pricelimit;
          }
          // Currency
          cCurrencyID = data[i].cCurrencyId;
        }
      }
      if (noPrice) {
        data = SEExpenseProductData.selectBasePriceList(this, strmProductId, strmPricelistId);
        for (int i = 0; data != null && i < data.length && noPrice; i++) {
          if (StringUtils.isEmpty(data[i].validfrom) || !StringUtils
              .equals(DateTimeData.compare(this, strDateexpense, data[i].validfrom), "-1")) {
            noPrice = false;
            // Price
            priceActual = data[i].pricestd;
            if (StringUtils.isEmpty(priceActual)) {
              priceActual = StringUtils.isNotEmpty(data[i].pricelist) ? data[i].pricelist
                  : data[i].pricelimit;
            }
            // Currency
            cCurrencyID = data[i].cCurrencyId;
          }
        }
      }
      info.vars.setSessionValue(strWindowId + "|lastNetUnitPrice", priceActual);
    } else {
      priceActual = strInvPrice;
    }

    if (StringUtils.equals(strChanged, "inpqty") || StringUtils.isEmpty(cCurrencyID)) {
      cCurrencyID = strcCurrencyId;
    }

    int stdPrecision = 0;
    if (StringUtils.isNotEmpty(cCurrencyID)) {
      stdPrecision = Integer.valueOf(SEExpenseProductData.selectPrecision(this, cCurrencyID));
    }

    // Update the UOM
    String strUOM = SEExpenseProductData.selectUOM(this, strmProductId);
    info.addResult("inpcUomId", strUOM);

    // Calculate the Price and Expense Amount
    BigDecimal price = BigDecimal.ZERO;
    BigDecimal amount = BigDecimal.ZERO;
    if (StringUtils.isNotEmpty(priceActual)) {
      price = new BigDecimal(priceActual);
      amount = price.multiply(qty);
    }
    if (amount.scale() > stdPrecision) {
      amount = amount.setScale(stdPrecision, RoundingMode.HALF_UP);
    }
    info.addResult("inpexpenseamt", amount);
    info.addResult("inpinvoiceprice", price);

    // Get currency to from org's currency or client's currency if it doesn't exists
    final Organization org = OBDal.getInstance()
        .get(Sheet.class, strsTimeexpenseId)
        .getOrganization();
    String c_Currency_To_ID = getCurrency(org.getId());
    if (c_Currency_To_ID == null) {
      c_Currency_To_ID = OBDal.getInstance()
          .get(Sheet.class, strsTimeexpenseId)
          .getClient()
          .getCurrency()
          .getId();
    }

    // Checks if there is a conversion rate for each of the transactions of
    // the report
    if (StringUtils.isNotEmpty(cCurrencyID)) {
      BigDecimal convAmount = amount;
      if (!StringUtils.equals(cCurrencyID, c_Currency_To_ID)) {
        String convertedAmount = amount.toPlainString();
        try {
          convertedAmount = SEExpenseProductData.selectConvertedAmt(this, amount.toPlainString(),
              cCurrencyID, c_Currency_To_ID, strDateexpense, info.vars.getClient(), org.getId());
        } catch (Exception e) {
          convertedAmount = "";
          OBDal.getInstance().rollbackAndClose();
          info.showMessage(
              Utility.translateError(this, info.vars, info.vars.getLanguage(), e.getMessage())
                  .getMessage());
          log4j.warn("Currency does not exist. Exception:" + e);
        }
        convAmount = StringUtils.isNotEmpty(convertedAmount) ? new BigDecimal(convertedAmount)
            : BigDecimal.ZERO;
        int stdPrecisionConv = 0;
        if (StringUtils.isNotEmpty(c_Currency_To_ID)) {
          stdPrecisionConv = Integer
              .valueOf(SEExpenseProductData.selectPrecision(this, c_Currency_To_ID));
        }
        if (convAmount.scale() > stdPrecisionConv) {
          convAmount = convAmount.setScale(stdPrecisionConv, RoundingMode.HALF_UP);
        }
      }
      // Update Converted Amount
      info.addResult("inpconvertedamt",
          convAmount.compareTo(BigDecimal.ZERO) != 0 ? convAmount : null);
    }

    // If the product was changed, then update the currency
    if (StringUtils.equals(strChanged, "inpmProductId") && StringUtils.isNotEmpty(cCurrencyID)) {
      info.addResult("inpcCurrencyId", cCurrencyID);
    }
  }

  private static String getCurrency(String org) {
    if (StringUtils.equals(org, "0")) {
      return null;
    } else {
      Organization organization = OBDal.getInstance().get(Organization.class, org);
      if (organization.getCurrency() != null) {
        return organization.getCurrency().getId();
      } else {
        return getCurrency(new OrganizationStructureProvider().getParentOrg(org));
      }
    }
  }

}
