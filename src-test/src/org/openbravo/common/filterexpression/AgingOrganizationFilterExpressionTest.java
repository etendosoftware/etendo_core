package org.openbravo.common.filterexpression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.common.enterprise.Organization;

/**
 * Unit tests for the {@link AgingOrganizationFilterExpression} class.
 * Verifies the behavior of the filter expression logic for retrieving organization IDs.
 */
public class AgingOrganizationFilterExpressionTest {

  private AgingOrganizationFilterExpression filterExpression;
  private MockedStatic<OBContext> obContextMock;
  private MockedStatic<OBDal> obDalMock;

  @Mock
  private OBQuery<Organization> mockQuery;

  /**
   * Sets up the test environment before each test.
   * Mocks the static methods of {@link OBContext} and {@link OBDal}, and initializes the filter expression.
   */
  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    filterExpression = new AgingOrganizationFilterExpression();

    obContextMock = mockStatic(OBContext.class);
    obDalMock = mockStatic(OBDal.class);

    OBContext obContext = mock(OBContext.class);
    when(OBContext.getOBContext()).thenReturn(obContext);

    Organization mockOrg = mock(Organization.class);
    when(obContext.getCurrentOrganization()).thenReturn(mockOrg);
    when(mockOrg.getId()).thenReturn("SampleOrgID");

    OBDal mockOBDal = mock(OBDal.class);
    obDalMock.when(OBDal::getInstance).thenReturn(mockOBDal);
    when(mockOBDal.createQuery(eq(Organization.class), any(String.class))).thenReturn(mockQuery);
  }

  /**
   * Cleans up the test environment after each test.
   * Closes the mocked static methods to release resources.
   */
  @AfterEach
  public void tearDown() {
    if (obContextMock != null) {
      obContextMock.close();
    }
    if (obDalMock != null) {
      obDalMock.close();
    }
  }

  /**
   * Tests the {@code getExpression} method when a valid organization is found in the context.
   * Verifies that the correct organization ID is returned.
   */
  @Test
  public void testGetExpressionValidContextOrg() {
    Organization validOrg = mock(Organization.class);
    when(validOrg.getId()).thenReturn("ValidOrgID");
    when(mockQuery.uniqueResult()).thenReturn(validOrg);

    String result = filterExpression.getExpression(Map.of());

    assertEquals("ValidOrgID", result);
    verify(mockQuery).setMaxResult(1);
  }

  /**
   * Tests the {@code getExpression} method when no valid organization is found in the context.
   * Verifies that the fallback organization ID is returned.
   */
  @Test
  public void testGetExpressionInvalidContextOrgFallback() {
    when(mockQuery.uniqueResult()).thenReturn(null);

    Organization fallbackOrg = mock(Organization.class);
    when(fallbackOrg.getId()).thenReturn("FallbackOrgID");
    when(mockQuery.uniqueResult()).thenReturn(fallbackOrg);

    String result = filterExpression.getExpression(Map.of());

    assertEquals("FallbackOrgID", result);
  }

  /**
   * Tests the {@code getExpression} method when no valid organization is found at all.
   * Verifies that the result is {@code null}.
   */
  @Test
  public void testGetExpressionNoValidOrg() {
    when(mockQuery.uniqueResult()).thenReturn(null);

    String result = filterExpression.getExpression(Map.of());

    assertNull(result);
  }

  /**
   * Tests the {@code getExpression} method with a valid organization in the context.
   * Verifies that the correct organization ID is returned and the query is properly parameterized.
   */
  @Test
  public void testGetValidOrganizationWithContextOrg() {
    Organization validOrg = mock(Organization.class);
    when(validOrg.getId()).thenReturn("ValidContextOrgID");
    when(mockQuery.uniqueResult()).thenReturn(validOrg);

    String result = filterExpression.getExpression(Map.of());

    assertEquals("ValidContextOrgID", result);
    verify(mockQuery).setNamedParameter("contextOrgId", "SampleOrgID");
  }
}
