/*
 * Copyright (C) 2026 Openbravo SLU
 * All Rights Reserved.
 */
package org.openbravo.service.datasource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Tests for {@link CheckTreeOperationManager} inner class ActionResponse.
 */
@RunWith(MockitoJUnitRunner.class)
public class CheckTreeOperationManagerTest {

  private TestableCheckTreeOperationManager manager;
  /** Sets up test fixtures. */

  @Before
  public void setUp() {
    manager = new TestableCheckTreeOperationManager();
  }
  /** Action response success only constructor. */

  @Test
  public void testActionResponseSuccessOnlyConstructor() {
    CheckTreeOperationManager.ActionResponse response = manager.createResponse(true);
    assertTrue(response.isSuccess());
    assertNull(response.getMessage());
    assertNull(response.getMessageType());
  }
  /** Action response failure only constructor. */

  @Test
  public void testActionResponseFailureOnlyConstructor() {
    CheckTreeOperationManager.ActionResponse response = manager.createResponse(false);
    assertFalse(response.isSuccess());
  }
  /** Action response full constructor. */

  @Test
  public void testActionResponseFullConstructor() {
    CheckTreeOperationManager.ActionResponse response = manager.createResponse(true, "info",
        "Operation completed");
    assertTrue(response.isSuccess());
    assertEquals("info", response.getMessageType());
    assertEquals("Operation completed", response.getMessage());
  }
  /** Action response setters. */

  @Test
  public void testActionResponseSetters() {
    CheckTreeOperationManager.ActionResponse response = manager.createResponse(false);
    response.setSuccess(true);
    response.setMessageType("warning");
    response.setMessage("Some warning");

    assertTrue(response.isSuccess());
    assertEquals("warning", response.getMessageType());
    assertEquals("Some warning", response.getMessage());
  }
  /** Action response error message. */

  @Test
  public void testActionResponseErrorMessage() {
    CheckTreeOperationManager.ActionResponse response = manager.createResponse(false, "error",
        "Node cannot be moved to that position");
    assertFalse(response.isSuccess());
    assertEquals("error", response.getMessageType());
    assertEquals("Node cannot be moved to that position", response.getMessage());
  }

  /**
   * Concrete subclass to allow instantiation and access to the protected ActionResponse inner
   * class.
   */
  private static class TestableCheckTreeOperationManager extends CheckTreeOperationManager {
    /** Check node movement. */

    @Override
    public ActionResponse checkNodeMovement(Map<String, String> parameters, String nodeId,
        String newParentId, String prevNodeId, String nextNodeId) {
      return new ActionResponse(true);
    }
    /** Create response. */

    public ActionResponse createResponse(boolean success) {
      return new ActionResponse(success);
    }
    /** Create response. */

    public ActionResponse createResponse(boolean success, String messageType, String message) {
      return new ActionResponse(success, messageType, message);
    }
  }
}
