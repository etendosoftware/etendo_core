package org.openbravo.erpCommon.ad_forms;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Field;
import java.math.BigDecimal;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;
/** Tests for {@link DocDPManagement}. */
@SuppressWarnings({"java:S120"})

@RunWith(MockitoJUnitRunner.Silent.class)
public class DocDPManagementTest {

  private DocDPManagement instance;
  /**
   * Sets up test fixtures.
   * @throws Exception if an error occurs
   */

  @Before
  public void setUp() throws Exception {
    ObjenesisStd objenesis = new ObjenesisStd();
    instance = objenesis.newInstance(DocDPManagement.class);
    // Set the SeqNo field to its default value
    Field seqNoField = DocDPManagement.class.getDeclaredField("SeqNo");
    seqNoField.setAccessible(true);
    seqNoField.set(instance, "0");
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
  /** Get serial version uid. */

  @Test
  public void testGetSerialVersionUID() {
    assertEquals(1L, DocDPManagement.getSerialVersionUID());
  }
  /**
   * Get balance returns zero.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetBalanceReturnsZero() throws Exception {
    // Set ZERO field from AcctServer
    Field zeroField = AcctServer.class.getDeclaredField("ZERO");
    zeroField.setAccessible(true);
    zeroField.set(instance, BigDecimal.ZERO);

    BigDecimal balance = instance.getBalance();
    assertEquals(BigDecimal.ZERO, balance);
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
    assertEquals("20", instance.getSeqNo());
  }
  /** Next seq no from large number. */

  @Test
  public void testNextSeqNoFromLargeNumber() {
    String result = instance.nextSeqNo("990");
    assertEquals("1000", result);
    assertEquals("1000", instance.getSeqNo());
  }
  /** Get servlet info. */

  @Test
  public void testGetServletInfo() {
    assertEquals("Servlet for the accounting", instance.getServletInfo());
  }
}
