package org.openbravo.erpCommon.ad_forms;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;
import org.openbravo.model.financialmgmt.payment.FIN_BankStatement;
import org.openbravo.model.financialmgmt.payment.FIN_BankStatementLine;
/** Tests for {@link DocFINBankStatement}. */
@SuppressWarnings({"java:S120"})

@RunWith(MockitoJUnitRunner.Silent.class)
public class DocFINBankStatementTest {

  private DocFINBankStatement instance;
  /**
   * Sets up test fixtures.
   * @throws Exception if an error occurs
   */

  @Before
  public void setUp() throws Exception {
    ObjenesisStd objenesis = new ObjenesisStd();
    instance = objenesis.newInstance(DocFINBankStatement.class);
    // Initialize SeqNo field
    Field seqNoField = DocFINBankStatement.class.getDeclaredField("SeqNo");
    seqNoField.setAccessible(true);
    seqNoField.set(instance, "0");
  }
  /**
   * Get balance returns zero.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetBalanceReturnsZero() throws Exception {
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
  }
  /** Next seq no from ten. */

  @Test
  public void testNextSeqNoFromTen() {
    String result = instance.nextSeqNo("10");
    assertEquals("20", result);
  }
  /**
   * Next seq no updates instance field.
   * @throws Exception if an error occurs
   */

  @Test
  public void testNextSeqNoUpdatesInstanceField() throws Exception {
    instance.nextSeqNo("30");
    Field seqNoField = DocFINBankStatement.class.getDeclaredField("SeqNo");
    seqNoField.setAccessible(true);
    assertEquals("40", seqNoField.get(instance));
  }
  /** Get total amount with no lines. */

  @Test
  public void testGetTotalAmountWithNoLines() {
    FIN_BankStatement bankStatement = mock(FIN_BankStatement.class);
    when(bankStatement.getFINBankStatementLineList()).thenReturn(Collections.emptyList());

    BigDecimal total = instance.getTotalAmount(bankStatement);
    assertEquals(BigDecimal.ZERO, total);
  }
  /** Get total amount with debit lines. */

  @Test
  public void testGetTotalAmountWithDebitLines() {
    FIN_BankStatement bankStatement = mock(FIN_BankStatement.class);
    FIN_BankStatementLine line1 = mock(FIN_BankStatementLine.class);
    when(line1.getDramount()).thenReturn(new BigDecimal("100.00"));
    when(line1.getCramount()).thenReturn(BigDecimal.ZERO);

    FIN_BankStatementLine line2 = mock(FIN_BankStatementLine.class);
    when(line2.getDramount()).thenReturn(new BigDecimal("50.00"));
    when(line2.getCramount()).thenReturn(BigDecimal.ZERO);

    when(bankStatement.getFINBankStatementLineList()).thenReturn(Arrays.asList(line1, line2));

    BigDecimal total = instance.getTotalAmount(bankStatement);
    assertEquals(new BigDecimal("150.00"), total);
  }
  /** Get total amount with credit lines. */

  @Test
  public void testGetTotalAmountWithCreditLines() {
    FIN_BankStatement bankStatement = mock(FIN_BankStatement.class);
    FIN_BankStatementLine line = mock(FIN_BankStatementLine.class);
    when(line.getDramount()).thenReturn(BigDecimal.ZERO);
    when(line.getCramount()).thenReturn(new BigDecimal("200.00"));

    when(bankStatement.getFINBankStatementLineList()).thenReturn(Collections.singletonList(line));

    BigDecimal total = instance.getTotalAmount(bankStatement);
    assertEquals(new BigDecimal("-200.00"), total);
  }
  /** Get total amount mixed. */

  @Test
  public void testGetTotalAmountMixed() {
    FIN_BankStatement bankStatement = mock(FIN_BankStatement.class);
    FIN_BankStatementLine line1 = mock(FIN_BankStatementLine.class);
    when(line1.getDramount()).thenReturn(new BigDecimal("300.00"));
    when(line1.getCramount()).thenReturn(new BigDecimal("100.00"));

    FIN_BankStatementLine line2 = mock(FIN_BankStatementLine.class);
    when(line2.getDramount()).thenReturn(new BigDecimal("50.00"));
    when(line2.getCramount()).thenReturn(new BigDecimal("75.00"));

    when(bankStatement.getFINBankStatementLineList()).thenReturn(Arrays.asList(line1, line2));

    BigDecimal total = instance.getTotalAmount(bankStatement);
    assertEquals(new BigDecimal("175.00"), total);
  }
}
