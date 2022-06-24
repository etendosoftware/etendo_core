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
 * All portions are Copyright (C) 2012-2018 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.erpCommon.ad_process;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.time.DateUtils;
import org.openbravo.advpaymentmngt.utility.FIN_Utility;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.common.hooks.ConvertQuotationIntoOrderHookManager;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderDiscount;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.order.OrderLineOffer;
import org.openbravo.model.financialmgmt.tax.TaxRate;
import org.openbravo.model.pricing.pricelist.PriceListVersion;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.CallStoredProcedure;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.service.db.DbUtility;

public class ConvertQuotationIntoOrder extends DalBaseProcess {

  @Override
  public void doExecute(ProcessBundle bundle) throws Exception {

    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    boolean recalculatePrices = "N"
        .equals(vars.getStringParameter("inprecalculateprices", false, "N"));
    String quotationId = (String) bundle.getParams().get("C_Order_ID");

    try {
      Order salesOrder = convertQuotationIntoSalesOrder(recalculatePrices, quotationId);
      OBError msg = OBErrorBuilder.buildMessage(null, "success",
          "@SalesOrderDocumentno@ " + salesOrder.getDocumentNo() + " @beenCreated@");
      bundle.setResult(msg);
      bundle.getParams().put("SalesOrderId", salesOrder.getId());
      return;
    } catch (OBException e) {
      final OBError error = OBErrorBuilder.buildMessage(null, "error", e.getMessage());
      bundle.setResult(error);
    } catch (Exception e) {
      Throwable t = DbUtility.getUnderlyingSQLException(e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), t.getMessage());
      bundle.setResult(error);
    }
  }

  /**
   * Converts a Quotation Into a Sales Order based on the given ID
   * <ul>
   * <li>1. Creates a Sales Order in Draft Status based on a clone of the Quotation</li>
   * <li>2. Sets the proper Document Type to the new Sales Order</li>
   * <li>3. Sets the proper values to the Sales Order header</li>
   * <li>4. For each line that is not a discount it creates a clone into the Sales Order
   * Document</li>
   * <li>5. If the parameter recalculatePrices is true, it recalculates the prices</li>
   * <li>6. Recalculates the discounts for each line</li>
   * <li>7. Calls C_Order_Post and updates the status of the Quotation to Already Converted to Order
   * </li>
   * </ul>
   * 
   * @param recalculatePrices
   *          If true, the prices of the new Sales Order line will be recalculated. If false, it
   *          will be the same prices as the Quotation
   * @param quotationId
   *          The Id of the Quotation
   * @return An OBError message with the result message
   */
  public Order convertQuotationIntoSalesOrder(final boolean recalculatePrices,
      final String quotationId) {
    // Create Sales Order
    Order quotation = OBDal.getInstance().get(Order.class, quotationId);
    Order newSalesOrder = (Order) DalUtil.copy(quotation, false);

    if (FIN_Utility.isBlockedBusinessPartner(quotation.getBusinessPartner().getId(), true, 1)) {
      // If the Business Partner is blocked, the Order should not be completed.
      String message = OBMessageUtils.messageBD("ThebusinessPartner") + " "
          + quotation.getBusinessPartner().getIdentifier() + " "
          + OBMessageUtils.messageBD("BusinessPartnerBlocked");
      OBDal.getInstance().rollbackAndClose();
      throw new OBException(message);
    }

    // Set status of the new Order to Draft and Processed = N
    newSalesOrder.setDocumentAction("CO");
    newSalesOrder.setDocumentStatus("DR");
    newSalesOrder.setProcessed(false);
    newSalesOrder.setPosted("N");

    // Set the Sales Order Document Type
    DocumentType docType = newSalesOrder.getDocumentType().getDocumentTypeForOrder();
    if (docType == null) {
      OBDal.getInstance().rollbackAndClose();
      String message = OBMessageUtils.messageBD("@NoOrderDocType@");
      throw new OBException(message);
    }

    // Set values of the Sales Order Header
    newSalesOrder.setDocumentType(docType);
    newSalesOrder.setTransactionDocument(docType);
    newSalesOrder.setProcessed(false);
    newSalesOrder.setSalesTransaction(true);
    newSalesOrder.setDocumentNo(null);
    newSalesOrder.setOrderDate(DateUtils.truncate(new Date(), Calendar.DATE));
    newSalesOrder.setRejectReason(null);
    newSalesOrder.setValidUntil(null);
    newSalesOrder.setSummedLineAmount(BigDecimal.ZERO);
    newSalesOrder.setGrandTotalAmount(BigDecimal.ZERO);
    newSalesOrder.setQuotation(quotation);
    OBDal.getInstance().save(newSalesOrder);
    OBDal.getInstance().flush();
    OBDal.getInstance().refresh(newSalesOrder);

    Map<String, BigDecimal> taxForDiscounts = new HashMap<String, BigDecimal>();
    int lineNo = 10;
    StringBuilder strMessage = new StringBuilder();

    // Copy the Lines of the Quotation in the new Sales Order.
    for (OrderLine quotationLine : quotation.getOrderLineList()) {
      if (quotationLine.getOrderDiscount() != null) {
        // If the line is a discount line do not copy it
        continue;
      }

      // Copy line to the new Sales Order
      OrderLine newSalesOrderLine = (OrderLine) DalUtil.copy(quotationLine, false);

      String strCTaxID = newSalesOrderLine.getTax().getId();
      TaxRate lineTax = OBDal.getInstance().get(TaxRate.class, strCTaxID);

      if (lineTax == null) {
        if (strMessage.length() > 0) {
          strMessage = strMessage.append(", ");
        }
        strMessage = strMessage.append(lineNo);
      }

      // Update the HashMap of the Taxes. HashMap<TaxId, TotalAmount>
      BigDecimal price = BigDecimal.ZERO;
      try {
        OBContext.setAdminMode(true);
        if (newSalesOrder.getPriceList().isPriceIncludesTax()) {
          price = newSalesOrderLine.getLineGrossAmount();
        } else {
          price = newSalesOrderLine.getLineNetAmount();
        }
      } finally {
        OBContext.restorePreviousMode();
      }
      if (taxForDiscounts.containsKey(strCTaxID)) {
        taxForDiscounts.put(strCTaxID, taxForDiscounts.get(strCTaxID).add(price));
      } else {
        taxForDiscounts.put(strCTaxID, price);
      }

      if (recalculatePrices) {
        try {
          OBContext.setAdminMode(true);
          recalculatePrices(quotation, quotationLine, newSalesOrder, newSalesOrderLine);
        } finally {
          OBContext.restorePreviousMode();
        }
      } else {
        for (OrderLineOffer quotationLineOffer : quotationLine.getOrderLineOfferList()) {
          // Copy Promotions and Discounts.
          OrderLineOffer newSalesOrderLineOffer = (OrderLineOffer) DalUtil.copy(quotationLineOffer,
              false);
          newSalesOrderLineOffer.setSalesOrderLine(newSalesOrderLine);
          newSalesOrderLine.getOrderLineOfferList().add(newSalesOrderLineOffer);
        }
      }
      // Set last values of new Sales Order line
      newSalesOrderLine.setSalesOrder(newSalesOrder);
      newSalesOrderLine.setReservedQuantity(BigDecimal.ZERO);
      newSalesOrderLine.setDeliveredQuantity(BigDecimal.ZERO);
      newSalesOrderLine.setInvoicedQuantity(BigDecimal.ZERO);
      newSalesOrderLine.setQuotationLine(quotationLine);
      newSalesOrder.getOrderLineList().add(newSalesOrderLine);
      lineNo = lineNo + 10;
    }

    if (strMessage.length() > 0) {
      OBDal.getInstance().rollbackAndClose();
      String message = "@TaxCategoryWithoutTaxRate@".concat(strMessage.toString());
      throw new OBException(message);
    }
    OBDal.getInstance().flush();
    OBDal.getInstance().refresh(newSalesOrder);

    // Delete created discounts for Order
    for (OrderDiscount disCloneLine : newSalesOrder.getOrderDiscountList()) {
      OBDal.getInstance().remove(disCloneLine);
    }
    newSalesOrder.getOrderDiscountList().clear();

    OBDal.getInstance().flush();
    OBDal.getInstance().refresh(newSalesOrder);

    BigDecimal cumulativeDiscount = new BigDecimal(100);

    // Create the discounts to be able to add the appropriate discount_id in c_orderline
    for (OrderDiscount quotationDiscount : quotation.getOrderDiscountList()) {
      // Copy discounts
      OrderDiscount newSalesOrderDiscount = (OrderDiscount) DalUtil.copy(quotationDiscount, false);
      newSalesOrderDiscount.setSalesOrder(newSalesOrder);
      newSalesOrder.getOrderDiscountList().add(newSalesOrderDiscount);
      if (!recalculatePrices) {
        // Copy the Invoice Lines that are created from the Discounts
        Iterator<Entry<String, BigDecimal>> it = taxForDiscounts.entrySet().iterator();
        OBDal.getInstance().flush();
        try {
          OBContext.setAdminMode(true);
          while (it.hasNext()) {
            Map.Entry<String, BigDecimal> taxesWithPrices = it.next();
            BigDecimal discountAmount;

            if (newSalesOrderDiscount.isCascade()) {
              discountAmount = newSalesOrderDiscount.getDiscount().getDiscount();
              discountAmount = cumulativeDiscount.multiply(discountAmount)
                  .divide(new BigDecimal(100));

            } else {
              discountAmount = newSalesOrderDiscount.getDiscount().getDiscount();
            }
            cumulativeDiscount = cumulativeDiscount.subtract(discountAmount);

            OrderLine olDiscount = generateOrderLineDiscount(taxesWithPrices, newSalesOrderDiscount,
                quotation, newSalesOrder, lineNo, discountAmount);
            lineNo = lineNo + 10;
            newSalesOrder.getOrderLineList().add(olDiscount);
          }
        } finally {
          OBContext.restorePreviousMode();
        }
      }
    }

    OBDal.getInstance().flush();
    OBDal.getInstance().refresh(newSalesOrder);

    // Hook entry point
    executeHooks(newSalesOrder);

    // If prices are going to be recalculated, call C_Order_Post
    callCOrderPost(newSalesOrder, recalculatePrices);
    OBDal.getInstance().flush();
    OBDal.getInstance().refresh(newSalesOrder);

    // Set the Status of the Quotation to Closed - Converted
    quotation.setDocumentStatus("CA");

    OBDal.getInstance().save(quotation);
    OBDal.getInstance().save(newSalesOrder);

    OBDal.getInstance().flush();
    OBDal.getInstance().refresh(newSalesOrder);
    OBDal.getInstance().refresh(quotation);

    return newSalesOrder;
  }

  /**
   * Given an Order Line and a Clone Order Line, it recalculates the prices of the second one
   */
  private void recalculatePrices(Order quotation, OrderLine quotationLine, Order newSalesOrder,
      OrderLine newSalesOrderLine) {

    String strPriceVersionId = getPriceListVersion(quotation.getPriceList().getId(),
        quotation.getClient().getId(), newSalesOrder.getOrderDate());
    BigDecimal bdPriceList = getPriceList(quotationLine.getProduct().getId(), strPriceVersionId);
    BigDecimal bdPriceStd = getPriceStd(quotationLine.getProduct().getId(), strPriceVersionId);

    if (bdPriceList != null && bdPriceList.compareTo(BigDecimal.ZERO) != 0) {
      // List Price
      if (quotation.getPriceList().isPriceIncludesTax()) {
        // If is Price Including Taxes, change only gross
        newSalesOrderLine.setGrossListPrice(bdPriceList);
        newSalesOrderLine.setListPrice(BigDecimal.ZERO);
      } else {
        // If is not Price Including Taxes, change only net
        newSalesOrderLine.setListPrice(bdPriceList);
      }
    }

    if (bdPriceStd != null && bdPriceStd.compareTo(BigDecimal.ZERO) != 0) {
      // Unit Price
      if (quotation.getPriceList().isPriceIncludesTax()) {
        // If is Price Including Taxes, change only gross
        newSalesOrderLine.setGrossUnitPrice(bdPriceStd);
        newSalesOrderLine.setUnitPrice(BigDecimal.ZERO);
      } else {
        // If is not Price Including Taxes, change only net
        newSalesOrderLine.setUnitPrice(bdPriceStd);
      }
    }

    // Discount
    if (bdPriceList == null) {
      bdPriceList = BigDecimal.ZERO;
    }
    if (bdPriceStd == null) {
      bdPriceStd = BigDecimal.ZERO;
    }
    BigDecimal discount = BigDecimal.ZERO;
    if (bdPriceList.compareTo(BigDecimal.ZERO) != 0) {
      discount = bdPriceList.subtract(bdPriceStd)
          .multiply(new BigDecimal("100"))
          .divide(bdPriceList, newSalesOrder.getCurrency().getStandardPrecision().intValue(),
              RoundingMode.HALF_EVEN);
    }
    newSalesOrderLine.setDiscount(discount);
    // Line Price
    if (quotation.getPriceList().isPriceIncludesTax()) {
      // If is Price Including Taxes, change gross
      newSalesOrderLine.setLineGrossAmount(
          newSalesOrderLine.getGrossUnitPrice().multiply(newSalesOrderLine.getOrderedQuantity()));
    }
    newSalesOrderLine.setLineNetAmount(
        newSalesOrderLine.getUnitPrice().multiply(newSalesOrderLine.getOrderedQuantity()));
  }

  /**
   * Call C_Order_Post
   */
  private void callCOrderPost(Order newSalesOrder, boolean recalculatePrices) {
    try {
      final List<Object> parameters = new ArrayList<Object>();
      parameters.add(null);
      parameters.add(newSalesOrder.getId());
      parameters.add(recalculatePrices ? "Y" : "N");
      final String procedureName = "c_order_post1";
      CallStoredProcedure.getInstance().call(procedureName, parameters, null, true, false);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  /**
   * Get the Current version of a price list
   */
  private String getPriceListVersion(String priceList, String clientId, Date orderDate) {
    try {
      String whereClause = " as plv left outer join plv.priceList pl where pl.active='Y' and plv.active='Y' and "
          + " pl.id = :priceList and plv.client.id = :clientId and plv.validFromDate<= :orderDate  order by plv.validFromDate desc";

      OBQuery<PriceListVersion> ppriceListVersion = OBDal.getInstance()
          .createQuery(PriceListVersion.class, whereClause);
      ppriceListVersion.setNamedParameter("priceList", priceList);
      ppriceListVersion.setNamedParameter("clientId", clientId);
      ppriceListVersion.setNamedParameter("orderDate", orderDate);
      ppriceListVersion.setMaxResult(1);

      if (!ppriceListVersion.list().isEmpty()) {
        return ppriceListVersion.list().get(0).getId();
      } else {
        return "0";
      }
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  /**
   * Call Database Procedure to get the List Price of a Product
   */
  private BigDecimal getPriceList(String strProductID, String strPriceVersionId) {
    BigDecimal bdPriceList = null;
    try {
      final List<Object> parameters = new ArrayList<Object>();
      parameters.add(strProductID);
      parameters.add(strPriceVersionId);
      final String procedureName = "M_BOM_PriceList";
      bdPriceList = (BigDecimal) CallStoredProcedure.getInstance()
          .call(procedureName, parameters, null);
    } catch (Exception e) {
      throw new OBException(e);
    }

    return bdPriceList;
  }

  /**
   * Call Database Procedure to get the Standard Price of a Product
   */
  private BigDecimal getPriceStd(String strProductID, String strPriceVersionId) {
    BigDecimal bdPriceList = null;
    try {
      final List<Object> parameters = new ArrayList<Object>();
      parameters.add(strProductID);
      parameters.add(strPriceVersionId);
      final String procedureName = "M_BOM_PriceStd";
      bdPriceList = (BigDecimal) CallStoredProcedure.getInstance()
          .call(procedureName, parameters, null);
    } catch (Exception e) {
      throw new OBException(e);
    }

    return bdPriceList;
  }

  /**
   * Create a new Invoice Line related to the Discount.
   */
  private OrderLine generateOrderLineDiscount(Entry<String, BigDecimal> taxesWithPrices,
      OrderDiscount newSalesOrderDiscount, Order quotation, Order newSalesOrder, int lineNo,
      BigDecimal discountAmount) {

    BigDecimal amount = taxesWithPrices.getValue();
    BigDecimal discountedAmount = amount.multiply(discountAmount).divide(new BigDecimal(100));

    OrderLine olDiscount = OBProvider.getInstance().get(OrderLine.class);
    olDiscount.setOrderDiscount(newSalesOrderDiscount);
    olDiscount.setTax(OBDal.getInstance().get(TaxRate.class, taxesWithPrices.getKey()));
    if (quotation.getPriceList().isPriceIncludesTax()) {
      olDiscount.setGrossUnitPrice(discountedAmount.negate());
      olDiscount.setLineGrossAmount(discountedAmount.negate());
      olDiscount.setGrossListPrice(discountedAmount.negate());
      olDiscount.setUnitPrice(BigDecimal.ZERO);
      olDiscount.setLineNetAmount(BigDecimal.ZERO);
      olDiscount.setListPrice(BigDecimal.ZERO);
    } else {
      olDiscount.setUnitPrice(discountedAmount.negate());
      olDiscount.setLineNetAmount(discountedAmount.negate());
      olDiscount.setListPrice(discountedAmount.negate());
    }

    olDiscount.setSalesOrder(newSalesOrder);
    olDiscount.setReservedQuantity(BigDecimal.ZERO);
    olDiscount.setDeliveredQuantity(BigDecimal.ZERO);
    olDiscount.setInvoicedQuantity(BigDecimal.ZERO);
    olDiscount.setOrganization(newSalesOrderDiscount.getOrganization());
    olDiscount.setLineNo((long) lineNo);
    olDiscount.setOrderDate(new Date());
    olDiscount.setWarehouse(newSalesOrder.getWarehouse());
    olDiscount.setUOM(newSalesOrderDiscount.getDiscount().getProduct().getUOM());
    olDiscount.setCurrency(newSalesOrder.getCurrency());
    olDiscount.setProduct(newSalesOrderDiscount.getDiscount().getProduct());
    olDiscount.setDescription(newSalesOrderDiscount.getDiscount().getProduct().getName());
    return olDiscount;
  }

  private void executeHooks(Order salesOrder) {
    ConvertQuotationIntoOrderHookManager manager = WeldUtils
        .getInstanceFromStaticBeanManager(ConvertQuotationIntoOrderHookManager.class);

    manager.executeHooks(salesOrder);
  }

}
