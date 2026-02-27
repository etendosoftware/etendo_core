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
@SuppressWarnings({"java:S120"})
@RunWith(MockitoJUnitRunner.Silent.class)
public class EmailUtilitiesTest {

  private static final String NO_SALES_REP_EMAIL = "NoSalesRepEmail";
  private static final String DEFAULT_TEST_COM = "default@test.com";
  private static final String ALT_TEST_COM = "alt@test.com";

  private MockedStatic<OBMessageUtils> obMessageUtilsStatic;
  /** Sets up test fixtures. */

  @Before
  public void setUp() {
    obMessageUtilsStatic = mockStatic(OBMessageUtils.class);
    obMessageUtilsStatic.when(() -> OBMessageUtils.messageBD(NO_SALES_REP_EMAIL))
        .thenReturn("No sales rep email");
    obMessageUtilsStatic.when(() -> OBMessageUtils.messageBD("NoCustomerEmail"))
        .thenReturn("No customer email");
  }
  /** Tears down test fixtures. */

  @After
  public void tearDown() {
    if (obMessageUtilsStatic != null) {
      obMessageUtilsStatic.close();
    }
  }
  /**
   * Get email value condition true returns default.
   * @throws ServletException if an error occurs
   */

  @Test
  public void testGetEmailValueConditionTrueReturnsDefault() throws ServletException {
    String result = EmailUtilities.getEmailValue(true, DEFAULT_TEST_COM, ALT_TEST_COM, false, "");
    assertEquals(DEFAULT_TEST_COM, result);
  }
  /**
   * Get email value condition false returns alternative.
   * @throws ServletException if an error occurs
   */

  @Test
  public void testGetEmailValueConditionFalseReturnsAlternative() throws ServletException {
    String result = EmailUtilities.getEmailValue(false, DEFAULT_TEST_COM, ALT_TEST_COM, false, "");
    assertEquals(ALT_TEST_COM, result);
  }
  /**
   * Get email value null allowed when fail if empty false.
   * @throws ServletException if an error occurs
   */

  @Test
  public void testGetEmailValueNullAllowedWhenFailIfEmptyFalse() throws ServletException {
    String result = EmailUtilities.getEmailValue(true, null, ALT_TEST_COM, false, "");
    assertNull(result);
  }
  /**
   * Get email value empty allowed when fail if empty false.
   * @throws ServletException if an error occurs
   */

  @Test
  public void testGetEmailValueEmptyAllowedWhenFailIfEmptyFalse() throws ServletException {
    String result = EmailUtilities.getEmailValue(true, "", ALT_TEST_COM, false, "");
    assertEquals("", result);
  }
  /**
   * Get email value null throws when fail if empty true.
   * @throws ServletException if an error occurs
   */

  @Test(expected = ServletException.class)
  public void testGetEmailValueNullThrowsWhenFailIfEmptyTrue() throws ServletException {
    EmailUtilities.getEmailValue(true, null, ALT_TEST_COM, true, NO_SALES_REP_EMAIL);
  }
  /**
   * Get email value empty throws when fail if empty true.
   * @throws ServletException if an error occurs
   */

  @Test(expected = ServletException.class)
  public void testGetEmailValueEmptyThrowsWhenFailIfEmptyTrue() throws ServletException {
    EmailUtilities.getEmailValue(true, "", ALT_TEST_COM, true, NO_SALES_REP_EMAIL);
  }
  /**
   * Get email value non empty passes when fail if empty true.
   * @throws ServletException if an error occurs
   */

  @Test
  public void testGetEmailValueNonEmptyPassesWhenFailIfEmptyTrue() throws ServletException {
    String result = EmailUtilities.getEmailValue(true, "valid@test.com", ALT_TEST_COM, true, NO_SALES_REP_EMAIL);
    assertEquals("valid@test.com", result);
  }
  /**
   * Get email value alternative null throws when fail if empty.
   * @throws ServletException if an error occurs
   */

  @Test(expected = ServletException.class)
  public void testGetEmailValueAlternativeNullThrowsWhenFailIfEmpty() throws ServletException {
    EmailUtilities.getEmailValue(false, DEFAULT_TEST_COM, null, true, "NoCustomerEmail");
  }
}
