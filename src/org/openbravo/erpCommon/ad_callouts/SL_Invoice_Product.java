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
import java.math.BigDecimal;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.openbravo.base.filter.IsIDFilter;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.security.OrganizationStructureProvider;
import org.openbravo.dal.service.OBDal;
import org.openbravo.data.FieldProvider;
import org.openbravo.erpCommon.businessUtility.PAttributeSet;
import org.openbravo.erpCommon.businessUtility.PAttributeSetData;
import org.openbravo.erpCommon.businessUtility.PriceAdjustment;
import org.openbravo.erpCommon.businessUtility.Tax;
import org.openbravo.erpCommon.utility.AccDefUtility;
import org.openbravo.erpCommon.utility.ComboTableData;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.materialmgmt.UOMUtil;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.plm.Product;
import org.openbravo.utils.FormatUtilities;

public class SL_Invoice_Product extends SimpleCallout {

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    String strChanged = info.getLastFieldChanged();
    if (log4j.isDebugEnabled()) {
      log4j.debug("CHANGED: " + strChanged);
    }

    // Parameters
    String strUOM = info.getStringParameter("inpmProductId_UOM", IsIDFilter.instance);
    BigDecimal priceList = info.getBigDecimalParameter("inpmProductId_PLIST");
    BigDecimal priceStd = info.getBigDecimalParameter("inpmProductId_PSTD");
    BigDecimal priceLimit = info.getBigDecimalParameter("inpmProductId_PLIM");
    String strCurrency = info.getStringParameter("inpmProductId_CURR", IsIDFilter.instance);
    BigDecimal qty = info.getBigDecimalParameter("inpqtyinvoiced");
    String strMProductID = info.getStringParameter("inpmProductId", IsIDFilter.instance);
    String strADOrgID = info.getStringParameter("inpadOrgId", IsIDFilter.instance);
    String strCInvoiceID = info.getStringParameter("inpcInvoiceId", IsIDFilter.instance);
    String strIsSOTrx = Utility.getContext(this, info.vars, "isSOTrx", info.getWindowId());
    String strWarehouse = Utility.getContext(this, info.vars, "#M_Warehouse_ID",
        info.getWindowId());
    String strUOMProduct = info.getStringParameter("inpmProductUomId", IsIDFilter.instance);

    // Warehouse
    String strWarehouseOrg = SLOrderProductData.getWarehouseOrg(this, strWarehouse);
    String strWarehouseForOrg = "";
    final OrganizationStructureProvider osp = OBContext.getOBContext()
        .getOrganizationStructureProvider(info.vars.getClient());
    if (!StringUtils.equals(strADOrgID, strWarehouseOrg)) {
      Organization org = OBDal.getInstance().get(Organization.class, strADOrgID);
      if (strWarehouseOrg != null) {
        Organization warehouseOrg = OBDal.getInstance().get(Organization.class, strWarehouseOrg);
        if (!osp.isInNaturalTree(org, warehouseOrg) && !osp.isInNaturalTree(warehouseOrg, org)) {
          strWarehouseForOrg = SLOrderProductData.getWarehouseOfOrg(this, info.vars.getClient(),
              strADOrgID);
        }
        if (StringUtils.isNotEmpty(strWarehouseForOrg)) {
          strWarehouse = strWarehouseForOrg;
        }
      }
    }

    // Unit Of Measure
    info.addResult("inpcUomId", strUOM);

    // Prices
    BigDecimal priceActual = BigDecimal.ZERO;
    Invoice invoice = OBDal.getInstance().get(Invoice.class, strCInvoiceID);
    boolean priceIncludeTaxes = invoice.getPriceList().isPriceIncludesTax();
    if (StringUtils.isNotEmpty(strMProductID)) {
      Product product = OBDal.getInstance().get(Product.class, strMProductID);
      priceActual = PriceAdjustment.calculatePriceActual(invoice, product, qty, priceStd);
    }
    if (priceIncludeTaxes) {
      info.addResult("inpgrossUnitPrice", priceActual);
      info.addResult("inpgrosspricestd", priceStd);
      info.addResult("inpgrosspricelist", priceList);
    } else {
      info.addResult("inppricestd", priceStd);
      info.addResult("inppriceactual", priceActual);
    }
    info.addResult("inppricelist", priceList);
    info.addResult("inppricelimit", priceLimit);

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

    // Second Unit Of Measure
    String strHasSecondaryUOM = SLOrderProductData.hasSecondaryUOM(this, strMProductID);
    info.addResult("inphasseconduom", Integer.parseInt(strHasSecondaryUOM));

    // Currency
    if (StringUtils.isNotEmpty(strCurrency)) {
      info.addResult("inpcCurrencyId", strCurrency);
    }

    // Tax
    SLInvoiceTaxData[] data = SLInvoiceTaxData.select(this, strCInvoiceID);
    if (data != null && data.length > 0) {
      try {
        String strCTaxID = Tax.get(this, strMProductID, data[0].dateinvoiced, strADOrgID,
            strWarehouse, data[0].cBpartnerLocationId, data[0].cBpartnerLocationId,
            data[0].cProjectId, StringUtils.equals(strIsSOTrx, "Y"),
            StringUtils.equals(data[0].iscashvat, "Y"));
        info.addResult("inpcTaxId", strCTaxID);
      } catch (IOException e) {
        log4j.error(e.getMessage());
      }
    }

    // Isdeferred, defplantype, periodno, periodId
    if (StringUtils.isNotEmpty(strCInvoiceID)) {
      final String CURRENT_MONTH = "C";
      final String NEXT_MONTH = "N";
      // Set empty values
      String isdeferred = "N";
      String defplantype = "";
      String periodnumber = "";
      String cPeriodId = "";
      try {
        if (invoice.isSalesTransaction() && StringUtils.isNotEmpty(strMProductID)) {
          Product product = OBDal.getInstance().get(Product.class, strMProductID);
          if (product.isDeferredRevenue()) {
            isdeferred = "Y";
            defplantype = product.getRevenuePlanType();
            periodnumber = product.getPeriodNumber().toString();
            if (StringUtils.equals(product.getDefaultPeriod(), CURRENT_MONTH)) {
              cPeriodId = AccDefUtility
                  .getCurrentPeriod(invoice.getAccountingDate(),
                      AccDefUtility.getCalendar(invoice.getOrganization()))
                  .getId();
            } else if (StringUtils.equals(product.getDefaultPeriod(), NEXT_MONTH)) {
              cPeriodId = AccDefUtility
                  .getNextPeriod(AccDefUtility.getCurrentPeriod(invoice.getAccountingDate(),
                      AccDefUtility.getCalendar(invoice.getOrganization())))
                  .getId();
            }
          }
        } else if (!invoice.isSalesTransaction() && StringUtils.isNotEmpty(strMProductID)) {
          Product product = OBDal.getInstance().get(Product.class, strMProductID);
          if (product.isDeferredexpense()) {
            isdeferred = "Y";
            defplantype = product.getExpplantype();
            periodnumber = product.getPeriodnumberExp().toString();
            if (StringUtils.equals(product.getDefaultPeriodExpense(), CURRENT_MONTH)) {
              cPeriodId = AccDefUtility
                  .getCurrentPeriod(invoice.getAccountingDate(),
                      AccDefUtility.getCalendar(invoice.getOrganization()))
                  .getId();
            } else if (StringUtils.equals(product.getDefaultPeriodExpense(), NEXT_MONTH)) {
              cPeriodId = AccDefUtility
                  .getNextPeriod(AccDefUtility.getCurrentPeriod(invoice.getAccountingDate(),
                      AccDefUtility.getCalendar(invoice.getOrganization())))
                  .getId();
            }
          }
        }
      } catch (Exception e) {
        isdeferred = "N";
        defplantype = "";
        periodnumber = "";
        cPeriodId = "";
        log4j.error("Error calculating Accruals and Deferrals Plan");
      }
      info.addResult("inpisdeferred", isdeferred);
      info.addResult("inpdefplantype", defplantype);
      info.addResult("inpperiodnumber", periodnumber);
      info.addResult("inpcPeriodId", cPeriodId);
    }

    // Set AUM based on default
    if (UOMUtil.isUomManagementEnabled() && StringUtils.isEmpty(strUOMProduct)) {
      String finalAUM = UOMUtil.getDefaultAUMForDocument(strMProductID,
          invoice.getTransactionDocument().getId());
      if (finalAUM != null) {
        info.addResult("inpcAum", finalAUM);
      }
    }

    // Load Product UOM in case Second UOM exists for product
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
  }
}
