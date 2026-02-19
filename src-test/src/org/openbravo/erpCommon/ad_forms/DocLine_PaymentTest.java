package org.openbravo.erpCommon.ad_forms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class DocLine_PaymentTest {

  private DocLine_Payment instance;

  @Before
  public void setUp() {
    instance = new DocLine_Payment("DOCTYPE", "HEADER_001", "LINE_001");
  }

  @Test
  public void testConstructorSetsLineId() {
    assertEquals("LINE_001", instance.getLine_ID());
  }

  @Test
  public void testConstructorSetsRecordId2() {
    assertEquals("LINE_001", instance.m_Record_Id2);
  }

  @Test
  public void testGetSetAmount() {
    instance.setAmount("500.00");
    assertEquals("500.00", instance.getAmount());
  }

  @Test
  public void testGetSetIsReceipt() {
    instance.setIsReceipt("Y");
    assertEquals("Y", instance.getIsReceipt());
  }

  @Test
  public void testGetSetIsManual() {
    instance.setIsManual("N");
    assertEquals("N", instance.getIsManual());
  }

  @Test
  public void testGetSetIsPaid() {
    instance.setIsPaid("Y");
    assertEquals("Y", instance.getIsPaid());
  }

  @Test
  public void testGetSetWriteOffAmt() {
    instance.setWriteOffAmt("25.00");
    assertEquals("25.00", instance.getWriteOffAmt());
  }

  @Test
  public void testGetSetSettlementCancelId() {
    instance.setC_Settlement_Cancel_ID("CANCEL_001");
    assertEquals("CANCEL_001", instance.getC_Settlement_Cancel_ID());
  }

  @Test
  public void testGetSetSettlementGenerateId() {
    instance.setC_Settlement_Generate_ID("GEN_001");
    assertEquals("GEN_001", instance.getC_Settlement_Generate_ID());
  }

  @Test
  public void testGetSetGLItemId() {
    instance.setC_GLItem_ID("GL_001");
    assertEquals("GL_001", instance.getC_GLItem_ID());
  }

  @Test
  public void testGetSetIsDirectPosting() {
    instance.setIsDirectPosting("Y");
    assertEquals("Y", instance.getIsDirectPosting());
  }

  @Test
  public void testGetSetDpStatus() {
    instance.setDpStatus("ACTIVE");
    assertEquals("ACTIVE", instance.getDpStatus());
  }

  @Test
  public void testGetSetConversionDate() {
    instance.setConversionDate("2024-06-15");
    assertEquals("2024-06-15", instance.getConversionDate());
  }

  @Test
  public void testGetSetInvoiceId() {
    instance.setC_INVOICE_ID("INV_001");
    assertEquals("INV_001", instance.getC_INVOICE_ID());
  }

  @Test
  public void testGetSetBPartnerId() {
    instance.setC_BPARTNER_ID("BP_001");
    assertEquals("BP_001", instance.getC_BPARTNER_ID());
  }

  @Test
  public void testGetSetWithholdingId() {
    instance.setC_WITHHOLDING_ID("WH_001");
    assertEquals("WH_001", instance.getC_WITHHOLDING_ID());
  }

  @Test
  public void testGetSetWithHoldAmt() {
    instance.setWithHoldAmt("10.00");
    assertEquals("10.00", instance.getWithHoldAmt());
  }

  @Test
  public void testGetSetBankAccountId() {
    instance.setC_BANKACCOUNT_ID("BA_001");
    assertEquals("BA_001", instance.getC_BANKACCOUNT_ID());
  }

  @Test
  public void testGetSetBankStatementLineId() {
    instance.setC_BANKSTATEMENTLINE_ID("BSL_001");
    assertEquals("BSL_001", instance.getC_BANKSTATEMENTLINE_ID());
  }

  @Test
  public void testGetSetCashbookId() {
    instance.setC_CASHBOOK_ID("CB_001");
    assertEquals("CB_001", instance.getC_CASHBOOK_ID());
  }

  @Test
  public void testGetSetCashlineId() {
    instance.setC_CASHLINE_ID("CL_001");
    assertEquals("CL_001", instance.getC_CASHLINE_ID());
  }

  @Test
  public void testGetSetCurrencyIdFrom() {
    instance.setC_Currency_ID_From("USD");
    assertEquals("USD", instance.getC_Currency_ID_From());
  }

  @Test
  public void testCloneCopiesAllFields() {
    instance.setAmount("1000.00");
    instance.setIsReceipt("Y");
    instance.setIsManual("N");
    instance.setIsPaid("Y");
    instance.setWriteOffAmt("50.00");
    instance.setC_Settlement_Cancel_ID("SC_001");
    instance.setC_Settlement_Generate_ID("SG_001");
    instance.setC_GLItem_ID("GL_001");
    instance.setIsDirectPosting("N");
    instance.setDpStatus("ACTIVE");
    instance.setConversionDate("2024-01-01");
    instance.setC_INVOICE_ID("INV_001");
    instance.setC_BPARTNER_ID("BP_001");
    instance.setC_WITHHOLDING_ID("WH_001");

    DocLine_Payment cloned = DocLine_Payment.clone(instance);

    assertEquals("1000.00", cloned.getAmount());
    assertEquals("Y", cloned.getIsReceipt());
    assertEquals("N", cloned.getIsManual());
    assertEquals("Y", cloned.getIsPaid());
    assertEquals("50.00", cloned.getWriteOffAmt());
    assertEquals("SC_001", cloned.getC_Settlement_Cancel_ID());
    assertEquals("SG_001", cloned.getC_Settlement_Generate_ID());
    assertEquals("GL_001", cloned.getC_GLItem_ID());
    assertEquals("N", cloned.getIsDirectPosting());
    assertEquals("ACTIVE", cloned.getDpStatus());
    assertEquals("2024-01-01", cloned.getConversionDate());
    assertEquals("INV_001", cloned.getC_INVOICE_ID());
    assertEquals("BP_001", cloned.getC_BPARTNER_ID());
    assertEquals("WH_001", cloned.getC_WITHHOLDING_ID());
  }

  @Test
  public void testClonePreservesLineId() {
    DocLine_Payment cloned = DocLine_Payment.clone(instance);
    assertEquals("LINE_001", cloned.getLine_ID());
  }

  @Test
  public void testClonePreservesDocumentType() {
    DocLine_Payment cloned = DocLine_Payment.clone(instance);
    assertEquals("DOCTYPE", cloned.p_DocumentType);
  }

  @Test
  public void testDefaultFieldValues() {
    assertEquals("", instance.getAmount());
    assertEquals("", instance.getIsReceipt());
    assertEquals("", instance.getIsManual());
    assertEquals("", instance.getIsPaid());
    assertEquals("", instance.getWriteOffAmt());
    assertEquals("", instance.getDpStatus());
    assertEquals("", instance.getC_GLItem_ID());
    assertEquals("", instance.getIsDirectPosting());
  }

  @Test
  public void testGetServletInfo() {
    assertEquals("Servlet for accounting", instance.getServletInfo());
  }
}
