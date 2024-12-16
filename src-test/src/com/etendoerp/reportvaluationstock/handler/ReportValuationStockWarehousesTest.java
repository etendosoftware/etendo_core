package com.etendoerp.reportvaluationstock.handler;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.security.OrganizationStructureProvider;
import org.openbravo.dal.service.OBDal;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the {@link ReportValuationStock} class.
 * <p>
 * This class validates the behavior of the {@code getWarehouses} method in various scenarios,
 * including cases with results and cases with no results.
 * </p>
 */
@RunWith(MockitoJUnitRunner.class)
public class ReportValuationStockWarehousesTest {

  private static final String CLIENT_ID = "testClient";
  private static final String ORG_ID = "testOrg";
  private static final String WAREHOUSE_ID_1 = "warehouse1";
  private static final String WAREHOUSE_ID_2 = "warehouse2";

  @InjectMocks
  private ReportValuationStock reportValuationStock;

  @Mock
  private OBDal mockOBDal;

  @Mock
  private Session mockSession;

  @Mock
  private Query<String> mockQuery;

  @Mock
  private OBContext mockOBContext;

  @Mock
  private OrganizationStructureProvider mockOsp;

  private Method getWarehousesMethod;

  /**
   * Sets up the test environment by initializing mocks and reflective access to the method under test.
   *
   * @throws Exception if the method cannot be accessed.
   */
  @Before
  public void setUp() throws Exception {
    getWarehousesMethod = ReportValuationStock.class.getDeclaredMethod(
        "getWarehouses",
        String.class,
        String.class
    );
    getWarehousesMethod.setAccessible(true);
  }

  /**
   * Tests the {@code getWarehouses} method with valid inputs, expecting a non-empty list of results.
   *
   * @throws Exception if the method invocation fails.
   */
  @Test
  public void testGetWarehousesWithResults() throws Exception {
    List<String> expectedOrgIds = Arrays.asList("org1", "org2");
    List<String> expectedWarehouseIds = Arrays.asList(WAREHOUSE_ID_1, WAREHOUSE_ID_2);

    try (MockedStatic<OBContext> obContextMock = mockStatic(OBContext.class);
         MockedStatic<OBDal> obDalMock = mockStatic(OBDal.class)) {

      obContextMock.when(OBContext::getOBContext).thenReturn(mockOBContext);
      when(mockOBContext.getOrganizationStructureProvider(CLIENT_ID))
          .thenReturn(mockOsp);
      when(mockOsp.getNaturalTree(ORG_ID))
          .thenReturn(Set.copyOf(expectedOrgIds));

      obDalMock.when(OBDal::getReadOnlyInstance).thenReturn(mockOBDal);
      when(mockOBDal.getSession()).thenReturn(mockSession);
      when(mockSession.createQuery(anyString(), eq(String.class)))
          .thenReturn(mockQuery);
      when(mockQuery.setParameterList((String) eq("orgIds"), (Collection) any()))
          .thenReturn(mockQuery);
      when(mockQuery.setParameter(eq("clientId"), any()))
          .thenReturn(mockQuery);
      when(mockQuery.list())
          .thenReturn(expectedWarehouseIds);

      @SuppressWarnings("unchecked")
      List<String> result = (List<String>) getWarehousesMethod.invoke(
          reportValuationStock,
          CLIENT_ID,
          ORG_ID
      );

      assertEquals("Should return correct number of warehouses",
          expectedWarehouseIds.size(), result.size());
      assertTrue("Should contain expected warehouse IDs",
          result.containsAll(expectedWarehouseIds));
    }
  }

  /**
   * Tests the {@code getWarehouses} method with valid inputs, expecting an empty list when no results are found.
   *
   * @throws Exception if the method invocation fails.
   */
  @Test
  public void testGetWarehousesWithNoResults() throws Exception {
    List<String> expectedOrgIds = Arrays.asList("org1", "org2");
    List<String> emptyWarehouseList = List.of();

    try (MockedStatic<OBContext> obContextMock = mockStatic(OBContext.class);
         MockedStatic<OBDal> obDalMock = mockStatic(OBDal.class)) {

      obContextMock.when(OBContext::getOBContext).thenReturn(mockOBContext);
      when(mockOBContext.getOrganizationStructureProvider(CLIENT_ID))
          .thenReturn(mockOsp);
      when(mockOsp.getNaturalTree(ORG_ID))
          .thenReturn(Set.copyOf(expectedOrgIds));

      obDalMock.when(OBDal::getReadOnlyInstance).thenReturn(mockOBDal);
      when(mockOBDal.getSession()).thenReturn(mockSession);
      when(mockSession.createQuery(anyString(), eq(String.class)))
          .thenReturn(mockQuery);
      when(mockQuery.setParameterList((String) eq("orgIds"), (Collection) any()))
          .thenReturn(mockQuery);
      when(mockQuery.setParameter(eq("clientId"), any()))
          .thenReturn(mockQuery);
      when(mockQuery.list())
          .thenReturn(emptyWarehouseList);

      @SuppressWarnings("unchecked")
      List<String> result = (List<String>) getWarehousesMethod.invoke(
          reportValuationStock,
          CLIENT_ID,
          ORG_ID
      );

      assertTrue("Should return empty list when no warehouses found",
          result.isEmpty());
    }
  }
}