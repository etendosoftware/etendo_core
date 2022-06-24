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
 * All portions are Copyright (C) 2013-2020 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.erpCommon.ad_forms;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.hibernate.query.Query;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.invoice.InvoiceTaxCashVAT_V;

/**
 * Use for DocLine that must support Cash VAT regime
 * 
 * 
 */
public class DocLineCashVATReady_PaymentTransactionReconciliation extends DocLine {

  private List<String> invoiceTaxCashVAT_V = new ArrayList<>();

  public DocLineCashVATReady_PaymentTransactionReconciliation(final String documentType,
      final String trxHeaderID, String trxLineID) {
    super(documentType, trxHeaderID, trxLineID);
  }

  /**
   * Returns a list of different InvoiceTaxCashVAT_V records.
   * 
   * It internally creates a Set from the invoiceTaxCashVAT_V attribute and returns a List
   */
  public List<String> getInvoiceTaxCashVAT_V_IDs() {
    final Set<String> invoiceTaxCashVATVSet = new HashSet<>(invoiceTaxCashVAT_V);
    return new ArrayList<>(invoiceTaxCashVATVSet);
  }

  /**
   * Returns a list of different InvoiceTaxCashVAT_V records.
   * 
   * It internally creates a Set from the invoiceTaxCashVAT_V attribute and returns a List
   * 
   * @deprecated Use {@link #getInvoiceTaxCashVAT_V_IDs()}
   */
  @Deprecated
  public List<InvoiceTaxCashVAT_V> getInvoiceTaxCashVAT_V() {
    final List<InvoiceTaxCashVAT_V> itcvList = new ArrayList<>();
    for (String itcv : invoiceTaxCashVAT_V) {
      itcvList.add(OBDal.getInstance().get(InvoiceTaxCashVAT_V.class, itcv));
    }
    final Set<InvoiceTaxCashVAT_V> invoiceTaxCashVATVSet = new HashSet<>(itcvList);
    return new ArrayList<>(invoiceTaxCashVATVSet);
  }

  public void setInvoiceTaxCashVAT_V_IDs(final List<String> invoiceTaxCashVATV) {
    this.invoiceTaxCashVAT_V = invoiceTaxCashVATV;
  }

  /**
   * 
   * @deprecated Use {@link #setInvoiceTaxCashVAT_V_IDs(List)}
   */
  @Deprecated
  public void setInvoiceTaxCashVAT_V(final List<InvoiceTaxCashVAT_V> invoiceTaxCashVATV) {
    for (InvoiceTaxCashVAT_V itcv : invoiceTaxCashVATV) {
      this.invoiceTaxCashVAT_V.add(itcv.getId());
    }
  }

  /**
   * Given the payment detail id (finPaymentDetailID), the method calculates the linked
   * InvoiceTaxCashVAT_V records and adds them to the invoiceTaxCashVAT_V list associated to the
   * object. If this method is called several times for different finPaymentDetailID, the system
   * will add (not override) the associated invoiceTaxCashVAT_V records to the object
   * 
   */
  @SuppressWarnings("unchecked")
  public void setInvoiceTaxCashVAT_V(final String finPaymentDetailID) {
    if (StringUtils.isNotBlank(finPaymentDetailID)) {
      try {
        OBContext.setAdminMode(true);
        //@formatter:off
        final String hql =
                      "select distinct id" +
                      "  from C_InvoiceTax_CashVAT_V as itcv " +
                      " where itcv.paymentDetails.id = :finPaymentDetailID " +
                      "   and itcv.canceled = false" +
                      "   and itcv.active = true" +
                      "   and (itcv.isPaidAtInvoicing = false" +
                      "        or itcv.isPaidAtInvoicing is null)";
        //@formatter:on

        final Query<String> obq = OBDal.getInstance()
            .getSession()
            .createQuery(hql)
            .setParameter("finPaymentDetailID", finPaymentDetailID);

        this.invoiceTaxCashVAT_V.addAll(obq.list());
      } finally {
        OBContext.restorePreviousMode();
      }
    }
  }
}
