package org.openbravo.advpaymentmngt.filterexpression;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.financialmgmt.gl.GLItem;

@RunWith(MockitoJUnitRunner.class)
public class FundsTransferGLItemDefaultValueExpressionTest extends WeldBaseTest {

    private FundsTransferGLItemDefaultValueExpression expression;
    private MockedStatic<OBContext> mockedOBContext;
    private MockedStatic<OBDal> mockedOBDal;

    @Mock
    private OBDal mockOBDal;

    @Mock
    private Session mockSession;

    @Mock
    private Organization mockOrganization;

    @Mock
    private GLItem mockGLItem;

    @Mock
    private Query<GLItem> mockQuery;

    @Before
    public void setUp() throws Exception {
        expression = new FundsTransferGLItemDefaultValueExpression();

        // Setup static mocks
        mockedOBContext = mockStatic(OBContext.class);
        mockedOBDal = mockStatic(OBDal.class);
        mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDal);

        // Setup common mock behavior
        when(mockOBDal.getSession()).thenReturn(mockSession);
        when(mockSession.createQuery(anyString(), eq(GLItem.class))).thenReturn(mockQuery);
        when(mockQuery.setParameter(anyString(), any())).thenReturn(mockQuery);
        when(mockQuery.setMaxResults(1)).thenReturn(mockQuery);
    }

    @After
    public void tearDown() {
        if (mockedOBContext != null) {
            mockedOBContext.close();
        }
        if (mockedOBDal != null) {
            mockedOBDal.close();
        }
    }

    @Test
    public void testGetExpression_WithValidOrganizationAndGLItem() throws Exception {
        // Given
        Map<String, String> requestMap = new HashMap<>();
        JSONObject context = new JSONObject();
        context.put("ad_org_id", "TEST_ORG_ID");
        requestMap.put("context", context.toString());

        String expectedGLItemId = "TEST_GL_ITEM_ID";
        when(mockOBDal.get(Organization.class, "TEST_ORG_ID")).thenReturn(mockOrganization);
        when(mockOrganization.getAPRMGlitem()).thenReturn(mockGLItem);
        when(mockGLItem.getId()).thenReturn(expectedGLItemId);

        // When
        String result = expression.getExpression(requestMap);

        // Then
        assertEquals(expectedGLItemId, result);
    }

    @Test
    public void testGetExpression_WithInpAdOrgId() throws Exception {
        // Given
        Map<String, String> requestMap = new HashMap<>();
        JSONObject context = new JSONObject();
        context.put("inpadOrgId", "TEST_ORG_ID");
        requestMap.put("context", context.toString());

        String expectedGLItemId = "TEST_GL_ITEM_ID";
        when(mockOBDal.get(Organization.class, "TEST_ORG_ID")).thenReturn(mockOrganization);
        when(mockOrganization.getAPRMGlitem()).thenReturn(mockGLItem);
        when(mockGLItem.getId()).thenReturn(expectedGLItemId);

        // When
        String result = expression.getExpression(requestMap);

        // Then
        assertEquals(expectedGLItemId, result);
    }

    @Test
    public void testGetExpression_WithNoDirectGLItem() throws Exception {
        // Given
        Map<String, String> requestMap = new HashMap<>();
        JSONObject context = new JSONObject();
        context.put("ad_org_id", "TEST_ORG_ID");
        requestMap.put("context", context.toString());

        String expectedGLItemId = "PARENT_GL_ITEM_ID";
        when(mockOBDal.get(Organization.class, "TEST_ORG_ID")).thenReturn(mockOrganization);
        when(mockOrganization.getAPRMGlitem()).thenReturn(null);
        when(mockOrganization.getId()).thenReturn("TEST_ORG_ID");

        GLItem parentGLItem = mock(GLItem.class);
        when(parentGLItem.getId()).thenReturn(expectedGLItemId);
        when(mockQuery.uniqueResult()).thenReturn(parentGLItem);

        // When
        String result = expression.getExpression(requestMap);

        // Then
        assertEquals(expectedGLItemId, result);
    }

    @Test
    public void testGetExpression_WithInvalidContext() throws Exception {
        // Given
        Map<String, String> requestMap = new HashMap<>();
        requestMap.put("context", "invalid-json");

        // When
        String result = expression.getExpression(requestMap);

        // Then
        assertNull(result);
    }

    @Test
    public void testGetExpression_WithNullOrganizationId() throws Exception {
        // Given
        Map<String, String> requestMap = new HashMap<>();
        JSONObject context = new JSONObject();
        context.put("ad_org_id", JSONObject.NULL);
        requestMap.put("context", context.toString());

        // When
        String result = expression.getExpression(requestMap);

        // Then
        assertNull(result);
    }

    @Test
    public void testGetExpression_WithEmptyOrganizationId() throws Exception {
        // Given
        Map<String, String> requestMap = new HashMap<>();
        JSONObject context = new JSONObject();
        context.put("ad_org_id", "");
        requestMap.put("context", context.toString());

        // When
        String result = expression.getExpression(requestMap);

        // Then
        assertNull(result);
    }

    @Test
    public void testGetExpression_WithNonExistentOrganization() throws Exception {
        // Given
        Map<String, String> requestMap = new HashMap<>();
        JSONObject context = new JSONObject();
        context.put("ad_org_id", "NON_EXISTENT_ORG");
        requestMap.put("context", context.toString());

        when(mockOBDal.get(Organization.class, "NON_EXISTENT_ORG")).thenReturn(null);

        // When
        String result = expression.getExpression(requestMap);

        // Then
        assertNull(result);
    }

    @Test
    public void testGetExpression_WithNoGLItemInOrganizationTree() throws Exception {
        // Given
        Map<String, String> requestMap = new HashMap<>();
        JSONObject context = new JSONObject();
        context.put("ad_org_id", "TEST_ORG_ID");
        requestMap.put("context", context.toString());

        when(mockOBDal.get(Organization.class, "TEST_ORG_ID")).thenReturn(mockOrganization);
        when(mockOrganization.getAPRMGlitem()).thenReturn(null);
        when(mockOrganization.getId()).thenReturn("TEST_ORG_ID");
        when(mockQuery.uniqueResult()).thenReturn(null);

        // When
        String result = expression.getExpression(requestMap);

        // Then
        assertNull(result);
    }
}