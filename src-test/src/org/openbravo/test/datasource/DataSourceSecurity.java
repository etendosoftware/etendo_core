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
 * All portions are Copyright (C) 2016-2021 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.datasource;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.RoleOrganization;
import org.openbravo.model.ad.access.UserRoles;
import org.openbravo.model.ad.domain.Preference;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.uom.UOM;
import org.openbravo.model.materialmgmt.onhandquantity.Reservation;
import org.openbravo.service.json.JsonConstants;

/**
 * Test cases to ensure that mechanism of security DataSource access is working properly.
 *
 * @author inigo.sanchez
 *
 */
@RunWith(Parameterized.class)
public class DataSourceSecurity extends BaseDataSourceTestDal {
  private static final Logger log = LogManager.getLogger();
  private static final String ASTERISK_ORG_ID = "0";
  private static final String CONTEXT_USER = "100";
  private static final String LANGUAGE_ID = "192";
  private static final String WAREHOUSE_ID = "B2D40D8A5D644DD89E329DC297309055";
  private static final String ROLE_INTERNATIONAL_ADMIN = "42D0EEB1C66F497A90DD526DC597E6F0";
  private static final String ROLE_NO_ACCESS = "1";
  private static final String ROLE_SYSTEM_ADMIN = "0";
  private static final String ROLE_EMPLOYEE = "D615084948E046E3A439915008F464A6";

  private static final String ESP_ORG = "E443A31992CB4635AFCAEABE7183CE85";
  private static final String CLIENT = "23C59575B9CF467C9620760EB255B389";

  private static final String TABLE_WINDOWS_TABS_FIELDS_ID = "105";
  private static final String RECORD_OF_WINDOWS_TABS_FIELDS_ID = "283";
  private static final String ID_TESTING = "11";
  private static final Object PROD_RESERVATION = "DA7FC1BB3BA44EC48EC1AB9C74168CED";

  private static final String OPERATION_FETCH = "fetch";
  private static final String OPERATION_UPDATE = "update";

  private RoleType role;
  private DataSource dataSource;
  private int expectedResponseStatus;

  private enum RoleType {
    ADMIN_ROLE(ROLE_INTERNATIONAL_ADMIN, ESP_ORG), //
    NO_ACCESS_ROLE(ROLE_NO_ACCESS, ESP_ORG), //
    SYSTEM_ROLE(ROLE_SYSTEM_ADMIN, ASTERISK_ORG_ID), //
    EMPLOYEE_ROLE(ROLE_EMPLOYEE, ESP_ORG);

    private String roleId;
    private String orgId;

    private RoleType(String roleId, String orgId) {
      this.roleId = roleId;
      this.orgId = orgId;
    }
  }

  private enum JSONObjectURL {
    // Move node in Account Tree
    MOVEMENT_NODE("?_skinVersion=Default&_create=true&Constants_FIELDSEPARATOR=$&_new=true"
        + "&_startRow=0&_endRow=200&referencedTableId=188&parentRecordId=56E65CF592BD4DAF8A8A879810646266&tabId=132"
        + "&@FinancialMgmtElement.client@=23C59575B9CF467C9620760EB255B389"
        + "&@FinancialMgmtElement.id@=56E65CF592BD4DAF8A8A879810646266"
        + "&@FinancialMgmtElement.organization@=B843C30461EA4501935CB1D125C9C25A&@FinancialMgmtElement.type@=A"
        + "&@FinancialMgmtElementValue.organization@=B843C30461EA4501935CB1D125C9C25A"
        + "&@FinancialMgmtElementValue.client@=23C59575B9CF467C9620760EB255B389"
        + "&@FinancialMgmtElementValue.accountingElement@=56E65CF592BD4DAF8A8A879810646266"
        + "&@FinancialMgmtElementValue.id@=A45B7570F9BE4A69A3BF53CFEBB29FC0&dropIndex=2"
        + "&nextNodeId=FF30CF29CE614360AF85020438BFE328&isc_dataFormat=json&prevNodeId=C3FE5804602E481FAEDCA5D4D71B6CF",
        createJsonObjectForNodeMovement()), //
    NO_APPLIED("", "");

    private String url;
    private String content;

    private JSONObjectURL(String url, String content) {
      this.url = url;
      this.content = content;
    }

    private static String createJsonObjectForNodeMovement() {
      JSONObject dataObject = new JSONObject();
      JSONObject oldValuesJSON = new JSONObject();
      JSONObject contentJson = new JSONObject();
      try {
        dataObject.put("_identifier", "P - PATRIMONIO NETO Y PASIVO");
        dataObject.put("_entityName", "FinancialMgmtElementValue");
        dataObject.put("$ref", "FinancialMgmtElementValue/C3FE5804602E481FAEDCA5D4D71B6CF3");
        dataObject.put("id", "C3FE5804602E481FAEDCA5D4D71B6CF3");
        dataObject.put("client", "23C59575B9CF467C9620760EB255B389");
        dataObject.put("parentId", "-1");
        dataObject.put("organization", "B843C30461EA4501935CB1D125C9C25A");
        dataObject.put("searchKey", "A");
        dataObject.put("accountingElement", "56E65CF592BD4DAF8A8A879810646266");
        dataObject.put("nodeId", "A45B7570F9BE4A69A3BF53CFEBB29FC0");
        dataObject.put("seqno", "210");

        oldValuesJSON.put("_identifier", "A - ACTIVO");
        oldValuesJSON.put("_entityName", "FinancialMgmtElementValue");
        oldValuesJSON.put("$ref", "FinancialMgmtElementValue/A45B7570F9BE4A69A3BF53CFEBB29FC0");
        oldValuesJSON.put("id", "A45B7570F9BE4A69A3BF53CFEBB29FC0");
        oldValuesJSON.put("client", "23C59575B9CF467C9620760EB255B389");
        oldValuesJSON.put("organization", "B843C30461EA4501935CB1D125C9C25A");
        oldValuesJSON.put("organization", "B843C30461EA4501935CB1D125C9C25A");
        oldValuesJSON.put("searchKey", "A");
        oldValuesJSON.put("name", "ACTIVO");
        oldValuesJSON.put("accountingElement", "56E65CF592BD4DAF8A8A879810646266");
        oldValuesJSON.put("nodeId", "A45B7570F9BE4A69A3BF53CFEBB29FC0");

        contentJson.put("dataSource",
            "D2F94DC86DEC48D69E4BFCE59DC670CF_1462265510526_1462271742157");
        contentJson.put("operationType", "update");
        contentJson.put("data", dataObject);
        contentJson.put("oldValues", oldValuesJSON);
      } catch (JSONException e) {
        e.printStackTrace();
      }
      return contentJson.toString();
    }
  }

  @SuppressWarnings("serial")
  private enum DataSource {
    Order("Order", JSONObjectURL.NO_APPLIED, OPERATION_FETCH), //
    Alert("DB9F062472294F12A0291A7BD203F922", JSONObjectURL.NO_APPLIED, OPERATION_FETCH), //
    ProductByPriceAndWarehouse("ProductByPriceAndWarehouse", JSONObjectURL.NO_APPLIED,
        OPERATION_FETCH, new HashMap<String, String>() {
          {
            try {
              put("_selectorDefinitionId", "2E64F551C7C4470C80C29DBA24B34A5F");
              put("filterClass", "org.openbravo.userinterface.selector.SelectorDataSourceFilter");
              put("_sortBy", "_identifier");
              put("_requestType", "Window");
              put("_distinct", "productPrice");

              // To reproduce this problem is important not to add the targetProperty parameter. For
              // this reason targetProperty=null.
              put("_inpTableId", "293");
              put("_textMatchStyle", "substring");

              // Filter selector
              JSONObject criteria = new JSONObject();
              criteria.put("fieldName", "productPrice$priceListVersion$_identifier");
              criteria.put("operator", "iContains");
              criteria.put("value", "Tarifa");
              put("criteria", criteria.toString());
            } catch (Exception ignore) {
            }
          }
        }), //
    PropertySelector("83B60C4C19AE4A9EBA947B948C5BA04D", JSONObjectURL.NO_APPLIED, OPERATION_FETCH,
        new HashMap<String, String>() {
          {
            // Property selector invocation from Windows > Tab > Field > Property field
            put("_selectorDefinitionId", "387D9FFC48A74054835C5DF6E6FD08F7");
            put("inpadTableId", "259");
            put("inpTabId", "107");
            put("targetProperty", "property");
          }
        }), //
    ManageVariants("6654D607F650425A9DFF7B6961D54920", JSONObjectURL.NO_APPLIED, OPERATION_FETCH,
        new HashMap<String, String>() {
          {
            put("@Product.id@", ID_TESTING);
          }
        }), //
    Note("090A37D22E61FE94012E621729090048", JSONObjectURL.NO_APPLIED, OPERATION_FETCH,
        new HashMap<String, String>() {
          {
            // Note of a record in Windows, Tabs and Fields.
            String criteria = "{\"fieldName\":\"table\",\"operator\":\"equals\",\"value\":\""
                + TABLE_WINDOWS_TABS_FIELDS_ID
                + "\"}__;__{\"fieldName\":\"record\",\"operator\":\"equals\",\"value\":\""
                + RECORD_OF_WINDOWS_TABS_FIELDS_ID + "\"}";
            String entityName = "OBUIAPP_Note";
            put("criteria", criteria);
            put("_entityName", entityName);
          }
        }), //
    ProductCharacteristics("BE2735798ECC4EF88D131F16F1C4EC72", JSONObjectURL.NO_APPLIED,
        OPERATION_FETCH), //
    Combo("ComboTableDatasourceService", JSONObjectURL.NO_APPLIED, OPERATION_FETCH,
        new HashMap<String, String>() {
          {
            // Sales Order > Payment Terms
            put("fieldId", "1099");
          }
        }), //
    CustomQuerySelectorDatasource("F8DD408F2F3A414188668836F84C21AF", JSONObjectURL.NO_APPLIED,
        OPERATION_FETCH, new HashMap<String, String>() {
          {
            // Sales Invoice > Selector Business Partner
            put("_selectorDefinitionId", "862F54CB1B074513BD791C6789F4AA42");
            put("inpTableId", "318");
            put("targetProperty", "businessPartner");
          }
        }), //
    CustomQuerySelectorDatasourceProcess("ADList", JSONObjectURL.NO_APPLIED, OPERATION_FETCH,
        new HashMap<String, String>() {
          {
            // Sales Order > Add Payment process > Selector Action Regarding Document
            put("_selectorDefinitionId", "41B3A5EA61AB46FBAF4567E3755BA190");
            put("_processDefinitionId", "9BED7889E1034FE68BD85D5D16857320");
            put("targetProperty", "businessPartner");
          }
        }), //
    SelectorGLItemDatasource("FinancialMgmtGLItem", JSONObjectURL.NO_APPLIED, OPERATION_FETCH,
        new HashMap<String, String>() {
          {
            // Payment In > Add Details process > GLItem section > Selector GLItem
            put("_selectorDefinitionId", "9FAD469CE4414A25974CF45C0AD22D35");
            put("inpTableId", "D1A97202E832470285C9B1EB026D54E2");
            put("targetProperty", "gLItem");
          }
        }), //
    HQLDataSource("3C1148C0AB604DE1B51B7EA4112C325F", JSONObjectURL.NO_APPLIED, OPERATION_FETCH,
        new HashMap<String, String>() {
          {
            // Invocation from Sales Order > Add Payment process > Credit to Use.
            put("tableId", "58AF4D3E594B421A9A7307480736F03E");
          }
        }), //
    ADTree("90034CAE96E847D78FBEF6D38CB1930D", JSONObjectURL.NO_APPLIED, OPERATION_FETCH,
        new HashMap<String, String>() {
          {
            // Organization tree view.
            put("referencedTableId", "155");
            put("tabId", "143");
            String selectedPro = "[\"searchKey\",\"name\",\"description\",\"active\",\"summaryLevel\",\"socialName\",\"organizationType\",\"currency\",\"allowPeriodControl\",\"calendar\"]";
            put("_selectedProperties", selectedPro);
          }
        }), //
    AccountTree("D2F94DC86DEC48D69E4BFCE59DC670CF", JSONObjectURL.NO_APPLIED, OPERATION_FETCH,
        new HashMap<String, String>() {
          {
            // Account tree > Element value > Open tree view.
            put("referencedTableId", "188");
            put("tabId", "132");
            String selectedPro = "[\"searchKey\",\"name\",\"elementLevel\",\"accountType\",\"showValueCondition\",\"summaryLevel\"]";
            put("_selectedProperties", selectedPro);
            put("@FinancialMgmtElement.client@", CLIENT);
            put("@FinancialMgmtElement.id@", "56E65CF592BD4DAF8A8A879810646266");
            put("@FinancialMgmtElement.organization@", "B843C30461EA4501935CB1D125C9C25A");
          }
        }), //
    StockReservations("2F5B70D7F12E4F5C8FE20D6F17D69ECF", JSONObjectURL.NO_APPLIED, OPERATION_FETCH,
        new HashMap<String, String>() {
          {
            // Manage Stock from Stock Reservations
            put("@MaterialMgmtReservation.id@", ID_TESTING);
          }
        }), //
    QueryList("DD17275427E94026AD721067C3C91C18", JSONObjectURL.NO_APPLIED, OPERATION_FETCH,
        new HashMap<String, String>() {
          {
            // Query List Widget > Best Sellers
            put("widgetId", "CD1B06C4ED974B5F905A5A01B097DF4E");
          }
        }), //
    AccountTreeMovement("D2F94DC86DEC48D69E4BFCE59DC670CF", JSONObjectURL.MOVEMENT_NODE,
        OPERATION_UPDATE), //
    ProductStockView("ProductStockView", JSONObjectURL.NO_APPLIED, OPERATION_FETCH,
        new HashMap<String, String>() {
          {
            // Requisition Window > Requisition Lines > Product Complete Selector.
            put("_selectorDefinitionId", "4C8BC3E8E56441F4B8C98C684A0C9212");
            put("_inpTableId", "800214");
            put("targetProperty", "product");

          }
        });

    private String ds;
    private JSONObjectURL urlAndJson;
    private String operation;
    private Map<String, String> params;

    private DataSource(String ds, JSONObjectURL urlAndJson, String operation) {
      this.ds = ds;
      this.urlAndJson = urlAndJson;
      this.operation = operation;

      params = new HashMap<String, String>();
      params.put("_operationType", operation);
      params.put("_startRow", "0");
      params.put("_endRow", "1");
    }

    private DataSource(String ds, JSONObjectURL urlAndJson, String operation,
        Map<String, String> extraParams) {
      this(ds, urlAndJson, operation);
      params.putAll(extraParams);
    }
  }

  public DataSourceSecurity(RoleType role, DataSource dataSource, int expectedResponseStatus) {
    this.role = role;
    this.dataSource = dataSource;
    this.expectedResponseStatus = expectedResponseStatus;
  }

  @Parameters(name = "{0} - dataSource: {1}")
  public static Collection<Object[]> parameters() {
    List<Object[]> testCases = new ArrayList<Object[]>();
    for (RoleType type : RoleType.values()) {
      int accessForAdminOnly = type == RoleType.ADMIN_ROLE ? JsonConstants.RPCREQUEST_STATUS_SUCCESS
          : JsonConstants.RPCREQUEST_STATUS_VALIDATION_ERROR;
      int accessForAdminAndSystemOnly = (type == RoleType.NO_ACCESS_ROLE
          || type == RoleType.EMPLOYEE_ROLE) ? JsonConstants.RPCREQUEST_STATUS_VALIDATION_ERROR
              : JsonConstants.RPCREQUEST_STATUS_SUCCESS;
      int accessForAdminAndSystemAndEmployee = type == RoleType.NO_ACCESS_ROLE
          ? JsonConstants.RPCREQUEST_STATUS_VALIDATION_ERROR
          : JsonConstants.RPCREQUEST_STATUS_SUCCESS;

      testCases.add(new Object[] { type, DataSource.Order, accessForAdminOnly });
      testCases.add(new Object[] { type, DataSource.ManageVariants, accessForAdminOnly });
      testCases.add(new Object[] { type, DataSource.ProductCharacteristics, accessForAdminOnly });
      testCases.add(new Object[] { type, DataSource.Combo, accessForAdminOnly });
      testCases.add(new Object[] { type, DataSource.CustomQuerySelectorDatasource,
          accessForAdminAndSystemAndEmployee });
      testCases.add(new Object[] { type, DataSource.CustomQuerySelectorDatasourceProcess,
          accessForAdminAndSystemOnly });

      testCases.add(new Object[] { type, DataSource.HQLDataSource, accessForAdminOnly });
      testCases.add(new Object[] { type, DataSource.ADTree, accessForAdminAndSystemOnly });
      testCases.add(new Object[] { type, DataSource.AccountTree, accessForAdminOnly });
      testCases.add(new Object[] { type, DataSource.StockReservations, accessForAdminOnly });

      // QueryList ds is accessible if current role has access to widgetId
      testCases.add(new Object[] { type, DataSource.QueryList,
          JsonConstants.RPCREQUEST_STATUS_VALIDATION_ERROR });
      testCases.add(new Object[] { type, DataSource.PropertySelector,
          type == RoleType.SYSTEM_ROLE ? JsonConstants.RPCREQUEST_STATUS_SUCCESS
              : JsonConstants.RPCREQUEST_STATUS_VALIDATION_ERROR });

      // Alert ds should be always accessible
      testCases
          .add(new Object[] { type, DataSource.Alert, JsonConstants.RPCREQUEST_STATUS_SUCCESS });

      // Note ds is accessible if current role has access to entity of the notes. This note is
      // invocated from a record in Windows, Tabs and Fields.
      testCases.add(new Object[] { type, DataSource.Note, accessForAdminAndSystemOnly });

      // Selector into a datasource into a P&E Window.
      testCases.add(
          new Object[] { type, DataSource.SelectorGLItemDatasource, accessForAdminAndSystemOnly });

      // Moving a tree node : https://issues.openbravo.com/view.php?id=32833
      testCases.add(new Object[] { type, DataSource.AccountTreeMovement, accessForAdminOnly });

      // Testing a problem detected in how permissions for the entities of the selectors with Search
      // parent reference are calculated. See issue https://issues.openbravo.com/view.php?id=34823
      testCases.add(
          new Object[] { type, DataSource.ProductStockView, accessForAdminAndSystemAndEmployee });
    }
    // testing a problem detected in how properties are initialized.
    testCases.add(new Object[] { RoleType.ADMIN_ROLE, DataSource.ProductByPriceAndWarehouse,
        JsonConstants.RPCREQUEST_STATUS_SUCCESS });

    return testCases;
  }

  /** Creates dummy role without any access for testing purposes */
  @BeforeClass
  public static void createNoAccessRoleAndGenericProduct() {
    OBContext.setOBContext(CONTEXT_USER);

    Role noAccessRole = OBProvider.getInstance().get(Role.class);
    noAccessRole.setId("1");
    noAccessRole.setNewOBObject(true);
    noAccessRole.setOrganization(OBDal.getInstance().get(Organization.class, ASTERISK_ORG_ID));
    noAccessRole.setName("Test No Access");
    noAccessRole.setManual(true);
    noAccessRole.setUserLevel(" CO");
    OBDal.getInstance().save(noAccessRole);

    RoleOrganization noAcessRoleOrg = OBProvider.getInstance().get(RoleOrganization.class);
    noAcessRoleOrg.setOrganization(
        (Organization) OBDal.getInstance().getProxy(Organization.ENTITY_NAME, ESP_ORG));
    noAcessRoleOrg.setRole(noAccessRole);
    OBDal.getInstance().save(noAcessRoleOrg);

    UserRoles noAccessRoleUser = OBProvider.getInstance().get(UserRoles.class);
    noAccessRoleUser.setOrganization(noAccessRole.getOrganization());
    noAccessRoleUser.setUserContact(OBContext.getOBContext().getUser());
    noAccessRoleUser.setRole(noAccessRole);
    OBDal.getInstance().save(noAccessRoleUser);

    // Create product generic for manage variants
    Product productToClone = OBDal.getInstance()
        .get(Product.class, "DA7FC1BB3BA44EC48EC1AB9C74168CED");
    Product product = (Product) DalUtil.copy(productToClone, false);
    product.setId(ID_TESTING);
    product.setNewOBObject(true);
    product.setOrganization(OBDal.getInstance().get(Organization.class, ASTERISK_ORG_ID));
    product.setName("Generic Product Test");
    product.setSearchKey("GEN-1 ");
    product.setClient(OBDal.getInstance().get(Client.class, CLIENT));
    product.setGeneric(true);
    OBDal.getInstance().save(product);

    // Preference StockReservations
    Preference preference = OBProvider.getInstance().get(Preference.class);
    preference.setId(ID_TESTING);
    preference.setNewOBObject(true);
    preference.setClient(OBDal.getInstance().get(Client.class, CLIENT));
    preference.setOrganization(OBDal.getInstance().get(Organization.class, ESP_ORG));
    preference.setVisibleAtClient(OBDal.getInstance().get(Client.class, CLIENT));
    preference.setVisibleAtOrganization(OBDal.getInstance().get(Organization.class, ESP_ORG));
    preference.setPropertyList(true);
    preference.setProperty("StockReservations");
    preference.setSearchKey("Y");
    OBDal.getInstance().save(preference);

    Reservation reservation = OBProvider.getInstance().get(Reservation.class);
    reservation.setId(ID_TESTING);
    reservation.setNewOBObject(true);
    reservation.setClient(OBDal.getInstance().get(Client.class, CLIENT));
    reservation.setOrganization(OBDal.getInstance().get(Organization.class, ESP_ORG));
    reservation.setProduct(OBDal.getInstance().get(Product.class, PROD_RESERVATION));
    reservation.setQuantity(new BigDecimal(1));
    reservation.setUOM(OBDal.getInstance().get(UOM.class, "100"));
    OBDal.getInstance().save(reservation);

    OBDal.getInstance().commitAndClose();
  }

  /** Tests datasource allows or denies fetch action based on role access */
  @Test
  public void fetchShouldBeAllowedOnlyIfRoleIsGranted() throws Exception {
    OBContext.setOBContext(CONTEXT_USER);
    changeProfile(role.roleId, LANGUAGE_ID, role.orgId, WAREHOUSE_ID);

    JSONObject jsonResponse = null;
    if (dataSource.operation.equals(OPERATION_FETCH)) {
      if (dataSource.ds.equalsIgnoreCase("3C1148C0AB604DE1B51B7EA4112C325F"))
        dataSource.params.put("ad_org_id", role.orgId);
      if (dataSource.ds.equalsIgnoreCase("2F5B70D7F12E4F5C8FE20D6F17D69ECF"))
        dataSource.params.put("@MaterialMgmtReservation.organization@", role.orgId);
      jsonResponse = fetchDataSource();
    } else if (dataSource.operation.equals(OPERATION_UPDATE)) {
      jsonResponse = updateDataSource();
    }
    assertThat("Response status for: " + jsonResponse.toString(), jsonResponse.getInt("status"),
        is(expectedResponseStatus));
  }

  private JSONObject updateDataSource() throws Exception {
    String parameters = addCsrfTokenToParameters(dataSource.urlAndJson.content);
    String responseUpdate = doRequest(
        "/org.openbravo.service.datasource/" + dataSource.ds + dataSource.urlAndJson.url,
        parameters, 200, "PUT", "application/json");

    return new JSONObject(responseUpdate).getJSONObject("response");
  }

  private String addCsrfTokenToParameters(String content) {
    JSONObject params = initializeJSONObject(content);
    try {
      params.put(JsonConstants.CSRF_TOKEN_PARAMETER, getSessionCsrfToken());
    } catch (JSONException e) {
      log.error("Cannot add the CSRF Token to request params", e);
    }
    return params.toString();
  }

  private JSONObject initializeJSONObject(String content) {
    try {
      return new JSONObject(content);
    } catch (JSONException e) {
      return new JSONObject();
    }
  }

  private JSONObject fetchDataSource() throws Exception {
    String response = doRequest("/org.openbravo.service.datasource/" + dataSource.ds,
        dataSource.params, 200, "POST");
    return new JSONObject(response).getJSONObject("response");
  }

  /** Deletes dummy testing role and product */
  @AfterClass
  public static void cleanUp() {
    OBContext.setOBContext(CONTEXT_USER);
    OBDal.getInstance().remove(OBDal.getInstance().get(Role.class, ROLE_NO_ACCESS));
    OBDal.getInstance().remove(OBDal.getInstance().get(Product.class, ID_TESTING));
    OBDal.getInstance().remove(OBDal.getInstance().get(Preference.class, ID_TESTING));
    OBDal.getInstance().remove(OBDal.getInstance().get(Reservation.class, ID_TESTING));

    OBDal.getInstance().commitAndClose();
  }
}
