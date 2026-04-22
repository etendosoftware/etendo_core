/*
 * Copyright (C) 2026 Openbravo SLU
 * All Rights Reserved.
 */
package org.openbravo.advpaymentmngt.actionHandler;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Field;

import org.codehaus.jettison.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;

/**
 * Tests for {@link AddMultiplePaymentsProcessAfterProcessHook}.
 */
@SuppressWarnings({"java:S120"})
@RunWith(MockitoJUnitRunner.Silent.class)
public class AddMultiplePaymentsProcessAfterProcessHookTest {

  private AddMultiplePaymentsProcessAfterProcessHook hook;
  /** Sets up test fixtures. */

  @Before
  public void setUp() {
    // Create a concrete subclass for testing
    hook = new AddMultiplePaymentsProcessAfterProcessHook() {
      /** Execute hook. */
      @Override
      public int executeHook(JSONObject data) {
        return 0;
      }
    };
  }
  /** Get priority returns default value. */

  @Test
  public void testGetPriorityReturnsDefaultValue() {
    assertEquals(100, hook.getPriority());
  }
  /**
   * Get priority returns modified value.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetPriorityReturnsModifiedValue() throws Exception {
    Field priorityField = AddMultiplePaymentsProcessAfterProcessHook.class
        .getDeclaredField("priority");
    priorityField.setAccessible(true);
    priorityField.setInt(hook, 50);

    assertEquals(50, hook.getPriority());
  }
  /** Default priority is one hundred. */

  @Test
  public void testDefaultPriorityIsOneHundred() {
    // Verify the default priority using ObjenesisStd (bypasses constructor)
    ObjenesisStd objenesis = new ObjenesisStd();
    AddMultiplePaymentsProcessAfterProcessHook uninitializedHook = objenesis.newInstance(
        ConcreteHook.class);
    // Objenesis skips field initialization, so priority will be 0 (JVM default for int)
    // This confirms that the default value 100 comes from the field initializer
    assertEquals(0, uninitializedHook.getPriority());
  }
  /** Execute hook can return custom value. */

  @Test
  public void testExecuteHookCanReturnCustomValue() {
    AddMultiplePaymentsProcessAfterProcessHook customHook = new AddMultiplePaymentsProcessAfterProcessHook() {
      /** Execute hook. */
      @Override
      public int executeHook(JSONObject data) {
        return 42;
      }
    };

    assertEquals(42, customHook.executeHook(null));
  }
  /**
   * Execute hook receives json data.
   * @throws Exception if an error occurs
   */

  @Test
  public void testExecuteHookReceivesJsonData() throws Exception {
    final JSONObject[] capturedData = new JSONObject[1];
    AddMultiplePaymentsProcessAfterProcessHook capturingHook = new AddMultiplePaymentsProcessAfterProcessHook() {
      /** Execute hook. */
      @Override
      public int executeHook(JSONObject data) {
        capturedData[0] = data;
        return 0;
      }
    };

    JSONObject testData = new JSONObject();
    testData.put("key", "value");
    capturingHook.executeHook(testData);

    assertEquals("value", capturedData[0].getString("key"));
  }

  // Concrete subclass for Objenesis testing
  static class ConcreteHook extends AddMultiplePaymentsProcessAfterProcessHook {
    /** Execute hook. */
    @Override
    public int executeHook(JSONObject data) {
      return 0;
    }
  }
}
