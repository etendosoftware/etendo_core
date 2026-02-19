package org.openbravo.erpCommon.ad_forms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
/** Tests for {@link DocLine_DPManagement}. */
@SuppressWarnings({"java:S101", "java:S120"})

@RunWith(MockitoJUnitRunner.Silent.class)
public class DocLine_DPManagementTest {

  private DocLine_DPManagement instance;
  /** Sets up test fixtures. */

  @Before
  public void setUp() {
    instance = new DocLine_DPManagement("DOCTYPE", "HEADER_001", "LINE_001");
  }
  /** Constructor sets document type. */

  @Test
  public void testConstructorSetsDocumentType() {
    assertEquals("DOCTYPE", instance.p_DocumentType);
  }
  /** Constructor sets header id. */

  @Test
  public void testConstructorSetsHeaderId() {
    assertEquals("HEADER_001", instance.m_TrxHeader_ID);
  }
  /** Constructor sets line id. */

  @Test
  public void testConstructorSetsLineId() {
    assertEquals("LINE_001", instance.m_TrxLine_ID);
  }
  /** Fields default null. */

  @Test
  public void testFieldsDefaultNull() {
    assertAllFieldsNull();
  }
  /** Set fields directly. */

  @Test
  public void testSetFieldsDirectly() {
    setAllFields("100.00", "Y", "ACTIVE", "PENDING", "2024-01-01", "N", "Y");

    assertEquals("100.00", instance.Amount);
    assertEquals("Y", instance.Isreceipt);
    assertEquals("ACTIVE", instance.StatusTo);
    assertEquals("PENDING", instance.StatusFrom);
    assertEquals("2024-01-01", instance.conversionDate);
    assertEquals("N", instance.IsManual);
    assertEquals("Y", instance.IsDirectPosting);
  }

  private void assertAllFieldsNull() {
    assertNull(instance.Amount);
    assertNull(instance.Isreceipt);
    assertNull(instance.StatusTo);
    assertNull(instance.StatusFrom);
    assertNull(instance.conversionDate);
    assertNull(instance.IsManual);
    assertNull(instance.IsDirectPosting);
  }

  private void setAllFields(String amount, String isReceipt, String statusTo, String statusFrom,
      String convDate, String isManual, String isDirectPosting) {
    instance.Amount = amount;
    instance.Isreceipt = isReceipt;
    instance.StatusTo = statusTo;
    instance.StatusFrom = statusFrom;
    instance.conversionDate = convDate;
    instance.IsManual = isManual;
    instance.IsDirectPosting = isDirectPosting;
  }
  /** Get servlet info. */

  @Test
  public void testGetServletInfo() {
    assertEquals("Servlet for the accounting", instance.getServletInfo());
  }
}
