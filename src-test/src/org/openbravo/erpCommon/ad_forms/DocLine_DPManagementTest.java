package org.openbravo.erpCommon.ad_forms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class DocLine_DPManagementTest {

  private DocLine_DPManagement instance;

  @Before
  public void setUp() {
    instance = new DocLine_DPManagement("DOCTYPE", "HEADER_001", "LINE_001");
  }

  @Test
  public void testConstructorSetsDocumentType() {
    assertEquals("DOCTYPE", instance.p_DocumentType);
  }

  @Test
  public void testConstructorSetsHeaderId() {
    assertEquals("HEADER_001", instance.m_TrxHeader_ID);
  }

  @Test
  public void testConstructorSetsLineId() {
    assertEquals("LINE_001", instance.m_TrxLine_ID);
  }

  @Test
  public void testFieldsDefaultNull() {
    assertNull(instance.Amount);
    assertNull(instance.Isreceipt);
    assertNull(instance.StatusTo);
    assertNull(instance.StatusFrom);
    assertNull(instance.conversionDate);
    assertNull(instance.IsManual);
    assertNull(instance.IsDirectPosting);
  }

  @Test
  public void testSetFieldsDirectly() {
    instance.Amount = "100.00";
    instance.Isreceipt = "Y";
    instance.StatusTo = "ACTIVE";
    instance.StatusFrom = "PENDING";
    instance.conversionDate = "2024-01-01";
    instance.IsManual = "N";
    instance.IsDirectPosting = "Y";

    assertEquals("100.00", instance.Amount);
    assertEquals("Y", instance.Isreceipt);
    assertEquals("ACTIVE", instance.StatusTo);
    assertEquals("PENDING", instance.StatusFrom);
    assertEquals("2024-01-01", instance.conversionDate);
    assertEquals("N", instance.IsManual);
    assertEquals("Y", instance.IsDirectPosting);
  }

  @Test
  public void testGetServletInfo() {
    assertEquals("Servlet for the accounting", instance.getServletInfo());
  }
}
