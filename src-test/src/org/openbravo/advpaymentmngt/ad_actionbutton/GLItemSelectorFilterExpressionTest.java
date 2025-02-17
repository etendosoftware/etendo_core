package org.openbravo.advpaymentmngt.ad_actionbutton;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jettison.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.openbravo.dal.core.OBContext;
import org.openbravo.test.base.OBBaseTest;

public class GLItemSelectorFilterExpressionTest extends OBBaseTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private GLItemSelectorFilterExpression filterExpression;

  @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        filterExpression = new GLItemSelectorFilterExpression();
  }


    @Test
    public void testGetExpression_ExceptionHandling() throws Exception {
        // GIVEN
        Map<String, String> requestMap = new HashMap<>();
        requestMap.put("ad_org_id", "123");

        try (MockedStatic<OBContext> obContextMock = Mockito.mockStatic(OBContext.class)) {
            obContextMock.when(OBContext::getOBContext).thenThrow(new RuntimeException("Test Exception"));

            // WHEN
            String result = filterExpression.getExpression(requestMap);

            // THEN
            assertNull(result);
        }
    }

    @Test
    public void testPrivateMethodContextHasADOrgIDParam() throws Exception {
        // Use reflection to test private method
        java.lang.reflect.Method method = GLItemSelectorFilterExpression.class
            .getDeclaredMethod("contextHasADOrgIDParam", JSONObject.class);
        method.setAccessible(true);

        // Test with valid AD_ORG_ID
        JSONObject validContext = new JSONObject();
        validContext.put("ad_org_id", "123");
        assertEquals(true, method.invoke(filterExpression, validContext));

        // Test with null AD_ORG_ID
        JSONObject nullContext = new JSONObject();
        nullContext.put("ad_org_id", JSONObject.NULL);
        assertEquals(false, method.invoke(filterExpression, nullContext));

        // Test with empty AD_ORG_ID
        JSONObject emptyContext = new JSONObject();
        emptyContext.put("ad_org_id", "");
        assertEquals(false, method.invoke(filterExpression, emptyContext));
    }

    @Test
    public void testPrivateMethodContextHasInpADOrgIDParam() throws Exception {
        // Use reflection to test private method
        java.lang.reflect.Method method = GLItemSelectorFilterExpression.class
            .getDeclaredMethod("contextHasInpADOrgIDParam", JSONObject.class);
        method.setAccessible(true);

        // Test with valid inpadOrgId
        JSONObject validContext = new JSONObject();
        validContext.put("inpadOrgId", "456");
        assertEquals(true, method.invoke(filterExpression, validContext));

        // Test with null inpadOrgId
        JSONObject nullContext = new JSONObject();
        nullContext.put("inpadOrgId", JSONObject.NULL);
        assertEquals(false, method.invoke(filterExpression, nullContext));

        // Test with empty inpadOrgId
        JSONObject emptyContext = new JSONObject();
        emptyContext.put("inpadOrgId", "");
        assertEquals(false, method.invoke(filterExpression, emptyContext));
    }
}