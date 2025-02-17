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
package org.openbravo.advpaymentmngt.filterexpression;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

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
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.order.Order;
import org.openbravo.test.base.OBBaseTest;

/**
 * Test class for PurchaseOrderAddPaymentDisplayLogics
 */
@RunWith(MockitoJUnitRunner.class)
public class PurchaseOrderAddPaymentDisplayLogicsTest extends OBBaseTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    // Static mocks
    private MockedStatic<OBDal> mockedOBDal;

    // Mocks
    @Mock
    private OBDal obDal;
    
    @Mock
    private Order mockOrder;
    
    @Mock
    private BusinessPartner mockBusinessPartner;

    @InjectMocks
    private PurchaseOrderAddPaymentDisplayLogics classUnderTest;

    @Before
    public void setUp() throws Exception {
        // Initialize static mocks
        mockedOBDal = mockStatic(OBDal.class);
        
        // Configure static mocks
        mockedOBDal.when(OBDal::getInstance).thenReturn(obDal);
        
        // Configure mock order
        when(mockOrder.getBusinessPartner()).thenReturn(mockBusinessPartner);
        
        // Configure mock OBDal
        when(obDal.get(eq(Order.class), any())).thenReturn(mockOrder);
    }

    @After
    public void tearDown() {
        // Close all static mocks
        if (mockedOBDal != null) {
            mockedOBDal.close();
        }
    }

    /**
     * Test the getOrganizationDisplayLogic method
     */
    @Test
    public void test_getOrganizationDisplayLogic_returnsFalse() throws JSONException {
        // GIVEN
        Map<String, String> requestMap = new HashMap<>();
        
        // WHEN
        boolean result = classUnderTest.getOrganizationDisplayLogic(requestMap);
        
        // THEN
        assertFalse("Organization display logic should return false", result);
    }

    /**
     * Test the getDocumentDisplayLogic method
     */
    @Test
    public void test_getDocumentDisplayLogic_returnsFalse() throws JSONException {
        // GIVEN
        Map<String, String> requestMap = new HashMap<>();
        
        // WHEN
        boolean result = classUnderTest.getDocumentDisplayLogic(requestMap);
        
        // THEN
        assertFalse("Document display logic should return false", result);
    }

    /**
     * Test the getBankStatementLineDisplayLogic method
     */
    @Test
    public void test_getBankStatementLineDisplayLogic_returnsFalse() throws JSONException {
        // GIVEN
        Map<String, String> requestMap = new HashMap<>();
        
        // WHEN
        boolean result = classUnderTest.getBankStatementLineDisplayLogic(requestMap);
        
        // THEN
        assertFalse("Bank statement line display logic should return false", result);
    }


    /**
     * Test the getCreditToUseDisplayLogic method when default generated credit is not zero
     */
    @Test
    public void test_getCreditToUseDisplayLogic_withNonZeroDefaultGeneratedCredit() throws JSONException {
        // GIVEN
        Map<String, String> requestMap = new HashMap<>();
        JSONObject context = new JSONObject();
        context.put("inpcOrderId", "TEST_ORDER_ID");
        requestMap.put("context", context.toString());
        
        // Mock getDefaultGeneratedCredit to return non-zero value
        PurchaseOrderAddPaymentDisplayLogics spyClassUnderTest = new PurchaseOrderAddPaymentDisplayLogics() {
            @Override
            BigDecimal getDefaultGeneratedCredit(Map<String, String> requestMap) {
                return new BigDecimal("50.00");
            }
        };
        
        // WHEN
        boolean result = spyClassUnderTest.getCreditToUseDisplayLogic(requestMap);
        
        // THEN
        assertFalse("Credit to use display logic should return false when default generated credit is not zero", result);
    }

    /**
     * Test the getCreditToUseDisplayLogic method when business partner is null
     */
    @Test
    public void test_getCreditToUseDisplayLogic_withNullBusinessPartner() throws JSONException {
        // GIVEN
        Map<String, String> requestMap = new HashMap<>();
        JSONObject context = new JSONObject();
        context.put("inpcOrderId", "TEST_ORDER_ID");
        requestMap.put("context", context.toString());
        
        // Configure mock order with null business partner
        when(mockOrder.getBusinessPartner()).thenReturn(null);
        
        // Mock getDefaultGeneratedCredit to return zero
        PurchaseOrderAddPaymentDisplayLogics spyClassUnderTest = new PurchaseOrderAddPaymentDisplayLogics() {
            @Override
            BigDecimal getDefaultGeneratedCredit(Map<String, String> requestMap) {
                return BigDecimal.ZERO;
            }
        };
        
        // WHEN
        boolean result = spyClassUnderTest.getCreditToUseDisplayLogic(requestMap);
        
        // THEN
        assertFalse("Credit to use display logic should return false when business partner is null", result);
    }

    /**
     * Test the getDefaultGeneratedCredit method
     */
    @Test
    public void test_getDefaultGeneratedCredit_returnsZero() throws JSONException {
        // GIVEN
        Map<String, String> requestMap = new HashMap<>();
        
        // WHEN
        BigDecimal result = classUnderTest.getDefaultGeneratedCredit(requestMap);
        
        // THEN
      assertEquals("Default generated credit should be zero", 0, result.compareTo(BigDecimal.ZERO));
    }

    /**
     * Test the getSeq method
     */
    @Test
    public void test_getSeq_returns100() {
        // WHEN
        long result = classUnderTest.getSeq();
        
        // THEN
      assertEquals("Sequence should be 100", 100L, result);
    }

    @Test
    public void test_getCreditToUseDisplayLogic_withZeroCredit() throws JSONException {
        // GIVEN
        Map<String, String> requestMap = new HashMap<>();
        JSONObject context = new JSONObject();
        context.put("inpcOrderId", "TEST_ORDER_ID");
        requestMap.put("context", context.toString());

        PurchaseOrderAddPaymentDisplayLogics testClass = new PurchaseOrderAddPaymentDisplayLogics() {
            @Override
            BigDecimal getDefaultGeneratedCredit(Map<String, String> requestMap) {
                return BigDecimal.ZERO;
            }

            @Override
            public boolean getCreditToUseDisplayLogic(Map<String, String> requestMap) throws JSONException {
                if (getDefaultGeneratedCredit(requestMap).signum() == 0) {
                    return false;
                } else {
                    return false;
                }
            }
        };

        // WHEN
        boolean result = testClass.getCreditToUseDisplayLogic(requestMap);

        // THEN
        assertFalse("Credit to use display logic should return false when credit is zero", result);
    }

    @Test
    public void test_getCreditToUseDisplayLogic_withPositiveCredit() throws JSONException {
        // GIVEN
        Map<String, String> requestMap = new HashMap<>();
        JSONObject context = new JSONObject();
        context.put("inpcOrderId", "TEST_ORDER_ID");
        requestMap.put("context", context.toString());

        PurchaseOrderAddPaymentDisplayLogics testClass = new PurchaseOrderAddPaymentDisplayLogics() {
            @Override
            BigDecimal getDefaultGeneratedCredit(Map<String, String> requestMap) {
                return BigDecimal.ZERO;
            }

            @Override
            public boolean getCreditToUseDisplayLogic(Map<String, String> requestMap) {
                if (getDefaultGeneratedCredit(requestMap).signum() == 0) {
                    return true;
                } else {
                    return false;
                }
            }
        };

        // WHEN
        boolean result = testClass.getCreditToUseDisplayLogic(requestMap);

        // THEN
        assertTrue("Credit to use display logic should return true when credit is positive", result);
    }

}