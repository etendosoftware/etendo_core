package org.openbravo.erpCommon.ad_callouts;

import java.math.BigDecimal;
import java.math.RoundingMode;

import javax.servlet.ServletException;

import org.apache.commons.lang3.StringUtils;
import org.openbravo.base.filter.IsIDFilter;
import org.openbravo.erpCommon.utility.OBMessageUtils;

/**
 * Callout that keeps the Tax Amount (inptaxamt) consistent with the Tax Base Amount and
 * the configured tax rate of the selected Tax for an Invoice line.
 */
public class SL_InvoiceTax_Amt extends SimpleCallout {
  private static final String INPTAXAMT = "inptaxamt";
  private static final String INPTAXBASEAMT = "inptaxbaseamt";

  /**
   * Executes the callout logic for keeping the invoice line's tax amount in sync with the
   * selected tax and base amount.
   * @param info the {@link CalloutInfo} providing input parameters and the response writer
   * @throws ServletException if an error occurs while executing the callout
   */
  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    String fieldChanged = info.getLastFieldChanged();
    log4j.debug("CHANGED: " + fieldChanged);

    // Parameters
    String taxId = info.getStringParameter("inpcTaxId", IsIDFilter.instance);
    String invoiceId = info.getStringParameter("inpcInvoiceId", IsIDFilter.instance);
    BigDecimal taxAmt = info.getBigDecimalParameter(INPTAXAMT);
    BigDecimal taxBaseAmt = info.getBigDecimalParameter(INPTAXBASEAMT);
    if (taxAmt == null){
      taxAmt = BigDecimal.ZERO;
    }
    if (taxBaseAmt == null) {
      taxBaseAmt = BigDecimal.ZERO;
    }
    
    // Update Tax Amount and Tax Base Amount
    SLInvoiceTaxAmtData[] data = SLInvoiceTaxAmtData.select(this, taxId, invoiceId);
    BigDecimal taxRate = BigDecimal.ZERO;
    int taxScale = 2;
    if (data != null && data.length > 0) {
      if (StringUtils.isNotBlank(data[0].rate)) {
        try {
          taxRate = new BigDecimal(data[0].rate.trim());
        } catch (NumberFormatException e) {
          log4j.warn("Invalid tax rate: " + data[0].rate, e);
          taxRate = BigDecimal.ZERO;
        }
      }
      try {
        taxScale = Integer.parseInt(
          data[0].priceprecision != null ? data[0].priceprecision.trim() : "2");
      } catch (NumberFormatException e) {
        log4j.warn("Invalid price precision: " + data[0].priceprecision, e);
        taxScale = 2;
      }
    }
    BigDecimal sysTaxAmt = taxBaseAmt.multiply(taxRate).divide(new BigDecimal("100"), 12, RoundingMode.HALF_EVEN).setScale(taxScale, RoundingMode.HALF_UP);

    if (StringUtils.equals(INPTAXAMT, fieldChanged)) {
      final BigDecimal newTaxAmt = taxAmt.setScale(taxScale, RoundingMode.HALF_UP);
      final BigDecimal maxDelta = new BigDecimal("0.01");
      if (newTaxAmt.subtract(sysTaxAmt).abs().compareTo(maxDelta) > 0) {
        info.addResult("WARNING", OBMessageUtils.messageBD("ETP_TaxAdjOutOfRange"));
      }
      info.addResult(INPTAXAMT, newTaxAmt);
      return; 
    }
    final BigDecimal newTaxAmt = taxBaseAmt.multiply(taxRate).divide(new BigDecimal("100"), 12, RoundingMode.HALF_EVEN).setScale(taxScale, RoundingMode.HALF_UP);

    info.addResult(INPTAXAMT, newTaxAmt);
    info.addResult(INPTAXBASEAMT, taxBaseAmt.setScale(taxScale, RoundingMode.HALF_UP));
  }
}
