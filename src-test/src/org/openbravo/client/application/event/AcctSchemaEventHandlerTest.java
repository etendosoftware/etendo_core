package org.openbravo.client.application.event;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;

/**
 * Unit tests for {@link AcctSchemaEventHandler}.
 * Tests the getAccountSign private method via reflection, which contains
 * pure business logic for determining account sign based on account type and flags.
 * Uses ObjenesisStd to bypass static initializer that calls ModelProvider.
 */
@RunWith(MockitoJUnitRunner.class)
public class AcctSchemaEventHandlerTest {

  private AcctSchemaEventHandler instance;
  private Method getAccountSignMethod;

  @Before
  public void setUp() throws Exception {
    ObjenesisStd objenesis = new ObjenesisStd();
    instance = objenesis.newInstance(AcctSchemaEventHandler.class);

    getAccountSignMethod = AcctSchemaEventHandler.class.getDeclaredMethod(
        "getAccountSign", String.class, boolean.class, boolean.class,
        boolean.class, boolean.class, boolean.class);
    getAccountSignMethod.setAccessible(true);
  }

  private boolean invokeGetAccountSign(String accountType, boolean assetPositive,
      boolean liabilityPositive, boolean ownersEquityPositive,
      boolean expensePositive, boolean revenuePositive) throws Exception {
    return (boolean) getAccountSignMethod.invoke(instance,
        accountType, assetPositive, liabilityPositive,
        ownersEquityPositive, expensePositive, revenuePositive);
  }

  // Asset tests
  @Test
  public void testAssetPositiveReturnsFalse() throws Exception {
    boolean result = invokeGetAccountSign("A", true, false, false, false, false);
    assertFalse(result);
  }

  @Test
  public void testAssetNotPositiveReturnsTrue() throws Exception {
    boolean result = invokeGetAccountSign("A", false, false, false, false, false);
    assertTrue(result);
  }

  // Liability tests
  @Test
  public void testLiabilityPositiveReturnsTrue() throws Exception {
    boolean result = invokeGetAccountSign("L", false, true, false, false, false);
    assertTrue(result);
  }

  @Test
  public void testLiabilityNotPositiveReturnsFalse() throws Exception {
    boolean result = invokeGetAccountSign("L", false, false, false, false, false);
    assertFalse(result);
  }

  // Owners Equity tests
  @Test
  public void testOwnersEquityPositiveReturnsTrue() throws Exception {
    boolean result = invokeGetAccountSign("O", false, false, true, false, false);
    assertTrue(result);
  }

  @Test
  public void testOwnersEquityNotPositiveReturnsFalse() throws Exception {
    boolean result = invokeGetAccountSign("O", false, false, false, false, false);
    assertFalse(result);
  }

  // Expense tests
  @Test
  public void testExpensePositiveReturnsFalse() throws Exception {
    boolean result = invokeGetAccountSign("E", false, false, false, true, false);
    assertFalse(result);
  }

  @Test
  public void testExpenseNotPositiveReturnsTrue() throws Exception {
    boolean result = invokeGetAccountSign("E", false, false, false, false, false);
    assertTrue(result);
  }

  // Revenue tests
  @Test
  public void testRevenuePositiveReturnsTrue() throws Exception {
    boolean result = invokeGetAccountSign("R", false, false, false, false, true);
    assertTrue(result);
  }

  @Test
  public void testRevenueNotPositiveReturnsFalse() throws Exception {
    boolean result = invokeGetAccountSign("R", false, false, false, false, false);
    assertFalse(result);
  }

  // Unknown type
  @Test
  public void testUnknownAccountTypeReturnsFalse() throws Exception {
    boolean result = invokeGetAccountSign("X", true, true, true, true, true);
    assertFalse(result);
  }

  @Test
  public void testMemoAccountTypeReturnsFalse() throws Exception {
    boolean result = invokeGetAccountSign("M", true, true, true, true, true);
    assertFalse(result);
  }

  // All positive flags
  @Test
  public void testAssetWithAllPositive() throws Exception {
    boolean result = invokeGetAccountSign("A", true, true, true, true, true);
    assertFalse(result);
  }

  @Test
  public void testLiabilityWithAllPositive() throws Exception {
    boolean result = invokeGetAccountSign("L", true, true, true, true, true);
    assertTrue(result);
  }

  @Test
  public void testEquityWithAllPositive() throws Exception {
    boolean result = invokeGetAccountSign("O", true, true, true, true, true);
    assertTrue(result);
  }

  @Test
  public void testExpenseWithAllPositive() throws Exception {
    boolean result = invokeGetAccountSign("E", true, true, true, true, true);
    assertFalse(result);
  }

  @Test
  public void testRevenueWithAllPositive() throws Exception {
    boolean result = invokeGetAccountSign("R", true, true, true, true, true);
    assertTrue(result);
  }

  // All negative flags
  @Test
  public void testAssetWithAllNegative() throws Exception {
    boolean result = invokeGetAccountSign("A", false, false, false, false, false);
    assertTrue(result);
  }

  @Test
  public void testLiabilityWithAllNegative() throws Exception {
    boolean result = invokeGetAccountSign("L", false, false, false, false, false);
    assertFalse(result);
  }

  @Test
  public void testEquityWithAllNegative() throws Exception {
    boolean result = invokeGetAccountSign("O", false, false, false, false, false);
    assertFalse(result);
  }

  @Test
  public void testExpenseWithAllNegative() throws Exception {
    boolean result = invokeGetAccountSign("E", false, false, false, false, false);
    assertTrue(result);
  }

  @Test
  public void testRevenueWithAllNegative() throws Exception {
    boolean result = invokeGetAccountSign("R", false, false, false, false, false);
    assertFalse(result);
  }

  @Test
  public void testNullAccountTypeReturnsFalse() throws Exception {
    boolean result = invokeGetAccountSign(null, true, true, true, true, true);
    assertFalse(result);
  }

  @Test
  public void testEmptyAccountTypeReturnsFalse() throws Exception {
    boolean result = invokeGetAccountSign("", true, true, true, true, true);
    assertFalse(result);
  }
}
