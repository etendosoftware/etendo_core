package org.openbravo.erpCommon.ad_forms;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Field;
import java.math.BigDecimal;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;
/** Tests for {@link DocPayment}. */
@SuppressWarnings({"java:S120"})

@RunWith(MockitoJUnitRunner.Silent.class)
public class DocPaymentTest {

  private static final String VAL_2024_01_01 = "2024-01-01";

  private DocPayment instance;
  /**
   * Sets up test fixtures.
   * @throws Exception if an error occurs
   */

  @Before
  public void setUp() throws Exception {
    ObjenesisStd objenesis = new ObjenesisStd();
    instance = objenesis.newInstance(DocPayment.class);
    // Initialize private fields
    Field seqNoField = DocPayment.class.getDeclaredField("SeqNo");
    seqNoField.setAccessible(true);
    seqNoField.set(instance, "0");
    Field settlementTypeField = DocPayment.class.getDeclaredField("SettlementType");
    settlementTypeField.setAccessible(true);
    settlementTypeField.set(instance, "");
    // Initialize ZERO
    Field zeroField = AcctServer.class.getDeclaredField("ZERO");
    zeroField.setAccessible(true);
    zeroField.set(instance, BigDecimal.ZERO);
  }
  /** Get seq no default. */

  @Test
  public void testGetSeqNoDefault() {
    assertEquals("0", instance.getSeqNo());
  }
  /** Set seq no. */

  @Test
  public void testSetSeqNo() {
    instance.setSeqNo("50");
    assertEquals("50", instance.getSeqNo());
  }
  /** Get settlement type default. */

  @Test
  public void testGetSettlementTypeDefault() {
    assertEquals("", instance.getSettlementType());
  }
  /** Set settlement type. */

  @Test
  public void testSetSettlementType() {
    instance.setSettlementType("I");
    assertEquals("I", instance.getSettlementType());
  }
  /** Get serial version uid. */

  @Test
  public void testGetSerialVersionUID() {
    assertEquals(1L, DocPayment.getSerialVersionUID());
  }
  /** Get zero. */

  @Test
  public void testGetZERO() {
    assertEquals(BigDecimal.ZERO, DocPayment.getZERO());
  }
  /** Next seq no from zero. */

  @Test
  public void testNextSeqNoFromZero() {
    String result = instance.nextSeqNo("0");
    assertEquals("10", result);
    assertEquals("10", instance.getSeqNo());
  }
  /** Next seq no from ten. */

  @Test
  public void testNextSeqNoFromTen() {
    String result = instance.nextSeqNo("10");
    assertEquals("20", result);
  }
  /** Next seq no from large number. */

  @Test
  public void testNextSeqNoFromLargeNumber() {
    String result = instance.nextSeqNo("990");
    assertEquals("1000", result);
  }
  /** Get balance returns zero. */

  @Test
  public void testGetBalanceReturnsZero() {
    BigDecimal balance = instance.getBalance();
    assertEquals(BigDecimal.ZERO, balance);
  }
  /**
   * Convert amount same currency.
   * @throws Exception if an error occurs
   */

  @Test
  public void testConvertAmountSameCurrency() throws Exception {
    // When currencies are the same, convertAmount returns the amount unchanged
    String result = instance.convertAmount("100.00", true, VAL_2024_01_01, VAL_2024_01_01,
        "USD", "USD", null, null, null, null, null);
    assertEquals("100.00", result);
  }
  /**
   * Convert amount null amount.
   * @throws Exception if an error occurs
   */

  @Test
  public void testConvertAmountNullAmount() throws Exception {
    String result = instance.convertAmount(null, true, VAL_2024_01_01, VAL_2024_01_01,
        "USD", "USD", null, null, null, null, null);
    assertEquals("0", result);
  }
  /**
   * Convert amount empty amount.
   * @throws Exception if an error occurs
   */

  @Test
  public void testConvertAmountEmptyAmount() throws Exception {
    String result = instance.convertAmount("", true, VAL_2024_01_01, VAL_2024_01_01,
        "USD", "USD", null, null, null, null, null);
    assertEquals("0", result);
  }
  /** Get servlet info. */

  @Test
  public void testGetServletInfo() {
    assertEquals("Servlet for the accounting", instance.getServletInfo());
  }
}
