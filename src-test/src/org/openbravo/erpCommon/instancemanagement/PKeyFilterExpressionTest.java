/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2009-2025 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.erpCommon.instancemanagement;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.criterion.Criterion;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.system.System;

/**
 * Unit test class for PKeyFilterExpression
 *
 * <p>This test class validates the filter expression functionality
 * for retrieving the system instance key. It uses Mockito to mock
 * database dependencies and verify behavior without requiring an
 * actual database connection.</p>
 */
@RunWith(MockitoJUnitRunner.class)
public class PKeyFilterExpressionTest {

  private PKeyFilterExpression pKeyFilterExpression;
  private OBDal mockOBDal;
  private OBCriteria<System> mockCriteria;

  @Before
  public void setUp() {
    pKeyFilterExpression = new PKeyFilterExpression();
    mockOBDal = mock(OBDal.class);
    mockCriteria = mock(OBCriteria.class);
  }

  /**
   * Test that getExpression returns the instance key when an active system is found
   */
  @Test
  public void testGetExpression_WithActiveSystem() {
    // Given
    System mockSystem = mock(System.class);
    String expectedInstanceKey = "TEST-INSTANCE-KEY-123";
    when(mockSystem.getInstanceKey()).thenReturn(expectedInstanceKey);

    Map<String, String> requestMap = new HashMap<>();

    // Mock the criteria chain
    when(mockCriteria.add(any(Criterion.class))).thenReturn(mockCriteria);
    when(mockCriteria.setMaxResults(anyInt())).thenReturn(mockCriteria);
    when(mockCriteria.uniqueResult()).thenReturn(mockSystem);
    when(mockOBDal.createCriteria(eq(System.class))).thenReturn(mockCriteria);

    // When
    String result;
    try (MockedStatic<OBDal> mockedOBDal = Mockito.mockStatic(OBDal.class)) {
      mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDal);
      result = pKeyFilterExpression.getExpression(requestMap);
    }

    // Then
    assertEquals("Should return the instance key from active system", expectedInstanceKey, result);
  }

  /**
   * Test that getExpression returns empty string when no active system is found
   */
  @Test
  public void testGetExpression_WithNoActiveSystem() {
    // Given
    Map<String, String> requestMap = new HashMap<>();

    // Mock the criteria chain to return null
    when(mockCriteria.add(any(Criterion.class))).thenReturn(mockCriteria);
    when(mockCriteria.setMaxResults(anyInt())).thenReturn(mockCriteria);
    when(mockCriteria.uniqueResult()).thenReturn(null);
    when(mockOBDal.createCriteria(eq(System.class))).thenReturn(mockCriteria);

    // When
    String result;
    try (MockedStatic<OBDal> mockedOBDal = Mockito.mockStatic(OBDal.class)) {
      mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDal);
      result = pKeyFilterExpression.getExpression(requestMap);
    }

    // Then
    assertEquals("Should return empty string when no active system found", "", result);
  }

  /**
   * Test that getExpression handles empty request map correctly
   */
  @Test
  public void testGetExpression_WithEmptyRequestMap() {
    // Given
    System mockSystem = mock(System.class);
    String expectedInstanceKey = "ANOTHER-KEY-456";
    when(mockSystem.getInstanceKey()).thenReturn(expectedInstanceKey);

    Map<String, String> emptyRequestMap = new HashMap<>();

    // Mock the criteria chain
    when(mockCriteria.add(any(Criterion.class))).thenReturn(mockCriteria);
    when(mockCriteria.setMaxResults(anyInt())).thenReturn(mockCriteria);
    when(mockCriteria.uniqueResult()).thenReturn(mockSystem);
    when(mockOBDal.createCriteria(eq(System.class))).thenReturn(mockCriteria);

    // When
    String result;
    try (MockedStatic<OBDal> mockedOBDal = Mockito.mockStatic(OBDal.class)) {
      mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDal);
      result = pKeyFilterExpression.getExpression(emptyRequestMap);
    }

    // Then
    assertEquals("Should work correctly with empty request map", expectedInstanceKey, result);
  }

  /**
   * Test that getExpression handles null request map correctly
   */
  @Test
  public void testGetExpression_WithNullRequestMap() {
    // Given
    System mockSystem = mock(System.class);
    String expectedInstanceKey = "NULL-MAP-KEY-789";
    when(mockSystem.getInstanceKey()).thenReturn(expectedInstanceKey);

    // Mock the criteria chain
    when(mockCriteria.add(any(Criterion.class))).thenReturn(mockCriteria);
    when(mockCriteria.setMaxResults(anyInt())).thenReturn(mockCriteria);
    when(mockCriteria.uniqueResult()).thenReturn(mockSystem);
    when(mockOBDal.createCriteria(eq(System.class))).thenReturn(mockCriteria);

    // When
    String result;
    try (MockedStatic<OBDal> mockedOBDal = Mockito.mockStatic(OBDal.class)) {
      mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDal);
      result = pKeyFilterExpression.getExpression(null);
    }

    // Then
    assertEquals("Should work correctly with null request map", expectedInstanceKey, result);
  }
}
