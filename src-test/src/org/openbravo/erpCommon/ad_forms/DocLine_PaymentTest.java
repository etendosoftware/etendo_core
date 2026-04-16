package org.openbravo.erpCommon.ad_forms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
/** Tests for {@link DocLine_Payment}. */
@SuppressWarnings({"java:S101", "java:S120"})

@RunWith(MockitoJUnitRunner.Silent.class)
public class DocLine_PaymentTest {

  private static final String LINE_001 = "LINE_001";
  private static final String GL_001 = "GL_001";
  private static final String ACTIVE = "ACTIVE";
  private static final String INV_001 = "INV_001";
  private static final String BP_001 = "BP_001";
  private static final String WH_001 = "WH_001";

  private DocLine_Payment instance;
  /** Sets up test fixtures. */

  @Before
  public void setUp() {
    instance = new DocLine_Payment("DOCTYPE", "HEADER_001", LINE_001);
  }
  /** Constructor sets line id. */

  @Test
  public void testConstructorSetsLineId() {
    assertEquals(LINE_001, instance.getLine_ID());
  }
  /** Constructor sets record id2. */

  @Test
  public void testConstructorSetsRecordId2() {
    assertEquals(LINE_001, instance.m_Record_Id2);
  }
  /** Get set amount. */

  @Test
  public void testGetSetAmount() {
    instance.setAmount("500.00");
    assertEquals("500.00", instance.getAmount());
  }
  /** Get set is receipt. */

  @Test
  public void testGetSetIsReceipt() {
    instance.setIsReceipt("Y");
    assertEquals("Y", instance.getIsReceipt());
  }
  /** Get set is manual. */

  @Test
  public void testGetSetIsManual() {
    instance.setIsManual("N");
    assertEquals("N", instance.getIsManual());
  }
  /** Get set is paid. */

  @Test
  public void testGetSetIsPaid() {
    instance.setIsPaid("Y");
    assertEquals("Y", instance.getIsPaid());
  }
  /** Get set write off amt. */

  @Test
  public void testGetSetWriteOffAmt() {
    instance.setWriteOffAmt("25.00");
    assertEquals("25.00", instance.getWriteOffAmt());
  }
  /** Get set settlement cancel id. */

  @Test
  public void testGetSetSettlementCancelId() {
    instance.setC_Settlement_Cancel_ID("CANCEL_001");
    assertEquals("CANCEL_001", instance.getC_Settlement_Cancel_ID());
  }
  /** Get set settlement generate id. */

  @Test
  public void testGetSetSettlementGenerateId() {
    instance.setC_Settlement_Generate_ID("GEN_001");
    assertEquals("GEN_001", instance.getC_Settlement_Generate_ID());
  }
  /** Get set gl item id. */

  @Test
  public void testGetSetGLItemId() {
    instance.setC_GLItem_ID(GL_001);
    assertEquals(GL_001, instance.getC_GLItem_ID());
  }
  /** Get set is direct posting. */

  @Test
  public void testGetSetIsDirectPosting() {
    instance.setIsDirectPosting("Y");
    assertEquals("Y", instance.getIsDirectPosting());
  }
  /** Get set dp status. */

  @Test
  public void testGetSetDpStatus() {
    instance.setDpStatus(ACTIVE);
    assertEquals(ACTIVE, instance.getDpStatus());
  }
  /** Get set conversion date. */

  @Test
  public void testGetSetConversionDate() {
    instance.setConversionDate("2024-06-15");
    assertEquals("2024-06-15", instance.getConversionDate());
  }
  /** Get set invoice id. */

  @Test
  public void testGetSetInvoiceId() {
    instance.setC_INVOICE_ID(INV_001);
    assertEquals(INV_001, instance.getC_INVOICE_ID());
  }
  /** Get set b partner id. */

  @Test
  public void testGetSetBPartnerId() {
    instance.setC_BPARTNER_ID(BP_001);
    assertEquals(BP_001, instance.getC_BPARTNER_ID());
  }
  /** Get set withholding id. */

  @Test
  public void testGetSetWithholdingId() {
    instance.setC_WITHHOLDING_ID(WH_001);
    assertEquals(WH_001, instance.getC_WITHHOLDING_ID());
  }
  /** Get set with hold amt. */

  @Test
  public void testGetSetWithHoldAmt() {
    instance.setWithHoldAmt("10.00");
    assertEquals("10.00", instance.getWithHoldAmt());
  }
  /** Get set bank account id. */

  @Test
  public void testGetSetBankAccountId() {
    instance.setC_BANKACCOUNT_ID("BA_001");
    assertEquals("BA_001", instance.getC_BANKACCOUNT_ID());
  }
  /** Get set bank statement line id. */

  @Test
  public void testGetSetBankStatementLineId() {
    instance.setC_BANKSTATEMENTLINE_ID("BSL_001");
    assertEquals("BSL_001", instance.getC_BANKSTATEMENTLINE_ID());
  }
  /** Get set cashbook id. */

  @Test
  public void testGetSetCashbookId() {
    instance.setC_CASHBOOK_ID("CB_001");
    assertEquals("CB_001", instance.getC_CASHBOOK_ID());
  }
  /** Get set cashline id. */

  @Test
  public void testGetSetCashlineId() {
    instance.setC_CASHLINE_ID("CL_001");
    assertEquals("CL_001", instance.getC_CASHLINE_ID());
  }
  /** Get set currency id from. */

  @Test
  public void testGetSetCurrencyIdFrom() {
    instance.setC_Currency_ID_From("USD");
    assertEquals("USD", instance.getC_Currency_ID_From());
  }
  /** Clone copies all fields. */

  @Test
  public void testCloneCopiesAllFields() {
    instance.setAmount("1000.00");
    instance.setIsReceipt("Y");
    instance.setIsManual("N");
    instance.setIsPaid("Y");
    instance.setWriteOffAmt("50.00");
    instance.setC_Settlement_Cancel_ID("SC_001");
    instance.setC_Settlement_Generate_ID("SG_001");
    instance.setC_GLItem_ID(GL_001);
    instance.setIsDirectPosting("N");
    instance.setDpStatus(ACTIVE);
    instance.setConversionDate("2024-01-01");
    instance.setC_INVOICE_ID(INV_001);
    instance.setC_BPARTNER_ID(BP_001);
    instance.setC_WITHHOLDING_ID(WH_001);

    DocLine_Payment cloned = DocLine_Payment.clone(instance);

    assertEquals("1000.00", cloned.getAmount());
    assertEquals("Y", cloned.getIsReceipt());
    assertEquals("N", cloned.getIsManual());
    assertEquals("Y", cloned.getIsPaid());
    assertEquals("50.00", cloned.getWriteOffAmt());
    assertEquals("SC_001", cloned.getC_Settlement_Cancel_ID());
    assertEquals("SG_001", cloned.getC_Settlement_Generate_ID());
    assertEquals(GL_001, cloned.getC_GLItem_ID());
    assertEquals("N", cloned.getIsDirectPosting());
    assertEquals(ACTIVE, cloned.getDpStatus());
    assertEquals("2024-01-01", cloned.getConversionDate());
    assertEquals(INV_001, cloned.getC_INVOICE_ID());
    assertEquals(BP_001, cloned.getC_BPARTNER_ID());
    assertEquals(WH_001, cloned.getC_WITHHOLDING_ID());
  }
  /** Clone preserves line id. */

  @Test
  public void testClonePreservesLineId() {
    DocLine_Payment cloned = DocLine_Payment.clone(instance);
    assertEquals(LINE_001, cloned.getLine_ID());
  }
  /** Clone preserves document type. */

  @Test
  public void testClonePreservesDocumentType() {
    DocLine_Payment cloned = DocLine_Payment.clone(instance);
    assertEquals("DOCTYPE", cloned.p_DocumentType);
  }
  /** Default field values. */

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
  /** Get servlet info. */

  @Test
  public void testGetServletInfo() {
    assertEquals("Servlet for accounting", instance.getServletInfo());
  }
}
