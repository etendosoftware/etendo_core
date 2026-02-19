/*
 * Copyright (C) 2026 Openbravo SLU
 * All Rights Reserved.
 */
package org.openbravo.common.filterexpression;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.client.application.ParameterUtils;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.erpCommon.utility.OBLedgerUtils;

/**
 * Tests for {@link AgingGeneralLedgerFilterExpression}.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class AgingGeneralLedgerFilterExpressionTest {

  private MockedStatic<RequestContext> requestContextStatic;
  private MockedStatic<ParameterUtils> parameterUtilsStatic;
  private MockedStatic<OBLedgerUtils> obLedgerUtilsStatic;

  @Mock
  private RequestContext requestContext;

  @Mock
  private HttpSession httpSession;

  private AgingGeneralLedgerFilterExpression filterExpression;
  /** Sets up test fixtures. */

  @Before
  public void setUp() {
    filterExpression = new AgingGeneralLedgerFilterExpression();

    requestContextStatic = mockStatic(RequestContext.class);
    requestContextStatic.when(RequestContext::get).thenReturn(requestContext);
    lenient().when(requestContext.getSession()).thenReturn(httpSession);

    parameterUtilsStatic = mockStatic(ParameterUtils.class);
    obLedgerUtilsStatic = mockStatic(OBLedgerUtils.class);
  }
  /** Tears down test fixtures. */

  @After
  public void tearDown() {
    if (obLedgerUtilsStatic != null) {
      obLedgerUtilsStatic.close();
    }
    if (parameterUtilsStatic != null) {
      parameterUtilsStatic.close();
    }
    if (requestContextStatic != null) {
      requestContextStatic.close();
    }
  }
  /**
   * Get expression returns ledger for org.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetExpressionReturnsLedgerForOrg() throws Exception {
    Map<String, String> requestMap = new HashMap<>();
    String expectedOrg = "ORG123";
    String expectedLedger = "LEDGER456";

    parameterUtilsStatic.when(() -> ParameterUtils.getJSExpressionResult(
        eq(requestMap), eq(httpSession), any(String.class)))
        .thenReturn(expectedOrg);

    obLedgerUtilsStatic.when(() -> OBLedgerUtils.getOrgLedger(expectedOrg))
        .thenReturn(expectedLedger);

    String result = filterExpression.getExpression(requestMap);
    assertEquals(expectedLedger, result);
  }
  /** Get expression returns null on exception. */

  @Test
  public void testGetExpressionReturnsNullOnException() {
    Map<String, String> requestMap = new HashMap<>();

    parameterUtilsStatic.when(() -> ParameterUtils.getJSExpressionResult(
        any(), any(), any(String.class)))
        .thenThrow(new RuntimeException("test error"));

    String result = filterExpression.getExpression(requestMap);
    assertNull(result);
  }
  /**
   * Get expression returns null when org is null.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetExpressionReturnsNullWhenOrgIsNull() throws Exception {
    Map<String, String> requestMap = new HashMap<>();

    parameterUtilsStatic.when(() -> ParameterUtils.getJSExpressionResult(
        eq(requestMap), eq(httpSession), any(String.class)))
        .thenReturn(null);

    obLedgerUtilsStatic.when(() -> OBLedgerUtils.getOrgLedger(null))
        .thenReturn(null);

    String result = filterExpression.getExpression(requestMap);
    assertNull(result);
  }
  /**
   * Get expression returns empty string when ledger empty.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetExpressionReturnsEmptyStringWhenLedgerEmpty() throws Exception {
    Map<String, String> requestMap = new HashMap<>();
    String orgId = "ORG001";

    parameterUtilsStatic.when(() -> ParameterUtils.getJSExpressionResult(
        eq(requestMap), eq(httpSession), any(String.class)))
        .thenReturn(orgId);

    obLedgerUtilsStatic.when(() -> OBLedgerUtils.getOrgLedger(orgId))
        .thenReturn("");

    String result = filterExpression.getExpression(requestMap);
    assertEquals("", result);
  }
}
