package org.openbravo.erpCommon.ad_forms;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Field;
import java.math.BigDecimal;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;

@RunWith(MockitoJUnitRunner.Silent.class)
public class DocPaymentTest {

  private DocPayment instance;

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

  @Test
  public void testGetSeqNoDefault() {
    assertEquals("0", instance.getSeqNo());
  }

  @Test
  public void testSetSeqNo() {
    instance.setSeqNo("50");
    assertEquals("50", instance.getSeqNo());
  }

  @Test
  public void testGetSettlementTypeDefault() {
    assertEquals("", instance.getSettlementType());
  }

  @Test
  public void testSetSettlementType() {
    instance.setSettlementType("I");
    assertEquals("I", instance.getSettlementType());
  }

  @Test
  public void testGetSerialVersionUID() {
    assertEquals(1L, DocPayment.getSerialVersionUID());
  }

  @Test
  public void testGetZERO() {
    assertEquals(BigDecimal.ZERO, DocPayment.getZERO());
  }

  @Test
  public void testNextSeqNoFromZero() {
    String result = instance.nextSeqNo("0");
    assertEquals("10", result);
    assertEquals("10", instance.getSeqNo());
  }

  @Test
  public void testNextSeqNoFromTen() {
    String result = instance.nextSeqNo("10");
    assertEquals("20", result);
  }

  @Test
  public void testNextSeqNoFromLargeNumber() {
    String result = instance.nextSeqNo("990");
    assertEquals("1000", result);
  }

  @Test
  public void testGetBalanceReturnsZero() {
    BigDecimal balance = instance.getBalance();
    assertEquals(BigDecimal.ZERO, balance);
  }

  @Test
  public void testConvertAmountSameCurrency() throws Exception {
    // When currencies are the same, convertAmount returns the amount unchanged
    String result = instance.convertAmount("100.00", true, "2024-01-01", "2024-01-01",
        "USD", "USD", null, null, null, null, null);
    assertEquals("100.00", result);
  }

  @Test
  public void testConvertAmountNullAmount() throws Exception {
    String result = instance.convertAmount(null, true, "2024-01-01", "2024-01-01",
        "USD", "USD", null, null, null, null, null);
    assertEquals("0", result);
  }

  @Test
  public void testConvertAmountEmptyAmount() throws Exception {
    String result = instance.convertAmount("", true, "2024-01-01", "2024-01-01",
        "USD", "USD", null, null, null, null, null);
    assertEquals("0", result);
  }

  @Test
  public void testGetServletInfo() {
    assertEquals("Servlet for the accounting", instance.getServletInfo());
  }
}
