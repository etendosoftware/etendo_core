package org.openbravo.test.stockValuationReport;

import static org.openbravo.test.base.OBBaseTest.*;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.enterprise.OrgWarehouse;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.geography.Location;
import org.openbravo.test.base.OBBaseTest;

public class ReportValuationStockTestUtil {

  public static final String FB_US_WEST_COAST_ORG_ID = "BAE22373FEBE4CCCA24517E23F0C8A48";
  public static final String LOCATION = "03125C49897943BC9215F894EB74059A";

  private ReportValuationStockTestUtil(){
    throw new UnsupportedOperationException("This class should not be instantiated");
  }

  /**
   * Creates a test warehouse with the specified name and organization.
   *
   * @param name
   *     the name of the warehouse
   * @param orgId
   *     the ID of the organization to which the warehouse will be associated
   * @return the created {@link Warehouse} object
   */
  public static Warehouse createTestWarehouse(String name, String orgId) {
    Warehouse warehouse = OBProvider.getInstance().get(Warehouse.class);
    warehouse.setName(name);
    Organization organization = OBDal.getInstance().get(Organization.class, orgId);
    warehouse.setOrganization(organization);
    warehouse.setSearchKey("WarehouseTest");
    Location location = getLocation(LOCATION);
    warehouse.setLocationAddress(location);
    OBDal.getInstance().save(warehouse);
    OBDal.getInstance().flush();
    return warehouse;
  }

  /**
   * Deletes a test warehouse by its ID.
   *
   * @param warehouseId
   *     the ID of the warehouse to be deleted
   */
  public static void deleteTestWarehouse(String warehouseId) {
    try {
      OBContext.setAdminMode();
      Warehouse warehouse = OBDal.getInstance().get(Warehouse.class, warehouseId);
      if (warehouse != null) {
        OBDal.getInstance().remove(warehouse);
        OBDal.getInstance().flush();
        OBDal.getInstance().commitAndClose();
      }
    } catch (Exception e) {
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }


  /**
   * Retrieves an {@link Organization} by its ID.
   *
   * @param orgId
   *     the ID of the organization
   * @return the {@link Organization} object associated with the given ID
   */
  public static Organization getOrganization(String orgId) {
    return OBDal.getInstance().get(Organization.class, orgId);
  }

  /**
   * Retrieves a {@link Location} by its ID.
   *
   * @param locId
   *     the ID of the location
   * @return the {@link Location} object associated with the given ID
   */
  public static Location getLocation(String locId) {
    return OBDal.getInstance().get(Location.class, locId);
  }


  /**
   * Assigns a warehouse to an organization by creating a link between them.
   *
   * @param warehouse
   *     the warehouse to assign
   * @param organization
   *     the organization to which the warehouse will be assigned
   */
  public static void assignWarehouseToOrganization(Warehouse warehouse, Organization organization) {
    OrgWarehouse orgWarehouse = OBProvider.getInstance().get(OrgWarehouse.class);
    orgWarehouse.setOrganization(organization);
    orgWarehouse.setWarehouse(warehouse);
    OBDal.getInstance().save(orgWarehouse);
    OBDal.getInstance().flush();
  }

  /**
   * Builds a JSON object with parameters for a stock valuation report.
   *
   * @param orgId
   *     the ID of the organization
   * @param warehouseId
   *     the ID of the warehouse
   * @param date
   *     the date for the report in the format "yyyy-MM-dd"
   * @param currencyId
   *     the ID of the currency to be used in the report
   * @return a {@link JSONObject} containing the parameters for the stock valuation report
   * @throws JSONException
   *     if an error occurs while constructing the JSON object
   */
  public static JSONObject buildJsonObject(String orgId, String warehouseId, String date,
      String currencyId) throws JSONException {
    JSONObject jsonObject = new JSONObject();

    jsonObject.put("_buttonValue", "HTML");

    JSONObject params = new JSONObject();
    params.put("AD_Org_ID", orgId);
    params.put("M_Warehouse_ID", warehouseId);
    params.put("Date", date);
    params.put("WarehouseConsolidation", false);
    params.put("M_Product_Category_ID", JSONObject.NULL);
    params.put("C_Currency_ID", currencyId);
    jsonObject.put("_params", params);

    return jsonObject;
  }

}
