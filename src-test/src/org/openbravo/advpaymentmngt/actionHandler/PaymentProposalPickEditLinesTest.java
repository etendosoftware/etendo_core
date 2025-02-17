/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2023 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.advpaymentmngt.actionHandler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
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
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBDao;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentMethod;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentPropDetail;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentProposal;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentScheduleDetail;

@RunWith(MockitoJUnitRunner.class)
public class PaymentProposalPickEditLinesTest extends WeldBaseTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    // Static mocks
    private MockedStatic<OBDal> mockedOBDal;
    private MockedStatic<OBContext> mockedOBContext;
    private MockedStatic<OBDao> mockedOBDao;
    private MockedStatic<OBProvider> mockedOBProvider;
    private MockedStatic<OBMessageUtils> mockedOBMessageUtils;

    // Mocks
    @Mock
    private OBDal mockOBDal;

    @Mock
    private FIN_PaymentProposal mockPaymentProposal;

    @Mock
    private FIN_PaymentMethod mockPaymentMethod;

    @Mock
    private FIN_PaymentPropDetail mockPaymentPropDetail;

    @Mock
    private FIN_PaymentScheduleDetail mockPaymentScheduleDetail;

    @Mock
    private List<FIN_PaymentPropDetail> mockPaymentPropDetailList;

    @InjectMocks
    private PaymentProposalPickEditLines classUnderTest;

    private AutoCloseable mocks;

    @Before
    public void setUp() throws Exception {
        mocks = MockitoAnnotations.openMocks(this);

        // Setup static mocks
        mockedOBDal = mockStatic(OBDal.class);
        mockedOBContext = mockStatic(OBContext.class);
        mockedOBDao = mockStatic(OBDao.class);
        mockedOBProvider = mockStatic(OBProvider.class);
        mockedOBMessageUtils = mockStatic(OBMessageUtils.class);

        // Configure static mocks
        mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDal);
        mockedOBContext.when(() -> OBContext.setAdminMode()).thenAnswer(invocation -> null);
        mockedOBContext.when(() -> OBContext.restorePreviousMode()).thenAnswer(invocation -> null);

        // Setup common mock behaviors
        when(mockPaymentProposal.getFINPaymentPropDetailList()).thenReturn(mockPaymentPropDetailList);
    }

    @After
    public void tearDown() throws Exception {
        if (mockedOBDal != null) {
            mockedOBDal.close();
        }
        if (mockedOBContext != null) {
            mockedOBContext.close();
        }
        if (mockedOBDao != null) {
            mockedOBDao.close();
        }
        if (mockedOBProvider != null) {
            mockedOBProvider.close();
        }
        if (mockedOBMessageUtils != null) {
            mockedOBMessageUtils.close();
        }
        if (mocks != null) {
            mocks.close();
        }
    }



    @Test
    public void testDoExecute_WithDifferentPaymentMethod() throws Exception {
        // GIVEN
        String paymentProposalId = "TEST_PROPOSAL_ID";
        String paymentMethodId = "TEST_METHOD_ID";
        String linePaymentMethodId = "DIFFERENT_METHOD_ID";

        // Create test content JSON with selection that will trigger different payment method warning
        String content = createTestContentJsonWithSelection(paymentProposalId, paymentMethodId, linePaymentMethodId);

        // Setup mocks
        when(mockOBDal.get(FIN_PaymentProposal.class, paymentProposalId)).thenReturn(mockPaymentProposal);
        when(mockOBDal.get(FIN_PaymentMethod.class, paymentMethodId)).thenReturn(mockPaymentMethod);

        FIN_PaymentMethod differentPaymentMethod = mock(FIN_PaymentMethod.class);
        when(mockOBDal.get(FIN_PaymentMethod.class, linePaymentMethodId)).thenReturn(differentPaymentMethod);

        List<String> idList = new ArrayList<>();
        mockedOBDao.when(() -> OBDao.getIDListFromOBObject(mockPaymentPropDetailList)).thenReturn(idList);

        // Mock payment prop detail
        FIN_PaymentPropDetail newPPD = mock(FIN_PaymentPropDetail.class);
        OBProvider provider = mock(OBProvider.class);
        mockedOBProvider.when(() -> OBProvider.getInstance()).thenReturn(provider);
        when(provider.get(FIN_PaymentPropDetail.class)).thenReturn(newPPD);

        // Mock payment schedule detail
        when(mockOBDal.get(FIN_PaymentScheduleDetail.class, "TEST_PSD_ID")).thenReturn(mockPaymentScheduleDetail);

        mockedOBMessageUtils.when(() -> OBMessageUtils.messageBD("Success")).thenReturn("Success");
        mockedOBMessageUtils.when(() -> OBMessageUtils.messageBD("APRM_Different_PaymentMethod_Selected"))
                .thenReturn("Different payment method selected");

        Map<String, Object> parameters = new HashMap<>();

        // WHEN
        JSONObject result = classUnderTest.doExecute(parameters, content);

        // THEN
        assertNotNull(result);
        assertTrue(result.has("message"));
        JSONObject message = result.getJSONObject("message");
        assertEquals("warning", message.getString("severity"));
        assertEquals("Different payment method selected", message.getString("text"));
    }

    @Test
    public void testDoExecute_WithException() throws Exception {
        // GIVEN
        String paymentProposalId = "TEST_PROPOSAL_ID";
        String paymentMethodId = "TEST_METHOD_ID";

        // Create test content JSON
        String content = createTestContentJson(paymentProposalId, paymentMethodId, false);

        // Setup mocks to throw exception
        when(mockOBDal.get(FIN_PaymentProposal.class, paymentProposalId))
                .thenThrow(new OBException("Test exception"));

        mockedOBMessageUtils.when(() -> OBMessageUtils.messageBD("Test exception"))
                .thenReturn("Test exception message");

        Map<String, Object> parameters = new HashMap<>();

        // WHEN
        JSONObject result = classUnderTest.doExecute(parameters, content);

        // THEN
        assertNotNull(result);
        assertTrue(result.has("message"));
        JSONObject message = result.getJSONObject("message");
        assertEquals("error", message.getString("severity"));
        assertEquals("Test exception message", message.getString("text"));

        // Verify rollback was called
        verify(mockOBDal).rollbackAndClose();
    }

    @Test
    public void testDoExecute_NoSelectedLines() throws Exception {
        // GIVEN
        String paymentProposalId = "TEST_PROPOSAL_ID";
        String paymentMethodId = "TEST_METHOD_ID";

        // Create test content JSON with no selected lines
        String content = createTestContentJson(paymentProposalId, paymentMethodId, true);

        // Setup mocks
        when(mockOBDal.get(FIN_PaymentProposal.class, paymentProposalId)).thenReturn(mockPaymentProposal);
        when(mockOBDal.get(FIN_PaymentMethod.class, paymentMethodId)).thenReturn(mockPaymentMethod);

        List<String> idList = new ArrayList<>();
        mockedOBDao.when(() -> OBDao.getIDListFromOBObject(mockPaymentPropDetailList)).thenReturn(idList);

        mockedOBMessageUtils.when(() -> OBMessageUtils.messageBD("Success")).thenReturn("Success");

        Map<String, Object> parameters = new HashMap<>();

        // WHEN
        JSONObject result = classUnderTest.doExecute(parameters, content);

        // THEN
        assertNotNull(result);
        assertTrue(result.has("message"));
        JSONObject message = result.getJSONObject("message");
        assertEquals("success", message.getString("severity"));
        assertEquals("Success", message.getString("text"));
    }

    @Test
    public void testRemoveNonSelectedLines() throws Exception {
        // GIVEN
        List<String> idList = new ArrayList<>();
        String detailId = "TEST_DETAIL_ID";
        idList.add(detailId);

        when(mockOBDal.get(FIN_PaymentPropDetail.class, detailId)).thenReturn(mockPaymentPropDetail);

        // WHEN
        // We need to use reflection to access the private method
        java.lang.reflect.Method method = PaymentProposalPickEditLines.class.getDeclaredMethod(
                "removeNonSelectedLines", List.class, FIN_PaymentProposal.class);
        method.setAccessible(true);
        method.invoke(classUnderTest, idList, mockPaymentProposal);

        // THEN
        verify(mockPaymentPropDetailList).remove(mockPaymentPropDetail);
        verify(mockOBDal).remove(mockPaymentPropDetail);
        verify(mockOBDal).save(mockPaymentProposal);
        verify(mockOBDal).flush();
    }

    // Helper methods to create test JSON content
    private String createTestContentJson(String paymentProposalId, String paymentMethodId, boolean emptySelection)
            throws JSONException {
        JSONObject jsonContent = new JSONObject();
        jsonContent.put("Fin_Payment_Proposal_ID", paymentProposalId);
        jsonContent.put("inpfinPaymentmethodId", paymentMethodId);

        JSONObject params = new JSONObject();
        JSONObject grid = new JSONObject();
        JSONArray selection = new JSONArray();

        if (!emptySelection) {
            // Add a sample selected line if needed
            JSONObject selectedLine = new JSONObject();
            selectedLine.put("id", "TEST_LINE_ID");
            selectedLine.put("payment", "100.00");
            selectedLine.put("paymentMethod", paymentMethodId);
            selectedLine.put("paymentScheduleDetail", "TEST_PSD_ID");
            selectedLine.put("difference", "0.00");
            selectedLine.put("writeoff", "false");
            selection.put(selectedLine);
        }

        grid.put("_selection", selection);
        params.put("grid", grid);
        jsonContent.put("_params", params);

        return jsonContent.toString();
    }

    private String createTestContentJsonWithSelection(String paymentProposalId, String paymentMethodId,
            String linePaymentMethodId) throws JSONException {
        JSONObject jsonContent = new JSONObject();
        jsonContent.put("Fin_Payment_Proposal_ID", paymentProposalId);
        jsonContent.put("inpfinPaymentmethodId", paymentMethodId);

        JSONObject params = new JSONObject();
        JSONObject grid = new JSONObject();
        JSONArray selection = new JSONArray();

        // Add a sample selected line with different payment method
        JSONObject selectedLine = new JSONObject();
        selectedLine.put("id", "TEST_LINE_ID");
        selectedLine.put("payment", "100.00");
        selectedLine.put("paymentMethod", linePaymentMethodId); // Different payment method
        selectedLine.put("paymentScheduleDetail", "TEST_PSD_ID");
        selectedLine.put("difference", "0.00");
        selectedLine.put("writeoff", "false");
        selection.put(selectedLine);

        grid.put("_selection", selection);
        params.put("grid", grid);
        jsonContent.put("_params", params);

        return jsonContent.toString();
    }
}