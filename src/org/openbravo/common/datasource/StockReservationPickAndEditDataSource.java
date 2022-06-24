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
 * All portions are Copyright (C) 2014-2020 Openbravo SLU
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.common.datasource;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.query.Query;
import org.openbravo.base.exception.OBSecurityException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.security.OrganizationStructureProvider;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.materialmgmt.ReservationUtils;
import org.openbravo.model.common.enterprise.Locator;
import org.openbravo.model.common.enterprise.OrgWarehouse;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.AttributeSetInstance;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.materialmgmt.onhandquantity.InventoryStatus;
import org.openbravo.model.materialmgmt.onhandquantity.Reservation;
import org.openbravo.model.materialmgmt.onhandquantity.ReservationStock;
import org.openbravo.model.materialmgmt.onhandquantity.StorageDetail;
import org.openbravo.model.procurement.POInvoiceMatch;
import org.openbravo.service.datasource.ReadOnlyDataSourceService;
import org.openbravo.service.json.DataToJsonConverter;
import org.openbravo.service.json.JsonConstants;
import org.openbravo.service.json.JsonUtils;

public class StockReservationPickAndEditDataSource extends ReadOnlyDataSourceService {

  private static Logger log4j = LogManager.getLogger();
  private static final String AD_TABLE_ID = "7BDAC914CA60418795E453BC0E8C89DC";

  String ol = null;

  @Override
  public Entity getEntity() {
    return ModelProvider.getInstance().getEntityByTableId(AD_TABLE_ID);
  }

  @Override
  public String fetch(Map<String, String> parameters) {
    int startRow = 0;

    final List<JSONObject> jsonObjects = fetchJSONObject(parameters);

    final JSONObject jsonResult = new JSONObject();
    final JSONObject jsonResponse = new JSONObject();
    try {
      jsonResponse.put(JsonConstants.RESPONSE_STATUS, JsonConstants.RPCREQUEST_STATUS_SUCCESS);
      jsonResponse.put(JsonConstants.RESPONSE_STARTROW, startRow);
      jsonResponse.put(JsonConstants.RESPONSE_ENDROW, jsonObjects.size() - 1);
      jsonResponse.put(JsonConstants.RESPONSE_TOTALROWS, jsonObjects.size());
      jsonResponse.put(JsonConstants.RESPONSE_DATA, new JSONArray(jsonObjects));
      jsonResult.put(JsonConstants.RESPONSE_RESPONSE, jsonResponse);
    } catch (JSONException e) {
    }

    return jsonResult.toString();
  }

  @Override
  public void checkFetchDatasourceAccess(Map<String, String> parameter) {
    final OBContext obContext = OBContext.getOBContext();
    try {
      Entity entityToCheck = ModelProvider.getInstance().getEntityByTableId(AD_TABLE_ID);
      if (entityToCheck != null) {
        obContext.getEntityAccessChecker().checkReadableAccess(entityToCheck);
      }
    } catch (OBSecurityException e) {
      handleExceptionUnsecuredDSAccess(e);
    }
  }

  private List<JSONObject> fetchJSONObject(Map<String, String> parameters) {
    final int startRow = Integer.parseInt(parameters.get(JsonConstants.STARTROW_PARAMETER));
    final int endRow = Integer.parseInt(parameters.get(JsonConstants.ENDROW_PARAMETER));
    final List<Map<String, Object>> data = getData(parameters, startRow, endRow);
    final DataToJsonConverter toJsonConverter = OBProvider.getInstance()
        .get(DataToJsonConverter.class);
    toJsonConverter.setAdditionalProperties(JsonUtils.getAdditionalProperties(parameters));
    return toJsonConverter.convertToJsonObjects(data);
  }

  @Override
  protected List<Map<String, Object>> getData(Map<String, String> parameters, int startRow,
      int endRow) {
    List<Map<String, Object>> result = new ArrayList<>();
    if (parameters.get(JsonConstants.DISTINCT_PARAMETER) != null) {
      String distinct = parameters.get(JsonConstants.DISTINCT_PARAMETER);
      log4j.debug("Distinct param: " + distinct);
      if ("warehouse".equals(distinct)) {
        result = getWarehouseFilterData(parameters);

      } else if ("storageBin".equals(distinct)) {
        result = getStorageFilterData(parameters);

      } else if ("attributeSetValue".equals(distinct)) {
        result = getAttributeSetValueFilterData(parameters);

      } else if ("purchaseOrderLine".equals(distinct)) {
        result = getOrderLineSetValueFilterData(parameters);

      } else if ("inventoryStatus".equals(distinct)) {
        result = getInventoryStatusFilterData(parameters);

      }
    } else {
      result = getGridData(parameters);
      log4j.debug("data length: " + result.size());
    }
    return result;
  }

  private List<Map<String, Object>> getOrderLineSetValueFilterData(Map<String, String> parameters) {
    List<Map<String, Object>> result = new ArrayList<>();
    Map<String, String> filterCriteria = buildCriteria(parameters);
    OBContext.setAdminMode();
    try {
      for (OrderLine o : getOrderLineFromGrid(filterCriteria.get("orderLine$_identifier"),
          getGridData(parameters))) {
        Map<String, Object> myMap = new HashMap<>();
        myMap.put("id", o.getId());
        myMap.put("name", o.getIdentifier());
        myMap.put("_identifier", o.getIdentifier());
        myMap.put("_entityName", "OrderLine");
        result.add(myMap);
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return result;
  }

  private List<OrderLine> getOrderLineFromGrid(String contains, List<Map<String, Object>> data) {
    Set<String> ids = new HashSet<>();
    ids.add("-");
    for (Map<String, Object> record : data) {
      if (isOrderLineIdentifierContained(contains, record)) {
        continue;
      }
      ids.add((String) record.get("purchaseOrderLine"));
    }

    return OBDal.getInstance()
        .createCriteria(OrderLine.class)
        .add(Restrictions.in(OrderLine.PROPERTY_ID, ids))
        .setFilterOnReadableClients(false)
        .setFilterOnReadableOrganization(false)
        .setFilterOnActive(false)
        .addOrderBy(OrderLine.PROPERTY_SALESORDER, false)
        .addOrderBy(OrderLine.PROPERTY_LINENO, true)
        .list();
  }

  private boolean isOrderLineIdentifierContained(final String contains,
      final Map<String, Object> record) {
    return !StringUtils.isEmpty(contains) && OBDal.getInstance()
        .get(OrderLine.class, record.get("purchaseOrderLine"))
        .getIdentifier()
        .contains(contains);
  }

  private List<Map<String, Object>> getWarehouseFilterData(Map<String, String> parameters) {
    List<Map<String, Object>> result = new ArrayList<>();
    Map<String, String> filterCriteria = buildCriteria(parameters);
    OBContext.setAdminMode();
    try {
      for (Warehouse o : getWarehouseFromGrid(filterCriteria.get("warehouse$_identifier"),
          getGridData(parameters))) {
        Map<String, Object> myMap = new HashMap<>();
        myMap.put("id", o.getId());
        myMap.put("name", o.getIdentifier());
        myMap.put("_identifier", o.getIdentifier());
        myMap.put("_entityName", "Warehouse");
        result.add(myMap);
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return result;
  }

  private List<Warehouse> getWarehouseFromGrid(String contains, List<Map<String, Object>> data) {
    Set<String> ids = new HashSet<>();
    ids.add("-");
    for (Map<String, Object> record : data) {
      if (isWareouseIdentifierContained(contains, record)) {
        continue;
      }
      ids.add((String) record.get("warehouse"));
    }

    return OBDal.getInstance()
        .createCriteria(Warehouse.class)
        .add(Restrictions.in(Warehouse.PROPERTY_ID, ids))
        .setFilterOnReadableClients(false)
        .setFilterOnReadableOrganization(false)
        .setFilterOnActive(false)
        .addOrderBy(Warehouse.PROPERTY_NAME, true)
        .list();
  }

  private boolean isWareouseIdentifierContained(final String contains,
      final Map<String, Object> record) {
    return !StringUtils.isEmpty(contains) && OBDal.getInstance()
        .get(Warehouse.class, record.get("warehouse"))
        .getIdentifier()
        .contains(contains);
  }

  private List<Warehouse> getFilteredWarehouse(String contains, Map<String, String> parameters) {
    String strReservation = parameters.get("@MaterialMgmtReservation.id@");
    Reservation reservation = OBDal.getInstance().get(Reservation.class, strReservation);
    new OrganizationStructureProvider().getChildTree(reservation.getOrganization().getId(), true);
    OBCriteria<Warehouse> obc = OBDal.getInstance().createCriteria(Warehouse.class);
    if (reservation.getWarehouse() != null) {
      obc.add(Restrictions.eq(Warehouse.PROPERTY_ID, reservation.getWarehouse().getId()));
      return obc.list();
    }
    if (reservation.getStorageBin() != null) {
      obc.add(Restrictions.eq(Warehouse.PROPERTY_ID,
          reservation.getStorageBin().getWarehouse().getId()));
      return obc.list();
    }
    // Just on hand warehouses are taken into account as per window validation
    obc.add(Restrictions.in(Warehouse.PROPERTY_ID,
        getOnHandWarehouseIds(reservation.getOrganization())));
    if (contains != null && !"".equals(contains)) {
      if (contains.startsWith("[")) {
        try {
          JSONArray myJSON = new JSONArray(contains);

          Criterion myCriterion = null;
          for (int i = 0; i < myJSON.length(); i++) {
            JSONObject myJSONObject = (JSONObject) myJSON.get(i);
            String operator = (String) myJSONObject.get("operator");

            if (myJSONObject.get("fieldName").equals("warehouse$_identifier")) {
              if (myCriterion == null) {
                if (operator.equals("iEquals")) {
                  myCriterion = Restrictions.ilike(Warehouse.PROPERTY_NAME,
                      myJSONObject.get("value"));
                } else if (operator.equals("iContains")) {
                  myCriterion = Restrictions.ilike(Warehouse.PROPERTY_NAME,
                      "%" + myJSONObject.get("value") + "%");
                }
              } else {
                myCriterion = Restrictions.or(myCriterion, Restrictions
                    .ilike(Warehouse.PROPERTY_NAME, "%" + myJSONObject.get("value") + "%"));
              }
            } else if (myJSONObject.get("fieldName").equals("warehouse")
                && myJSONObject.get("operator").equals("equals") && myJSONObject.has("value")) {
              if (myCriterion == null) {
                myCriterion = Restrictions.eq(Warehouse.PROPERTY_ID, myJSONObject.get("value"));
              } else {
                myCriterion = Restrictions.or(myCriterion,
                    Restrictions.eq(Warehouse.PROPERTY_ID, myJSONObject.get("value")));
              }
            }
          }
          if (myCriterion != null) {
            obc.add(myCriterion);
          }
        } catch (JSONException e) {
          log4j.error("Error getting filter for warehouses", e);
        }
      } else {
        obc.add(Restrictions.ilike(Warehouse.PROPERTY_NAME, "%" + contains + "%"));
      }
    }
    obc.addOrder(Order.asc(Warehouse.PROPERTY_NAME));
    return obc.list();
  }

  private List<String> getOnHandWarehouseIds(Organization organization) {
    List<String> result = new ArrayList<>();
    for (Warehouse o : getOnHandWarehouses(organization)) {
      result.add(o.getId());
    }
    return result;
  }

  private List<Warehouse> getOnHandWarehouses(final Organization organization) {
    final List<Warehouse> result = new ArrayList<>();

    final List<OrgWarehouse> orgWarehosueList = OBDal.getInstance()
        .createCriteria(OrgWarehouse.class)
        .add(Restrictions.eq(OrgWarehouse.PROPERTY_ORGANIZATION, organization))
        .list();

    for (final OrgWarehouse ow : orgWarehosueList) {
      result.add(ow.getWarehouse());
    }
    return result;
  }

  private List<Map<String, Object>> getStorageFilterData(Map<String, String> parameters) {
    List<Map<String, Object>> result = new ArrayList<>();
    Map<String, String> filterCriteria = buildCriteria(parameters);
    OBContext.setAdminMode();
    try {
      for (Locator o : getStorageBinFromGrid(filterCriteria.get("storageBin$_identifier"),
          getGridData(parameters))) {
        Map<String, Object> myMap = new HashMap<>();
        myMap.put("id", o.getId());
        myMap.put("name", o.getIdentifier());
        myMap.put("_identifier", o.getIdentifier());
        myMap.put("_entityName", "Locator");
        result.add(myMap);
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return result;
  }

  private List<Locator> getStorageBinFromGrid(String contains, List<Map<String, Object>> data) {
    Set<String> ids = new HashSet<>();
    ids.add("-");
    for (Map<String, Object> record : data) {
      if (contains != null && !"".equals(contains)) {
        Locator locator = OBDal.getInstance().get(Locator.class, record.get("storageBin"));
        if (locator != null && locator.getIdentifier().contains(contains)) {
          continue;
        }
      }
      ids.add((String) record.get("storageBin"));
    }
    return OBDal.getInstance()
        .createCriteria(Locator.class)
        .add(Restrictions.in(Locator.PROPERTY_ID, ids))
        .setFilterOnReadableClients(false)
        .setFilterOnReadableOrganization(false)
        .setFilterOnActive(false)
        .addOrderBy(Locator.PROPERTY_WAREHOUSE, true)
        .addOrderBy(Locator.PROPERTY_ROWX, true)
        .addOrderBy(Locator.PROPERTY_STACKY, true)
        .addOrderBy(Locator.PROPERTY_LEVELZ, true)
        .list();
  }

  private List<Locator> getFilteredStorageBin(String contains, Map<String, String> parameters) {
    String strReservation = parameters.get("@MaterialMgmtReservation.id@");
    Reservation reservation = OBDal.getInstance().get(Reservation.class, strReservation);
    new OrganizationStructureProvider().getChildTree(reservation.getOrganization().getId(), true);
    OBCriteria<Locator> obc = OBDal.getInstance()
        .createCriteria(Locator.class)
        .add(Restrictions.in(Locator.PROPERTY_WAREHOUSE,
            getOnHandWarehouses(reservation.getOrganization())));
    if (reservation.getWarehouse() != null) {
      obc.add(Restrictions.eq(Locator.PROPERTY_WAREHOUSE, reservation.getWarehouse()));
    }
    if (reservation.getStorageBin() != null) {
      obc.add(Restrictions.eq(Locator.PROPERTY_ID, reservation.getStorageBin().getId()));
      return obc.list();
    }
    if (contains != null && !"".equals(contains)) {
      if (contains.startsWith("[")) {
        try {
          JSONArray myJSON = new JSONArray(contains);
          Criterion myCriterion = null;
          for (int i = 0; i < myJSON.length(); i++) {
            JSONObject myJSONObject = (JSONObject) myJSON.get(i);
            String operator = (String) myJSONObject.get("operator");
            if (myJSONObject.get("fieldName").equals("storageBin$_identifier")
                && myJSONObject.has("value")) {
              if (myCriterion == null) {
                if (operator.equals("iEquals")) {
                  myCriterion = Restrictions.ilike(Locator.PROPERTY_SEARCHKEY,
                      myJSONObject.get("value"));
                } else if (operator.equals("iContains")) {
                  myCriterion = Restrictions.ilike(Locator.PROPERTY_SEARCHKEY,
                      "%" + myJSONObject.get("value") + "%");
                }
              } else {
                myCriterion = Restrictions.or(myCriterion, Restrictions
                    .ilike(Locator.PROPERTY_SEARCHKEY, "%" + myJSONObject.get("value") + "%"));
              }
            } else if (myJSONObject.get("fieldName").equals("storageBin")
                && operator.equals("equals") && myJSONObject.has("value")) {
              if (myCriterion == null) {
                myCriterion = Restrictions.eq(Locator.PROPERTY_ID, myJSONObject.get("value"));
              } else {
                myCriterion = Restrictions.or(myCriterion,
                    Restrictions.eq(Locator.PROPERTY_ID, myJSONObject.get("value")));
              }
            }
          }
          if (myCriterion != null) {
            obc.add(myCriterion);
          }
        } catch (JSONException e) {
          log4j.error("Error getting filter for storage bins", e);
        }
      } else {
        obc.add(Restrictions.ilike(Locator.PROPERTY_SEARCHKEY, "%" + contains + "%"));
      }
    }

    return obc.addOrderBy(Locator.PROPERTY_WAREHOUSE, true)
        .addOrderBy(Locator.PROPERTY_ROWX, true)
        .addOrderBy(Locator.PROPERTY_STACKY, true)
        .addOrderBy(Locator.PROPERTY_LEVELZ, true)
        .list();
  }

  private List<Map<String, Object>> getAttributeSetValueFilterData(Map<String, String> parameters) {
    List<Map<String, Object>> result = new ArrayList<>();
    Map<String, String> filterCriteria = buildCriteria(parameters);
    OBContext.setAdminMode();
    try {
      for (AttributeSetInstance o : getAttributeSetValueFromGrid(
          filterCriteria.get("attributeSetValue$_identifier"), getGridData(parameters))) {
        Map<String, Object> myMap = new HashMap<>();
        myMap.put("id", o.getId());
        myMap.put("name", o.getIdentifier());
        myMap.put("_identifier", o.getIdentifier());
        myMap.put("_entityName", "Locator");
        result.add(myMap);
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return result;
  }

  private List<AttributeSetInstance> getAttributeSetValueFromGrid(String contains,
      List<Map<String, Object>> data) {
    Set<String> ids = new HashSet<>();
    ids.add("-");
    for (Map<String, Object> record : data) {
      ids.add((String) record.get("attributeSetValue"));
    }
    return OBDal.getInstance()
        .createCriteria(AttributeSetInstance.class)
        .add(Restrictions.in(AttributeSetInstance.PROPERTY_ID, ids))
        .setFilterOnReadableClients(false)
        .setFilterOnReadableOrganization(false)
        .setFilterOnActive(false)
        .addOrderBy(AttributeSetInstance.PROPERTY_DESCRIPTION, true)
        .list();
  }

  private List<Map<String, Object>> getInventoryStatusFilterData(Map<String, String> parameters) {
    List<Map<String, Object>> result = new ArrayList<>();
    Map<String, String> filterCriteria = buildCriteria(parameters);
    OBContext.setAdminMode();
    try {
      for (InventoryStatus o : getInventoryStatusFromGrid(
          filterCriteria.get("inventoryStatus$_identifier"), getGridData(parameters))) {
        Map<String, Object> myMap = new HashMap<>();
        myMap.put("id", o.getId());
        myMap.put("name", o.getIdentifier());
        myMap.put("_identifier", o.getIdentifier());
        myMap.put("_entityName", "InventoryStatus");
        result.add(myMap);
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return result;
  }

  private List<InventoryStatus> getInventoryStatusFromGrid(String contains,
      List<Map<String, Object>> data) {
    Set<String> ids = new HashSet<>();
    ids.add("-");
    for (Map<String, Object> record : data) {
      ids.add((String) record.get("inventoryStatus"));
    }

    return OBDal.getInstance()
        .createCriteria(InventoryStatus.class)
        .add(Restrictions.in(InventoryStatus.PROPERTY_ID, ids))
        .setFilterOnReadableClients(false)
        .setFilterOnReadableOrganization(false)
        .setFilterOnActive(false)
        .addOrderBy(InventoryStatus.PROPERTY_NAME, true)
        .list();
  }

  private List<Map<String, Object>> getGridData(Map<String, String> parameters) {
    List<Map<String, Object>> result = new ArrayList<>();
    Map<String, String> filterCriteria = new HashMap<>();
    ArrayList<String> selectedIds = new ArrayList<>();
    try {
      // Builds the criteria based on the fetch parameters
      JSONArray criterias = (JSONArray) JsonUtils.buildCriteria(parameters).get("criteria");

      for (int i = 0; i < criterias.length(); i++) {
        final JSONObject criteria = criterias.getJSONObject(i);
        if (criteria.has("fieldName") && criteria.getString("fieldName").equals("id")) {
          if (criteria.has("value")) {
            selectedIds
                .add(criteria.has("value") ? criteria.getString("value") : criteria.toString());
          }
        }

        // Multiple selection
        if (criteria.has("criteria") && criteria.has("operator")) {
          JSONArray mySon = new JSONArray(criteria.getString("criteria"));
          for (int j = 0; j < mySon.length(); j++) {
            final JSONObject criteria2 = mySon.getJSONObject(j);
            if (criteria2.has("criteria") && criteria2.has("operator")) {
              JSONArray mySonSon = new JSONArray(criteria2.getString("criteria"));
              for (int k = 0; k < mySonSon.length(); k++) {
                final JSONObject criteria3 = mySonSon.getJSONObject(k);
                if (criteria3.has("fieldName")) {
                  if (filterCriteria.containsKey(criteria3.getString("fieldName"))) {
                    JSONArray values = new JSONArray(
                        filterCriteria.get(criteria3.getString("fieldName")));
                    filterCriteria.put(criteria3.getString("fieldName"),
                        values.put(criteria3).toString());
                  } else {
                    filterCriteria.put(criteria3.getString("fieldName"),
                        new JSONArray().put(criteria3).toString());
                  }
                }
              }
            } else if (criteria2.has("fieldName")) {
              if (filterCriteria.containsKey(criteria2.getString("fieldName"))) {
                JSONArray values = new JSONArray(
                    filterCriteria.get(criteria2.getString("fieldName")));
                filterCriteria.put(criteria2.getString("fieldName"),
                    values.put(criteria2).toString());
              } else {
                filterCriteria.put(criteria2.getString("fieldName"),
                    new JSONArray().put(criteria2).toString());
              }
            }
          }
          // lessOrEqual
        } else if (criteria.has("operator") && ("greaterThan".equals(criteria.getString("operator"))
            || "lessThan".equals(criteria.getString("operator"))
            || "greaterOrEqual".equals(criteria.getString("operator"))
            || "lessOrEqual".equals(criteria.getString("operator")))) {
          filterCriteria.put(criteria.getString("fieldName"), criteria.toString());
        } else if (criteria.has("operator") && ("equals".equals(criteria.getString("operator"))
            || "iEquals".equals(criteria.getString("operator"))
            || "iContains".equals(criteria.getString("operator")))) {

          if (filterCriteria.containsKey(criteria.getString("fieldName"))) {
            JSONArray myson = new JSONArray(filterCriteria.get(criteria.getString("fieldName")));
            filterCriteria.put(criteria.getString("fieldName"), myson.put(criteria).toString());
          } else {
            JSONArray myson = new JSONArray();
            filterCriteria.put(criteria.getString("fieldName"), myson.put(criteria).toString());
          }

        } else {
          filterCriteria.put(criteria.getString("fieldName"),
              criteria.has("value") ? criteria.getString("value") : criteria.toString());
        }
      }

    } catch (JSONException e) {
      log4j.error("Error while building the criteria", e);
    }
    OBContext.setAdminMode();
    String strReservation = parameters.get("@MaterialMgmtReservation.id@");
    ol = parameters.get("@OrderLine.id@");
    Reservation reservation = null;
    if (ol != null && !"".equals(ol)) {
      reservation = ReservationUtils
          .getReservationFromOrder(OBDal.getInstance().get(OrderLine.class, ol));
      parameters.put("@MaterialMgmtReservation.id@", reservation.getId());
    }
    // Filters
    List<Warehouse> warehousesFiltered = null;
    if (filterCriteria.get("warehouse$_identifier") != null
        || filterCriteria.get("warehouse") != null) {
      String warehouseCriteria = filterCriteria.get("warehouse$_identifier");
      if (warehouseCriteria == null) {
        warehouseCriteria = filterCriteria.get("warehouse");
      }
      warehousesFiltered = getFilteredWarehouse(warehouseCriteria, parameters);
    }
    List<Locator> locatorsFiltered = null;
    if (filterCriteria.get("storageBin$_identifier") != null
        || filterCriteria.get("storageBin") != null) {
      String locatorCriteria = filterCriteria.get("storageBin$_identifier");
      if (locatorCriteria == null) {
        locatorCriteria = filterCriteria.get("storageBin");
      }
      locatorsFiltered = getFilteredStorageBin(locatorCriteria, parameters);
    }
    List<AttributeSetInstance> attributesFiltered = null;
    if (filterCriteria.get("attributeSetValue$_identifier") != null
        || filterCriteria.get("attributeSetValue") != null) {
      String attributesCriteria = filterCriteria.get("attributeSetValue$_identifier");
      if (attributesCriteria == null) {
        attributesCriteria = filterCriteria.get("attributeSetValue");
      }
      attributesFiltered = getFilteredAttribute(attributesCriteria, parameters);
    }
    List<OrderLine> orderLinesFiltered = null;
    if (filterCriteria.get("purchaseOrderLine$_identifier") != null
        || filterCriteria.get("purchaseOrderLine") != null) {
      String orderLinesCriteria = filterCriteria.get("purchaseOrderLine$_identifier");
      if (orderLinesCriteria == null) {
        orderLinesCriteria = filterCriteria.get("purchaseOrderLine");
      }
      orderLinesFiltered = getFilteredOrderline(orderLinesCriteria, parameters);
    }
    List<InventoryStatus> inventoryStatusFiltered = null;
    if (filterCriteria.get("inventoryStatus$_identifier") != null
        || filterCriteria.get("inventoryStatus") != null) {
      String inventoryStatusCriteria = filterCriteria.get("inventoryStatus$_identifier");
      if (inventoryStatusCriteria == null) {
        inventoryStatusCriteria = filterCriteria.get("inventoryStatus");
      }
      inventoryStatusFiltered = getFilteredInventoryStatus(inventoryStatusCriteria, parameters);
    }
    String availableQtyFilterCriteria = "";
    if (filterCriteria.get("availableQty") != null) {
      availableQtyFilterCriteria = filterCriteria.get("availableQty");
    }
    String reservedinothersFilterCriteria = "";
    if (filterCriteria.get("reservedinothers") != null) {
      reservedinothersFilterCriteria = filterCriteria.get("reservedinothers");
    }
    String releasedFilterCriteria = "";
    if (filterCriteria.get("released") != null) {
      releasedFilterCriteria = filterCriteria.get("released");
    }
    String allocatedCriteria = "";
    if (filterCriteria.get("allocated") != null) {
      allocatedCriteria = filterCriteria.get("allocated");
    }
    String quantityCriteria = "";
    if (filterCriteria.get("quantity") != null) {
      quantityCriteria = filterCriteria.get("quantity");
    }

    if (ol != null && !"".equals(ol)) {
      reservation = ReservationUtils
          .getReservationFromOrder(OBDal.getInstance().get(OrderLine.class, ol));
      if (reservation.getRESStatus().equals("DR")) {
        ReservationUtils.processReserve(reservation, "PR");
      }
    } else {
      reservation = OBDal.getInstance().get(Reservation.class, strReservation);
    }
    String strOrganization = parameters.get("@MaterialMgmtReservation.organization@");
    if (strOrganization == null || strOrganization.equals("")) {
      strOrganization = parameters.get("@Order.organization@");
    }
    Set<String> organizations = new OrganizationStructureProvider().getChildTree(strOrganization,
        true);
    try {
      result.addAll(getSelectedLines(reservation, organizations, warehousesFiltered,
          locatorsFiltered, attributesFiltered, orderLinesFiltered, availableQtyFilterCriteria,
          reservedinothersFilterCriteria, releasedFilterCriteria, allocatedCriteria,
          quantityCriteria, selectedIds, inventoryStatusFiltered));

      if (orderLinesFiltered == null || orderLinesFiltered.isEmpty()) {
        result.addAll(getStorageDetail(reservation, organizations, warehousesFiltered,
            locatorsFiltered, attributesFiltered, availableQtyFilterCriteria,
            reservedinothersFilterCriteria, releasedFilterCriteria, allocatedCriteria,
            quantityCriteria, selectedIds, inventoryStatusFiltered));
      }
      if (locatorsFiltered == null) {
        result.addAll(getPurchaseOrderLines(reservation, organizations, warehousesFiltered,
            attributesFiltered, orderLinesFiltered, availableQtyFilterCriteria,
            reservedinothersFilterCriteria, releasedFilterCriteria, allocatedCriteria,
            quantityCriteria, selectedIds));
      }

    } finally {
      OBContext.restorePreviousMode();
    }
    if (ol != null && !"".equals(ol)) {
      OBDal.getInstance().rollbackAndClose();
    }
    result = sortResult(result, parameters.get("_sortBy"));
    return result;
  }

  private List<Map<String, Object>> sortResult(List<Map<String, Object>> result, String sortBy) {
    if (sortBy == null || "".equals(sortBy)) {
      return result;
    } else {

      try {
        Collections.sort(result, new MapComparator(sortBy));
      } catch (Exception e) {
        log4j.error("Error in sortResult. sortBy: " + sortBy != null ? sortBy : "null");
        return result;
      }

      return result;
    }
  }

  private static class MapComparator implements Comparator<Map<String, Object>> {
    private String currentCompareBy = "convertedFreightAmount";
    boolean desc = false;

    public MapComparator(String compareBy) {
      currentCompareBy = compareBy;
      if (currentCompareBy.startsWith("-")) {
        desc = true;
        currentCompareBy = currentCompareBy.substring(1);
      }
    }

    @Override
    public int compare(Map<String, Object> o1, Map<String, Object> o2) {
      Object obj1 = o1.get(currentCompareBy);
      Object obj2 = o2.get(currentCompareBy);
      if (obj2 == null) {
        return -1;
      } else if (obj1 == null) {
        return 1;
      }
      if (obj1 instanceof BigDecimal) {
        final BigDecimal v1 = (BigDecimal) o1.get(currentCompareBy);
        final BigDecimal v2 = (BigDecimal) o2.get(currentCompareBy);
        return desc ? v2.compareTo(v1) : v1.compareTo(v2);
      } else if (obj1 instanceof String) {
        final String v1 = ((String) o1.get(currentCompareBy)).toLowerCase();
        final String v2 = ((String) o2.get(currentCompareBy)).toLowerCase();
        return desc ? v2.compareTo(v1) : v1.compareTo(v2);
      } else if (obj1 instanceof BaseOBObject) {
        final String v1 = ((BaseOBObject) o1.get(currentCompareBy)).getIdentifier();
        final String v2 = ((BaseOBObject) o2.get(currentCompareBy)).getIdentifier();
        return desc ? v2.compareTo(v1) : v1.compareTo(v2);
      } else if (obj1 instanceof Boolean) {
        final boolean v1 = (Boolean) o1.get(currentCompareBy);
        final boolean v2 = (Boolean) o2.get(currentCompareBy);
        if (v1 == v2) {
          return 0;
        }
        if (v1) {
          return desc ? -1 : 1;
        } else {
          return desc ? 1 : -1;
        }
      } else {
        // unable to compare
        return 0;
      }
    }
  }

  private List<AttributeSetInstance> getFilteredAttribute(String contains,
      Map<String, String> parameters) {
    String strReservation = parameters.get("@MaterialMgmtReservation.id@");
    Reservation reservation = OBDal.getInstance().get(Reservation.class, strReservation);
    new OrganizationStructureProvider().getChildTree(reservation.getOrganization().getId(), true);
    OBCriteria<AttributeSetInstance> obc = OBDal.getInstance()
        .createCriteria(AttributeSetInstance.class);
    if (reservation.getAttributeSetValue() != null) {
      obc.add(
          Restrictions.eq(AttributeSetInstance.PROPERTY_ID, reservation.getAttributeSetValue()));
    }
    if (contains != null && !"".equals(contains)) {
      Criterion myCriterion = null;
      if (contains.startsWith("[")) {
        try {
          JSONArray myJSON = new JSONArray(contains);
          for (int i = 0; i < myJSON.length(); i++) {
            JSONObject myJSONObject = (JSONObject) myJSON.get(i);
            String operator = (String) myJSONObject.get("operator");
            if (myJSONObject.get("fieldName").equals("attributeSetValue$_identifier")
                && myJSONObject.has("value")) {
              if (myCriterion == null) {
                if (operator.equals("iContains")) {
                  myCriterion = Restrictions.ilike(AttributeSetInstance.PROPERTY_DESCRIPTION,
                      "%" + myJSONObject.get("value") + "%");
                } else if (operator.equals("iEquals")) {
                  myCriterion = Restrictions.ilike(AttributeSetInstance.PROPERTY_DESCRIPTION,
                      myJSONObject.get("value"));
                }
              } else {
                myCriterion = Restrictions.or(myCriterion,
                    Restrictions.ilike(AttributeSetInstance.PROPERTY_DESCRIPTION,
                        "%" + myJSONObject.get("value") + "%"));
              }
            } else if (myJSONObject.get("fieldName").equals("attributeSetValue")
                && operator.equals("equals") && myJSONObject.has("value")) {
              if (myCriterion == null) {
                myCriterion = Restrictions.eq(AttributeSetInstance.PROPERTY_ID,
                    myJSONObject.get("value"));
              } else {
                myCriterion = Restrictions.or(myCriterion,
                    Restrictions.eq(AttributeSetInstance.PROPERTY_ID, myJSONObject.get("value")));
              }
            }
          }
          if (myCriterion != null) {
            obc.add(myCriterion);
          }
        } catch (JSONException e) {
          log4j.error("Error getting filter for attribute", e);
        }
      } else {
        obc.add(
            Restrictions.ilike(AttributeSetInstance.PROPERTY_DESCRIPTION, "%" + contains + "%"));
      }
    }
    obc.addOrder(Order.asc(AttributeSetInstance.PROPERTY_DESCRIPTION));
    return obc.list();
  }

  private List<OrderLine> getFilteredOrderline(String contains, Map<String, String> parameters) {
    String strReservation = parameters.get("@MaterialMgmtReservation.id@");
    Reservation reservation = OBDal.getInstance().get(Reservation.class, strReservation);
    new OrganizationStructureProvider().getChildTree(reservation.getOrganization().getId(), true);
    OBCriteria<OrderLine> obc = OBDal.getInstance().createCriteria(OrderLine.class);
    obc.createAlias(OrderLine.PROPERTY_SALESORDER, "o");
    if (reservation.getAttributeSetValue() != null) {
      obc.add(Restrictions.eq(OrderLine.PROPERTY_ATTRIBUTESETVALUE,
          reservation.getAttributeSetValue()));
    }
    obc.add(Restrictions.eq(OrderLine.PROPERTY_PRODUCT, reservation.getProduct()));
    if (contains != null && !"".equals(contains)) {
      Criterion myCriterion = null;
      if (contains.startsWith("[")) {
        try {
          JSONArray myJSON = new JSONArray(contains);
          for (int i = 0; i < myJSON.length(); i++) {
            JSONObject myJSONObject = (JSONObject) myJSON.get(i);
            String operator = (String) myJSONObject.get("operator");
            if (myJSONObject.getString("fieldName").equals("purchaseOrderLine$_identifier")) {
              if (myCriterion == null) {
                if (operator.equals("iContains")) {
                  myCriterion = Restrictions.ilike(
                      "o." + org.openbravo.model.common.order.Order.PROPERTY_DOCUMENTNO,
                      "%" + getOrderDocumentNo((String) myJSONObject.get("value")) + "%");
                } else if (operator.equals("iEquals")) {
                  myCriterion = Restrictions.ilike(
                      "o." + org.openbravo.model.common.order.Order.PROPERTY_DOCUMENTNO,
                      getOrderDocumentNo((String) myJSONObject.get("value")));
                }
              } else {
                myCriterion = Restrictions.or(myCriterion,
                    Restrictions.ilike(
                        "o." + org.openbravo.model.common.order.Order.PROPERTY_DOCUMENTNO,
                        "%" + getOrderDocumentNo((String) myJSONObject.get("value")) + "%"));
              }
            } else if (myJSONObject.getString("fieldName").equals("purchaseOrderLine")
                && operator.equals("equals") && myJSONObject.has("value")) {
              if (myCriterion == null) {
                myCriterion = Restrictions.eq(
                    org.openbravo.model.common.order.OrderLine.PROPERTY_ID,
                    myJSONObject.get("value"));
              } else {
                myCriterion = Restrictions.or(myCriterion,
                    Restrictions.eq(org.openbravo.model.common.order.OrderLine.PROPERTY_ID,
                        myJSONObject.get("value")));
              }
            }
          }
          if (myCriterion != null) {
            obc.add(myCriterion);
          }
        } catch (JSONException e) {
          log4j.error("Error getting filter for attribute", e);
        }
      } else {
        obc.add(
            Restrictions.ilike("o." + org.openbravo.model.common.order.Order.PROPERTY_DOCUMENTNO,
                "%" + getOrderDocumentNo(contains) + "%"));
      }
    }

    return obc.list();
  }

  private List<InventoryStatus> getFilteredInventoryStatus(String contains,
      Map<String, String> parameters) {
    String strReservation = parameters.get("@MaterialMgmtReservation.id@");
    Reservation reservation = OBDal.getInstance().get(Reservation.class, strReservation);
    new OrganizationStructureProvider().getChildTree(reservation.getOrganization().getId(), true);
    OBCriteria<InventoryStatus> obc = OBDal.getInstance().createCriteria(InventoryStatus.class);
    if (contains != null && !"".equals(contains)) {
      Criterion myCriterion = null;
      if (contains.startsWith("[")) {
        try {
          JSONArray myJSON = new JSONArray(contains);
          for (int i = 0; i < myJSON.length(); i++) {
            JSONObject myJSONObject = (JSONObject) myJSON.get(i);
            String operator = (String) myJSONObject.get("operator");
            if (myJSONObject.getString("fieldName").equals("inventoryStatus$_identifier")) {
              if (myCriterion == null) {
                if (operator.equals("iContains")) {
                  myCriterion = Restrictions.ilike(InventoryStatus.PROPERTY_NAME,
                      "%" + myJSONObject.get("value") + "%");
                } else if (operator.equals("iEquals")) {
                  myCriterion = Restrictions.ilike(InventoryStatus.PROPERTY_NAME,
                      myJSONObject.get("value"));
                }
              } else {
                myCriterion = Restrictions.or(myCriterion, Restrictions
                    .ilike(InventoryStatus.PROPERTY_NAME, "%" + myJSONObject.get("value") + "%"));
              }
            } else if (myJSONObject.getString("fieldName").equals("inventoryStatus")
                && operator.equals("equals") && myJSONObject.has("value")) {
              if (myCriterion == null) {
                myCriterion = Restrictions.eq(InventoryStatus.PROPERTY_ID,
                    myJSONObject.get("value"));
              } else {
                myCriterion = Restrictions.or(myCriterion,
                    Restrictions.eq(InventoryStatus.PROPERTY_ID, myJSONObject.get("value")));
              }
            }
          }
          if (myCriterion != null) {
            obc.add(myCriterion);
          }
        } catch (JSONException e) {
          log4j.error("Error getting filter for attribute", e);
        }
      } else {
        obc.add(Restrictions.ilike(InventoryStatus.PROPERTY_NAME, "%" + contains + "%"));
      }
    }

    return obc.list();
  }

  private String getOrderDocumentNo(String orderLineIdentifier) {
    return new StringTokenizer(orderLineIdentifier).nextToken();
  }

  private List<Map<String, Object>> getSelectedLines(Reservation reservation,
      Set<String> organizations, List<Warehouse> warehousesFiltered, List<Locator> locatorsFiltered,
      List<AttributeSetInstance> attributeSetInstancesFiltered, List<OrderLine> orderLinesFiltered,
      String availableQtyFilterCriteria, String reservedinothersFilterCriteria,
      String releasedFilterCriteria, String allocatedCriteria, String quantityCriteria,
      ArrayList<String> selectedIds, List<InventoryStatus> inventoryStatusFiltered) {
    List<Map<String, Object>> result = new ArrayList<>();

    //@formatter:off
    String hql =
            "select rs from MaterialMgmtReservationStock rs " +
            "  join rs.reservation as r" +
            "  left join rs.storageBin as sb" +
            " where rs.reservation.id = :reservationId ";
    //@formatter:on
    if (reservation.getAttributeSetValue() != null) {
      //@formatter:off
      hql +=
            "   and rs.attributeSetValue.id = :attributeSetValueId ";
      //@formatter:on
    }
    if (attributeSetInstancesFiltered != null) {
      if (attributeSetInstancesFiltered.isEmpty()) {
        //@formatter:off
        hql +=
            "   and 1 = 2 ";
        //@formatter:on
      } else {
        //@formatter:off
        hql +=
            "   and rs.attributeSetValue in :attributeSetInstancesFiltered ";
        //@formatter:on
      }
    }
    if (reservation.getStorageBin() != null) {
      //@formatter:off
      hql += 
            "   and sb.id = :storageBinId ";
      //@formatter:on
    }
    if (locatorsFiltered != null) {
      if (locatorsFiltered.isEmpty()) {
        //@formatter:off
        hql += 
            "   and 1 = 2 ";
        //@formatter:on
      } else {
        //@formatter:off
        hql += 
            "   and sb in :locatorsFiltered ";
        //@formatter:on
      }
    }
    if (reservation.getWarehouse() != null) {
      //@formatter:off
      hql += 
            "   and sb.warehouse.id = :warehouseId ";
      //@formatter:on
    }
    if (warehousesFiltered != null) {
      if (warehousesFiltered.isEmpty()) {
        //@formatter:off
        hql += 
            "   and 1 = 2 ";
        //@formatter:on
      } else {
        //@formatter:off
        hql += 
            "   and sb.warehouse in :warehousesFiltered ";
        //@formatter:on
      }
    }
    if (orderLinesFiltered != null) {
      if (orderLinesFiltered.isEmpty()) {
        //@formatter:off
        hql += 
            "   and 1 = 2 ";
        //@formatter:on
      } else {
        //@formatter:off
        hql += 
            "   and rs.salesOrderLine in :orderLinesFiltered ";
        //@formatter:on
      }
    }
    if (inventoryStatusFiltered != null) {
      if (inventoryStatusFiltered.isEmpty()) {
        //@formatter:off
        hql += 
            "   and 1 = 2 ";
        //@formatter:on
      } else {
        //@formatter:off
        hql += 
            "   and sb.inventoryStatus in :inventoryStatusFiltered ";
        //@formatter:on
      }
    }
    //@formatter:off
    hql += 
            " order by rs.salesOrderLine DESC, r.warehouse, sb";
    //@formatter:on

    Query<ReservationStock> query = OBDal.getInstance()
        .getSession()
        .createQuery(hql, ReservationStock.class)
        .setParameter("reservationId", reservation.getId());

    if (reservation.getAttributeSetValue() != null) {
      query.setParameter("attributeSetValueId", reservation.getAttributeSetValue().getId());
    }
    if (attributeSetInstancesFiltered != null && !attributeSetInstancesFiltered.isEmpty()) {
      query.setParameterList("attributeSetInstancesFiltered", attributeSetInstancesFiltered);
    }
    if (reservation.getStorageBin() != null) {
      query.setParameter("storageBinId", reservation.getStorageBin().getId());
    }
    if (locatorsFiltered != null && !locatorsFiltered.isEmpty()) {
      query.setParameterList("locatorsFiltered", locatorsFiltered);
    }
    if (reservation.getWarehouse() != null) {
      query.setParameter("warehouseId", reservation.getWarehouse().getId());
    }
    if (warehousesFiltered != null && !warehousesFiltered.isEmpty()) {
      query.setParameterList("warehousesFiltered", warehousesFiltered);
    }
    if (orderLinesFiltered != null && !orderLinesFiltered.isEmpty()) {
      query.setParameterList("orderLinesFiltered", orderLinesFiltered);
    }
    if (inventoryStatusFiltered != null && !inventoryStatusFiltered.isEmpty()) {
      query.setParameterList("inventoryStatusFiltered", inventoryStatusFiltered);
    }

    for (ReservationStock rs : query.list()) {
      Map<String, Object> myMap = new HashMap<>();
      if (!selectedIds.isEmpty()) {
        for (int i = 0; i < selectedIds.size(); i++) {
          if (!(rs.getId().equals(selectedIds.get(i)))) {
            // Check Filter Criteria
            if (availableQtyFilterCriteria != null && !"".equals(availableQtyFilterCriteria)
                && !isInScope("availableQty", availableQtyFilterCriteria,
                    getQtyOnHand(reservation.getProduct(),
                        rs.getStorageBin() != null ? rs.getStorageBin() : null,
                        rs.getAttributeSetValue() != null ? rs.getAttributeSetValue() : null,
                        rs.getSalesOrderLine() != null ? rs.getSalesOrderLine() : null))) {
              continue;
            }
            if (reservedinothersFilterCriteria != null && !"".equals(reservedinothersFilterCriteria)
                && !isInScope("reservedinothers", reservedinothersFilterCriteria,
                    rs.getSalesOrderLine() != null
                        ? getQtyReserved(reservation, rs.getSalesOrderLine())
                        : getQtyReserved(reservation, reservation.getProduct(),
                            rs.getAttributeSetValue(), rs.getStorageBin()))) {
              continue;
            }
            if (releasedFilterCriteria != null && !"".equals(releasedFilterCriteria)
                && !isInScope("released", releasedFilterCriteria, BigDecimal.ZERO)) {
              continue;
            }
            if (quantityCriteria != null && !"".equals(quantityCriteria)
                && !isInScope("quantity", quantityCriteria, rs.getQuantity())) {
              continue;
            }
            if (StringUtils.isNotBlank(allocatedCriteria)
                && !isInScope("allocated", allocatedCriteria, rs.isAllocated())) {
              continue;
            }
            myMap = tomap(rs, reservation);
            if (!reservationStockFiltered(result, rs)) {
              result.add(myMap);
            }
          }
        }
      } else {
        // Check Filter Criterias
        if (availableQtyFilterCriteria != null && !"".equals(availableQtyFilterCriteria)
            && !isInScope("availableQty", availableQtyFilterCriteria,
                getQtyOnHand(reservation.getProduct(),
                    rs.getStorageBin() != null ? rs.getStorageBin() : null,
                    rs.getAttributeSetValue() != null ? rs.getAttributeSetValue() : null,
                    rs.getSalesOrderLine() != null ? rs.getSalesOrderLine() : null))) {
          continue;
        }
        if (reservedinothersFilterCriteria != null && !"".equals(reservedinothersFilterCriteria)
            && !isInScope("reservedinothers", reservedinothersFilterCriteria,
                rs.getSalesOrderLine() != null ? getQtyReserved(reservation, rs.getSalesOrderLine())
                    : getQtyReserved(reservation, reservation.getProduct(),
                        rs.getAttributeSetValue(), rs.getStorageBin()))) {
          continue;
        }
        if (releasedFilterCriteria != null && !"".equals(releasedFilterCriteria)
            && !isInScope("released", releasedFilterCriteria, BigDecimal.ZERO)) {
          continue;
        }
        if (quantityCriteria != null && !"".equals(quantityCriteria)
            && !isInScope("quantity", quantityCriteria, rs.getQuantity())) {
          continue;
        }
        if (StringUtils.isNotBlank(allocatedCriteria)
            && !isInScope("allocated", allocatedCriteria, rs.isAllocated())) {
          continue;
        }

        myMap = tomap(rs, reservation);
        if (!reservationStockFiltered(result, rs)) {
          result.add(myMap);
        }
      }
    }

    for (int i = 0; i < selectedIds.size(); i++) {
      Map<String, Object> myMap = new HashMap<>();
      ReservationStock rs = OBDal.getInstance().get(ReservationStock.class, selectedIds.get(i));
      if (rs != null) {
        myMap = tomap(rs, reservation);
        if (!reservationStockFiltered(result, rs)) {
          result.add(myMap);
        }
      }
    }

    return result;
  }

  private ReservationStock getPrereservedStock(Reservation reservation, Locator sb,
      AttributeSetInstance as) {
    ReservationStock rs = null;
    //@formatter:off
    String hql =
            "select rs from MaterialMgmtReservationStock rs " +
            "  join rs.reservation as r" +
            " where rs.reservation.id = :reservationId " +
            "   and rs.storageBin.id = :storageBinId " +
            "   and rs.attributeSetValue.id = :attributeSetValueId " +
            " order by rs.salesOrderLine DESC, r.warehouse, rs.storageBin";
  //@formatter:on

    Query<ReservationStock> query = OBDal.getInstance()
        .getSession()
        .createQuery(hql, ReservationStock.class)
        .setParameter("reservationId", reservation.getId())
        .setParameter("storageBinId", sb.getId())
        .setParameter("attributeSetValueId", as.getId())
        .setMaxResults(1);

    rs = !query.list().isEmpty() ? query.list().get(0) : null;
    return rs;
  }

  private List<Map<String, Object>> getPurchaseOrderLines(Reservation reservation,
      Set<String> organizations, List<Warehouse> warehousesFiltered,
      List<AttributeSetInstance> attributeSetInstancesFiltered, List<OrderLine> orderLinesFiltered,
      String availableQtyFilterCriteria, String reservedinothersFilterCriteria,
      String releasedFilterCriteria, String allocatedCriteria, String quantityCriteria,
      ArrayList<String> selectedIds) {
    List<Map<String, Object>> result = new ArrayList<>();

    //@formatter:off
    String hql =
            "select ol from OrderLine as ol " +
            "  join  ol.salesOrder as o " +
            " where o.salesTransaction = false and o.documentStatus = 'CO' " +
            "   and ol.product.id = :productId " +
            "   and o.warehouse in :warehouses " +
            "   and not exists ( " +
            "     select 1 from MaterialMgmtReservationStock as rs " +
            "      where rs.reservation.id = :reservationId " +
            "        and rs.salesOrderLine = ol " +
            "   ) ";
    //@formatter:off

    if (reservation.getAttributeSetValue() != null) {
      //@formatter:off
      hql += 
            "   and ol.attributeSetValue.id = :attributeSetValueId ";
      //@formatter:on
    }
    if (attributeSetInstancesFiltered != null) {
      if (attributeSetInstancesFiltered.isEmpty()) {
        //@formatter:off
        hql += 
            "   and 1 = 2 ";
        //@formatter:on
      } else {
        //@formatter:off
        hql += 
            "   and ol.attributeSetValue in :attributeSetInstancesFiltered ";
        //@formatter:on
      }
    }
    if (reservation.getWarehouse() != null) {
      //@formatter:off
      hql += 
          "   and o.warehouse.id = :warehouseId ";
      //@formatter:on
    }
    if (warehousesFiltered != null) {
      //@formatter:off
      hql += 
          "   and 1 = 2 ";
      //@formatter:on
    }
    if (orderLinesFiltered != null) {
      if (orderLinesFiltered.isEmpty()) {
        //@formatter:off
        hql +=
            "   and 1 = 2 ";
        //@formatter:on
      } else {
        //@formatter:off
        hql +=
            "   and ol in :orderLinesFiltered ";
        //@formatter:on
      }
    }
    //@formatter:off
    hql +=
            "   and ol.orderedQuantity <> coalesce((select Sum(mpo.quantity) from ProcurementPOInvoiceMatch as mpo where mpo.salesOrderLine.id = ol.id and mpo.goodsShipmentLine is not null),0)" +
            " order by o.documentNo, ol.lineNo";
    //@formatter:on

    Query<OrderLine> query = OBDal.getInstance()
        .getSession()
        .createQuery(hql, OrderLine.class)
        .setParameter("productId", reservation.getProduct().getId())
        .setParameter("reservationId", reservation.getId())
        .setParameterList("warehouses", getOnHandWarehouses(reservation.getOrganization()));

    if (reservation.getAttributeSetValue() != null) {
      query.setParameter("attributeSetValueId", reservation.getAttributeSetValue().getId());
    }
    if (attributeSetInstancesFiltered != null && !attributeSetInstancesFiltered.isEmpty()) {
      query.setParameterList("attributeSetInstancesFiltered", attributeSetInstancesFiltered);
    }
    if (reservation.getWarehouse() != null) {
      query.setParameter("warehouseId", reservation.getWarehouse().getId());
    }
    if (orderLinesFiltered != null && !orderLinesFiltered.isEmpty()) {
      query.setParameterList("orderLinesFiltered", orderLinesFiltered);
    }
    for (OrderLine orderLine : query.list()) {
      Map<String, Object> myMap = new HashMap<>();
      myMap.put("id", orderLine.getId());
      myMap.put("obSelected", false);
      myMap.put("reservationStock", null);
      myMap.put("reservationStock$_identifier", "");
      myMap.put("storageBin$_identifier", "");
      myMap.put("storageBin", "");
      myMap.put("warehouse", null);
      myMap.put("warehouse$_identifier", "");
      myMap.put("attributeSetValue",
          orderLine.getAttributeSetValue() != null ? orderLine.getAttributeSetValue().getId()
              : null);
      myMap.put("attributeSetValue$_identifier",
          orderLine.getAttributeSetValue() != null
              ? orderLine.getAttributeSetValue().getIdentifier()
              : "");
      myMap.put("purchaseOrderLine", orderLine.getId());
      myMap.put("purchaseOrderLine$_identifier", orderLine.getIdentifier());
      // Check Filter Criterias

      if (availableQtyFilterCriteria != null && !"".equals(availableQtyFilterCriteria)
          && !isInScope("availableQty", availableQtyFilterCriteria,
              orderLine.getOrderedQuantity())) {
        continue;
      }
      BigDecimal reservedinothers = getQtyReserved(reservation, orderLine);
      if (reservedinothersFilterCriteria != null && !"".equals(reservedinothersFilterCriteria)
          && !isInScope("reservedinothers", reservedinothersFilterCriteria, reservedinothers)) {
        continue;
      }
      if (releasedFilterCriteria != null && !"".equals(releasedFilterCriteria)
          && !isInScope("released", releasedFilterCriteria, BigDecimal.ZERO)) {
        continue;
      }
      if (quantityCriteria != null && !"".equals(quantityCriteria)
          && !isInScope("quantity", quantityCriteria, BigDecimal.ZERO)) {
        continue;
      }
      if (StringUtils.isNotBlank(allocatedCriteria) && !isInScope("allocated", allocatedCriteria,
          orderLine.getSalesOrder().getWarehouse().isAllocated())) {
        continue;
      }

      myMap.put("availableQty",
          orderLine.getOrderedQuantity().subtract(getDeliveredQuantity(orderLine)));
      myMap.put("reservedinothers", reservedinothers);
      myMap.put("quantity", BigDecimal.ZERO);
      myMap.put("reservationQuantity", reservation.getQuantity());
      myMap.put("released", BigDecimal.ZERO);
      myMap.put("allocated", orderLine.getSalesOrder().getWarehouse().isAllocated());
      result.add(myMap);
    }
    return result;
  }

  private List<Map<String, Object>> getStorageDetail(Reservation reservation,
      Set<String> organizations, List<Warehouse> warehousesFiltered, List<Locator> locatorsFiltered,
      List<AttributeSetInstance> attributeSetInstancesFiltered, String availableQtyFilterCriteria,
      String reservedinothersFilterCriteria, String releasedFilterCriteria,
      String allocatedCriteria, String quantityCriteria, ArrayList<String> selectedIds,
      List<InventoryStatus> inventoryStatusFiltered) {
    List<Map<String, Object>> result = new ArrayList<>();

    //@formatter:off
    String hql =
            "select sd from MaterialMgmtStorageDetail as sd " +
            "  join sd.storageBin as sb " +
            "    join sb.inventoryStatus as invs " +
            " where sd.quantityOnHand > 0 and sd.orderUOM is null " +
            "   and sd.product.id = :productId " +
            "   and sd.uOM.id = :uomId " +
            "   and invs.available = true " +
            "   and not exists ( " +
            "     select 1 from MaterialMgmtReservationStock as rs " +
            "      where rs.reservation.id = :reservationId " +
            "        and (rs.attributeSetValue.id = sd.attributeSetValue.id or rs.attributeSetValue is null) " +
            "        and rs.storageBin.id = sd.storageBin.id " +
            "   ) ";
    //@formatter:on

    if (reservation.getAttributeSetValue() != null) {
      //@formatter:off
      hql +=
            "   and sd.attributeSetValue.id = :attributeSetValueId ";
      //@formatter:on
    }
    if (attributeSetInstancesFiltered != null) {
      if (attributeSetInstancesFiltered.isEmpty()) {
        //@formatter:off
        hql +=
            "   and 1 = 2 ";
        //@formatter:off
      } else {
        //@formatter:off
        hql +=
            "   and sd.attributeSetValue in :attributeSetInstancesFiltered ";
        //@formatter:on
      }
    }
    if (reservation.getStorageBin() != null) {
      //@formatter:off
      hql +=
            "   and sd.storageBin.id = :storageBinId ";
      //@formatter:on
    }
    if (locatorsFiltered != null) {
      if (locatorsFiltered.isEmpty()) {
        //@formatter:off
        hql +=
            "   and 1 = 2 ";
        //@formatter:on
      } else {
        //@formatter:off
        hql +=
            "   and sd.storageBin in :locatorsFiltered ";
        //@formatter:on
      }
    }
    if (reservation.getWarehouse() != null) {
      //@formatter:off
      hql +=
            "   and sb.warehouse.id = :warehouseId ";
      //@formatter:on
    }
    if (warehousesFiltered != null) {
      if (warehousesFiltered.isEmpty()) {
        //@formatter:off
        hql +=
            "   and 1 = 2 ";
        //@formatter:on
      } else {
        //@formatter:off
        hql +=
            "   and sb.warehouse in :warehousesFiltered ";
        //@formatter:on
      }
    }
    if (inventoryStatusFiltered != null) {
      if (inventoryStatusFiltered.isEmpty()) {
        //@formatter:off
        hql +=
            "   and 1 = 2 ";
        //@formatter:on
      } else {
      //@formatter:off
        hql +=
            "   and sb.inventoryStatus in :inventoryStatusFiltered ";
      //@formatter:on
      }
    }
    //@formatter:off
    hql +=
            "   and sb.warehouse in :warehouses " +
            " order by sb.warehouse DESC, sd.storageBin.rowX, sd.storageBin.stackY, " +
            "   sd.storageBin.levelZ, sd.attributeSetValue.description";
    //@formatter:on

    Query<StorageDetail> query = OBDal.getInstance()
        .getSession()
        .createQuery(hql, StorageDetail.class)
        .setParameter("productId", reservation.getProduct().getId())
        .setParameter("uomId", reservation.getUOM().getId())
        .setParameter("reservationId", reservation.getId());

    if (reservation.getAttributeSetValue() != null) {
      query.setParameter("attributeSetValueId", reservation.getAttributeSetValue().getId());
    }
    if (attributeSetInstancesFiltered != null && !attributeSetInstancesFiltered.isEmpty()) {
      query.setParameterList("attributeSetInstancesFiltered", attributeSetInstancesFiltered);
    }
    if (reservation.getStorageBin() != null) {
      query.setParameter("storageBinId", reservation.getStorageBin().getId());
    }
    if (locatorsFiltered != null && !locatorsFiltered.isEmpty()) {
      query.setParameterList("locatorsFiltered", locatorsFiltered);
    }
    if (reservation.getWarehouse() != null) {
      query.setParameter("warehouseId", reservation.getWarehouse().getId());
    }
    if (warehousesFiltered != null && !warehousesFiltered.isEmpty()) {
      query.setParameterList("warehousesFiltered", warehousesFiltered);
    }
    if (inventoryStatusFiltered != null && !inventoryStatusFiltered.isEmpty()) {
      query.setParameterList("inventoryStatusFiltered", inventoryStatusFiltered);
    }

    query.setParameterList("warehouses", getOnHandWarehouses(reservation.getOrganization()));
    for (int i = 0; i < selectedIds.size(); i++) {
      StorageDetail sd = OBDal.getInstance().get(StorageDetail.class, selectedIds.get(i));

      if (sd != null) {
        BigDecimal reservedinothers = getQtyReserved(reservation, reservation.getProduct(),
            sd.getAttributeSetValue(), sd.getStorageBin());

        result = tomap(sd, true, result, reservedinothers, reservation);
      }
    }

    for (StorageDetail sd : query.list()) {
      if (!selectedIds.isEmpty()) {
        for (int i = 0; i < selectedIds.size(); i++) {
          if (!(sd.getId().equals(selectedIds.get(i)))) {
            // Check Filter Criteria
            if (availableQtyFilterCriteria != null && !"".equals(availableQtyFilterCriteria)
                && !isInScope("availableQty", availableQtyFilterCriteria, sd.getQuantityOnHand())) {
              continue;
            }
            BigDecimal reservedinothers = getQtyReserved(reservation, reservation.getProduct(),
                sd.getAttributeSetValue(), sd.getStorageBin());

            if (reservedinothersFilterCriteria != null && !"".equals(reservedinothersFilterCriteria)
                && !isInScope("reservedinothers", reservedinothersFilterCriteria,
                    reservedinothers)) {
              continue;
            }
            if (releasedFilterCriteria != null && !"".equals(releasedFilterCriteria)
                && !isInScope("released", releasedFilterCriteria, BigDecimal.ZERO)) {
              continue;
            }
            BigDecimal qtty = BigDecimal.ZERO;
            if (reservation.getRESStatus().equals("DR")) {
              ReservationStock rs = getPrereservedStock(reservation, sd.getStorageBin(),
                  sd.getAttributeSetValue());
              if (rs != null) {
                qtty = rs.getQuantity();
              }
            }
            if (quantityCriteria != null && !"".equals(quantityCriteria)
                && !isInScope("quantity", quantityCriteria, qtty)) {
              continue;
            }
            if (StringUtils.isNotBlank(allocatedCriteria) && !isInScope("allocated",
                allocatedCriteria, sd.getStorageBin().getWarehouse().isAllocated())) {
              continue;
            }
            result = tomap(sd, false, result, reservedinothers, reservation);
          }
        }
      } else {
        // Check Filter Criterias
        if (availableQtyFilterCriteria != null && !"".equals(availableQtyFilterCriteria)
            && !isInScope("availableQty", availableQtyFilterCriteria, sd.getQuantityOnHand())) {
          continue;
        }
        BigDecimal reservedinothers = getQtyReserved(reservation, reservation.getProduct(),
            sd.getAttributeSetValue(), sd.getStorageBin());

        if (reservedinothersFilterCriteria != null && !"".equals(reservedinothersFilterCriteria)
            && !isInScope("reservedinothers", reservedinothersFilterCriteria, reservedinothers)) {
          continue;
        }
        if (releasedFilterCriteria != null && !"".equals(releasedFilterCriteria)
            && !isInScope("released", releasedFilterCriteria, BigDecimal.ZERO)) {
          continue;
        }
        BigDecimal qtty = BigDecimal.ZERO;
        if (reservation.getRESStatus().equals("DR")) {
          ReservationStock rs = getPrereservedStock(reservation, sd.getStorageBin(),
              sd.getAttributeSetValue());
          if (rs != null) {
            qtty = rs.getQuantity();
          }
        }
        if (quantityCriteria != null && !"".equals(quantityCriteria)
            && !isInScope("quantity", quantityCriteria, qtty)) {
          continue;
        }
        if (StringUtils.isNotBlank(allocatedCriteria) && !isInScope("allocated", allocatedCriteria,
            sd.getStorageBin().getWarehouse().isAllocated())) {
          continue;
        }
        result = tomap(sd, false, result, reservedinothers, reservation);
      }
    }
    return result;
  }

  private Map<String, Object> tomap(ReservationStock rs, Reservation reservation) {
    Map<String, Object> myMap = new HashMap<>();
    myMap.put("id", rs.getId());
    myMap.put("obSelected", true);
    if (ol == null || "".equals(ol)) {
      myMap.put("reservationStock", rs.getId());
      myMap.put("reservationStock$_identifier", rs.getIdentifier());
    } else {
      myMap.put("reservationStock", null);
      myMap.put("reservationStock$_identifier", "");
    }
    myMap.put("storageBin$_identifier",
        rs.getStorageBin() != null ? rs.getStorageBin().getIdentifier() : "");
    myMap.put("storageBin", rs.getStorageBin() != null ? rs.getStorageBin().getId() : null);
    myMap.put("warehouse",
        (rs.getStorageBin() != null && rs.getStorageBin().getWarehouse() != null)
            ? rs.getStorageBin().getWarehouse().getId()
            : null);
    myMap.put("warehouse$_identifier",
        (rs.getStorageBin() != null && rs.getStorageBin().getWarehouse() != null)
            ? rs.getStorageBin().getWarehouse().getIdentifier()
            : "");
    myMap.put("attributeSetValue",
        rs.getAttributeSetValue() != null && !rs.getAttributeSetValue().getId().equals("0")
            ? rs.getAttributeSetValue().getId()
            : null);
    myMap.put("attributeSetValue$_identifier",
        rs.getAttributeSetValue() != null && !rs.getAttributeSetValue().getId().equals("0")
            ? rs.getAttributeSetValue().getIdentifier()
            : "");
    myMap.put("purchaseOrderLine",
        rs.getSalesOrderLine() == null ? null : rs.getSalesOrderLine().getId());
    myMap.put("purchaseOrderLine$_identifier",
        rs.getSalesOrderLine() == null ? "" : rs.getSalesOrderLine().getIdentifier());
    myMap.put("availableQty",
        getQtyOnHand(reservation.getProduct(),
            rs.getStorageBin() != null ? rs.getStorageBin() : null,
            rs.getAttributeSetValue() != null ? rs.getAttributeSetValue() : null,
            rs.getSalesOrderLine() != null ? rs.getSalesOrderLine() : null));
    myMap.put("reservedinothers",
        rs.getSalesOrderLine() != null ? getQtyReserved(reservation, rs.getSalesOrderLine())
            : getQtyReserved(reservation, reservation.getProduct(), rs.getAttributeSetValue(),
                rs.getStorageBin()));
    myMap.put("quantity", rs.getQuantity());
    myMap.put("reservationQuantity", reservation.getQuantity());
    myMap.put("released", rs.getReleased());
    myMap.put("allocated", rs.isAllocated());
    myMap.put("inventoryStatus",
        (rs.getStorageBin() != null) ? rs.getStorageBin().getInventoryStatus().getId() : null);
    myMap.put("inventoryStatus$_identifier",
        (rs.getStorageBin() != null) ? rs.getStorageBin().getInventoryStatus().getIdentifier()
            : "");

    return myMap;

  }

  private List<Map<String, Object>> tomap(StorageDetail sd, boolean obSelected,
      List<Map<String, Object>> result, BigDecimal reservedinothers, Reservation reservation) {
    if (!storageDetailFiltered(result, sd)) {
      Map<String, Object> myMap = new HashMap<>();
      myMap.put("id", sd.getId());
      myMap.put("obSelected", obSelected);
      if (reservation.getRESStatus().equals("DR")) {
        ReservationStock rs = getPrereservedStock(reservation, sd.getStorageBin(),
            sd.getAttributeSetValue());
        if (rs != null) {
          myMap.put("obSelected", true);
        }
      }
      myMap.put("reservationStock", null);
      myMap.put("reservationStock$_identifier", "");
      myMap.put("storageBin", sd.getStorageBin() != null ? sd.getStorageBin().getId() : "");
      myMap.put("storageBin$_identifier",
          sd.getStorageBin() != null ? sd.getStorageBin().getIdentifier() : "");
      myMap.put("warehouse",
          (sd.getStorageBin().getWarehouse() != null) ? sd.getStorageBin().getWarehouse().getId()
              : null);
      myMap.put("warehouse$_identifier",
          (sd.getStorageBin().getWarehouse() != null)
              ? sd.getStorageBin().getWarehouse().getIdentifier()
              : "");
      myMap.put("attributeSetValue",
          sd.getAttributeSetValue() != null && !sd.getAttributeSetValue().getId().equals("0")
              ? sd.getAttributeSetValue().getId()
              : null);
      myMap.put("attributeSetValue$_identifier",
          sd.getAttributeSetValue() != null && !sd.getAttributeSetValue().getId().equals("0")
              ? sd.getAttributeSetValue().getIdentifier()
              : "");
      myMap.put("purchaseOrderLine", null);
      myMap.put("purchaseOrderLine$_identifier", "");

      myMap.put("availableQty", sd.getQuantityOnHand());
      myMap.put("reservedinothers", reservedinothers);
      myMap.put("reservationQuantity", reservation.getQuantity());
      myMap.put("quantity", BigDecimal.ZERO);
      if (reservation.getRESStatus().equals("DR")) {
        ReservationStock rs = getPrereservedStock(reservation, sd.getStorageBin(),
            sd.getAttributeSetValue());
        if (rs != null) {
          myMap.put("quantity", rs.getQuantity());
        }
      }
      myMap.put("released", BigDecimal.ZERO);
      myMap.put("allocated", sd.getStorageBin().getWarehouse().isAllocated());
      myMap.put("inventoryStatus", sd.getStorageBin().getInventoryStatus().getId());
      myMap.put("inventoryStatus$_identifier",
          sd.getStorageBin().getInventoryStatus().getIdentifier());

      result.add(myMap);
    }

    return result;

  }

  private boolean reservationStockFiltered(List<Map<String, Object>> result, ReservationStock rs) {
    boolean flag = false;
    for (Map<String, Object> myMap : result) {
      if (myMap.get("id").equals(rs.getId())) {
        flag = true;
        break;
      }
    }
    return flag;
  }

  private boolean storageDetailFiltered(List<Map<String, Object>> result, StorageDetail sd) {
    boolean flag = false;
    for (Map<String, Object> myMap : result) {
      if (myMap.get("id").equals(sd.getId())) {
        flag = true;
        break;
      }
    }
    return flag;
  }

  private boolean isInScope(String fieldName, String filterCriteria, BigDecimal amount) {
    if (amount == null) {
      return false;
    }
    try {
      if (filterCriteria.startsWith("[")) {
        JSONArray myJSON = new JSONArray(filterCriteria);
        if (myJSON.getJSONObject(0).getString("fieldName").equals(fieldName)) {
          return isInScope(fieldName, myJSON.getJSONObject(0).toString(), amount);
        }
      } else if (filterCriteria.startsWith("{")) {
        JSONObject myJSON = new JSONObject(filterCriteria);
        if (myJSON.getString("fieldName").equals(fieldName)) {
          try {
            BigDecimal value = new BigDecimal(myJSON.get("value").toString());
            if (myJSON.getString("operator").equals("equals")) {
              return amount.compareTo(value) == 0;
            } else if (myJSON.getString("operator").equals("greaterThan")) {
              return amount.compareTo(value) > 0;
            } else if (myJSON.getString("operator").equals("lessThan")) {
              return amount.compareTo(value) < 0;
            } else if (myJSON.getString("operator").equals("greaterOrEqual")) {
              return amount.compareTo(value) >= 0;
            } else if (myJSON.getString("operator").equals("lessOrEqual")) {
              return amount.compareTo(value) <= 0;
            }
          } catch (NumberFormatException e) {
            return false;
          }
        }

      } else {
        try {
          return amount.compareTo(new BigDecimal(filterCriteria)) == 0;
        } catch (NumberFormatException e) {
        }
      }
    } catch (JSONException e) {
      log4j.error("Error parsing criteria", e);
    }
    return true;
  }

  private boolean isInScope(String fieldName, String filterCriteria, boolean flag) {
    try {
      if (filterCriteria.startsWith("[")) {
        JSONArray myJSON = new JSONArray(filterCriteria);
        if (myJSON.getJSONObject(0).getString("fieldName").equals(fieldName)) {
          return isInScope(fieldName, myJSON.getJSONObject(0).toString(), flag);
        }
      } else if (filterCriteria.startsWith("{")) {
        JSONObject myJSON = new JSONObject(filterCriteria);
        if (myJSON.getString("fieldName").equals(fieldName)) {
          if (myJSON.getString("operator").equals("equals")) {
            return flag == myJSON.getBoolean("value");
          }
        }

      } else {
        return flag == "true".equals(filterCriteria);
      }
    } catch (JSONException e) {
      log4j.error("Error parsing criteria", e);
    }
    return true;
  }

  private BigDecimal getQtyOnHand(Product product, Locator storageBin,
      AttributeSetInstance attribute, OrderLine orderline) {
    if (orderline != null) {
      return getQtyOnHandFromOrderLine(orderline);
    }

    //@formatter:off
    String hql =
            "select sum(sd.quantityOnHand) from MaterialMgmtStorageDetail sd " +
            " where sd.product.id = :productId ";
    //@formatter:on
    if (storageBin != null) {
      //@formatter:off
      hql +=
            "   and sd.storageBin.id = :storageBinId ";
      //@formatter:on
    }
    if (attribute != null) {
      //@formatter:off
      hql +=
            "   and sd.attributeSetValue.id = :attributeSetValueId ";
      //@formatter:on
    }

    Query<BigDecimal> query = OBDal.getInstance()
        .getSession()
        .createQuery(hql, BigDecimal.class)
        .setParameter("productId", product.getId());

    if (storageBin != null) {
      query.setParameter("storageBinId", storageBin.getId());
    }
    if (attribute != null) {
      query.setParameter("attributeSetValueId", attribute.getId());
    }
    return query.uniqueResult();
  }

  private BigDecimal getQtyOnHandFromOrderLine(OrderLine orderline) {
    return orderline.getOrderedQuantity();
  }

  private BigDecimal getDeliveredQuantity(OrderLine orderline) {
    BigDecimal result = BigDecimal.ZERO;
    for (POInvoiceMatch match : orderline.getProcurementPOInvoiceMatchList()) {
      if (match.getGoodsShipmentLine() != null
          && "CO".equals(match.getGoodsShipmentLine().getShipmentReceipt().getDocumentStatus())) {
        result = result.add(match.getGoodsShipmentLine().getMovementQuantity());
      }
    }
    return result;
  }

  private BigDecimal getQtyReserved(Reservation reservation, OrderLine orderLine) {
    //@formatter:off
    String hql =
            "select coalesce(sum(rs.quantity - coalesce(rs.released,0)),0) from MaterialMgmtReservationStock rs " +
            "  join rs.reservation as r " +
            " where r.rESStatus not in ('CL', 'DR') " +
            "   and rs.salesOrderLine.id = :orderLineId " +
            "   and r.id <> :reservationId ";
    //@formatter:on

    return OBDal.getInstance()
        .getSession()
        .createQuery(hql, BigDecimal.class)
        .setParameter("orderLineId", orderLine.getId())
        .setParameter("reservationId", reservation.getId())
        .uniqueResult();
  }

  private BigDecimal getQtyReserved(Reservation reservation, Product product,
      AttributeSetInstance attribute, Locator storageBin) {
    //@formatter:off
    String hql =
            "select coalesce(sum(rs.quantity - coalesce(rs.released,0)),0) from MaterialMgmtReservationStock rs " +
            "  join rs.reservation as r " +
            " where r.rESStatus not in ('CL', 'DR') " +
            "   and r.product.id = :productId " +
            "   and r.id <> :reservationId " ;
    //@formatter:on
    if (attribute != null && !"0".equals(attribute.getId())) {
      //@formatter:off
      hql +=
            "   and rs.attributeSetValue.id = :attributeSetValueId ";
      //@formatter:on
    }
    if (storageBin != null && !"0".equals(storageBin.getId())) {
      //@formatter:off
      hql +=
            "   and rs.storageBin.id = :storageBinId ";
      //@formatter:on
    }
    Query<BigDecimal> query = OBDal.getInstance()
        .getSession()
        .createQuery(hql, BigDecimal.class)
        .setParameter("productId", product.getId())
        .setParameter("reservationId", reservation.getId());

    if (attribute != null && !"0".equals(attribute.getId())) {
      query.setParameter("attributeSetValueId", attribute.getId());
    }
    if (storageBin != null) {
      query.setParameter("storageBinId", storageBin.getId());
    }
    return query.uniqueResult();
  }

  private Map<String, String> buildCriteria(Map<String, String> parameters) {
    Map<String, String> filterCriteria = new HashMap<>();

    try {
      // Builds the criteria based on the fetch parameters
      JSONArray criterias = (JSONArray) JsonUtils.buildCriteria(parameters).get("criteria");
      for (int i = 0; i < criterias.length(); i++) {
        final JSONObject criteria1 = criterias.getJSONObject(i);
        if (criteria1.has("criteria") && criteria1.has("operator")) {
          JSONArray mySon = new JSONArray(criteria1.getString("criteria"));
          for (int j = 0; j < mySon.length(); j++) {
            final JSONObject criteria2 = mySon.getJSONObject(j);
            if (criteria2.has("criteria") && criteria2.has("operator")) {
              JSONArray mySonSon = new JSONArray(criteria2.getString("criteria"));
              for (int k = 0; k < mySonSon.length(); k++) {
                final JSONObject criteria3 = mySonSon.getJSONObject(k);
                if (criteria3.has("fieldName")) {
                  filterCriteria.put(criteria3.getString("fieldName"),
                      criteria3.has("value") ? criteria3.getString("value") : criteria3.toString());
                }
              }
            } else if (criteria2.has("fieldName")) {
              filterCriteria.put(criteria2.getString("fieldName"),
                  criteria2.has("value") ? criteria2.getString("value") : criteria2.toString());
            }
          }
        } else if (criteria1.has("fieldName")) {
          filterCriteria.put(criteria1.getString("fieldName"),
              criteria1.has("value") ? criteria1.getString("value") : criteria1.toString());
        }
      }
    } catch (JSONException e) {
      log4j.error("Error while building the criteria", e);
    }

    return filterCriteria;
  }

  @Override
  protected int getCount(Map<String, String> parameters) {
    // TODO Auto-generated method stub
    return 0;
  }
}
