package com.etendoerp.reportvaluationstock.handler;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.criteria.Predicate;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.erpCommon.utility.DateTimeData;
import org.openbravo.model.materialmgmt.transaction.MaterialTransaction;

/**
 * Unit tests for the `hasTrxWithNoCost` method in {@link ReportValuationStock}.
 */
@RunWith(MockitoJUnitRunner.class)
public class ReportValuationStockHasTrxWithNoCostTest {

  private ReportValuationStock reportValuationStock;
  private Method hasTrxWithNoCostMethod;

  /**
   * Sets up the test by instantiating the class and accessing the private method.
   */
  @Before
  public void setUp() throws Exception {
    reportValuationStock = new ReportValuationStock();
    hasTrxWithNoCostMethod = ReportValuationStock.class.getDeclaredMethod(
        "hasTrxWithNoCost",
        String.class,
        Set.class,
        String.class,
        String.class
    );
  }

  /**
   * Configures expectations on OBDal, criteria, and static methods.
   */
  private void setupMocks(
      OBDal mockOBDal,
      OBCriteria<MaterialTransaction> mockCriteria,
      MaterialTransaction expectedResult
  ) throws Exception {
    when(mockOBDal.createCriteria(MaterialTransaction.class)).thenReturn(mockCriteria);
    when(mockCriteria.add(any(Predicate.class))).thenReturn(mockCriteria);

    when(mockCriteria.createAlias(anyString(), anyString())).thenReturn(mockCriteria);
    when(mockCriteria.setMaxResults(1)).thenReturn(mockCriteria);
    when(mockCriteria.uniqueResult()).thenReturn(expectedResult);


    when(DateTimeData.nDaysAfter(any(), eq(TestUtils.TEST_DATE), eq("1")))
        .thenReturn(TestUtils.TEST_DATE);
  }

  /**
   * Test that verifies the method returns true when a transaction without cost exists.
   */
  @Test
  public void testHasTrxWithoutCostReturnsTrue() throws Exception {
    Set<String> orgs = new HashSet<>();
    orgs.add(TestUtils.TEST_ORG);

    OBDal mockOBDal = mock(OBDal.class);
    OBCriteria<MaterialTransaction> mockCriteria = mock(OBCriteria.class);

    try (
        MockedStatic<OBDal> obDalMock = mockStatic(OBDal.class);
        MockedStatic<Predicate> restrictionsMock = mockStatic(Predicate.class);
        MockedStatic<DateTimeData> dateTimeDataMock = mockStatic(DateTimeData.class)
    ) {
      obDalMock.when(OBDal::getReadOnlyInstance).thenReturn(mockOBDal);
      setupMocks(mockOBDal, mockCriteria, new MaterialTransaction());

      boolean result = (boolean) hasTrxWithNoCostMethod.invoke(
          reportValuationStock,
          TestUtils.TEST_DATE,
          orgs,
          TestUtils.WAREHOUSE_ID_1,
          TestUtils.TEST_CATEGORY_ID
      );

      assertTrue(result);
    }
  }

  /**
   * Test that verifies the method returns false when no transactions without cost exist.
   * @throws Exception if the method invocation via reflection fails
   */
  @Test
  public void testHasTrxWithoutCostReturnsFalse() throws Exception {
    Set<String> orgs = new HashSet<>();
    orgs.add(TestUtils.TEST_ORG);

    OBDal mockOBDal = mock(OBDal.class);
    OBCriteria<MaterialTransaction> mockCriteria = mock(OBCriteria.class);

    try (
        MockedStatic<OBDal> obDalMock = mockStatic(OBDal.class);
        MockedStatic<Predicate> restrictionsMock = mockStatic(Predicate.class);
        MockedStatic<DateTimeData> dateTimeDataMock = mockStatic(DateTimeData.class)
    ) {
      obDalMock.when(OBDal::getReadOnlyInstance).thenReturn(mockOBDal);
      setupMocks(mockOBDal, mockCriteria, null);

      boolean result = (boolean) hasTrxWithNoCostMethod.invoke(
          reportValuationStock,
          TestUtils.TEST_DATE,
          orgs,
          null,
          null
      );

      assertFalse(result);
    }
  }
}
