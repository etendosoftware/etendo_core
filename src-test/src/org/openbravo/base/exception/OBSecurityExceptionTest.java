/*
 *************************************************************************
 * The contents of this file are subject to the Etendo License
 * (the "License"), you may not use this file except in compliance with
 * the License.
 * You may obtain a copy of the License at
 * https://github.com/etendosoftware/etendo_core/blob/main/legal/Etendo_license.txt
 * Software distributed under the License is distributed on an
 * "AS IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing rights
 * and limitations under the License.
 * All portions are Copyright (C) 2021-2026 FUTIT SERVICES, S.L
 * All Rights Reserved.
 * Contributor(s): Futit Services S.L.
 *************************************************************************
 */
package org.openbravo.base.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link OBSecurityException}.
 */
@DisplayName("OBSecurityException")
public class OBSecurityExceptionTest {

  private static final String ACCESS_DENIED = "Access denied";

  @Test
  @DisplayName("extends OBException and RuntimeException")
  void testClassHierarchy() {
    OBSecurityException ex = new OBSecurityException("test");
    assertInstanceOf(OBException.class, ex);
    assertInstanceOf(RuntimeException.class, ex);
  }

  @Test
  @DisplayName("constructor with message stores the message")
  void testMessageConstructor() {
    OBSecurityException ex = new OBSecurityException(ACCESS_DENIED);
    assertEquals(ACCESS_DENIED, ex.getMessage());
  }

  @Test
  @DisplayName("constructor with message and cause stores both")
  void testMessageAndCauseConstructor() {
    Throwable cause = new IllegalStateException("root cause");
    OBSecurityException ex = new OBSecurityException(ACCESS_DENIED, cause);
    assertEquals(ACCESS_DENIED, ex.getMessage());
    assertNotNull(ex.getCause());
  }

  @Test
  @DisplayName("constructor with cause only stores the cause")
  void testCauseOnlyConstructor() {
    Throwable cause = new IllegalStateException("root cause");
    OBSecurityException ex = new OBSecurityException(cause);
    assertSame(cause, ex.getCause());
  }

  @Test
  @DisplayName("constructor with message and logException flag stores message")
  void testMessageWithLogFlag() {
    OBSecurityException ex = new OBSecurityException("Security violation", true);
    assertEquals("Security violation", ex.getMessage());
  }

  @Test
  @DisplayName("no-arg constructor creates instance without message")
  void testNoArgConstructor() {
    OBSecurityException ex = new OBSecurityException();
    assertNull(ex.getMessage());
  }

  @Test
  @DisplayName("exception can be thrown and caught as OBException")
  void testCatchAsOBException() {
    try {
      throw new OBSecurityException("forbidden");
    } catch (OBException e) {
      assertEquals("forbidden", e.getMessage());
    }
  }
}
