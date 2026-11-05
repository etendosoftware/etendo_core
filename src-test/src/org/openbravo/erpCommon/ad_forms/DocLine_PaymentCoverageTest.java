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
 * All portions are Copyright (C) 2021-2026 FUTIT SERVICES, S.L
 * All Rights Reserved.
 * Contributor(s): Futit Services S.L.
 *************************************************************************
 */
package org.openbravo.erpCommon.ad_forms; // NOSONAR java:S120 package must match the package-private class under test

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.Test;

/**
 * Additional coverage tests for {@link DocLine_Payment}: the null document type guard inherited
 * from {@link DocLine}, the line id setter, the bank/cash fields and a full {@code clone} that
 * also copies every inherited {@link DocLine} field.
 */
@SuppressWarnings({ "java:S101", "java:S120" })
public class DocLine_PaymentCoverageTest {

  private static final String DOC_TYPE = "ARR";
  private static final String HEADER = "HDR";
  private static final String LINE = "LN";

  private DocLine_Payment paymentLine;

  /** Creates a fresh line for each test. */
  @Before
  public void setUp() {
    paymentLine = new DocLine_Payment(DOC_TYPE, HEADER, LINE);
  }

  /** The inherited constructor rejects a null document type. */
  @Test
  public void constructorRejectsNullDocumentType() {
    assertThrows(IllegalArgumentException.class, () -> new DocLine_Payment(null, HEADER, LINE));
  }

  /** The constructor stores header/line ids and defaults the payment fields. */
  @Test
  public void constructorInitializesFields() {
    assertEquals(DOC_TYPE, paymentLine.p_DocumentType);
    assertEquals(HEADER, paymentLine.m_TrxHeader_ID);
    assertEquals(LINE, paymentLine.m_TrxLine_ID);
    assertEquals(LINE, paymentLine.getLine_ID());
    assertEquals(LINE, paymentLine.m_Record_Id2);
    assertNull(paymentLine.getC_Currency_ID_From());
    assertNull(paymentLine.getConversionDate());
    assertEquals("", paymentLine.getC_INVOICE_ID());
    assertEquals("", paymentLine.getC_BPARTNER_ID());
    assertEquals("", paymentLine.getC_WITHHOLDING_ID());
    assertEquals("", paymentLine.getWithHoldAmt());
    assertEquals("", paymentLine.getC_BANKACCOUNT_ID());
    assertEquals("", paymentLine.getC_BANKSTATEMENTLINE_ID());
    assertEquals("", paymentLine.getC_CASHBOOK_ID());
    assertEquals("", paymentLine.getC_CASHLINE_ID());
    assertEquals("", paymentLine.getC_Settlement_Cancel_ID());
    assertEquals("", paymentLine.getC_Settlement_Generate_ID());
  }

  /** A null line id is propagated to both the line id and the secondary record id. */
  @Test
  public void constructorAcceptsNullLineId() {
    DocLine_Payment nullLine = new DocLine_Payment(DOC_TYPE, null, null);
    assertNull(nullLine.getLine_ID());
    assertNull(nullLine.m_Record_Id2);
    assertNull(nullLine.m_TrxHeader_ID);
  }

  /** The line id setter overrides the value set by the constructor. */
  @Test
  public void setLineIdOverridesConstructorValue() {
    paymentLine.setLine_ID("OTHER");
    assertEquals("OTHER", paymentLine.getLine_ID());
    assertEquals(LINE, paymentLine.m_Record_Id2);
  }

  /** The bank, cash and withholding amount setters round-trip their values. */
  @Test
  public void bankAndCashSettersRoundTrip() {
    paymentLine.setWithHoldAmt("1.5");
    paymentLine.setC_BANKACCOUNT_ID("BA");
    paymentLine.setC_BANKSTATEMENTLINE_ID("BSL");
    paymentLine.setC_CASHBOOK_ID("CB");
    paymentLine.setC_CASHLINE_ID("CL");

    assertEquals("1.5", paymentLine.getWithHoldAmt());
    assertEquals("BA", paymentLine.getC_BANKACCOUNT_ID());
    assertEquals("BSL", paymentLine.getC_BANKSTATEMENTLINE_ID());
    assertEquals("CB", paymentLine.getC_CASHBOOK_ID());
    assertEquals("CL", paymentLine.getC_CASHLINE_ID());
  }

  /** {@code clone} returns a new instance copying every inherited and payment-specific field. */
  @Test
  public void cloneCopiesInheritedAndPaymentFields() {
    ProductInfo productInfo = mock(ProductInfo.class);
    Account account = mock(Account.class);
    populateInheritedFields(paymentLine, productInfo, account);
    populatePaymentFields(paymentLine);

    DocLine_Payment copy = DocLine_Payment.clone(paymentLine);

    assertNotSame(paymentLine, copy);
    assertEquals(DOC_TYPE, copy.p_DocumentType);
    assertEquals(HEADER, copy.m_TrxHeader_ID);
    assertEquals(LINE, copy.m_TrxLine_ID);
    assertEquals("REC2", copy.m_Record_Id2);
    assertEquals("ORG", copy.m_AD_Org_ID);
    assertEquals("BPI", copy.m_C_BPartner_ID);
    assertEquals("PROD", copy.m_M_Product_ID);
    assertEquals("ORGTRX", copy.m_AD_OrgTrx_ID);
    assertEquals("SR", copy.m_C_SalesRegion_ID);
    assertEquals("PRJ", copy.m_C_Project_ID);
    assertEquals("ASSET", copy.m_A_Asset_ID);
    assertEquals("CAMP", copy.m_C_Campaign_ID);
    assertEquals("ACT", copy.m_C_Activity_ID);
    assertEquals("LFROM", copy.m_C_LocFrom_ID);
    assertEquals("LTO", copy.m_C_LocTo_ID);
    assertEquals("U1", copy.m_User1_ID);
    assertEquals("U2", copy.m_User2_ID);
    assertEquals("10", copy.m_Line);
    assertEquals("DESC", copy.m_description);
    assertEquals("EUR", copy.m_C_Currency_ID);
    assertEquals("UOM", copy.m_C_UOM_ID);
    assertEquals("3", copy.m_qty);
    assertEquals("TAX", copy.m_C_Tax_ID);
    assertEquals("WHI", copy.m_C_WithHolding_ID);
    assertEquals("CHG", copy.m_C_Charge_ID);
    assertEquals("4", copy.m_ChargeAmt);
    assertEquals("2026-01-02", copy.m_DateAcct);
    assertEquals("2026-01-01", copy.m_DateDoc);
    assertEquals("5", copy.m_AmtSourceDr);
    assertEquals("6", copy.m_AmtSourceCr);
    assertEquals("7", copy.m_AmtAcctDr);
    assertEquals("8", copy.m_AmtAcctCr);
    assertEquals("SCHEMA", copy.m_C_AcctSchema_ID);
    assertSame(account, copy.m_account);
    assertSame(productInfo, copy.p_productInfo);

    assertEquals("NEWLINE", copy.getLine_ID());
    assertEquals("100", copy.getAmount());
    assertEquals("2", copy.getWriteOffAmt());
    assertEquals("Y", copy.getIsManual());
    assertEquals("N", copy.getIsReceipt());
    assertEquals("Y", copy.getIsPaid());
    assertEquals("SC", copy.getC_Settlement_Cancel_ID());
    assertEquals("SG", copy.getC_Settlement_Generate_ID());
    assertEquals("GL", copy.getC_GLItem_ID());
    assertEquals("N", copy.getIsDirectPosting());
    assertEquals("DE", copy.getDpStatus());
    assertEquals("USD", copy.getC_Currency_ID_From());
    assertEquals("2026-02-02", copy.getConversionDate());
    assertEquals("INV", copy.getC_INVOICE_ID());
    assertEquals("BP", copy.getC_BPARTNER_ID());
    assertEquals("WH", copy.getC_WITHHOLDING_ID());
  }

  /** {@code clone} does not copy the bank/cash/withholding amount fields; they keep defaults. */
  @Test
  public void cloneDoesNotCopyBankAndCashFields() {
    paymentLine.setWithHoldAmt("9");
    paymentLine.setC_BANKACCOUNT_ID("BA");
    paymentLine.setC_BANKSTATEMENTLINE_ID("BSL");
    paymentLine.setC_CASHBOOK_ID("CB");
    paymentLine.setC_CASHLINE_ID("CL");

    DocLine_Payment copy = DocLine_Payment.clone(paymentLine);

    assertEquals("", copy.getWithHoldAmt());
    assertEquals("", copy.getC_BANKACCOUNT_ID());
    assertEquals("", copy.getC_BANKSTATEMENTLINE_ID());
    assertEquals("", copy.getC_CASHBOOK_ID());
    assertEquals("", copy.getC_CASHLINE_ID());
  }

  /** The servlet info string is fixed. */
  @Test
  public void servletInfoIsFixed() {
    assertEquals("Servlet for accounting", paymentLine.getServletInfo());
  }

  private static void populateInheritedFields(DocLine_Payment target, ProductInfo productInfo,
      Account account) {
    target.m_Record_Id2 = "REC2";
    target.m_AD_Org_ID = "ORG";
    target.m_C_BPartner_ID = "BPI";
    target.m_M_Product_ID = "PROD";
    target.m_AD_OrgTrx_ID = "ORGTRX";
    target.m_C_SalesRegion_ID = "SR";
    target.m_C_Project_ID = "PRJ";
    target.m_A_Asset_ID = "ASSET";
    target.m_C_Campaign_ID = "CAMP";
    target.m_C_Activity_ID = "ACT";
    target.m_C_LocFrom_ID = "LFROM";
    target.m_C_LocTo_ID = "LTO";
    target.m_User1_ID = "U1";
    target.m_User2_ID = "U2";
    target.m_Line = "10";
    target.m_description = "DESC";
    target.m_C_Currency_ID = "EUR";
    target.m_C_UOM_ID = "UOM";
    target.m_qty = "3";
    target.m_C_Tax_ID = "TAX";
    target.m_C_WithHolding_ID = "WHI";
    target.m_C_Charge_ID = "CHG";
    target.m_ChargeAmt = "4";
    target.m_DateAcct = "2026-01-02";
    target.m_DateDoc = "2026-01-01";
    target.m_AmtSourceDr = "5";
    target.m_AmtSourceCr = "6";
    target.m_AmtAcctDr = "7";
    target.m_AmtAcctCr = "8";
    target.m_C_AcctSchema_ID = "SCHEMA";
    target.m_account = account;
    target.p_productInfo = productInfo;
  }

  private static void populatePaymentFields(DocLine_Payment target) {
    target.setLine_ID("NEWLINE");
    target.setAmount("100");
    target.setWriteOffAmt("2");
    target.setIsManual("Y");
    target.setIsReceipt("N");
    target.setIsPaid("Y");
    target.setC_Settlement_Cancel_ID("SC");
    target.setC_Settlement_Generate_ID("SG");
    target.setC_GLItem_ID("GL");
    target.setIsDirectPosting("N");
    target.setDpStatus("DE");
    target.setC_Currency_ID_From("USD");
    target.setConversionDate("2026-02-02");
    target.setC_INVOICE_ID("INV");
    target.setC_BPARTNER_ID("BP");
    target.setC_WITHHOLDING_ID("WH");
  }
}
