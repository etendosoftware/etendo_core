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
package org.openbravo.advpaymentmngt.hqlinjections;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.test.base.OBBaseTest;

/**
 * Tests for AddPaymentCreditToUseInjector class
 */
@RunWith(MockitoJUnitRunner.class)
public class AddPaymentCreditToUseInjectorTest extends OBBaseTest {

  private static final String BUSINESS_PARTNER_ID = "TEST_BP_ID";
  private static final String INVALID_BP_ID = "-1";

  @Mock
  private OBDal obDal;

  @Mock
  private BusinessPartner businessPartner;

  @InjectMocks
  private AddPaymentCreditToUseInjector classUnderTest;

  private MockedStatic<OBDal> mockedOBDal;

  @Before
  public void setUp() {
    // Initialize static mocks
    mockedOBDal = mockStatic(OBDal.class);
    mockedOBDal.when(OBDal::getInstance).thenReturn(obDal);
    
    // Setup business partner mock
    when(businessPartner.getId()).thenReturn(BUSINESS_PARTNER_ID);
  }

  @After
  public void tearDown() {
    // Close static mocks
    if (mockedOBDal != null) {
      mockedOBDal.close();
    }
  }

  /**
   * Test the insertHql method with a valid business partner ID and sales transaction set to true
   */
  @Test
  public void testInsertHql_validBusinessPartner_salesTransaction() {
    // GIVEN
    Map<String, String> requestParameters = new HashMap<>();
    requestParameters.put("received_from", BUSINESS_PARTNER_ID);
    requestParameters.put("issotrx", "true");
    
    Map<String, Object> queryNamedParameters = new HashMap<>();
    
    when(obDal.get(BusinessPartner.class, BUSINESS_PARTNER_ID)).thenReturn(businessPartner);
    
    // WHEN
    String result = classUnderTest.insertHql(requestParameters, queryNamedParameters);
    
    // THEN
    assertEquals("f.businessPartner.id = :bp and f.receipt = :issotrx", result);
    assertEquals(BUSINESS_PARTNER_ID, queryNamedParameters.get("bp"));
    assertEquals(true, queryNamedParameters.get("issotrx"));
  }

  /**
   * Test the insertHql method with a valid business partner ID and sales transaction set to false
   */
  @Test
  public void testInsertHql_validBusinessPartner_purchaseTransaction() {
    // GIVEN
    Map<String, String> requestParameters = new HashMap<>();
    requestParameters.put("received_from", BUSINESS_PARTNER_ID);
    requestParameters.put("issotrx", "false");
    
    Map<String, Object> queryNamedParameters = new HashMap<>();
    
    when(obDal.get(BusinessPartner.class, BUSINESS_PARTNER_ID)).thenReturn(businessPartner);
    
    // WHEN
    String result = classUnderTest.insertHql(requestParameters, queryNamedParameters);
    
    // THEN
    assertEquals("f.businessPartner.id = :bp and f.receipt = :issotrx", result);
    assertEquals(BUSINESS_PARTNER_ID, queryNamedParameters.get("bp"));
    assertEquals(false, queryNamedParameters.get("issotrx"));
  }

  /**
   * Test the insertHql method with a null business partner ID
   */
  @Test
  public void testInsertHql_nullBusinessPartner() {
    // GIVEN
    Map<String, String> requestParameters = new HashMap<>();
    requestParameters.put("issotrx", "true");
    
    Map<String, Object> queryNamedParameters = new HashMap<>();
    
    // WHEN
    String result = classUnderTest.insertHql(requestParameters, queryNamedParameters);
    
    // THEN
    assertEquals("f.businessPartner.id = :bp and f.receipt = :issotrx", result);
    assertEquals(INVALID_BP_ID, queryNamedParameters.get("bp"));
    assertEquals(true, queryNamedParameters.get("issotrx"));
  }

  /**
   * Test the insertHql method with a business partner ID that doesn't exist in the database
   */
  @Test
  public void testInsertHql_nonExistentBusinessPartner() {
    // GIVEN
    Map<String, String> requestParameters = new HashMap<>();
    requestParameters.put("received_from", BUSINESS_PARTNER_ID);
    requestParameters.put("issotrx", "true");
    
    Map<String, Object> queryNamedParameters = new HashMap<>();
    
    when(obDal.get(BusinessPartner.class, BUSINESS_PARTNER_ID)).thenReturn(null);
    
    // WHEN
    String result = classUnderTest.insertHql(requestParameters, queryNamedParameters);
    
    // THEN
    assertEquals("f.businessPartner.id = :bp and f.receipt = :issotrx", result);
    assertEquals(INVALID_BP_ID, queryNamedParameters.get("bp"));
    assertEquals(true, queryNamedParameters.get("issotrx"));
  }

  /**
   * Test the insertHql method with missing issotrx parameter (should default to false)
   */
  @Test
  public void testInsertHql_missingIsSoTrx() {
    // GIVEN
    Map<String, String> requestParameters = new HashMap<>();
    requestParameters.put("received_from", BUSINESS_PARTNER_ID);
    // No issotrx parameter
    
    Map<String, Object> queryNamedParameters = new HashMap<>();
    
    when(obDal.get(BusinessPartner.class, BUSINESS_PARTNER_ID)).thenReturn(businessPartner);
    
    // WHEN
    String result = classUnderTest.insertHql(requestParameters, queryNamedParameters);
    
    // THEN
    assertEquals("f.businessPartner.id = :bp and f.receipt = :issotrx", result);
    assertEquals(BUSINESS_PARTNER_ID, queryNamedParameters.get("bp"));
    assertEquals(false, queryNamedParameters.get("issotrx"));
  }
}