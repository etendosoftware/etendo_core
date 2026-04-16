package org.openbravo.erpCommon.businessUtility;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mockStatic;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;
import org.openbravo.dal.core.OBContext;

/**
 * Tests for {@link CancelOrderExecutor}.
 */
@SuppressWarnings({"java:S120"})
@RunWith(MockitoJUnitRunner.class)
public class CancelOrderExecutorTest {

  private static final String TEST_ORDER_ID = "ORDER001";
  private static final String TEST_ORG_ID = "ORG001";

  private CancelOrderExecutor instance;

  private MockedStatic<OBContext> obContextStatic;
  /** Sets up test fixtures. */

  @Before
  public void setUp() {
    ObjenesisStd objenesis = new ObjenesisStd();
    instance = objenesis.newInstance(CancelOrderExecutor.class);

    obContextStatic = mockStatic(OBContext.class);
  }
  /** Tears down test fixtures. */

  @After
  public void tearDown() {
    if (obContextStatic != null) {
      obContextStatic.close();
    }
  }
  /**
   * Init sets fields.
   * @throws Exception if an error occurs
   */

  @Test
  public void testInitSetsFields() throws Exception {
    JSONObject jsonOrder = new JSONObject();
    jsonOrder.put("id", "INV_ORDER_001");

    instance.init(TEST_ORDER_ID, TEST_ORG_ID, jsonOrder, true);

    Field oldOrderIdField = CancelOrderExecutor.class.getDeclaredField("oldOrderId");
    oldOrderIdField.setAccessible(true);
    assertEquals(TEST_ORDER_ID, oldOrderIdField.get(instance));

    Field paymentOrgField = CancelOrderExecutor.class.getDeclaredField("paymentOrganizationId");
    paymentOrgField.setAccessible(true);
    assertEquals(TEST_ORG_ID, paymentOrgField.get(instance));

    Field jsonOrderField = CancelOrderExecutor.class.getDeclaredField("jsonOrder");
    jsonOrderField.setAccessible(true);
    assertNotNull(jsonOrderField.get(instance));

    Field useDocNoField = CancelOrderExecutor.class.getDeclaredField(
        "useOrderDocumentNoForRelatedDocs");
    useDocNoField.setAccessible(true);
    assertEquals(true, useDocNoField.get(instance));
  }
  /**
   * Init with false use document no.
   * @throws Exception if an error occurs
   */

  @Test
  public void testInitWithFalseUseDocumentNo() throws Exception {
    JSONObject jsonOrder = new JSONObject();
    jsonOrder.put("id", "INV_ORDER_002");

    instance.init(TEST_ORDER_ID, TEST_ORG_ID, jsonOrder, false);

    Field useDocNoField = CancelOrderExecutor.class.getDeclaredField(
        "useOrderDocumentNoForRelatedDocs");
    useDocNoField.setAccessible(true);
    assertEquals(false, useDocNoField.get(instance));
  }
  /**
   * Init overwrites previous values.
   * @throws Exception if an error occurs
   */

  @Test
  public void testInitOverwritesPreviousValues() throws Exception {
    JSONObject jsonOrder1 = new JSONObject();
    jsonOrder1.put("id", "ORDER_A");
    instance.init("OLD_1", "ORG_1", jsonOrder1, true);

    JSONObject jsonOrder2 = new JSONObject();
    jsonOrder2.put("id", "ORDER_B");
    instance.init("OLD_2", "ORG_2", jsonOrder2, false);

    Field oldOrderIdField = CancelOrderExecutor.class.getDeclaredField("oldOrderId");
    oldOrderIdField.setAccessible(true);
    assertEquals("OLD_2", oldOrderIdField.get(instance));

    Field paymentOrgField = CancelOrderExecutor.class.getDeclaredField("paymentOrganizationId");
    paymentOrgField.setAccessible(true);
    assertEquals("ORG_2", paymentOrgField.get(instance));
  }
}
