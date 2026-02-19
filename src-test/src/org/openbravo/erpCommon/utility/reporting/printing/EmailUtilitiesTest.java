package org.openbravo.erpCommon.utility.reporting.printing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mockStatic;

import javax.servlet.ServletException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.erpCommon.utility.OBMessageUtils;

/**
 * Tests for {@link EmailUtilities}.
 * Focuses on the testable static utility method getEmailValue.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class EmailUtilitiesTest {

  private MockedStatic<OBMessageUtils> obMessageUtilsStatic;

  @Before
  public void setUp() {
    obMessageUtilsStatic = mockStatic(OBMessageUtils.class);
    obMessageUtilsStatic.when(() -> OBMessageUtils.messageBD("NoSalesRepEmail"))
        .thenReturn("No sales rep email");
    obMessageUtilsStatic.when(() -> OBMessageUtils.messageBD("NoCustomerEmail"))
        .thenReturn("No customer email");
  }

  @After
  public void tearDown() {
    if (obMessageUtilsStatic != null) {
      obMessageUtilsStatic.close();
    }
  }

  @Test
  public void testGetEmailValueConditionTrueReturnsDefault() throws ServletException {
    String result = EmailUtilities.getEmailValue(true, "default@test.com", "alt@test.com", false, "");
    assertEquals("default@test.com", result);
  }

  @Test
  public void testGetEmailValueConditionFalseReturnsAlternative() throws ServletException {
    String result = EmailUtilities.getEmailValue(false, "default@test.com", "alt@test.com", false, "");
    assertEquals("alt@test.com", result);
  }

  @Test
  public void testGetEmailValueNullAllowedWhenFailIfEmptyFalse() throws ServletException {
    String result = EmailUtilities.getEmailValue(true, null, "alt@test.com", false, "");
    assertNull(result);
  }

  @Test
  public void testGetEmailValueEmptyAllowedWhenFailIfEmptyFalse() throws ServletException {
    String result = EmailUtilities.getEmailValue(true, "", "alt@test.com", false, "");
    assertEquals("", result);
  }

  @Test(expected = ServletException.class)
  public void testGetEmailValueNullThrowsWhenFailIfEmptyTrue() throws ServletException {
    EmailUtilities.getEmailValue(true, null, "alt@test.com", true, "NoSalesRepEmail");
  }

  @Test(expected = ServletException.class)
  public void testGetEmailValueEmptyThrowsWhenFailIfEmptyTrue() throws ServletException {
    EmailUtilities.getEmailValue(true, "", "alt@test.com", true, "NoSalesRepEmail");
  }

  @Test
  public void testGetEmailValueNonEmptyPassesWhenFailIfEmptyTrue() throws ServletException {
    String result = EmailUtilities.getEmailValue(true, "valid@test.com", "alt@test.com", true, "NoSalesRepEmail");
    assertEquals("valid@test.com", result);
  }

  @Test(expected = ServletException.class)
  public void testGetEmailValueAlternativeNullThrowsWhenFailIfEmpty() throws ServletException {
    EmailUtilities.getEmailValue(false, "default@test.com", null, true, "NoCustomerEmail");
  }
}
