/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.0  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2010-2016 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */


package org.openbravo.advpaymentmngt.test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.Restrictions;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentMethod;

public class PaymentMethodTest {

  private static final String AUTOMATIC_EXECUTION = "A";
  private static final String MANUAL_EXECUTION = "M";
  private static final String CLEARED_ACCOUNT = "CLE";
  private static final String IN_TRANSIT_ACCOUNT = "INT";
  private static final String WITHDRAWN_ACCOUNT = "WIT";
  private static final String DEPOSIT_ACCOUNT = "DEP";
  private static final String STANDARD_DESCRIPTION = "JUnit Test";


  private MockedStatic<OBDal> mockedOBDal;
  private AutoCloseable mocks;

  @BeforeEach
  public void setUp() throws Exception {
    mocks = MockitoAnnotations.openMocks(this);
    mockedOBDal = mockStatic(OBDal.class);
    // Mock OBDal.getInstance() to return a mock OBDal
    OBDal mockOBDal = mock(OBDal.class);
    mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDal);

    // Mock createCriteria to return a mock OBCriteria
    OBCriteria<FIN_PaymentMethod> mockCriteria = mock(OBCriteria.class);
    when(mockOBDal.createCriteria(FIN_PaymentMethod.class)).thenReturn(mockCriteria);

    // Mock Restrictions.eq to just return null (not used in logic)
    // No need to mock Restrictions.eq unless you want to verify its usage

    // Mock list() to return an empty list by default
    when(mockCriteria.list()).thenReturn(new ArrayList<>());
  }

  @AfterEach
  public void tearDown() throws Exception {
    if (mockedOBDal != null) {
      mockedOBDal.close();
    }
    if (mocks != null) {
      mocks.close();
    }
  }


  @Test
  public void testAddPaymentMethodValid1() {
    TestUtility.insertPaymentMethod("APRM_PAYMENT_METHOD_1", STANDARD_DESCRIPTION, true, false,
        false, MANUAL_EXECUTION, null, false, IN_TRANSIT_ACCOUNT, DEPOSIT_ACCOUNT, CLEARED_ACCOUNT,
        true, false, false, MANUAL_EXECUTION, null, false, IN_TRANSIT_ACCOUNT, WITHDRAWN_ACCOUNT,
        CLEARED_ACCOUNT, true, false);
  }


  @Test
  public void testAddPaymentMethodValid2() {
    TestUtility.insertPaymentMethod("APRM_PAYMENT_METHOD_2", STANDARD_DESCRIPTION, true, false,
        false, AUTOMATIC_EXECUTION, null, false, IN_TRANSIT_ACCOUNT, DEPOSIT_ACCOUNT,
        CLEARED_ACCOUNT, true, false, false, AUTOMATIC_EXECUTION, null, false, IN_TRANSIT_ACCOUNT,
        WITHDRAWN_ACCOUNT, CLEARED_ACCOUNT, true, false);
  }


  @Test
  public void testAddPaymentMethodValid3() {
    TestUtility.insertPaymentMethod("APRM_PAYMENT_METHOD_3", STANDARD_DESCRIPTION, true, false,
        false, MANUAL_EXECUTION, null, false, null, null, null, true, false, false,
        MANUAL_EXECUTION, null, false, null, null, null, true, false);
  }


  @Test
  public void testAddPaymentMethodValid4() {
    TestUtility.insertPaymentMethod("APRM_PAYMENT_METHOD_4", STANDARD_DESCRIPTION, true, true, true,
        MANUAL_EXECUTION, null, false, null, null, null, true, true, true, MANUAL_EXECUTION, null,
        false, null, null, null, true, false);
  }


  // Requisite: at least one Execution Process created
  @Test
  public void testAddPaymentMethodValid5() {
    TestUtility.insertPaymentMethod("APRM_PAYMENT_METHOD_5", STANDARD_DESCRIPTION, true, false,
        false, AUTOMATIC_EXECUTION, /* getOneInstance(PaymentExecutionProcess.class) */null, false,
        IN_TRANSIT_ACCOUNT, DEPOSIT_ACCOUNT, CLEARED_ACCOUNT, true, false, false,
        AUTOMATIC_EXECUTION, /* getOneInstance(PaymentExecutionProcess.class) */null, false,
        IN_TRANSIT_ACCOUNT, WITHDRAWN_ACCOUNT, CLEARED_ACCOUNT, true, false);
  }


  /**
   * Deletes all the Payment Methods created for testing
   */
  @Test
  public void testDeletePaymentMethod() {
    // The OBDal and OBCriteria are already mocked in setUp, so this will not hit the real DB
    final OBCriteria<FIN_PaymentMethod> obCriteria = OBDal.getInstance()
        .createCriteria(FIN_PaymentMethod.class);
    obCriteria.add(Restrictions.eq(FIN_PaymentMethod.PROPERTY_DESCRIPTION, STANDARD_DESCRIPTION));
    final List<FIN_PaymentMethod> paymentMethods = obCriteria.list();
    for (FIN_PaymentMethod pm : paymentMethods) {
      OBDal.getInstance().remove(pm);
    }
  }
}
