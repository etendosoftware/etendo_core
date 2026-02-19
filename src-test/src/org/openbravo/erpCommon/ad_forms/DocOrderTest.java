package org.openbravo.erpCommon.ad_forms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.math.BigDecimal;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;

@RunWith(MockitoJUnitRunner.Silent.class)
public class DocOrderTest {

  private DocOrder instance;

  @Before
  public void setUp() throws Exception {
    ObjenesisStd objenesis = new ObjenesisStd();
    instance = objenesis.newInstance(DocOrder.class);
    // Initialize ZERO field from AcctServer parent
    Field zeroField = AcctServer.class.getDeclaredField("ZERO");
    zeroField.setAccessible(true);
    zeroField.set(instance, BigDecimal.ZERO);
  }

  @Test
  public void testGetSerialVersionUID() {
    assertEquals(1L, DocOrder.getSerialVersionUID());
  }

  @Test
  public void testGetSetMTaxes() {
    assertNull(instance.getM_taxes());

    DocTax[] taxes = new DocTax[]{new DocTax("TAX1", "Tax 1", "10", "100", "10")};
    instance.setM_taxes(taxes);

    assertEquals(1, instance.getM_taxes().length);
  }

  @Test
  public void testGetDocumentConfirmationAlwaysTrue() {
    assertTrue(instance.getDocumentConfirmation(null, "any_id"));
  }

  @Test
  public void testGetServletInfo() {
    assertEquals("Servlet for the accounting", instance.getServletInfo());
  }

  @Test
  public void testGetBalanceWithAmountsOnly() throws Exception {
    // Set up Amounts array
    Field amountsField = AcctServer.class.getDeclaredField("Amounts");
    amountsField.setAccessible(true);
    String[] amounts = new String[AcctServer.AMTTYPE_Charge + 1];
    amounts[AcctServer.AMTTYPE_Gross] = "1000.00";
    amounts[AcctServer.AMTTYPE_Net] = "900.00";
    amounts[AcctServer.AMTTYPE_Charge] = "50.00";
    amountsField.set(instance, amounts);

    // No taxes, no lines
    Field taxesField = DocOrder.class.getDeclaredField("m_taxes");
    taxesField.setAccessible(true);
    taxesField.set(instance, null);

    Field linesField = AcctServer.class.getDeclaredField("p_lines");
    linesField.setAccessible(true);
    linesField.set(instance, null);

    // Balance = Gross - Charge - taxes - lines = 1000 - 50 = 950
    BigDecimal balance = instance.getBalance();
    assertEquals(new BigDecimal("950.00"), balance);
  }

  @Test
  public void testGetBalanceWithTaxes() throws Exception {
    Field amountsField = AcctServer.class.getDeclaredField("Amounts");
    amountsField.setAccessible(true);
    String[] amounts = new String[AcctServer.AMTTYPE_Charge + 1];
    amounts[AcctServer.AMTTYPE_Gross] = "1000.00";
    amounts[AcctServer.AMTTYPE_Net] = "900.00";
    amounts[AcctServer.AMTTYPE_Charge] = "0";
    amountsField.set(instance, amounts);

    DocTax[] taxes = new DocTax[]{new DocTax("TAX1", "Tax", "10", "900", "100.00")};
    Field taxesField = DocOrder.class.getDeclaredField("m_taxes");
    taxesField.setAccessible(true);
    taxesField.set(instance, taxes);

    Field linesField = AcctServer.class.getDeclaredField("p_lines");
    linesField.setAccessible(true);
    linesField.set(instance, null);

    // Balance = 1000 - 0 - 100 = 900
    BigDecimal balance = instance.getBalance();
    assertEquals(new BigDecimal("900.00"), balance);
  }
}
