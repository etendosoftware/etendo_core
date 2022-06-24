/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.0  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2014-2017 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */
package org.openbravo.costing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.costing.CostingServer.TrxType;
import org.openbravo.dal.security.OrganizationStructureProvider;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.financial.FinancialUtils;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.materialmgmt.cost.CostAdjustment;
import org.openbravo.model.materialmgmt.transaction.MaterialTransaction;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.openbravo.model.procurement.ReceiptInvoiceMatch;

public class PriceDifferenceProcess {
  private static CostAdjustment costAdjHeader = null;

  private static boolean calculateTransactionPriceDifferenceLogic(Organization legalOrganization,
      MaterialTransaction materialTransaction) throws OBException {
    boolean costAdjCreated = false;

    // Without algorithm or with Standard Algorithm, no cost adjustment is needed
    if (materialTransaction.getCostingAlgorithm() == null
        || StringUtils.equals(materialTransaction.getCostingAlgorithm().getJavaClassName(),
            "org.openbravo.costing.StandardAlgorithm")) {
      return false;
    }

    if (materialTransaction.isCostPermanent()) {
      // Permanently adjusted transaction costs are not checked for price differences.
      return false;
    }
    Currency trxCurrency = materialTransaction.getCurrency();
    Organization trxOrg = materialTransaction.getOrganization();
    Date trxDate = materialTransaction.getMovementDate();
    int costCurPrecission = trxCurrency.getCostingPrecision().intValue();
    ShipmentInOutLine receiptLine = materialTransaction.getGoodsShipmentLine();
    if (receiptLine == null
        || !isValidPriceAdjTrx(receiptLine.getMaterialMgmtMaterialTransactionList().get(0))) {
      // We can only adjust cost of receipt lines.
      return false;
    }

    BigDecimal receiptQty = receiptLine.getMovementQuantity();
    boolean isNegativeReceipt = receiptQty.signum() == -1;
    if (isNegativeReceipt) {
      // If the receipt is negative convert the quantity to positive.
      receiptQty = receiptQty.negate();
    }

    Date costAdjDateAcct = null;

    // Calculate current transaction unit cost including existing adjustments.
    BigDecimal currentTrxCost = CostAdjustmentUtils.getTrxCost(materialTransaction, true,
        trxCurrency);

    // Calculate expected transaction unit cost based on current invoice amounts and purchase price.
    BigDecimal expectedCost = BigDecimal.ZERO;
    BigDecimal invoiceQty = BigDecimal.ZERO;
    for (ReceiptInvoiceMatch matchInv : receiptLine.getProcurementReceiptInvoiceMatchList()) {
      Invoice invoice = matchInv.getInvoiceLine().getInvoice();
      if (invoiceIsNotVoidedAndIsProcessed(invoice)) {
        invoiceQty = calculateInvoiceQuantity(isNegativeReceipt, invoiceQty, matchInv);
        expectedCost = calculateExpectedCost(trxCurrency, trxOrg, trxDate, expectedCost, matchInv,
            invoice);
        costAdjDateAcct = getCostAdjustmentDate(costAdjDateAcct, invoice);
      }
    }

    BigDecimal notInvoicedQty = receiptQty.subtract(invoiceQty);
    if (notInvoicedQty.signum() > 0) {
      // Not all the receipt line is invoiced, add pending invoice quantity valued with current
      // order price if exists or original unit cost.
      BigDecimal basePrice = BigDecimal.ZERO;
      Currency baseCurrency = trxCurrency;
      if (receiptLine.getSalesOrderLine() != null) {
        basePrice = receiptLine.getSalesOrderLine().getUnitPrice();
        baseCurrency = receiptLine.getSalesOrderLine().getSalesOrder().getCurrency();
      } else {
        basePrice = materialTransaction.getTransactionCost()
            .divide(receiptQty, costCurPrecission, RoundingMode.HALF_UP);
      }
      BigDecimal baseAmt = notInvoicedQty.multiply(basePrice)
          .setScale(costCurPrecission, RoundingMode.HALF_UP);
      if (!baseCurrency.getId().equals(trxCurrency.getId())) {
        baseAmt = FinancialUtils.getConvertedAmount(baseAmt, baseCurrency, trxCurrency, trxDate,
            trxOrg, FinancialUtils.PRECISION_STANDARD);
      }
      expectedCost = expectedCost.add(baseAmt);
    }

    // Since expected Cost already takes into account invoiced and not invoiced amount, expected
    // Quantity must take into account the same. If there is more invoiced quantity than received
    // quantity, then the invoiced quantity is used, else the recived quantity
    BigDecimal expectedQty = invoiceQty.compareTo(receiptQty) >= 0 ? invoiceQty : receiptQty;
    BigDecimal expectedUnitCost = expectedQty.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
        : expectedCost.divide(expectedQty, costCurPrecission, RoundingMode.HALF_UP);
    BigDecimal currentUnitCost = receiptQty.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
        : currentTrxCost.divide(receiptQty, costCurPrecission, RoundingMode.HALF_UP);

    // if the sum of trx costs with flag "isInvoiceCorrection" is distinct that the amount cost
    // generated by Match Invoice then New Cost Adjustment line is created by the difference
    if (expectedUnitCost.compareTo(currentUnitCost) != 0) {
      if (costAdjDateAcct == null) {
        costAdjDateAcct = trxDate;
      }
      createCostAdjustmenHeader(legalOrganization);

      BigDecimal trxCostDifference = (expectedUnitCost.multiply(receiptQty))
          .subtract(currentTrxCost);

      final CostAdjustmentLineParameters lineParameters = new CostAdjustmentLineParameters(
          materialTransaction, trxCostDifference, costAdjHeader);
      lineParameters.setSource(true);
      CostAdjustmentUtils.insertCostAdjustmentLine(lineParameters, costAdjDateAcct);
      costAdjCreated = true;
    }

    return costAdjCreated;
  }

  private static boolean calculateTransactionPriceDifference(Organization legalOrganization,
      MaterialTransaction materialTransaction) throws OBException {

    boolean costAdjCreated = calculateTransactionPriceDifferenceLogic(legalOrganization,
        materialTransaction);

    materialTransaction.setCheckpricedifference(Boolean.FALSE);
    OBDal.getInstance().save(materialTransaction);
    OBDal.getInstance().flush();

    return costAdjCreated;

  }

  public static JSONObject processPriceDifferenceTransaction(
      MaterialTransaction materialTransaction) throws OBException {
    costAdjHeader = null;

    Organization organizationForCostAdjustmentHeader = new OrganizationStructureProvider()
        .getLegalEntity(materialTransaction.getOrganization());
    calculateTransactionPriceDifference(organizationForCostAdjustmentHeader, materialTransaction);

    if (costAdjHeader != null) {
      OBDal.getInstance().flush();
      JSONObject message = CostAdjustmentProcess.doProcessCostAdjustment(costAdjHeader);
      try {
        message.put("documentNo", costAdjHeader.getDocumentNo());
        if (message.get("severity") != "success") {
          throw new OBException(OBMessageUtils.parseTranslation("@ErrorProcessingCostAdj@") + ": "
              + costAdjHeader.getDocumentNo() + " - " + message.getString("text"));
        }
      } catch (JSONException e) {
        throw new OBException(OBMessageUtils.parseTranslation("@ErrorProcessingCostAdj@"));
      }

      return message;
    } else {
      JSONObject message = new JSONObject();
      try {
        message.put("severity", "success");
        message.put("title", "");
        message.put("text", OBMessageUtils.messageBD("Success"));
      } catch (JSONException ignore) {
      }
      return message;
    }
  }

  /**
   * This process is going to calculate the differences in prices between the Orders and the related
   * Invoices. If there are any differences a Cost Adjustment Document will be created to adjust the
   * related Transactions
   * 
   * @param legalOrganization
   *          [Mandatory] Legal Organization for which the Price Difference Process is going to be
   *          executed
   * @return the message to be shown to the user properly formatted and translated to the user
   *         language.
   */
  public static JSONObject processPriceDifference(Organization legalOrganization) {
    return processPriceDifference(null, null, legalOrganization);
  }

  /**
   * This process is going to calculate the differences in prices between the Orders and the related
   * Invoices. If there are any differences a Cost Adjustment Document will be created to adjust the
   * related Transactions
   * 
   * @param date
   *          [Optional] Date from which the Price Differences Process is going to executed
   * @param product
   *          [Optional] Product for which the Price Difference Process is going to be executed
   * @param legalOrganization
   *          [Mandatory] Legal Organization for which the Price Difference Process is going to be
   *          executed
   * @return the message to be shown to the user properly formatted and translated to the user
   *         language.
   * @throws OBException
   *           when there is an error that prevents the cost adjustment to be processed.
   */
  public static JSONObject processPriceDifference(Date date, Product product,
      Organization legalOrganization) throws OBException {

    JSONObject message = null;
    costAdjHeader = null;
    boolean costAdjCreated = false;
    int count = 0;
    OBCriteria<MaterialTransaction> mTrxs = OBDal.getInstance()
        .createCriteria(MaterialTransaction.class);
    if (date != null) {
      mTrxs.add(Restrictions.le(MaterialTransaction.PROPERTY_MOVEMENTDATE, date));
    }
    if (product != null) {
      mTrxs.add(Restrictions.eq(MaterialTransaction.PROPERTY_PRODUCT, product));
    }
    mTrxs.add(Restrictions.eq(MaterialTransaction.PROPERTY_CHECKPRICEDIFFERENCE, true));
    mTrxs.add(Restrictions.eq(MaterialTransaction.PROPERTY_ISCOSTCALCULATED, true));
    mTrxs.add(
        Restrictions.in(MaterialTransaction.PROPERTY_ORGANIZATION + "." + Organization.PROPERTY_ID,
            new OrganizationStructureProvider().getChildTree(legalOrganization.getId(), true)));
    mTrxs.addOrderBy(MaterialTransaction.PROPERTY_MOVEMENTDATE, true);
    mTrxs.addOrderBy(MaterialTransaction.PROPERTY_TRANSACTIONPROCESSDATE, true);
    ScrollableResults lines = mTrxs.scroll(ScrollMode.FORWARD_ONLY);

    int i = 0;
    try {
      while (lines.next()) {
        MaterialTransaction line = (MaterialTransaction) lines.get(0);
        costAdjCreated = calculateTransactionPriceDifference(legalOrganization, line);
        if (costAdjCreated) {
          count++;
        }

        i++;
        if (i % 100 == 0) {
          // Not needed to do flush because it is already done at the end of
          // calculateTransactionPriceDifference method
          OBDal.getInstance().getSession().clear();
        }
      }
    } finally {
      lines.close();
    }

    if (costAdjHeader != null) {
      OBDal.getInstance().flush();
      message = CostAdjustmentProcess.doProcessCostAdjustment(costAdjHeader);
      try {
        if (!StringUtils.equalsIgnoreCase("success", (String) message.get("severity"))) {
          throw new OBException(OBMessageUtils.parseTranslation("@ErrorProcessingCostAdj@") + ": "
              + costAdjHeader.getDocumentNo() + " - " + message.getString("text"));
        } else {
          message.put("transactionsProcessed", count);
        }
      } catch (JSONException e) {
        throw new OBException(OBMessageUtils.parseTranslation("@ErrorProcessingCostAdj@"));
      }
      return message;
    } else {
      try {
        message = new JSONObject();
        message.put("transactionsProcessed", count);
      } catch (JSONException ignore) {
      }
      return message;
    }
  }

  private static void createCostAdjustmenHeader(Organization org) {
    if (costAdjHeader == null) {
      costAdjHeader = CostAdjustmentUtils.insertCostAdjustmentHeader(org, "PDC");
      // PDC: Price Dif Correction
    }
  }

  /**
   * True if is an Incoming Transaction
   */
  private static boolean isValidPriceAdjTrx(MaterialTransaction trx) {
    TrxType transacctionType = TrxType.getTrxType(trx);
    switch (transacctionType) {
      case Receipt:
        return true;
      default:
        return false;
    }
  }

  private static boolean invoiceIsNotVoidedAndIsProcessed(Invoice invoice) {
    return !invoice.getDocumentStatus().equals("VO") && invoice.isProcessed();
  }

  private static BigDecimal calculateInvoiceQuantity(boolean isNegativeReceipt,
      BigDecimal invoiceQty, ReceiptInvoiceMatch matchInv) {
    BigDecimal invoiceQuantity;
    if (isNegativeReceipt) {
      // If the receipt is negative negate the invoiced quantities.
      invoiceQuantity = invoiceQty.add(matchInv.getQuantity().negate());
    } else {
      invoiceQuantity = invoiceQty.add(matchInv.getQuantity());
    }
    return invoiceQuantity;
  }

  private static BigDecimal calculateExpectedCost(Currency trxCurrency, Organization trxOrg,
      Date trxDate, BigDecimal expectedCost, ReceiptInvoiceMatch matchInv, Invoice invoice) {
    BigDecimal invoiceAmt;
    BigDecimal cost;
    invoiceAmt = calculateInvoiceAmount(matchInv, invoice, trxCurrency, trxOrg, trxDate);
    cost = expectedCost.add(invoiceAmt);
    return cost;
  }

  private static BigDecimal calculateInvoiceAmount(ReceiptInvoiceMatch matchInv, Invoice invoice,
      Currency trxCurrency, Organization trxOrg, Date trxDate) {
    BigDecimal invoiceAmt;
    invoiceAmt = matchInv.getQuantity().multiply(matchInv.getInvoiceLine().getUnitPrice());

    invoiceAmt = FinancialUtils.getConvertedAmount(invoiceAmt, invoice.getCurrency(), trxCurrency,
        trxDate, trxOrg, FinancialUtils.PRECISION_STANDARD,
        invoice.getCurrencyConversionRateDocList());
    return invoiceAmt;
  }

  private static Date getCostAdjustmentDate(Date costAdjDateAcct, Invoice invoice) {
    Date invoiceDate = invoice.getInvoiceDate();
    Date costAdjustmentDate = costAdjDateAcct;
    if (costAdjDateAcct == null || costAdjDateAcct.before(invoiceDate)) {
      costAdjustmentDate = invoiceDate;
    }
    return costAdjustmentDate;
  }

}
