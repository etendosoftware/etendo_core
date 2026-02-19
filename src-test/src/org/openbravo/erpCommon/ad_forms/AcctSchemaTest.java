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
@RunWith(MockitoJUnitRunner.class)
public class AcctSchemaTest {

  private static final String TEST_SCHEMA_ID = "TEST_SCHEMA_001";
  private static final String TEST_CURRENCY_ID = "USD_001";
  private static final String TEST_RATE_TYPE = "S";

  private AcctSchema instance;

  @Before
  public void setUp() {
    ObjenesisStd objenesis = new ObjenesisStd();
    instance = objenesis.newInstance(AcctSchema.class);
  }

  @Test
  public void testGetCAcctSchemaID() {
    instance.m_C_AcctSchema_ID = TEST_SCHEMA_ID;
    assertEquals(TEST_SCHEMA_ID, instance.getC_AcctSchema_ID());
  }

  @Test
  public void testGetCCurrencyID() {
    instance.m_C_Currency_ID = TEST_CURRENCY_ID;
    assertEquals(TEST_CURRENCY_ID, instance.getC_Currency_ID());
  }

  @Test
  public void testGetCurrencyRateType() {
    instance.m_CurrencyRateType = TEST_RATE_TYPE;
    assertEquals(TEST_RATE_TYPE, instance.getCurrencyRateType());
  }

  @Test
  public void testIsAccrualTrue() {
    instance.m_IsAccrual = "Y";
    assertTrue(instance.isAccrual());
  }

  @Test
  public void testIsAccrualFalse() {
    instance.m_IsAccrual = "N";
    assertFalse(instance.isAccrual());
  }

  @Test
  public void testIsSuspenseBalancingTrue() {
    instance.m_UseSuspenseBalancing = "Y";
    assertTrue(instance.isSuspenseBalancing());
  }

  @Test
  public void testIsSuspenseBalancingFalse() {
    instance.m_UseSuspenseBalancing = "N";
    assertFalse(instance.isSuspenseBalancing());
  }

  @Test
  public void testIsCurrencyBalancingTrue() {
    instance.m_UseCurrencyBalancing = "Y";
    assertTrue(instance.isCurrencyBalancing());
  }

  @Test
  public void testIsCurrencyBalancingFalse() {
    instance.m_UseCurrencyBalancing = "N";
    assertFalse(instance.isCurrencyBalancing());
  }

  @Test
  public void testGetSuspenseBalancingAcct() {
    instance.m_SuspenseBalancing_Acct = null;
    assertNull(instance.getSuspenseBalancing_Acct());
  }

  @Test
  public void testGetCurrencyBalancingAcct() {
    instance.m_CurrencyBalancing_Acct = null;
    assertNull(instance.getCurrencyBalancing_Acct());
  }

  @Test
  public void testGetAcctSchemaElementFound() {
    AcctSchemaElement element = new AcctSchemaElement(
        "E1", "10", "Organization", "OO", "ELEM1", "DEFAULT1", "Y", "N");
    ArrayList<Object> elementList = new ArrayList<>();
    elementList.add(element);
    instance.m_elementList = elementList;

    AcctSchemaElement result = instance.getAcctSchemaElement("OO");
    assertEquals("OO", result.m_segmentType);
    assertEquals("Organization", result.m_name);
  }

  @Test
  public void testGetAcctSchemaElementNotFound() {
    ArrayList<Object> elementList = new ArrayList<>();
    elementList.add(new AcctSchemaElement(
        "E1", "10", "Organization", "OO", "ELEM1", "DEFAULT1", "Y", "N"));
    instance.m_elementList = elementList;

    AcctSchemaElement result = instance.getAcctSchemaElement("XX");
    assertNull(result);
  }

  @Test
  public void testGetAcctSchemaElementMultipleElements() {
    ArrayList<Object> elementList = new ArrayList<>();
    elementList.add(new AcctSchemaElement(
        "E1", "10", "Organization", "OO", "ELEM1", "DEFAULT1", "Y", "N"));
    elementList.add(new AcctSchemaElement(
        "E2", "20", "Account", "AC", "ELEM2", "DEFAULT2", "Y", "N"));
    elementList.add(new AcctSchemaElement(
        "E3", "30", "BPartner", "BP", "ELEM3", "DEFAULT3", "N", "N"));
    instance.m_elementList = elementList;

    AcctSchemaElement result = instance.getAcctSchemaElement("AC");
    assertEquals("Account", result.m_name);
    assertEquals("AC", result.m_segmentType);
  }

  @Test
  public void testIsAcctSchemaElementTrue() {
    ArrayList<Object> elementList = new ArrayList<>();
    elementList.add(new AcctSchemaElement(
        "E1", "10", "Organization", "OO", "ELEM1", "DEFAULT1", "Y", "N"));
    instance.m_elementList = elementList;

    assertTrue(instance.isAcctSchemaElement("OO"));
  }

  @Test
  public void testIsAcctSchemaElementFalse() {
    ArrayList<Object> elementList = new ArrayList<>();
    elementList.add(new AcctSchemaElement(
        "E1", "10", "Organization", "OO", "ELEM1", "DEFAULT1", "Y", "N"));
    instance.m_elementList = elementList;

    assertFalse(instance.isAcctSchemaElement("ZZ"));
  }

  @Test
  public void testGetAcctSchemaElementEmptyList() {
    instance.m_elementList = new ArrayList<>();
    assertNull(instance.getAcctSchemaElement("OO"));
  }

  @Test
  public void testCostingConstants() {
    assertEquals("A", AcctSchema.COSTING_AVERAGE);
    assertEquals("S", AcctSchema.COSTING_STANDARD);
    assertEquals("P", AcctSchema.COSTING_LASTPO);
  }
}
