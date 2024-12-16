package com.etendoerp.reportvaluationstock.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.junit.Before;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.dal.service.OBDal;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.openbravo.model.materialmgmt.cost.CostingRule;

import java.lang.reflect.Method;

/**
 * Test class for CostingRuleUtils, which verifies the logic related
 * to the calculation of costing rules based on organizations.
 */
public class CostingRuleUtilsTest {

  private Organization mockOrganization;
  private OBDal mockOBDal;
  private Method getLEsCostingAlgortithmMethod;

  /**
   * Sets up the initial state required for the tests. Prepare mocks and retrieves
   * the reflected method from ReportValuationStock for testing purposes.
   *
   * @throws Exception if an error occurs during the setup.
   */
  @Before
  public void setUp() throws Exception {
    getLEsCostingAlgortithmMethod = ReportValuationStock.class.getDeclaredMethod("getLEsCostingAlgortithm", Organization.class);
    getLEsCostingAlgortithmMethod.setAccessible(true);

    mockOrganization = Mockito.mock(Organization.class);
    mockOBDal = Mockito.mock(OBDal.class);
  }

  /**
   * Tests the getLEsCostingAlgortithm method with a valid organization. Verifies that
   * the method returns the expected costing rule when provided with correct data.
   *
   * @throws Exception if an error occurs during the test execution.
   */
  @Test
  public void testGetLEsCostingAlgortithmWithValidOrganization() throws Exception {
    String orgId = "TEST_ORG_ID";
    CostingRule expectedRule = Mockito.mock(CostingRule.class);

    Mockito.when(mockOrganization.getId()).thenReturn(orgId);

    try (MockedStatic<OBDal> mockedStatic = Mockito.mockStatic(OBDal.class)) {
      mockedStatic.when(OBDal::getReadOnlyInstance).thenReturn(mockOBDal);

      OBQuery<CostingRule> mockQuery = Mockito.mock(OBQuery.class);
      Mockito.when(mockOBDal.createQuery(Mockito.eq(CostingRule.class), Mockito.anyString()))
          .thenReturn(mockQuery);
      Mockito.when(mockQuery.setNamedParameter(Mockito.eq("orgId"), Mockito.eq(orgId)))
          .thenReturn(mockQuery);
      Mockito.when(mockQuery.setMaxResult(1)).thenReturn(mockQuery);
      Mockito.when(mockQuery.uniqueResult()).thenReturn(expectedRule);

      CostingRule result = (CostingRule) getLEsCostingAlgortithmMethod.invoke(null, mockOrganization);

      assertNotNull("El resultado no debería ser null", result);
      assertEquals("El resultado debería ser el CostingRule esperado", expectedRule, result);
    }
  }

  /**
   * Tests the getLEsCostingAlgortithm method when no costing rules are found.
   * Verifies that the method returns null when no results are available.
   *
   * @throws Exception if an error occurs during the test execution.
   */
  @Test
  public void testGetLEsCostingAlgortithmNoRulesFound() throws Exception {
    String orgId = "TEST_ORG_ID";

    Mockito.when(mockOrganization.getId()).thenReturn(orgId);

    try (MockedStatic<OBDal> mockedStatic = Mockito.mockStatic(OBDal.class)) {
      mockedStatic.when(OBDal::getReadOnlyInstance).thenReturn(mockOBDal);

      OBQuery mockQuery = Mockito.mock(OBQuery.class);
      Mockito.when(mockOBDal.createQuery(Mockito.eq(CostingRule.class), Mockito.anyString()))
          .thenReturn(mockQuery);
      Mockito.when(mockQuery.setNamedParameter(Mockito.eq("orgId"), Mockito.eq(orgId)))
          .thenReturn(mockQuery);
      Mockito.when(mockQuery.setMaxResult(1)).thenReturn(mockQuery);
      Mockito.when(mockQuery.uniqueResult()).thenReturn(null);

      CostingRule result = (CostingRule) getLEsCostingAlgortithmMethod.invoke(null, mockOrganization);

      assertNull("El resultado debería ser null cuando no se encuentran reglas", result);
    }
  }
}
