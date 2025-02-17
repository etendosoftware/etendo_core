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
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.domain.Preference;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;

public class MatchStatementOnLoadPreferenceActionHandlerTest {

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
    }

    @After
    public void tearDown() throws Exception {
        try {
            if (mockedOBDal != null) {
                mockedOBDal.close();
            }
            if (mockedOBContext != null) {
                mockedOBContext.close();
            }
            if (mockedOBProvider != null) {
                mockedOBProvider.close();
            }
        } finally {
            OBContext.restorePreviousMode();
            if (mocks != null) {
                mocks.close();
            }
        }
    }

    @Test
    public void testExecuteSuccess() {
        // Given
        Map<String, Object> parameters = new HashMap<>();
        String content = "";

        try {
            OBContext.setAdminMode();
            // When
            JSONObject result = handler.execute(parameters, content);

            // Then
            assertNotNull("Result should not be null", result);
            verify(mockPreference).setNewOBObject(true);
            verify(mockPreference).setOrganization(mockOrganization);
            verify(mockPreference).setClient(mockClient);
            verify(mockPreference).setSearchKey("Y");
            verify(mockPreference).setPropertyList(false);
            verify(mockPreference).setAttribute("APRM_NoPersistInfoMessageInMatching");
            verify(mockOBDal).save(mockPreference);
            verify(mockOBDal).flush();
        } finally {
            OBContext.restorePreviousMode();
        }
    }

    @Test
    public void testExecuteExceptionHandling() {
        // Given
        Map<String, Object> parameters = new HashMap<>();
        String content = "";
        doThrow(new RuntimeException("Test exception")).when(mockOBDal).save(any(Preference.class));

        // We expect the exception to be thrown
        expectedException.expect(RuntimeException.class);
        expectedException.expectMessage("Test exception");

        try {
            OBContext.setAdminMode();
            // When
            handler.execute(parameters, content);
        } finally {
            OBContext.restorePreviousMode();
        }
    }
}

