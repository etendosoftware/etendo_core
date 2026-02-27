/*
 * Copyright (C) 2026 Openbravo SLU
 * All Rights Reserved.
 */
package org.openbravo.erpCommon.ad_forms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Tests for {@link DocLine_Amortization}.
 */
@SuppressWarnings({"java:S101", "java:S120"})
@RunWith(MockitoJUnitRunner.class)
public class DocLine_AmortizationTest {

  private static final String LN001 = "LN001";
  private static final String HDR001 = "HDR001";
  /** Constructor sets fields. */

  @Test
  public void testConstructorSetsFields() {
    DocLine_Amortization line = new DocLine_Amortization("AMZ", HDR001, LN001);

    assertEquals("AMZ", line.p_DocumentType);
    assertEquals(HDR001, line.m_TrxHeader_ID);
    assertEquals(LN001, line.m_TrxLine_ID);
  }
  /** Amount field is public and accessible. */

  @Test
  public void testAmountFieldIsPublicAndAccessible() {
    DocLine_Amortization line = new DocLine_Amortization("AMZ", HDR001, LN001);

    assertNull(line.Amount);

    line.Amount = "100.50";
    assertEquals("100.50", line.Amount);
  }
  /** Get servlet info. */

  @Test
  public void testGetServletInfo() {
    DocLine_Amortization line = new DocLine_Amortization("AMZ", HDR001, LN001);
    assertEquals("Servlet for the accounting", line.getServletInfo());
  }
  /** Constructor throws on null document type. */

  @Test(expected = IllegalArgumentException.class)
  public void testConstructorThrowsOnNullDocumentType() {
    new DocLine_Amortization(null, HDR001, LN001);
  }
  /** Constructor allows null line id. */

  @Test
  public void testConstructorAllowsNullLineId() {
    DocLine_Amortization line = new DocLine_Amortization("AMZ", HDR001, null);
    assertNotNull(line);
  }
}
