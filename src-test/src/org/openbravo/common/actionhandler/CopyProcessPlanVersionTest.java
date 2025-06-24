package org.openbravo.common.actionhandler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.security.OrganizationStructureProvider;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.manufacturing.processplan.Operation;
import org.openbravo.model.manufacturing.processplan.OperationProduct;
import org.openbravo.model.manufacturing.processplan.ProcessPlan;
import org.openbravo.model.manufacturing.processplan.Version;
import org.openbravo.model.manufacturing.transaction.Activity;
import org.openbravo.service.db.DbUtility;

/**
 * Unit tests for the {@link CopyProcessPlanVersion} class.
 * Verifies the behavior of the `doExecute` method, including handling of data inconsistencies
 * and proper JSON response generation.
 */
@ExtendWith(MockitoExtension.class)
public class CopyProcessPlanVersionTest {

  @InjectMocks
  private CopyProcessPlanVersion copyProcessPlanVersion;
  @Mock
  private OBDal obDal;
  @Mock
  private OBContext obContext;
  @Mock
  private OrganizationStructureProvider orgStructureProvider;
  @Mock
  private ProcessPlan processPlan;
  @Mock
  private Version sourceVersion;
  @Mock
  private Organization organization;
  @Mock
  private Client client;
  @Mock
  private Operation sourceOperation;
  @Mock
  private Activity activity;
  @Mock
  private OperationProduct sourceOperationProduct;
  @Mock
  private Product product;
  @Mock
  private Organization activityOrganization;
  private MockedStatic<OBDal> mockedOBDal;
  private MockedStatic<OBContext> mockedOBContext;
  private MockedStatic<DalUtil> mockedDalUtil;
  private MockedStatic<DbUtility> mockedDbUtility;
  private MockedStatic<OBMessageUtils> mockedOBMessageUtils;
  private MockedStatic<Utility> mockedUtility;

  /**
   * Sets up the test environment before each test.
   * Initializes mocked static utilities and dependencies.
   */
  @BeforeEach
  void setUp() {
    mockedOBDal = mockStatic(OBDal.class);
    mockedOBContext = mockStatic(OBContext.class);
    mockedDalUtil = mockStatic(DalUtil.class);
    mockedDbUtility = mockStatic(DbUtility.class);
    mockedOBMessageUtils = mockStatic(OBMessageUtils.class);
    mockedUtility = mockStatic(Utility.class);

    mockedOBDal.when(OBDal::getInstance).thenReturn(obDal);

    mockedOBContext.when(OBContext::getOBContext).thenReturn(obContext);
    when(processPlan.getOrganization()).thenReturn(organization);
    when(client.getId()).thenReturn(ActionHandlerTestConstants.CLIENT_ID);

    when(organization.getClient()).thenReturn(client);

    when(activity.getOrganization()).thenReturn(activityOrganization);

    when(obDal.get(ProcessPlan.class, ActionHandlerTestConstants.PROCESS_PLAN_ID)).thenReturn(processPlan);
    when(obDal.get(Version.class, ActionHandlerTestConstants.VERSION_ID)).thenReturn(sourceVersion);

    when(obContext.getOrganizationStructureProvider(ActionHandlerTestConstants.CLIENT_ID)).thenReturn(orgStructureProvider);
  }

  /**
   * Cleans up the test environment after each test.
   * Closes mocked static utilities to release resources.
   */
  @AfterEach
  void tearDown() {
    if (mockedOBDal != null) mockedOBDal.close();
    if (mockedOBContext != null) mockedOBContext.close();
    if (mockedDalUtil != null) mockedDalUtil.close();
    if (mockedDbUtility != null) mockedDbUtility.close();
    if (mockedOBMessageUtils != null) mockedOBMessageUtils.close();
    if (mockedUtility != null) mockedUtility.close();
  }

  /**
   * Tests the `doExecute` method when there is a data inconsistency with an activity.
   * Verifies that the method returns a JSON response with an error message.
   *
   * @throws Exception
   *     if an error occurs during test execution
   */
  @Test
  void testDoExecuteDataInconsistencyWithActivity() throws Exception {
    String content = createJsonContent(ActionHandlerTestConstants.PROCESS_PLAN_ID, ActionHandlerTestConstants.VERSION_ID);
    Map<String, Object> parameters = new HashMap<>();

    List<Operation> operations = new ArrayList<>();
    operations.add(sourceOperation);
    when(sourceVersion.getManufacturingOperationList()).thenReturn(operations);

    when(sourceOperation.getActivity()).thenReturn(activity);
    when(activity.getIdentifier()).thenReturn("Test Activity");
    when(organization.getIdentifier()).thenReturn("Test Organization");

    when(orgStructureProvider.isInNaturalTree(any(Organization.class), any(Organization.class))).thenReturn(false);

    JSONObject result = copyProcessPlanVersion.doExecute(parameters, content);

    assertNotNull(result);
    assertTrue(result.has(ActionHandlerTestConstants.MESSAGE), "JSON response should contain 'message' field");
    JSONObject message = result.getJSONObject(ActionHandlerTestConstants.MESSAGE);
    assertEquals("error", message.getString("severity"));
    assertTrue(message.getString("text").contains("There is an inconsistency with Activity"));
  }

  /**
   * Tests the `doExecute` method when there is a data inconsistency with a product.
   * Verifies that the method returns a JSON response with an error message.
   *
   * @throws Exception
   *     if an error occurs during test execution
   */
  @Test
  void testDoExecuteDataInconsistencyWithProduct() throws Exception {
    String content = createJsonContent(ActionHandlerTestConstants.PROCESS_PLAN_ID, ActionHandlerTestConstants.VERSION_ID);
    Map<String, Object> parameters = new HashMap<>();

    List<Operation> operations = new ArrayList<>();
    operations.add(sourceOperation);
    when(sourceVersion.getManufacturingOperationList()).thenReturn(operations);

    when(sourceOperation.getActivity()).thenReturn(activity);

    when(orgStructureProvider.isInNaturalTree(any(Organization.class), any(Organization.class))).thenReturn(
        true).thenReturn(false);

    List<OperationProduct> operationProducts = new ArrayList<>();
    operationProducts.add(sourceOperationProduct);
    when(sourceOperation.getManufacturingOperationProductList()).thenReturn(operationProducts);
    when(sourceOperationProduct.getProduct()).thenReturn(product);
    when(product.getIdentifier()).thenReturn("Test Product");
    when(organization.getIdentifier()).thenReturn("Test Organization");

    JSONObject result = copyProcessPlanVersion.doExecute(parameters, content);

    assertNotNull(result);
    assertTrue(result.has(ActionHandlerTestConstants.MESSAGE), "JSON response should contain 'message' field");
    JSONObject message = result.getJSONObject(ActionHandlerTestConstants.MESSAGE);
    assertEquals("error", message.getString("severity"));
    assertTrue(message.getString("text").contains("There is an inconsistency with Product"));
  }

  /**
   * Creates a JSON content string for testing the `doExecute` method.
   *
   * @param processPlanId
   *     the ID of the process plan
   * @param versionId
   *     the ID of the version
   * @return a JSON string representing the input content
   * @throws JSONException
   *     if an error occurs while creating the JSON object
   */
  private String createJsonContent(String processPlanId, String versionId) throws JSONException {
    JSONObject jsonRequest = new JSONObject();
    JSONObject params = new JSONObject();
    params.put("MA_Processplan_Version_ID", versionId);
    jsonRequest.put("_params", params);
    jsonRequest.put("inpmaProcessplanId", processPlanId);
    return jsonRequest.toString();
  }
}
