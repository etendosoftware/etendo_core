package org.openbravo.erpCommon.ad_forms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;

/**
 * Unit tests for {@link AcctSchema}.
 * Uses ObjenesisStd to bypass the constructor which calls load() with DB access.
 */
@SuppressWarnings({"java:S120"})
@RunWith(MockitoJUnitRunner.class)
public class AcctSchemaTest {

  private static final String DEFAULT1 = "DEFAULT1";
  private static final String ELEM1 = "ELEM1";
  private static final String ORGANIZATION = "Organization";

  private static final String TEST_SCHEMA_ID = "TEST_SCHEMA_001";
  private static final String TEST_CURRENCY_ID = "USD_001";
  private static final String TEST_RATE_TYPE = "S";

  private AcctSchema instance;
  /** Sets up test fixtures. */

  @Before
  public void setUp() {
    ObjenesisStd objenesis = new ObjenesisStd();
    instance = objenesis.newInstance(AcctSchema.class);
  }
  /** Get c acct schema id. */

  @Test
  public void testGetCAcctSchemaID() {
    instance.m_C_AcctSchema_ID = TEST_SCHEMA_ID;
    assertEquals(TEST_SCHEMA_ID, instance.getC_AcctSchema_ID());
  }
  /** Get c currency id. */

  @Test
  public void testGetCCurrencyID() {
    instance.m_C_Currency_ID = TEST_CURRENCY_ID;
    assertEquals(TEST_CURRENCY_ID, instance.getC_Currency_ID());
  }
  /** Get currency rate type. */

  @Test
  public void testGetCurrencyRateType() {
    instance.m_CurrencyRateType = TEST_RATE_TYPE;
    assertEquals(TEST_RATE_TYPE, instance.getCurrencyRateType());
  }
  /** Is accrual true. */

  @Test
  public void testIsAccrualTrue() {
    instance.m_IsAccrual = "Y";
    assertTrue(instance.isAccrual());
  }
  /** Is accrual false. */

  @Test
  public void testIsAccrualFalse() {
    instance.m_IsAccrual = "N";
    assertFalse(instance.isAccrual());
  }
  /** Is suspense balancing true. */

  @Test
  public void testIsSuspenseBalancingTrue() {
    instance.m_UseSuspenseBalancing = "Y";
    assertTrue(instance.isSuspenseBalancing());
  }
  /** Is suspense balancing false. */

  @Test
  public void testIsSuspenseBalancingFalse() {
    instance.m_UseSuspenseBalancing = "N";
    assertFalse(instance.isSuspenseBalancing());
  }
  /** Is currency balancing true. */

  @Test
  public void testIsCurrencyBalancingTrue() {
    instance.m_UseCurrencyBalancing = "Y";
    assertTrue(instance.isCurrencyBalancing());
  }
  /** Is currency balancing false. */

  @Test
  public void testIsCurrencyBalancingFalse() {
    instance.m_UseCurrencyBalancing = "N";
    assertFalse(instance.isCurrencyBalancing());
  }
  /** Get suspense balancing acct. */

  @Test
  public void testGetSuspenseBalancingAcct() {
    instance.m_SuspenseBalancing_Acct = null;
    assertNull(instance.getSuspenseBalancing_Acct());
  }
  /** Get currency balancing acct. */

  @Test
  public void testGetCurrencyBalancingAcct() {
    instance.m_CurrencyBalancing_Acct = null;
    assertNull(instance.getCurrencyBalancing_Acct());
  }
  /** Get acct schema element found. */

  @Test
  public void testGetAcctSchemaElementFound() {
    AcctSchemaElement element = new AcctSchemaElement(
        "E1", "10", ORGANIZATION, "OO", ELEM1, DEFAULT1, "Y", "N");
    ArrayList<Object> elementList = new ArrayList<>();
    elementList.add(element);
    instance.m_elementList = elementList;

    AcctSchemaElement result = instance.getAcctSchemaElement("OO");
    assertEquals("OO", result.m_segmentType);
    assertEquals(ORGANIZATION, result.m_name);
  }
  /** Get acct schema element not found. */

  @Test
  public void testGetAcctSchemaElementNotFound() {
    ArrayList<Object> elementList = new ArrayList<>();
    elementList.add(new AcctSchemaElement(
        "E1", "10", ORGANIZATION, "OO", ELEM1, DEFAULT1, "Y", "N"));
    instance.m_elementList = elementList;

    AcctSchemaElement result = instance.getAcctSchemaElement("XX");
    assertNull(result);
  }
  /** Get acct schema element multiple elements. */

  @Test
  public void testGetAcctSchemaElementMultipleElements() {
    ArrayList<Object> elementList = new ArrayList<>();
    elementList.add(new AcctSchemaElement(
        "E1", "10", ORGANIZATION, "OO", ELEM1, DEFAULT1, "Y", "N"));
    elementList.add(new AcctSchemaElement(
        "E2", "20", "Account", "AC", "ELEM2", "DEFAULT2", "Y", "N"));
    elementList.add(new AcctSchemaElement(
        "E3", "30", "BPartner", "BP", "ELEM3", "DEFAULT3", "N", "N"));
    instance.m_elementList = elementList;

    AcctSchemaElement result = instance.getAcctSchemaElement("AC");
    assertEquals("Account", result.m_name);
    assertEquals("AC", result.m_segmentType);
  }
  /** Is acct schema element true. */

  @Test
  public void testIsAcctSchemaElementTrue() {
    ArrayList<Object> elementList = new ArrayList<>();
    elementList.add(new AcctSchemaElement(
        "E1", "10", ORGANIZATION, "OO", ELEM1, DEFAULT1, "Y", "N"));
    instance.m_elementList = elementList;

    assertTrue(instance.isAcctSchemaElement("OO"));
  }
  /** Is acct schema element false. */

  @Test
  public void testIsAcctSchemaElementFalse() {
    ArrayList<Object> elementList = new ArrayList<>();
    elementList.add(new AcctSchemaElement(
        "E1", "10", ORGANIZATION, "OO", ELEM1, DEFAULT1, "Y", "N"));
    instance.m_elementList = elementList;

    assertFalse(instance.isAcctSchemaElement("ZZ"));
  }
  /** Get acct schema element empty list. */

  @Test
  public void testGetAcctSchemaElementEmptyList() {
    instance.m_elementList = new ArrayList<>();
    assertNull(instance.getAcctSchemaElement("OO"));
  }
  /** Costing constants. */

  @Test
  public void testCostingConstants() {
    assertEquals("A", AcctSchema.COSTING_AVERAGE);
    assertEquals("S", AcctSchema.COSTING_STANDARD);
    assertEquals("P", AcctSchema.COSTING_LASTPO);
  }
}
