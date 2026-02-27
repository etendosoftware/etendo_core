package org.openbravo.advpaymentmngt.actionHandler;

import static org.junit.Assert.assertEquals;
import static org.openbravo.test.base.mock.MockitoStaticMockUtils.mockStaticSafely;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.advpaymentmngt.TestConstants;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.security.OrganizationStructureProvider;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.Organization;

/**
 * Unit tests for the AddPaymentOrganizationActionHandler class.
 */
@RunWith(MockitoJUnitRunner.class)
public class AddPaymentOrganizationActionHandlerTest {

  /**
   * Rule for handling expected exceptions in tests.
   */
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Mock
  private OBDal obDal;

  @Mock
  private OBContext obContext;

  @Mock
  private Organization organization;

  @Mock
  private Organization legalEntity;

  @Mock
  private Currency currency;

  @Mock
  private OrganizationStructureProvider orgStructureProvider;

  private MockedStatic<OBDal> mockedOBDal;
  private MockedStatic<OBContext> mockedOBContext;
  private MockedStatic<StringUtils> mockedStringUtils;

  @InjectMocks
  private AddPaymentOrganizationActionHandler handler;

  /**
   * Sets up the test environment before each test.
   */
  @Before
  public void setUp() {
    // Setup static mocks
    mockedOBDal = mockStaticSafely(OBDal.class);
    mockedOBContext = mockStatic(OBContext.class);
    mockedStringUtils = mockStatic(StringUtils.class);

    // Mock OBDal
    mockedOBDal.when(OBDal::getInstance).thenReturn(obDal);

    // Mock OBContext - Fix the unfinished stubbing by using thenAnswer for void methods
    mockedOBContext.when(OBContext::getOBContext).thenReturn(obContext);
    // For void methods, use thenAnswer that returns null to complete the stubbing
    mockedOBContext.when(() -> OBContext.setAdminMode(anyBoolean())).thenAnswer(invocation -> null);
    mockedOBContext.when(OBContext::restorePreviousMode).thenAnswer(invocation -> null);

    // Mock StringUtils
    mockedStringUtils.when(() -> StringUtils.isNotEmpty(TestConstants.ORG_ID)).thenReturn(true);
    mockedStringUtils.when(() -> StringUtils.isNotEmpty("")).thenReturn(false);
    mockedStringUtils.when(() -> StringUtils.isNotEmpty(null)).thenReturn(false);

    // Setup organization and currency
    when(obDal.get(Organization.class, TestConstants.ORG_ID)).thenReturn(organization);
    when(obContext.getOrganizationStructureProvider()).thenReturn(orgStructureProvider);
    when(orgStructureProvider.getLegalEntity(organization)).thenReturn(legalEntity);
    when(legalEntity.getCurrency()).thenReturn(currency);
    when(currency.getId()).thenReturn(TestConstants.CURRENCY_ID);
    when(currency.getIdentifier()).thenReturn(TestConstants.CURRENCY_IDENTIFIER);
  }

  /**
   * Cleans up the test environment after each test.
   */
  @After
  public void tearDown() {
    if (mockedOBDal != null) {
      mockedOBDal.close();
    }
    if (mockedOBContext != null) {
      mockedOBContext.close();
    }
    if (mockedStringUtils != null) {
      mockedStringUtils.close();
    }
  }

  /**
   * Tests the execute method with a valid organization.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testExecuteWithValidOrganization() throws Exception {
    // Given
    String jsonData = createJsonData(TestConstants.ORG_ID);
    Map<String, Object> parameters = new HashMap<>();

    // When
    JSONObject result = handler.execute(parameters, jsonData);

    // Then
    assertEquals(TestConstants.CURRENCY_ID, result.getString("currency"));
    assertEquals(TestConstants.CURRENCY_IDENTIFIER, result.getString("currencyIdIdentifier"));
  }

  /**
   * Tests the execute method with an empty organization.
   */
  @Test
  public void testExecuteWithEmptyOrganization() {
    // Given
    String jsonData = createJsonData("");
    Map<String, Object> parameters = new HashMap<>();

    // Expect exception since currency will be null
    expectedException.expect(OBException.class);

    // When
    handler.execute(parameters, jsonData);

    // Then - exception is expected
  }

  /**
   * Tests the execute method with a null organization.
   */
  @Test
  public void testExecuteWithNullOrganization() {
    // Given
    String jsonData = "{\"organization\":null}";
    Map<String, Object> parameters = new HashMap<>();

    // Expect exception
    expectedException.expect(OBException.class);

    // When
    handler.execute(parameters, jsonData);

    // Then - exception is expected
  }

  /**
   * Tests the execute method with invalid JSON data.
   */
  @Test
  public void testExecuteWithInvalidJson() {
    // Given
    String jsonData = "invalid json";
    Map<String, Object> parameters = new HashMap<>();

    // Expect exception
    expectedException.expect(OBException.class);

    // When
    handler.execute(parameters, jsonData);

    // Then - exception is expected
  }

  /**
   * Tests the execute method with a null currency.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testExecuteWithNullCurrency() throws Exception {
    // Given
    String jsonData = createJsonData(TestConstants.ORG_ID);
    Map<String, Object> parameters = new HashMap<>();

    // Mock null currency
    when(legalEntity.getCurrency()).thenReturn(null);

    // Expect exception
    expectedException.expect(OBException.class);

    // When
    handler.execute(parameters, jsonData);

    // Then - exception is expected
  }

  /**
   * Creates JSON data for the given organization ID.
   *
   * @param organizationId
   *     the organization ID
   * @return the JSON data as a string
   */
  private String createJsonData(String organizationId) {
    return String.format("{\"organization\":\"%s\"}", organizationId);
  }
}
