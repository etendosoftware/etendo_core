package org.openbravo.advpaymentmngt.actionHandler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.domain.Preference;

public class MatchStatementOnLoadGetPreferenceActionHandlerTest extends WeldBaseTest {
    
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private MockedStatic<OBDal> mockedOBDal;
    private MockedStatic<OBContext> mockedOBContext;
    
    @Mock
    private OBDal mockOBDal;
    
    @Mock
    private OBContext mockOBContext;
    
    @Mock
    private User mockUser;
    
    @Mock
    private OBQuery<Preference> mockQuery;

    private MatchStatementOnLoadGetPreferenceActionHandler handler;
    private AutoCloseable mocks;

    @Before
    public void setUp() throws Exception {
        mocks = MockitoAnnotations.openMocks(this);
        handler = new MatchStatementOnLoadGetPreferenceActionHandler();

        // Setup static mocks
        mockedOBDal = Mockito.mockStatic(OBDal.class);
        mockedOBContext = Mockito.mockStatic(OBContext.class);

        // Configure OBDal mock
        mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDal);

        // Configure OBContext mock
        mockedOBContext.when(OBContext::getOBContext).thenReturn(mockOBContext);
        when(mockOBContext.getUser()).thenReturn(mockUser);
        when(mockUser.getId()).thenReturn("testUserId");
    }

    @After
    public void tearDown() throws Exception {
        if (mockedOBDal != null) {
            mockedOBDal.close();
        }
        if (mockedOBContext != null) {
            mockedOBContext.close();
        }
        if (mocks != null) {
            mocks.close();
        }
    }

    @Test
    public void testExecute_WithExistingPreference() throws Exception {
        // Given
        Map<String, Object> params = new HashMap<>();
        String content = "";
        
        Preference mockPreference = mock(Preference.class);
        when(mockPreference.getSearchKey()).thenReturn("testPreferenceKey");
        
        List<Preference> preferenceList = new ArrayList<>();
        preferenceList.add(mockPreference);

        when(mockOBDal.createQuery(eq(Preference.class), anyString())).thenReturn(mockQuery);
        when(mockQuery.setNamedParameter(eq("userId"), anyString())).thenReturn(mockQuery);
        when(mockQuery.list()).thenReturn(preferenceList);

        // When
        JSONObject result = handler.execute(params, content);

        // Then
        assertNotNull("Result should not be null", result);
        assertEquals("Preference value should match", "testPreferenceKey", result.getString("preference"));
    }

    @Test
    public void testExecute_WithNoPreference() {
        // Given
        Map<String, Object> params = new HashMap<>();
        String content = "";
        
        List<Preference> emptyList = new ArrayList<>();

        when(mockOBDal.createQuery(eq(Preference.class), anyString())).thenReturn(mockQuery);
        when(mockQuery.setNamedParameter(eq("userId"), anyString())).thenReturn(mockQuery);
        when(mockQuery.list()).thenReturn(emptyList);

        // When
        JSONObject result = handler.execute(params, content);

        // Then
        assertNotNull("Result should not be null", result);
        assertEquals("Result should be empty JSONObject", 0, result.length());
    }
}