package org.openbravo.erpCommon.ad_forms;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
/** Tests for {@link DocLine_Cash}. */
@SuppressWarnings({"java:S101", "java:S120"})

@RunWith(MockitoJUnitRunner.Silent.class)
public class DocLine_CashTest {

  private DocLine_Cash instance;
  /** Sets up test fixtures. */

  @Before
  public void setUp() {
    instance = new DocLine_Cash("DOCTYPE", "HEADER_001", "LINE_001");
  }
  /** Cash type constants. */

  @Test
  public void testCashTypeConstants() {
    assertEquals("G", DocLine_Cash.CASHTYPE_GLITEM);
    assertEquals("C", DocLine_Cash.CASHTYPE_CHARGE);
    assertEquals("D", DocLine_Cash.CASHTYPE_DIFFERENCE);
    assertEquals("E", DocLine_Cash.CASHTYPE_EXPENSE);
    assertEquals("I", DocLine_Cash.CASHTYPE_INVOICE);
    assertEquals("R", DocLine_Cash.CASHTYPE_RECEIPT);
    assertEquals("T", DocLine_Cash.CASHTYPE_TRANSFER);
    assertEquals("P", DocLine_Cash.CASHTYPE_DEBTPAYMENT);
    assertEquals("O", DocLine_Cash.CASHTYPE_ORDER);
  }
  /** Default cash type. */

  @Test
  public void testDefaultCashType() {
    assertEquals("", instance.m_CashType);
  }
  /** Set cash type. */

  @Test
  public void testSetCashType() {
    instance.setCashType("G");
    assertEquals("G", instance.m_CashType);
  }
  /** Set cash type null. */

  @Test
  public void testSetCashTypeNull() {
    instance.setCashType(null);
    assertEquals("", instance.m_CashType);
  }
  /** Set cash type empty. */

  @Test
  public void testSetCashTypeEmpty() {
    instance.setCashType("");
    assertEquals("", instance.m_CashType);
  }
  /** Set amount three args. */

  @Test
  public void testSetAmountThreeArgs() {
    instance.setAmount("100.00", "5.00", "2.00");

    assertEquals("100.00", instance.m_Amount);
    assertEquals("5.00", instance.m_DiscountAmt);
    assertEquals("2.00", instance.m_WriteOffAmt);
  }
  /** Set amount empty args keep defaults. */

  @Test
  public void testSetAmountEmptyArgsKeepDefaults() {
    instance.setAmount("", "", "");

    assertEquals("0", instance.m_Amount);
    assertEquals("0", instance.m_DiscountAmt);
    assertEquals("0", instance.m_WriteOffAmt);
  }
  /** Get amount. */

  @Test
  public void testGetAmount() {
    instance.m_Amount = "250.50";
    assertEquals("250.50", instance.getAmount());
  }
  /** Default amounts. */

  @Test
  public void testDefaultAmounts() {
    assertEquals("0", instance.m_Amount);
    assertEquals("0", instance.m_DiscountAmt);
    assertEquals("0", instance.m_WriteOffAmt);
  }
  /** Default references. */

  @Test
  public void testDefaultReferences() {
    assertEquals("", instance.m_C_BankAccount_ID);
    assertEquals("", instance.m_C_Invoice_ID);
    assertEquals("", instance.m_C_Order_Id);
    assertEquals("", instance.m_C_Debt_Payment_Id);
    assertEquals("", instance.Line_ID);
  }
  /** Get glitem account returns null when empty. */

  @Test
  public void testGetGlitemAccountReturnsNullWhenEmpty() {
    Account result = instance.getGlitemAccount(null, null, null);
    assertEquals(null, result);
  }
  /** Get servlet info. */

  @Test
  public void testGetServletInfo() {
    assertEquals("Servlet for the accounting", instance.getServletInfo());
  }
}
