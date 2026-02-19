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
@RunWith(MockitoJUnitRunner.class)
public class DocLine_AmortizationTest {

  @Test
  public void testConstructorSetsFields() {
    DocLine_Amortization line = new DocLine_Amortization("AMZ", "HDR001", "LN001");

    assertEquals("AMZ", line.p_DocumentType);
    assertEquals("HDR001", line.m_TrxHeader_ID);
    assertEquals("LN001", line.m_TrxLine_ID);
  }

  @Test
  public void testAmountFieldIsPublicAndAccessible() {
    DocLine_Amortization line = new DocLine_Amortization("AMZ", "HDR001", "LN001");

    assertNull(line.Amount);

    line.Amount = "100.50";
    assertEquals("100.50", line.Amount);
  }

  @Test
  public void testGetServletInfo() {
    DocLine_Amortization line = new DocLine_Amortization("AMZ", "HDR001", "LN001");
    assertEquals("Servlet for the accounting", line.getServletInfo());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructorThrowsOnNullDocumentType() {
    new DocLine_Amortization(null, "HDR001", "LN001");
  }

  @Test
  public void testConstructorAllowsNullLineId() {
    DocLine_Amortization line = new DocLine_Amortization("AMZ", "HDR001", null);
    assertNotNull(line);
  }
}
