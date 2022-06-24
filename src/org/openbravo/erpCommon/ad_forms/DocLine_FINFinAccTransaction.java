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
 * All portions are Copyright (C) 2010-2016 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.ad_forms;

import java.math.BigDecimal;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.invoice.Invoice;

public class DocLine_FINFinAccTransaction
    extends DocLineCashVATReady_PaymentTransactionReconciliation {
  static Logger log4jDocLine_FINFinAccTransaction = LogManager.getLogger();

  String Line_ID = "";
  String PaymentAmount = "";
  String DepositAmount = "";
  String cGlItemId = "";
  String isPrepayment = "";
  String finPaymentId = "";
  String WriteOffAmt = "";
  boolean isPrepaymentAgainstInvoice = false;
  BigDecimal doubtFulDebtAmount = BigDecimal.ZERO;

  @Deprecated
  Invoice invoice = null;
  private String invoiceId;

  public String getcGlItemId() {
    return cGlItemId;
  }

  public void setcGlItemId(String cGlItemId) {
    this.cGlItemId = cGlItemId;
  }

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

  /**
   * @deprecated Use {@link #setInvoiceId(String)} instead, which avoids to store a object in memory
   *             so we can control from outside when to flush and/or clear the session to avoid Out
   *             Of Memory errors
   * 
   */
  @Deprecated
  public void setInvoice_ID(String invoiceId) {
    this.invoice = OBDal.getInstance().get(Invoice.class, invoiceId);
  }

  /**
   * @return the isPrepayment
   */
  public String getIsPrepayment() {
    return isPrepayment;
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
   * @param isPrepayment
   *          the isPrepayment to set
   */
  public void setIsPrepayment(String isPrepayment) {
    this.isPrepayment = isPrepayment;
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
   * @return the paymentAmount
   */
  public String getPaymentAmount() {
    return PaymentAmount;
  }

  /**
   * @param paymentAmount
   *          the paymentAmount to set
   */
  public void setPaymentAmount(String paymentAmount) {
    PaymentAmount = paymentAmount;
  }

  /**
   * @return the depositAmount
   */
  public String getDepositAmount() {
    return DepositAmount;
  }

  /**
   * @param depositAmount
   *          the depositAmount to set
   */
  public void setDepositAmount(String depositAmount) {
    DepositAmount = depositAmount;
  }

  /**
   * @return the finPaymentId
   */
  public String getFinPaymentId() {
    return finPaymentId;
  }

  /**
   * @param finPaymentId
   *          the finPaymentId to set
   */
  public void setFinPaymentId(String finPaymentId) {
    this.finPaymentId = finPaymentId;
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
   * @return the cGlItemId
   */
  public String getCGlItemId() {
    return cGlItemId;
  }

  /**
   * @param glItemId
   *          the cGlItemId to set
   */
  public void setCGlItemId(String glItemId) {
    cGlItemId = glItemId;
  }

  public BigDecimal getDoubtFulDebtAmount() {
    return doubtFulDebtAmount;
  }

  public void setDoubtFulDebtAmount(BigDecimal doubtFulDebtAmount) {
    this.doubtFulDebtAmount = doubtFulDebtAmount;
  }

  public String getInvoiceId() {
    return invoiceId;
  }

  public void setInvoiceId(String invoiceId) {
    this.invoiceId = invoiceId;
  }

  public DocLine_FINFinAccTransaction(String DocumentType, String TrxHeader_ID, String TrxLine_ID) {
    super(DocumentType, TrxHeader_ID, TrxLine_ID);
    Line_ID = TrxLine_ID;
    // m_Record_Id2 = Line_ID;
  }

  @Override
  public String getServletInfo() {
    return "Servlet for accounting";
  } // end of getServletInfo() method
}
