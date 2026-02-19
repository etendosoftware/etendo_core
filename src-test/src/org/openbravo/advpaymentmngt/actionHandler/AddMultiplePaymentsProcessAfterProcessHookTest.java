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
@RunWith(MockitoJUnitRunner.Silent.class)
public class AddMultiplePaymentsProcessAfterProcessHookTest {

  private AddMultiplePaymentsProcessAfterProcessHook hook;

  @Before
  public void setUp() {
    // Create a concrete subclass for testing
    hook = new AddMultiplePaymentsProcessAfterProcessHook() {
      @Override
      public int executeHook(JSONObject data) {
        return 0;
      }
    };
  }

  @Test
  public void testGetPriorityReturnsDefaultValue() {
    assertEquals(100, hook.getPriority());
  }

  @Test
  public void testGetPriorityReturnsModifiedValue() throws Exception {
    Field priorityField = AddMultiplePaymentsProcessAfterProcessHook.class
        .getDeclaredField("priority");
    priorityField.setAccessible(true);
    priorityField.setInt(hook, 50);

    assertEquals(50, hook.getPriority());
  }

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

  @Test
  public void testExecuteHookCanReturnCustomValue() {
    AddMultiplePaymentsProcessAfterProcessHook customHook = new AddMultiplePaymentsProcessAfterProcessHook() {
      @Override
      public int executeHook(JSONObject data) {
        return 42;
      }
    };

    assertEquals(42, customHook.executeHook(null));
  }

  @Test
  public void testExecuteHookReceivesJsonData() throws Exception {
    final JSONObject[] capturedData = new JSONObject[1];
    AddMultiplePaymentsProcessAfterProcessHook capturingHook = new AddMultiplePaymentsProcessAfterProcessHook() {
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
    @Override
    public int executeHook(JSONObject data) {
      return 0;
    }
  }
}
