package org.openbravo.erpCommon.ad_forms;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
/** Tests for {@link DocLine_Bank}. */
@SuppressWarnings({"java:S101", "java:S120"})

@RunWith(MockitoJUnitRunner.Silent.class)
public class DocLine_BankTest {

  private DocLine_Bank instance;
  /** Sets up test fixtures. */

  @Before
  public void setUp() {
    instance = new DocLine_Bank("DOCTYPE", "HEADER_001", "LINE_001");
  }
  /** Constructor sets document type. */

  @Test
  public void testConstructorSetsDocumentType() {
    assertEquals("DOCTYPE", instance.p_DocumentType);
  }
  /** Constructor sets header id. */

  @Test
  public void testConstructorSetsHeaderId() {
    assertEquals("HEADER_001", instance.m_TrxHeader_ID);
  }
  /** Constructor sets line id. */

  @Test
  public void testConstructorSetsLineId() {
    assertEquals("LINE_001", instance.m_TrxLine_ID);
  }
  /** Default field values. */

  @Test
  public void testDefaultFieldValues() {
    assertEquals("", instance.m_C_Payment_ID);
    assertEquals("", instance.m_C_GLItem_ID);
    assertEquals("", instance.isManual);
    assertEquals("", instance.chargeAmt);
  }
  /** Default amount values. */

  @Test
  public void testDefaultAmountValues() {
    assertEquals("0", instance.m_TrxAmt);
    assertEquals("0", instance.m_StmtAmt);
    assertEquals("0", instance.m_InterestAmt);
    assertEquals("0", instance.convertChargeAmt);
  }
  /** Set amount both values. */

  @Test
  public void testSetAmountBothValues() {
    instance.setAmount("500.00", "300.00");

    assertEquals("500.00", instance.m_StmtAmt);
    assertEquals("300.00", instance.m_TrxAmt);
  }
  /** Get servlet info. */

  @Test
  public void testGetServletInfo() {
    assertEquals("Servlet for the accounting", instance.getServletInfo());
  }
  /** Get glitem account returns null when empty. */

  @Test
  public void testGetGlitemAccountReturnsNullWhenEmpty() {
    instance.m_C_GLItem_ID = "";
    Account result = instance.getGlitemAccount(null, null, null);
    assertEquals(null, result);
  }
}
