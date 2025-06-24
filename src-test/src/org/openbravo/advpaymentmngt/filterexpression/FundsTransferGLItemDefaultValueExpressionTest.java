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
import org.openbravo.advpaymentmngt.TestConstants;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.financialmgmt.gl.GLItem;

/**
 * Unit tests for the FundsTransferGLItemDefaultValueExpression class.
 */
@RunWith(MockitoJUnitRunner.class)
public class FundsTransferGLItemDefaultValueExpressionTest {

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

    /**
     * Sets up the test environment before each test.
     *
     * @throws Exception if an error occurs during setup
     */
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

    /**
     * Cleans up the test environment after each test.
     */
    @After
    public void tearDown() {
        if (mockedOBContext != null) {
            mockedOBContext.close();
        }
        if (mockedOBDal != null) {
            mockedOBDal.close();
        }
    }

    /**
     * Tests the getExpression method with a valid organization and GL item.
     *
     * @throws Exception if an error occurs during execution
     */
    @Test
    public void testGetExpressionWithValidOrganizationAndGLItem() throws Exception {
        // Given
        Map<String, String> requestMap = new HashMap<>();
        JSONObject context = new JSONObject();
        context.put(TestConstants.AD_ORG_ID, TestConstants.ORGANIZATION_ID);
        requestMap.put(TestConstants.CONTEXT, context.toString());

        String expectedGLItemId = "TEST_GL_ITEM_ID";
        when(mockOBDal.get(Organization.class, TestConstants.ORGANIZATION_ID)).thenReturn(mockOrganization);
        when(mockOrganization.getAPRMGlitem()).thenReturn(mockGLItem);
        when(mockGLItem.getId()).thenReturn(expectedGLItemId);

        // When
        String result = expression.getExpression(requestMap);

        // Then
        assertEquals(expectedGLItemId, result);
    }

    /**
     * Tests the getExpression method with the inpadOrgId parameter.
     *
     * @throws Exception if an error occurs during execution
     */
    @Test
    public void testGetExpressionWithInpAdOrgId() throws Exception {
        // Given
        Map<String, String> requestMap = new HashMap<>();
        JSONObject context = new JSONObject();
        context.put("inpadOrgId", TestConstants.ORGANIZATION_ID);
        requestMap.put(TestConstants.CONTEXT, context.toString());

        String expectedGLItemId = "TEST_GL_ITEM_ID";
        when(mockOBDal.get(Organization.class, TestConstants.ORGANIZATION_ID)).thenReturn(mockOrganization);
        when(mockOrganization.getAPRMGlitem()).thenReturn(mockGLItem);
        when(mockGLItem.getId()).thenReturn(expectedGLItemId);

        // When
        String result = expression.getExpression(requestMap);

        // Then
        assertEquals(expectedGLItemId, result);
    }

    /**
     * Tests the getExpression method when there is no direct GL item.
     *
     * @throws Exception if an error occurs during execution
     */
    @Test
    public void testGetExpressionWithNoDirectGLItem() throws Exception {
        // Given
        Map<String, String> requestMap = new HashMap<>();
        JSONObject context = new JSONObject();
        context.put(TestConstants.AD_ORG_ID, TestConstants.ORGANIZATION_ID);
        requestMap.put(TestConstants.CONTEXT, context.toString());

        String expectedGLItemId = "PARENT_GL_ITEM_ID";
        when(mockOBDal.get(Organization.class, TestConstants.ORGANIZATION_ID)).thenReturn(mockOrganization);
        when(mockOrganization.getAPRMGlitem()).thenReturn(null);
        when(mockOrganization.getId()).thenReturn(TestConstants.ORGANIZATION_ID);

        GLItem parentGLItem = mock(GLItem.class);
        when(parentGLItem.getId()).thenReturn(expectedGLItemId);
        when(mockQuery.uniqueResult()).thenReturn(parentGLItem);

        // When
        String result = expression.getExpression(requestMap);

        // Then
        assertEquals(expectedGLItemId, result);
    }

    /**
     * Tests the getExpression method with an invalid context.
     *
     * @throws Exception if an error occurs during execution
     */
    @Test
    public void testGetExpressionWithInvalidContext() throws Exception {
        // Given
        Map<String, String> requestMap = new HashMap<>();
        requestMap.put(TestConstants.CONTEXT, "invalid-json");

        // When
        String result = expression.getExpression(requestMap);

        // Then
        assertNull(result);
    }

    /**
     * Tests the getExpression method with a null organization ID.
     *
     * @throws Exception if an error occurs during execution
     */
    @Test
    public void testGetExpressionWithNullOrganizationId() throws Exception {
        // Given
        Map<String, String> requestMap = new HashMap<>();
        JSONObject context = new JSONObject();
        context.put(TestConstants.AD_ORG_ID, JSONObject.NULL);
        requestMap.put(TestConstants.CONTEXT, context.toString());

        // When
        String result = expression.getExpression(requestMap);

        // Then
        assertNull(result);
    }

    /**
     * Tests the getExpression method with an empty organization ID.
     *
     * @throws Exception if an error occurs during execution
     */
    @Test
    public void testGetExpressionWithEmptyOrganizationId() throws Exception {
        // Given
        Map<String, String> requestMap = new HashMap<>();
        JSONObject context = new JSONObject();
        context.put(TestConstants.AD_ORG_ID, "");
        requestMap.put(TestConstants.CONTEXT, context.toString());

        // When
        String result = expression.getExpression(requestMap);

        // Then
        assertNull(result);
    }

    /**
     * Tests the getExpression method with a non-existent organization.
     *
     * @throws Exception if an error occurs during execution
     */
    @Test
    public void testGetExpressionWithNonExistentOrganization() throws Exception {
        // Given
        Map<String, String> requestMap = new HashMap<>();
        JSONObject context = new JSONObject();
        context.put(TestConstants.AD_ORG_ID, "NON_EXISTENT_ORG");
        requestMap.put(TestConstants.CONTEXT, context.toString());

        when(mockOBDal.get(Organization.class, "NON_EXISTENT_ORG")).thenReturn(null);

        // When
        String result = expression.getExpression(requestMap);

        // Then
        assertNull(result);
    }

    /**
     * Tests the getExpression method when there is no GL item in the organization tree.
     *
     * @throws Exception if an error occurs during execution
     */
    @Test
    public void testGetExpressionWithNoGLItemInOrganizationTree() throws Exception {
        // Given
        Map<String, String> requestMap = new HashMap<>();
        JSONObject context = new JSONObject();
        context.put(TestConstants.AD_ORG_ID, TestConstants.ORGANIZATION_ID);
        requestMap.put(TestConstants.CONTEXT, context.toString());

        when(mockOBDal.get(Organization.class, TestConstants.ORGANIZATION_ID)).thenReturn(mockOrganization);
        when(mockOrganization.getAPRMGlitem()).thenReturn(null);
        when(mockOrganization.getId()).thenReturn(TestConstants.ORGANIZATION_ID);
        when(mockQuery.uniqueResult()).thenReturn(null);

        // When
        String result = expression.getExpression(requestMap);

        // Then
        assertNull(result);
    }
}
