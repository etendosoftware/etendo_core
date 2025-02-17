package org.openbravo.advpaymentmngt.actionHandler;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
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
import org.openbravo.base.exception.OBException;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.security.OrganizationStructureProvider;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.Organization;

/**
 * Unit tests for the AddPaymentOrganizationActionHandler class.
 */
@RunWith(MockitoJUnitRunner.class)
public class AddPaymentOrganizationActionHandlerTest extends WeldBaseTest {

  private static final String ORG_ID = "TEST_ORG_ID";
  private static final String CURRENCY_ID = "TEST_CURRENCY_ID";
  private static final String CURRENCY_IDENTIFIER = "USD";

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

  @Before
  public void setUp() {
    // Setup static mocks
    mockedOBDal = mockStatic(OBDal.class);
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
    mockedStringUtils.when(() -> StringUtils.isNotEmpty(ORG_ID)).thenReturn(true);
    mockedStringUtils.when(() -> StringUtils.isNotEmpty("")).thenReturn(false);
    mockedStringUtils.when(() -> StringUtils.isNotEmpty(null)).thenReturn(false);

    // Setup organization and currency
    when(obDal.get(Organization.class, ORG_ID)).thenReturn(organization);
    when(obContext.getOrganizationStructureProvider()).thenReturn(orgStructureProvider);
    when(orgStructureProvider.getLegalEntity(organization)).thenReturn(legalEntity);
    when(legalEntity.getCurrency()).thenReturn(currency);
    when(currency.getId()).thenReturn(CURRENCY_ID);
    when(currency.getIdentifier()).thenReturn(CURRENCY_IDENTIFIER);
  }

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

  @Test
  public void testExecute_WithValidOrganization() throws Exception {
    // Given
    String jsonData = createJsonData(ORG_ID);
    Map<String, Object> parameters = new HashMap<>();

    // When
    JSONObject result = handler.execute(parameters, jsonData);

    // Then
    assertEquals(CURRENCY_ID, result.getString("currency"));
    assertEquals(CURRENCY_IDENTIFIER, result.getString("currencyIdIdentifier"));
  }

  @Test
  public void testExecute_WithEmptyOrganization() throws Exception {
    // Given
    String jsonData = createJsonData("");
    Map<String, Object> parameters = new HashMap<>();

    // Expect exception since currency will be null
    expectedException.expect(OBException.class);

    // When
    handler.execute(parameters, jsonData);

    // Then - exception is expected
  }

  @Test
  public void testExecute_WithNullOrganization() throws Exception {
    // Given
    String jsonData = "{\"organization\":null}";
    Map<String, Object> parameters = new HashMap<>();

    // Expect exception
    expectedException.expect(OBException.class);

    // When
    handler.execute(parameters, jsonData);

    // Then - exception is expected
  }

  @Test
  public void testExecute_WithInvalidJson() throws Exception {
    // Given
    String jsonData = "invalid json";
    Map<String, Object> parameters = new HashMap<>();

    // Expect exception
    expectedException.expect(OBException.class);

    // When
    handler.execute(parameters, jsonData);

    // Then - exception is expected
  }

  @Test
  public void testExecute_WithNullCurrency() throws Exception {
    // Given
    String jsonData = createJsonData(ORG_ID);
    Map<String, Object> parameters = new HashMap<>();

    // Mock null currency
    when(legalEntity.getCurrency()).thenReturn(null);

    // Expect exception
    expectedException.expect(OBException.class);

    // When
    handler.execute(parameters, jsonData);

    // Then - exception is expected
  }

  private String createJsonData(String organizationId) {
    return String.format("{\"organization\":\"%s\"}", organizationId);
  }
}