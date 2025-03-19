package org.openbravo.advpaymentmngt.actionHandler;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.openbravo.advpaymentmngt.TestConstants;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.domain.Preference;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;

/**
 * Unit tests for the MatchStatementOnLoadPreferenceActionHandler class.
 */
public class MatchStatementOnLoadPreferenceActionHandlerTest {

  /**
   * Rule for handling expected exceptions in tests.
   */
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private MatchStatementOnLoadPreferenceActionHandler handler;
  private MockedStatic<OBDal> mockedOBDal;
  private MockedStatic<OBContext> mockedOBContext;
  private MockedStatic<OBProvider> mockedOBProvider;
  private AutoCloseable mocks;

  @Mock
  private OBDal mockOBDal;
  @Mock
  private OBContext mockOBContext;
  @Mock
  private Preference mockPreference;
  @Mock
  private Organization mockOrganization;
  @Mock
  private Client mockClient;

  /**
   * Sets up the test environment before each test.
   *
   * @throws Exception
   *     if an error occurs during setup
   */
  @Before
  public void setUp() throws Exception {
    mocks = MockitoAnnotations.openMocks(this);
    handler = new MatchStatementOnLoadPreferenceActionHandler();

    // Setup static mocks
    mockedOBDal = mockStatic(OBDal.class);
    mockedOBContext = mockStatic(OBContext.class);
    mockedOBProvider = mockStatic(OBProvider.class);

    // Configure static mocks
    mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDal);
    mockedOBContext.when(OBContext::getOBContext).thenReturn(mockOBContext);

    // Configure OBProvider mock
    OBProvider mockProvider = mock(OBProvider.class);
    when(mockProvider.get(Preference.class)).thenReturn(mockPreference);
    mockedOBProvider.when(OBProvider::getInstance).thenReturn(mockProvider);

    // Configure basic mocks
    when(mockOBDal.get(Organization.class, "0")).thenReturn(mockOrganization);
    when(mockOBContext.getCurrentClient()).thenReturn(mockClient);

    // Mock setAdminMode and restorePreviousMode to prevent actual calls
    mockedOBContext.when(() -> OBContext.setAdminMode(true)).thenAnswer(invocation -> null);
    mockedOBContext.when(() -> OBContext.restorePreviousMode()).thenAnswer(invocation -> null);
  }

  /**
   * Cleans up the test environment after each test.
   *
   * @throws Exception
   *     if an error occurs during teardown
   */
  @After
  public void tearDown() throws Exception {
    if (mockedOBDal != null) {
      mockedOBDal.close();
    }
    if (mockedOBContext != null) {
      mockedOBContext.close();
    }
    if (mockedOBProvider != null) {
      mockedOBProvider.close();
    }
    if (mocks != null) {
      mocks.close();
    }
  }

  /**
   * Tests the execute method for successful execution.
   */
  @Test
  public void testExecuteSuccess() {
    // Given
    Map<String, Object> parameters = new HashMap<>();
    String content = "";

    // When
    JSONObject result = handler.execute(parameters, content);

    // Then
    assertNotNull(TestConstants.RESULT_NOT_NULL_MESSAGE, result);

    // Assert that all required properties were set on the preference object
    verify(mockPreference).setNewOBObject(true);
    verify(mockPreference).setOrganization(mockOrganization);
    verify(mockPreference).setClient(mockClient);
    verify(mockPreference).setSearchKey("Y");
    verify(mockPreference).setPropertyList(false);
    verify(mockPreference).setAttribute("APRM_NoPersistInfoMessageInMatching");

    // Assert that the preference was saved and changes were flushed
    verify(mockOBDal).save(mockPreference);
    verify(mockOBDal).flush();

    // Verify setAdminMode and restorePreviousMode were called in the correct order
    mockedOBContext.verify(() -> OBContext.setAdminMode(true));
    mockedOBContext.verify(() -> OBContext.restorePreviousMode());

  }

  /**
   * Tests the execute method for exception handling.
   */
  @Test
  public void testExecuteExceptionHandling() {
    // Given
    Map<String, Object> parameters = new HashMap<>();
    String content = "";

    // Configure the mock to throw an exception when save is called
    doThrow(new RuntimeException(TestConstants.TEST_EXCEPTION)).when(mockOBDal).save(any(Preference.class));

    // We expect the exception to be thrown
    expectedException.expect(RuntimeException.class);
    expectedException.expectMessage(TestConstants.TEST_EXCEPTION);

    try {
      // When
      handler.execute(parameters, content);
    } finally {
      mockedOBContext.verify(() -> OBContext.setAdminMode(true));

      mockedOBContext.verify(() -> OBContext.restorePreviousMode());

      verify(mockPreference).setNewOBObject(true);
      verify(mockPreference).setOrganization(mockOrganization);
    }
  }
}
