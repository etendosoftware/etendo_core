package org.openbravo.test.stockValuationReport;

import static org.openbravo.test.stockValuationReport.ReportValuationStockTestUtil.FB_US_WEST_COAST_ORG_ID;
import static org.openbravo.test.stockValuationReport.ReportValuationStockTestUtil.buildJsonObject;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;


import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.application.ReportDefinition;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.test.base.Issue;
import org.openbravo.test.base.OBBaseTest;
import org.openbravo.test.base.TestConstants;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.openbravo.test.stockValuationReport.ReportValuationStockTestUtil.deleteTestWarehouse;


import com.etendoerp.reportvaluationstock.handler.ReportValuationStock;

public class ReportValuationStockTest extends OBBaseTest {

  private static Warehouse warehouse;

  @AfterClass
  public static void cleanup() {
    OBContext.restorePreviousMode();
    deleteTestWarehouse(warehouse.getId());
  }

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    OBContext.setOBContext(TestConstants.Users.ADMIN, TestConstants.Roles.FB_GRP_ADMIN, TestConstants.Clients.FB_GRP,
        TestConstants.Orgs.US_WEST);
    OBContext currentContext = OBContext.getOBContext();
    VariablesSecureApp vsa = new VariablesSecureApp(currentContext.getUser().getId(),
        currentContext.getCurrentClient().getId(), currentContext.getCurrentOrganization().getId(),
        currentContext.getRole().getId());
    RequestContext.get().setVariableSecureApp(vsa);
  }

  @Test
  @Issue("#351")
  public void testCreateAndAssignWarehouse() {
    warehouse = ReportValuationStockTestUtil.createTestWarehouse("Test Warehouse", FB_US_WEST_COAST_ORG_ID);
    Organization org = ReportValuationStockTestUtil.getOrganization(FB_US_WEST_COAST_ORG_ID);
    ReportValuationStockTestUtil.assignWarehouseToOrganization(warehouse, org);
    Date currentDate = new Date();
    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
    String date = formatter.format(currentDate);

    Exception exception = assertThrows(InvocationTargetException.class, () -> {
      invokeMethod(org.getId(), warehouse.getId(), date, OBBaseTest.DOLLAR_ID);
    });

    String actualMessage = ((InvocationTargetException) exception).getTargetException().getMessage();
    assert (actualMessage.contains(OBMessageUtils.messageBD("NoDataFound")));
  }

  /**
   * Invokes the private method {@code addAdditionalParameters} from the {@link ReportValuationStock} class
   * using reflection. This method dynamically calls {@code addAdditionalParameters} with the given parameters.
   *
   * @param orgId
   *     the ID of the organization
   * @param warehouseId
   *     the ID of the warehouse
   * @param date
   *     the date for the report in the format "yyyy-MM-dd"
   * @param currencyId
   *     the ID of the currency to be used in the report
   * @throws NoSuchMethodException
   *     if the {@code addAdditionalParameters} method cannot be found
   * @throws InvocationTargetException
   *     if the invoked method throws an exception
   * @throws IllegalAccessException
   *     if this method object enforces Java language access control and the underlying method is inaccessible
   * @throws JSONException
   *     if an error occurs while building the JSON object
   */
  private void invokeMethod(String orgId, String warehouseId, String date,
      String currencyId) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, JSONException {
    Method method = ReportValuationStock.class.getDeclaredMethod("addAdditionalParameters", ReportDefinition.class,
        JSONObject.class, Map.class);
    method.setAccessible(true);
    ReportValuationStock rvs = new ReportValuationStock();
    JSONObject jsonContent = buildJsonObject(orgId, warehouseId, date, currencyId);
    method.invoke(rvs, null, jsonContent, null);
  }
}
