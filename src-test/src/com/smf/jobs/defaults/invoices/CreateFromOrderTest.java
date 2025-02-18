package com.smf.jobs.defaults.invoices;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.apache.commons.lang.mutable.MutableBoolean;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.model.common.order.Order;

import com.smf.jobs.ActionResult;
import com.smf.jobs.Result;
import com.smf.jobs.defaults.Utility;

/**
 * Unit tests for the {@link CreateFromOrder} class.
 *
 * <p>This test class verifies the behavior of the {@code action} method and the
 * {@code getInputClass} method in the {@code CreateFromOrder} class, ensuring
 * expected outputs and proper functionality.
 */
@RunWith(MockitoJUnitRunner.class)
public class CreateFromOrderTest {

  /**
   * A spy on the {@link CreateFromOrder} class to partially mock its behavior
   * while testing its real implementations.
   */
  @Spy
  @InjectMocks
  private CreateFromOrder createFromOrder;

  /**
   * Sets up the test environment before each test case.
   *
   * <p>Currently, no explicit initialization is required.
   */
  @Before
  public void setUp() {
    // Setup tasks (currently empty as no explicit initialization is needed)
  }

  /**
   * Tests the {@code action} method when the button value is "REFRESH".
   *
   * <p>Validates that the method correctly processes the "REFRESH" action, returning
   * a non-null {@link ActionResult} of type {@link Result.Type#SUCCESS}.
   *
   * @throws Exception if any JSON processing errors occur.
   */
  @Test
  public void testActionWithRefreshButton() throws Exception {
    JSONObject parameters = new JSONObject();
    parameters.put("_buttonValue", "REFRESH");
    MutableBoolean isStopped = new MutableBoolean(false);

    ActionResult result = createFromOrder.action(parameters, isStopped);

    assertNotNull(Utility.RESULT_SHOULD_NOT_BE_NULL, result);
    assertEquals(Utility.SHOULD_RETURN_SUCCESS_TYPE, Result.Type.SUCCESS, result.getType());
  }

  /**
   * Tests the {@code getInputClass} method.
   *
   * <p>Ensures that the method correctly returns {@link Order} as the expected input class.
   */
  @Test
  public void testGetInputClass() {
    assertEquals("Should return Order.class", Order.class, createFromOrder.getInputClass());
  }
}
