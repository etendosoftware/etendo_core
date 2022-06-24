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
 * All portions are Copyright (C) 2010-2020 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.ad_forms;

import java.math.BigDecimal;

import org.apache.commons.lang.StringUtils;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.order.Order;

public class DocLine_FINPayment extends DocLineCashVATReady_PaymentTransactionReconciliation {

  String Line_ID = "";
  String Amount = "";
  String WriteOffAmt = "";
  String isReceipt = "";
  String C_GLItem_ID = "";
  String isPrepayment = "";
  boolean isPrepaymentAgainstInvoice = false;
  BigDecimal doubtFulDebtAmount = BigDecimal.ZERO;
  String AmountExcludingCredit = "";

  @Deprecated
  Invoice invoice = null;
  private String invoiceId;
  @Deprecated
  Order order = null;
  private String orderId;

  public Invoice getInvoice() {
    if (invoice != null) {
      return invoice;
    } else if (StringUtils.isNotBlank(invoiceId)) {
      try {
        OBContext.setAdminMode(false);
        return OBDal.getInstance().get(Invoice.class, invoiceId);
      } finally {
        OBContext.restorePreviousMode();
      }
    } else {
      return null;
    }
  }

  /**
   * @deprecated Use {@link #setInvoiceId(String)} instead, which avoids to store a object in memory
   *             so we can control from outside when to flush and/or clear the session to avoid Out
   *             Of Memory errors
   */
  @Deprecated
  public void setInvoice(Invoice invoice) {
    this.invoice = invoice;
  }

  public Order getOrder() {
    if (order != null) {
      return order;
    } else if (StringUtils.isNotBlank(orderId)) {
      try {
        OBContext.setAdminMode(false);
        return OBDal.getInstance().get(Order.class, orderId);
      } finally {
        OBContext.restorePreviousMode();
      }
    } else {
      return null;
    }
  }

  /**
   * @deprecated Use {@link #setOrderId(String)} instead, which avoids to store a object in memory
   *             so we can control from outside when to flush and/or clear the session to avoid Out
   *             Of Memory errors
   */
  @Deprecated
  public void setOrder(Order order) {
    this.order = order;
  }

  /**
   * @param isReceipt
   *          the isReceipt to set
   */
  public void setIsReceipt(String isReceipt) {
    this.isReceipt = isReceipt;
  }

  /**
   * @return the isReceipt
   */
  public String getIsReceipt() {
    return isReceipt;
  }

  /**
   * @param isPrepayment
   *          the isPrepayment to set
   */
  public void setIsPrepayment(String isPrepayment) {
    this.isPrepayment = isPrepayment;
  }

  /**
   * @return the isPrepayment
   */
  public String getIsPrepayment() {
    return isPrepayment;
  }

  /**
   * @return the isPrepaymentAgainstInvoice
   */
  public boolean isPrepaymentAgainstInvoice() {
    return isPrepaymentAgainstInvoice;
  }

  /**
   * @param isPrepaymentAgainstInvoice
   *          the isPrepaymentAgainstInvoice to set
   */
  public void setPrepaymentAgainstInvoice(boolean isPrepaymentAgainstInvoice) {
    this.isPrepaymentAgainstInvoice = isPrepaymentAgainstInvoice;
  }

  /**
   * @return the amount
   */
  @Override
  public String getAmount() {
    return Amount;
  }

  /**
   * @return the amountExcludingCredit
   */
  public String getAmountExcludingCredit() {
    return AmountExcludingCredit;
  }

  /**
   * @return the line_ID
   */
  public String getLine_ID() {
    return Line_ID;
  }

  /**
   * @param line_ID
   *          the line_ID to set
   */
  public void setLine_ID(String line_ID) {
    Line_ID = line_ID;
  }

  /**
   * @return the writeOffAmt
   */
  public String getWriteOffAmt() {
    return WriteOffAmt;
  }

  /**
   * @param writeOffAmt
   *          the writeOffAmt to set
   */
  public void setWriteOffAmt(String writeOffAmt) {
    WriteOffAmt = writeOffAmt;
  }

  /**
   * @return the c_GLItem_ID
   */
  public String getC_GLItem_ID() {
    return C_GLItem_ID;
  }

  /**
   * @param item_ID
   *          the c_GLItem_ID to set
   */
  public void setC_GLItem_ID(String item_ID) {
    C_GLItem_ID = item_ID;
  }

  /**
   * @param amount
   *          the amount to set
   */
  @Override
  public void setAmount(String amount) {
    Amount = amount;
  }

  /**
   * @param amountExcludingCredit
   *          the amountExcludingCredit to set
   */
  public void setAmountExcludingCredit(String amountExcludingCredit) {
    AmountExcludingCredit = amountExcludingCredit;
  }

  public BigDecimal getDoubtFulDebtAmount() {
    return doubtFulDebtAmount;
  }

  public void setDoubtFulDebtAmount(BigDecimal doubtFulDebtAmount) {
    this.doubtFulDebtAmount = doubtFulDebtAmount;
  }

  public DocLine_FINPayment(String DocumentType, String TrxHeader_ID, String TrxLine_ID) {
    super(DocumentType, TrxHeader_ID, TrxLine_ID);
    Line_ID = TrxLine_ID;
    // TODO:Review Record_id2 implementation for new flow
    // m_Record_Id2 = Line_ID;
  }

  @Override
  public String getServletInfo() {
    return "Servlet for accounting";
  } // end of getServletInfo() method

  public String getInvoiceId() {
    return invoiceId;
  }

  public void setInvoiceId(String invoiceId) {
    this.invoiceId = invoiceId;
  }

  public String getOrderId() {
    return orderId;
  }

  public void setOrderId(String orderId) {
    this.orderId = orderId;
  }
}
