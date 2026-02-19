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
/** Tests for {@link DocOrder}. */
@SuppressWarnings({"java:S120"})

@RunWith(MockitoJUnitRunner.Silent.class)
public class DocOrderTest {

  private static final String VAL_900_00 = "900.00";

  private DocOrder instance;
  /**
   * Sets up test fixtures.
   * @throws Exception if an error occurs
   */

  @Before
  public void setUp() throws Exception {
    ObjenesisStd objenesis = new ObjenesisStd();
    instance = objenesis.newInstance(DocOrder.class);
    // Initialize ZERO field from AcctServer parent
    Field zeroField = AcctServer.class.getDeclaredField("ZERO");
    zeroField.setAccessible(true);
    zeroField.set(instance, BigDecimal.ZERO);
  }
  /** Get serial version uid. */

  @Test
  public void testGetSerialVersionUID() {
    assertEquals(1L, DocOrder.getSerialVersionUID());
  }
  /** Get set m taxes. */

  @Test
  public void testGetSetMTaxes() {
    assertNull(instance.getM_taxes());

    DocTax[] taxes = new DocTax[]{new DocTax("TAX1", "Tax 1", "10", "100", "10")};
    instance.setM_taxes(taxes);

    assertEquals(1, instance.getM_taxes().length);
  }
  /** Get document confirmation always true. */

  @Test
  public void testGetDocumentConfirmationAlwaysTrue() {
    assertTrue(instance.getDocumentConfirmation(null, "any_id"));
  }
  /** Get servlet info. */

  @Test
  public void testGetServletInfo() {
    assertEquals("Servlet for the accounting", instance.getServletInfo());
  }
  /**
   * Get balance with amounts only.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetBalanceWithAmountsOnly() throws Exception {
    // Set up Amounts array
    Field amountsField = AcctServer.class.getDeclaredField("Amounts");
    amountsField.setAccessible(true);
    String[] amounts = new String[AcctServer.AMTTYPE_Charge + 1];
    amounts[AcctServer.AMTTYPE_Gross] = "1000.00";
    amounts[AcctServer.AMTTYPE_Net] = VAL_900_00;
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
  /**
   * Get balance with taxes.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetBalanceWithTaxes() throws Exception {
    Field amountsField = AcctServer.class.getDeclaredField("Amounts");
    amountsField.setAccessible(true);
    String[] amounts = new String[AcctServer.AMTTYPE_Charge + 1];
    amounts[AcctServer.AMTTYPE_Gross] = "1000.00";
    amounts[AcctServer.AMTTYPE_Net] = VAL_900_00;
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
    assertEquals(new BigDecimal(VAL_900_00), balance);
  }
}
