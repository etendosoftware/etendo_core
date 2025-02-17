package org.openbravo.advpaymentmngt.actionHandler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
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
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.client.application.Parameter;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.system.Language;
import org.openbravo.model.ad.ui.Element;
import org.openbravo.model.ad.ui.ElementTrl;

public class AddPaymentReloadLabelsActionHandlerTest extends WeldBaseTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private AddPaymentReloadLabelsActionHandler actionHandler;
    private MockedStatic<OBDal> mockedOBDal;
    private MockedStatic<OBContext> mockedOBContext;

    @Mock
    private OBDal mockOBDal;
    @Mock
    private OBContext mockOBContext;
    @Mock
    private Language mockLanguage;
    @Mock
    private Parameter mockBusinessPartnerParam;
    @Mock
    private Parameter mockFinancialAccountParam;
    @Mock
    private Element mockBusinessPartnerElement;
    @Mock
    private Element mockFinancialAccountElement;
    @Mock
    private OBCriteria<ElementTrl> mockElementTrlCriteria;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        actionHandler = new AddPaymentReloadLabelsActionHandler();

        // Setup static mocks
        mockedOBDal = mockStatic(OBDal.class);
        mockedOBContext = mockStatic(OBContext.class);

        // Configure OBDal mock
        mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDal);

        // Configure OBContext mock
        mockedOBContext.when(OBContext::getOBContext).thenReturn(mockOBContext);
        when(mockOBContext.getLanguage()).thenReturn(mockLanguage);
        when(mockLanguage.getLanguage()).thenReturn("en_US");
    }

    @After
    public void tearDown() {
        if (mockedOBDal != null) {
            mockedOBDal.close();
        }
        if (mockedOBContext != null) {
            mockedOBContext.close();
        }
    }

    @Test
    public void testExecute_DefaultLanguage_Success() throws Exception {
        // Given
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("businessPartner", "BP_ID");
        parameters.put("financialAccount", "FA_ID");
        parameters.put("issotrx", "true");

        // Mock Parameter retrieval
        when(mockOBDal.get(eq(Parameter.class), eq("BP_ID"))).thenReturn(mockBusinessPartnerParam);
        when(mockOBDal.get(eq(Parameter.class), eq("FA_ID"))).thenReturn(mockFinancialAccountParam);

        // Mock Element retrieval
        when(mockBusinessPartnerParam.getApplicationElement()).thenReturn(mockBusinessPartnerElement);
        when(mockFinancialAccountParam.getApplicationElement()).thenReturn(mockFinancialAccountElement);

        // Set label properties
        when(mockBusinessPartnerElement.get(Element.PROPERTY_NAME)).thenReturn("Business Partner Label");
        when(mockFinancialAccountElement.get(Element.PROPERTY_NAME)).thenReturn("Financial Account Label");

        // When
        JSONObject result = actionHandler.execute(parameters, null);

        // Then
        assertNotNull(result);
        assertTrue(result.has("values"));
        JSONObject values = result.getJSONObject("values");
        assertEquals("Business Partner Label", values.getString("businessPartner"));
        assertEquals("Financial Account Label", values.getString("financialAccount"));
    }

    @Test
    public void testExecute_NonEnglishLanguage_Success() throws Exception {
        // Given
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("businessPartner", "BP_ID");
        parameters.put("financialAccount", "FA_ID");
        parameters.put("issotrx", "false");

        // Mock Language
        when(mockLanguage.getLanguage()).thenReturn("es_ES");

        // Mock Parameter retrieval
        when(mockOBDal.get(eq(Parameter.class), eq("BP_ID"))).thenReturn(mockBusinessPartnerParam);
        when(mockOBDal.get(eq(Parameter.class), eq("FA_ID"))).thenReturn(mockFinancialAccountParam);

        // Mock Element retrieval
        when(mockBusinessPartnerParam.getApplicationElement()).thenReturn(mockBusinessPartnerElement);
        when(mockFinancialAccountParam.getApplicationElement()).thenReturn(mockFinancialAccountElement);

        // Set label properties
        when(mockBusinessPartnerElement.get(Element.PROPERTY_PURCHASEORDERNAME)).thenReturn("Business Partner Label");
        when(mockFinancialAccountElement.get(Element.PROPERTY_PURCHASEORDERNAME)).thenReturn("Financial Account Label");

        // Mock ElementTrl criteria
        when(mockOBDal.createCriteria(ElementTrl.class)).thenReturn(mockElementTrlCriteria);
        when(mockElementTrlCriteria.uniqueResult()).thenReturn(null);

        // When
        JSONObject result = actionHandler.execute(parameters, null);

        // Then
        assertNotNull(result);
        assertTrue(result.has("values"));
        JSONObject values = result.getJSONObject("values");
        assertEquals("Business Partner Label", values.getString("businessPartner"));
        assertEquals("Financial Account Label", values.getString("financialAccount"));
    }


}