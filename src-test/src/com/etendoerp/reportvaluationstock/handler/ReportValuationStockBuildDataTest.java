package com.etendoerp.reportvaluationstock.handler;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.security.OrganizationStructureProvider;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;

/**
 * Test class for ReportValuationStock, verifying the behavior of the
 * buildData method under various scenarios.
 */
@RunWith(MockitoJUnitRunner.class)
public class ReportValuationStockBuildDataTest {

  /**
   * Rule to handle expected exceptions in test cases.
   */
  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @InjectMocks
  private ReportValuationStock reportValuationStock;

  @Mock
  private VariablesSecureApp vars;

  @Mock
  private Organization mockOrg;

  @Mock
  private Client mockClient;

  @Mock
  private OrganizationStructureProvider mockOsp;

  private Method buildDataMethod;



  /**
   * Sets up the initial state required for the tests. Prepare mocks and retrieves
   * the reflected buildData method for testing.
   *
   * @throws Exception if an error occurs during the setup.
   */
  @Before
  public void setUp() throws Exception {
    buildDataMethod = ReportValuationStock.class.getDeclaredMethod(
        "buildData",
        VariablesSecureApp.class,
        String.class,
        String.class,
        String.class,
        String.class,
        String.class,
        boolean.class,
        Map.class
    );
    buildDataMethod.setAccessible(true);

    when(mockClient.getId()).thenReturn(TestUtils.TEST_CLIENT_ID);

    Set<String> orgTree = new HashSet<>();
    orgTree.add(TestUtils.TEST_ORG_ID);
  }

  /**
   * Tests the buildData method when the legal entity is null. Verifies that a
   * ServletException is thrown with the expected error message.
   */
  @Test
  public void testBuildDataWithNullLegalEntity() {
    Map<String, Object> parameters = new HashMap<>();

    try (MockedStatic<OBDal> obDalMock = mockStatic(OBDal.class);
         MockedStatic<OBContext> obContextMock = mockStatic(OBContext.class);
         MockedStatic<DalConnectionProvider> connectionProviderMock = mockStatic(DalConnectionProvider.class);
         MockedStatic<OBMessageUtils> obMessageUtilsMock = mockStatic(OBMessageUtils.class)) {

      OBDal mockOBDal = mock(OBDal.class);
      obDalMock.when(OBDal::getReadOnlyInstance).thenReturn(mockOBDal);

      OBContext mockContext = mock(OBContext.class);
      when(mockContext.getCurrentClient()).thenReturn(mockClient);
      when(mockContext.getOrganizationStructureProvider(anyString())).thenReturn(mockOsp);
      obContextMock.when(OBContext::getOBContext).thenReturn(mockContext);

      DalConnectionProvider mockProvider = mock(DalConnectionProvider.class);
      connectionProviderMock.when(DalConnectionProvider::getReadOnlyConnectionProvider)
          .thenReturn(mockProvider);

      when(mockOBDal.get(eq(Organization.class), anyString())).thenReturn(mockOrg);
      when(mockOsp.getLegalEntity(any(Organization.class))).thenReturn(null);

      OBError mockError = mock(OBError.class);
      when(mockError.getMessage()).thenReturn(TestUtils.WAREHOUSE_NOT_IN_LE);
      obMessageUtilsMock.when(() -> OBMessageUtils.messageBD(anyString()))
          .thenReturn(TestUtils.WAREHOUSE_NOT_IN_LE);
      obMessageUtilsMock.when(() -> OBMessageUtils.translateError(anyString()))
          .thenReturn(mockError);

      try {
        buildDataMethod.invoke(
            reportValuationStock,
            vars,
            TestUtils.TEST_DATE,
            TestUtils.TEST_ORG_ID,
            TestUtils.TEST_WAREHOUSE_ID,
            TestUtils.TEST_CATEGORY_ID,
            TestUtils.TEST_CURRENCY_ID,
            false,
            parameters
        );
      } catch (Exception e) {
        assertTrue("Expected ServletException", e.getCause() instanceof ServletException);
        assertTrue("Expected correct error message",
            StringUtils.contains(e.getCause().getMessage(), TestUtils.WAREHOUSE_NOT_IN_LE));
      }
    }
  }
}
