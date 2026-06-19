/*
 *************************************************************************
 * The contents of this file are subject to the Etendo License
 * (the "License"), you may not use this file except in compliance with
 * the License.
 * You may obtain a copy of the License at
 * https://github.com/etendosoftware/etendo_core/blob/main/legal/Etendo_license.txt
 * Software distributed under the License is distributed on an
 * "AS IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing rights
 * and limitations under the License.
 * All portions are Copyright © 2021–2026 FUTIT SERVICES, S.L
 * All Rights Reserved.
 * Contributor(s): Futit Services S.L.
 *************************************************************************
 */
package org.openbravo.test.taxes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openbravo.advpaymentmngt.test.TestUtility;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.businesspartner.Location;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceLine;
import org.openbravo.model.common.invoice.InvoiceTax;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.financialmgmt.tax.TaxRate;
import org.openbravo.model.pricing.pricelist.PriceList;
import org.openbravo.test.base.OBBaseTest;

/**
 * Regression test for ETP-4250 / etendo_core#1059.
 *
 * Completing a purchase invoice failed with {@code @CashVATNotMatch@} when a line's tax was changed
 * from a Cash VAT tax to a regular one before completion. C_INVOICELINETAX_TRG zeroes the old
 * C_INVOICETAX aggregate row but never deletes it, so a residual 0/0 row with a mismatched
 * IsCashVat flag survived. The Cash VAT check in C_INVOICE_POST runs before the extension point
 * that invokes C_INVOICE_UPDATE_TAXES (the cleanup), so it counted that residual row as a mismatch
 * and aborted before the cleanup could remove it.
 *
 * The fix makes the check ignore exactly the rows the cleanup will delete (zero amounts and no live
 * C_INVOICELINETAX), while still rejecting a genuine mix of Cash VAT and regular taxes.
 *
 * Environment: QA Testing client / Spain organization (same master data used by
 * {@link ModifyTaxesTest}). Cash VAT is exercised by flagging an existing tax as Cash VAT during
 * setup and restoring it afterwards, so the test does not depend on the Spain localization Cash VAT
 * reference data being installed.
 *
 * Prerequisites to run: the C_INVOICE_POST DB function must be deployed (update.database), and the
 * QA Spain calendar must have an open period for {@link #ACCOUNTING_DATE} (adjust the constant if
 * your QA calendar differs).
 */
public class CashVatInvoiceCompletionTest extends OBBaseTest {

  private static final String USER_OPENBRAVO = "100";
  private static final String CLIENT_QA_TESTING = "4028E6C72959682B01295A070852010D";
  private static final String ORGANIZATION_SPAIN = "357947E87C284935AD1D783CF6F099A1";
  private static final String ROLE_QA_ADMIN = "4028E6C72959682B01295A071429011E";

  private static final String DOCTYPE_AP_INVOICE = "FF8080812C2ABFC6012C2B3BDF520075";
  private static final String VENDOR_A = "4028E6C72959682B01295F40BDDF02E3";
  private static final String VENDOR_A_LOCATION = "4028E6C72959682B01295F40C14A02E5";
  private static final String PRICELIST_PURCHASE_SPAIN = "4028E6C72959682B01295B03D2200252";
  private static final String PAYMENT_METHOD = "42E87E97974E4B35849A430B8F6F2884";
  private static final String PAYMENT_TERM = "CA9D9E3E296C48E8B3F8796445F0C17D";
  private static final String PRODUCT_FGA = "4028E6C72959682B01295ADC1D07022A";

  // Same rate (3%) is irrelevant; what matters is the IsCashVat flag differs from the regular tax.
  private static final String TAX_CASHVAT = "5A74E390B82747F9A5754C8EB1BDB47A"; // VAT 3% -> flagged
                                                                               // as Cash VAT here
  private static final String TAX_REGULAR = "DBFCCC14B64147168F0F516F82FAF38B"; // VAT 21%

  private static final Date ACCOUNTING_DATE = new Date();

  private Boolean originalCashVatFlag;

  @Before
  public void setUp() throws Exception {
    OBContext.setOBContext(USER_OPENBRAVO, ROLE_QA_ADMIN, CLIENT_QA_TESTING, ORGANIZATION_SPAIN);
    // Flag the chosen tax as Cash VAT for the duration of the test.
    TaxRate cashVatTax = OBDal.getInstance().get(TaxRate.class, TAX_CASHVAT);
    originalCashVatFlag = cashVatTax.isCashVAT();
    cashVatTax.setCashVAT(true);
    OBDal.getInstance().save(cashVatTax);
    OBDal.getInstance().flush();
    OBDal.getInstance().commitAndClose();
  }

  @After
  public void tearDown() {
    // Restore the tax flag so the shared QA data is left untouched.
    OBContext.setOBContext(USER_OPENBRAVO, ROLE_QA_ADMIN, CLIENT_QA_TESTING, ORGANIZATION_SPAIN);
    TaxRate cashVatTax = OBDal.getInstance().get(TaxRate.class, TAX_CASHVAT);
    cashVatTax.setCashVAT(originalCashVatFlag != null && originalCashVatFlag);
    OBDal.getInstance().save(cashVatTax);
    OBDal.getInstance().flush();
    OBDal.getInstance().commitAndClose();
  }

  /**
   * Core scenario: a line tax changed from Cash VAT to regular leaves a residual 0/0 C_INVOICETAX
   * row. The invoice must complete and the residual row must be cleaned up.
   */
  @Test
  public void completesAfterChangingCashVatTaxToRegular() throws Exception {
    Invoice invoice = createDraftPurchaseInvoice(TAX_CASHVAT, new BigDecimal("100"));

    // Pre-condition: with only the Cash VAT tax line, header is flagged Cash VAT and there is a
    // single tax row.
    OBDal.getInstance().refresh(invoice);
    assertEquals("Header should be Cash VAT before the tax change", Boolean.TRUE,
        invoice.isCashVAT());
    assertEquals("One tax row expected before the tax change", 1,
        invoice.getInvoiceTaxList().size());

    // Change the line tax to a regular (non Cash VAT) tax. The triggers recreate the line tax for
    // the new tax and leave the old aggregate row at 0/0.
    InvoiceLine line = invoice.getInvoiceLineList().get(0);
    line.setTax(OBDal.getInstance().getProxy(TaxRate.class, TAX_REGULAR));
    OBDal.getInstance().save(line);
    OBDal.getInstance().flush();
    OBDal.getInstance().refresh(invoice);

    // The residual 0/0 row for the old Cash VAT tax must be present and the header switched to
    // non Cash VAT.
    assertEquals("Header should no longer be Cash VAT after the change", Boolean.FALSE,
        invoice.isCashVAT());
    assertTrue("Expected a residual zero-amount Cash VAT tax row before completion",
        hasResidualZeroRow(invoice.getInvoiceTaxList(), TAX_CASHVAT));

    // Completion must succeed (this is what failed before the fix).
    // Note: TestUtility.processInvoice returns true when the process *fails* (result == 0).
    boolean processFailed = TestUtility.processInvoice(invoice);
    assertFalse("Invoice should complete after changing Cash VAT tax to a regular one",
        processFailed);

    Invoice processed = OBDal.getInstance().get(Invoice.class, invoice.getId());
    assertEquals("Invoice should be Completed", "CO", processed.getDocumentStatus());
    // The residual row must have been removed by C_INVOICE_UPDATE_TAXES during completion.
    List<InvoiceTax> taxes = processed.getInvoiceTaxList();
    assertEquals("Only the regular tax row should remain after completion", 1, taxes.size());
    assertEquals("Remaining tax row should be the regular tax", TAX_REGULAR,
        taxes.get(0).getTax().getId());
  }

  /**
   * Negative scenario: a genuine mix of a Cash VAT and a regular tax (both with real amounts and
   * live lines) must still be rejected, so the fix does not silence the validation.
   */
  @Test
  public void rejectsGenuinelyMixedCashVatInvoice() throws Exception {
    Invoice invoice = createDraftPurchaseInvoice(TAX_CASHVAT, new BigDecimal("100"));
    addLine(invoice, 20L, TAX_REGULAR, new BigDecimal("50"));
    OBDal.getInstance().flush();
    OBDal.getInstance().refresh(invoice);

    // Both tax rows carry real amounts -> genuine mix of regimes.
    assertEquals("Two real tax rows expected", 2, invoice.getInvoiceTaxList().size());

    // TestUtility.processInvoice returns true when the process *fails* (result == 0), which is
    // exactly what a genuine Cash VAT / regular mix must trigger.
    boolean processFailed = TestUtility.processInvoice(invoice);
    assertTrue("A genuine mix of Cash VAT and regular taxes must be rejected", processFailed);

    // Leave the still-draft invoice removed to keep the QA data clean.
    Invoice draft = OBDal.getInstance().get(Invoice.class, invoice.getId());
    if (StringUtils.equals("DR", draft.getDocumentStatus())) {
      removeInvoice(draft);
    }
  }

  private Invoice createDraftPurchaseInvoice(String taxId, BigDecimal lineNetAmount) {
    Invoice invoice = OBProvider.getInstance().get(Invoice.class);
    invoice.setOrganization(OBContext.getOBContext().getCurrentOrganization());
    invoice.setClient(OBContext.getOBContext().getCurrentClient());
    invoice.setDocumentType(OBDal.getInstance().getProxy(DocumentType.class, DOCTYPE_AP_INVOICE));
    invoice.setTransactionDocument(
        OBDal.getInstance().getProxy(DocumentType.class, DOCTYPE_AP_INVOICE));
    invoice.setDocumentNo("CASHVAT-" + System.nanoTime());
    invoice.setBusinessPartner(OBDal.getInstance().getProxy(BusinessPartner.class, VENDOR_A));
    invoice.setPartnerAddress(OBDal.getInstance().getProxy(Location.class, VENDOR_A_LOCATION));
    invoice.setPriceList(OBDal.getInstance().getProxy(PriceList.class, PRICELIST_PURCHASE_SPAIN));
    invoice.setCurrency(OBDal.getInstance().getProxy(Currency.class, EURO_ID));
    invoice.setPaymentMethod(
        OBDal.getInstance().getProxy(org.openbravo.model.financialmgmt.payment.FIN_PaymentMethod.class,
            PAYMENT_METHOD));
    invoice.setPaymentTerms(
        OBDal.getInstance().getProxy(org.openbravo.model.financialmgmt.payment.PaymentTerm.class,
            PAYMENT_TERM));
    invoice.setInvoiceDate(ACCOUNTING_DATE);
    invoice.setAccountingDate(ACCOUNTING_DATE);
    invoice.setTaxDate(ACCOUNTING_DATE);
    invoice.setSalesTransaction(false);
    invoice.setSummedLineAmount(BigDecimal.ZERO);
    invoice.setGrandTotalAmount(BigDecimal.ZERO);
    invoice.setWithholdingamount(BigDecimal.ZERO);
    OBDal.getInstance().save(invoice);

    addLine(invoice, 10L, taxId, lineNetAmount);
    OBDal.getInstance().flush();
    OBDal.getInstance().refresh(invoice);
    return invoice;
  }

  private void addLine(Invoice invoice, Long lineNo, String taxId, BigDecimal lineNetAmount) {
    Product product = OBDal.getInstance().get(Product.class, PRODUCT_FGA);
    InvoiceLine line = OBProvider.getInstance().get(InvoiceLine.class);
    line.setOrganization(invoice.getOrganization());
    line.setClient(invoice.getClient());
    line.setInvoice(invoice);
    line.setLineNo(lineNo);
    line.setProduct(product);
    line.setUOM(product.getUOM());
    line.setInvoicedQuantity(BigDecimal.ONE);
    line.setUnitPrice(lineNetAmount);
    line.setListPrice(lineNetAmount);
    line.setPriceLimit(lineNetAmount);
    line.setStandardPrice(lineNetAmount);
    line.setLineNetAmount(lineNetAmount);
    line.setTax(OBDal.getInstance().getProxy(TaxRate.class, taxId));
    OBDal.getInstance().save(line);
  }

  private boolean hasResidualZeroRow(List<InvoiceTax> taxes, String taxId) {
    for (InvoiceTax it : taxes) {
      if (taxId.equals(it.getTax().getId())
          && it.getTaxableAmount().compareTo(BigDecimal.ZERO) == 0
          && it.getTaxAmount().compareTo(BigDecimal.ZERO) == 0) {
        return true;
      }
    }
    return false;
  }

  private void removeInvoice(Invoice invoice) {
    for (InvoiceLine line : invoice.getInvoiceLineList()) {
      OBDal.getInstance().remove(line);
    }
    // Detach the removed lines from the parent collection, otherwise the flush would
    // re-save them by cascade (EntityNotFoundException: deleted object would be re-saved).
    invoice.getInvoiceLineList().clear();
    OBDal.getInstance().remove(invoice);
    OBDal.getInstance().flush();
    OBDal.getInstance().commitAndClose();
  }
}
