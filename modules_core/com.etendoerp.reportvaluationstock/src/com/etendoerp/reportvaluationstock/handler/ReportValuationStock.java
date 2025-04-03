package com.etendoerp.reportvaluationstock.handler;

import net.sf.jasperreports.engine.JRDataSource;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.domaintype.DateDomainType;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.client.application.ApplicationConstants;
import org.openbravo.client.application.ReportDefinition;
import org.openbravo.client.application.report.BaseReportActionHandler;
import org.openbravo.client.kernel.RequestContext;

import org.openbravo.costing.AverageAlgorithm;
import org.openbravo.costing.CostingUtils;
import org.openbravo.costing.StandardAlgorithm;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.security.OrganizationStructureProvider;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.data.FieldProvider;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.DateTimeData;
import org.openbravo.erpCommon.utility.FieldProviderFactory;
import org.openbravo.erpCommon.utility.JRFieldProviderDataSource;
import org.openbravo.erpCommon.utility.OBDateUtils;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.common.enterprise.Locator;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.materialmgmt.cost.CostingAlgorithm;
import org.openbravo.model.materialmgmt.cost.CostingRule;
import org.openbravo.model.materialmgmt.transaction.MaterialTransaction;
import org.openbravo.service.db.DalConnectionProvider;

import javax.servlet.ServletException;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ReportValuationStock extends BaseReportActionHandler {
  private static final String PARAM_SUB_REPORT = "SUMMARY_DATASET";
  private static final String PARAM_FORMAT = "OUTPUT_FORMAT";
  private static final String REPORT_VALUATION_STOCK = "ReportValuationStock";
  private static final String PARAMS = "_params";
  private static final String DATE_FORMAT_JAVA = "dateFormat.java";
  private static final String DATE_FORMAT_SQL = "dateTimeFormat.sql";
  private static final String CATEGORY = "category";
  private static final Logger log4j = Logger.getLogger(ReportValuationStock.class);
  private static final String WARNING = "warning";
  private static final String TRX_WITH_NO_COST = "TrxWithNoCost";
  private static final String ID = ".id";
  private static final String POST_ACTION = "postAction";

  @Override
  protected ConnectionProvider getReportConnectionProvider() {
    return DalConnectionProvider.getReadOnlyConnectionProvider();
  }

  @Override
  protected void addAdditionalParameters(ReportDefinition process, JSONObject jsonContent,
      Map<String, Object> parameters) {
    VariablesSecureApp vars = RequestContext.get().getVariablesSecureApp();

    try {
      JSONObject params = jsonContent.getJSONObject(PARAMS);
      String strOrg = params.getString("AD_Org_ID");
      String strWarehouse = StringUtils.equals(params.getString("M_Warehouse_ID"), "null") ? null : params.getString(
          "M_Warehouse_ID");
      boolean isWarehouseConsolidation = params.getBoolean("WarehouseConsolidation");
      String strProductCategory = StringUtils.equals(params.getString("M_Product_Category_ID"),
          "null") ? null : params.getString("M_Product_Category_ID");
      String strCurrency = params.getString("C_Currency_ID");

      DateDomainType dateDomainType = new DateDomainType();
      Date dataParam = (Date) dateDomainType.createFromString(params.getString("Date"));
      String strDate = DateFormatUtils.format(dataParam,
          OBPropertiesProvider.getInstance().getOpenbravoProperties().getProperty(DATE_FORMAT_JAVA));

      buildData(vars, strDate, strOrg, strWarehouse, strProductCategory, strCurrency, isWarehouseConsolidation,
          parameters);

      parameters.put(PARAM_FORMAT, jsonContent.getString(ApplicationConstants.BUTTON_VALUE));

    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  @Override
  protected JRDataSource getReportData(Map<String, Object> parameters) {
    @SuppressWarnings("unchecked")
    HashMap<String, Object> jrParams = (HashMap<String, Object>) parameters
        .get(JASPER_REPORT_PARAMETERS);
    return (JRFieldProviderDataSource) jrParams.get(PARAM_SUB_REPORT);
  }

  protected void buildData(VariablesSecureApp vars, String strDate,
      String strOrganization, String strWarehouse, String strCategoryProduct, String strCurrencyId,
      boolean isWarehouseConsolidation, Map<String, Object> parameters) throws ServletException {
    ReportValuationStockData[] data;

    CostingAlgorithm costingAlgorithm = null;
    String processTime = null;
    // By default, Warehouse Dimension is never enabled
    String warehouseDimension = "N";
    String strCostType = null;
    Set<String> orgs = null;
    Set<String> warehouseIds;
    String strWarehouseIncluded = null;
    ConnectionProvider readOnlyCP = DalConnectionProvider.getReadOnlyConnectionProvider();
    try {
      Organization filterOrg = OBDal.getReadOnlyInstance().get(Organization.class, strOrganization);
      OrganizationStructureProvider osp = OBContext.getOBContext()
          .getOrganizationStructureProvider(OBContext.getOBContext().getCurrentClient().getId());
      Organization legalEntity = osp.getLegalEntity(filterOrg);
      String strLegalEntity;
      if (legalEntity == null) {
        throw new OBException(OBMessageUtils.messageBD("WarehouseNotInLE"));
      } else {
        strLegalEntity = legalEntity.getId();
      }
      orgs = osp.getNaturalTree(strLegalEntity);
      String orgIds = Utility.getInStrSet(orgs);

      CostingRule costRule = getLEsCostingAlgortithm(legalEntity);
      if (costRule != null) {
        costingAlgorithm = costRule.getCostingAlgorithm();
        processTime = OBDateUtils.formatDate(CostingUtils.getCostingRuleStartingDate(costRule),
            OBPropertiesProvider.getInstance().getOpenbravoProperties().getProperty(DATE_FORMAT_JAVA));
        warehouseDimension = costRule.isWarehouseDimension() ? "Y" : "N";
      }
      String compare = DateTimeData.compare(readOnlyCP, strDate, ReportValuationStockData
          .getCostingMigrationDate(readOnlyCP, legalEntity.getClient().getId()));
      String strCostOrg = "";
      String strCostClientId = "";
      if (StringUtils.equals(compare, "-1")) {
        // Date is before migration, available types are ST and AV. These costs are created at
        // client level and organization can be any that belong to the client.
        strCostType = "'AV', 'ST'";
        strCostClientId = OBContext.getOBContext().getCurrentClient().getId();
      } else if (costingAlgorithm != null) {
        strCostType = getCostType(costingAlgorithm);
        strCostOrg = strLegalEntity;
      }
      String strDateNext = DateTimeData.nDaysAfter(readOnlyCP, strDate, "1");
      String strMaxAggDate = ReportValuationStockData.selectMaxAggregatedDate(readOnlyCP,
          OBDal.getReadOnlyInstance().get(Organization.class, strLegalEntity).getClient().getId(),
          strDateNext, orgIds);
      if (StringUtils.isEmpty(strMaxAggDate)) {
        DateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
        Date maxAggDate = formatter.parse("01-01-0000");
        strMaxAggDate = OBDateUtils.formatDate(maxAggDate);
      }

      String dateFormat = OBPropertiesProvider.getInstance().getOpenbravoProperties().getProperty(DATE_FORMAT_SQL);
      warehouseIds = new HashSet<>();
      if (StringUtils.isEmpty(strWarehouse)) {
        warehouseIds.addAll(getWarehouses(vars.getClient(), strOrganization));
      } else {
        warehouseIds.add(strWarehouse);
      }
      if (!warehouseIds.isEmpty()) {
        strWarehouseIncluded = "(" + Utility.getInStrSet(warehouseIds) + ")";
      }
      data = getReportValuationStockData(vars, strDate, strCategoryProduct, strCurrencyId, isWarehouseConsolidation,
          processTime, warehouseDimension, strCostType, strWarehouseIncluded, readOnlyCP, filterOrg, strLegalEntity,
          orgIds, strCostOrg, strCostClientId, strDateNext, strMaxAggDate, dateFormat);

    } catch (Exception ex) {
      OBError myMessage = OBMessageUtils.translateError(ex.getMessage());
      vars.setMessage(REPORT_VALUATION_STOCK, myMessage);
      throw new ServletException(myMessage.getMessage());
    }

    if (data.length == 0) {
      OBError myMessage = new OBError();
      myMessage.setTitle(OBMessageUtils.messageBD("ProcessStatus-W"));
      myMessage.setMessage(OBMessageUtils.messageBD("NoDataFound"));
      myMessage.setType("WARNING");
      throw new ServletException(myMessage.getMessage());
    }

    boolean hasTrxWithNoCost = hasTrxWithNoCost(strDate, orgs, strWarehouse, strCategoryProduct);
    if (hasTrxWithNoCost) {
      OBError msg = new OBError();
      msg.setType(WARNING);
      msg.setMessage(OBMessageUtils.messageBD(TRX_WITH_NO_COST));
      parameters.put(POST_ACTION, msg);
    }
    printReport(vars, strDate, data, strCostType, costingAlgorithm, parameters);
  }

  private ReportValuationStockData[] getReportValuationStockData(VariablesSecureApp vars, String strDate,
      String strCategoryProduct, String strCurrencyId, boolean isWarehouseConsolidation, String processTime,
      String warehouseDimension, String strCostType, String strWarehouseIncluded, ConnectionProvider readOnlyCP,
      Organization filterOrg, String strLegalEntity, String orgIds, String strCostOrg, String strCostClientId,
      String strDateNext, String strMaxAggDate, String dateFormat) throws ServletException {
    ReportValuationStockData[] data;
    if (strCostType != null && !isWarehouseConsolidation) {
      data = ReportValuationStockData.select(readOnlyCP, vars.getLanguage(), strCurrencyId,
          strLegalEntity, strDate, strDateNext, strMaxAggDate, processTime, dateFormat, orgIds,
          strWarehouseIncluded, strCostOrg, strCostClientId, strCostType, warehouseDimension,
          strCategoryProduct);
    } else if (strCostType == null && !isWarehouseConsolidation) {
      data = ReportValuationStockData.selectWithoutCost(readOnlyCP, vars.getLanguage(),
          strCurrencyId, strLegalEntity, strDateNext, strMaxAggDate, processTime, dateFormat,
          orgIds, strWarehouseIncluded, strCategoryProduct);
    } else if (strCostType != null && isWarehouseConsolidation) {
      data = ReportValuationStockData.selectClusteredByWarehouse(readOnlyCP, vars.getLanguage(),
          strDate, strLegalEntity, strCurrencyId, strDateNext, strMaxAggDate, processTime,
          dateFormat, orgIds, strWarehouseIncluded, strCategoryProduct, strCostOrg,
          strCostClientId, strCostType, warehouseDimension, filterOrg.getId());
    } else {
      data = ReportValuationStockData.selectClusteredByWarehouseWithoutCost(readOnlyCP,
          vars.getLanguage(), strCurrencyId, strLegalEntity, strDateNext, strMaxAggDate,
          processTime, dateFormat, orgIds, strWarehouseIncluded, strCategoryProduct);
    }
    return data;
  }

  private void printReport(VariablesSecureApp vars, String strDate,
      ReportValuationStockData[] data, String strCostType, CostingAlgorithm costingAlgorithm,
      Map<String, Object> parameters) {
    log4j.debug("Output: XLS");

    String strCostHeader = "";
    String strValuationHeader = "";
    if (costingAlgorithm != null && strCostType != null) {
      Map<String, String> params = new HashMap<>();
      params.put("algorithm", costingAlgorithm.getName());
      strCostHeader = OBMessageUtils
          .parseTranslation(OBMessageUtils.messageBD("ValuedStockReport_CostHeader"), params);
      strValuationHeader = OBMessageUtils
          .parseTranslation(OBMessageUtils.messageBD("ValuedStockReport_ValuationHeader"), params);
    }
    parameters.put("ALG_COST", strCostHeader);
    parameters.put("SUM_ALG_COST", strValuationHeader);
    parameters.put("COSTFORMAT", Utility.getFormat(vars, "generalQtyExcel"));
    parameters.put("TITLE", "Valued Stock Report");
    parameters.put("DATE", strDate);

    FieldProvider[] dataSummary = getSummaryProductCategories(data);
    parameters.put(PARAM_SUB_REPORT, new JRFieldProviderDataSource(data, vars.getJavaDateFormat()));

    parameters.put("SummaryData", new JRFieldProviderDataSource(dataSummary, vars.getJavaDateFormat()));

    final DecimalFormatSymbols dfs = new DecimalFormatSymbols();
    dfs.setDecimalSeparator(vars.getSessionValue("#AD_ReportDecimalSeparator").charAt(0));
    dfs.setGroupingSeparator(vars.getSessionValue("#AD_ReportGroupingSeparator").charAt(0));
    final DecimalFormat numberFormat = new DecimalFormat(
        vars.getSessionValue("#AD_ReportNumberFormat"), dfs);
    parameters.put("NUMBERFORMAT", numberFormat);
  }

  public static CostingRule getLEsCostingAlgortithm(Organization legalEntity) {
    StringBuilder hql = new StringBuilder();
    hql.append(" as cosrule ");
    hql.append(" where cosrule.organization.id = :orgId ");
    hql.append(" and cosrule.validated = true ");
    hql.append(" order by startingDate desc ");

    return OBDal.getReadOnlyInstance()
        .createQuery(CostingRule.class, hql.toString())
        .setNamedParameter("orgId", legalEntity.getId())
        .setMaxResult(1)
        .uniqueResult();
  }

  private String getCostType(CostingAlgorithm costingAlgorithm) {
    Class<?> algorithm;
    String strCostType = null;
    try {
      algorithm = Class.forName(costingAlgorithm.getJavaClassName());
      if (AverageAlgorithm.class.isAssignableFrom(algorithm)) {
        strCostType = "'AVA'";
      } else if (StandardAlgorithm.class.isAssignableFrom(algorithm)) {
        strCostType = "'STA'";
      }
    } catch (ClassNotFoundException e) {
      // The class name defined in the costing rule is not found.
      return null;
    }
    return strCostType;
  }

  protected List<String> getWarehouses(String clientId, String orgId) {
    final OrganizationStructureProvider osp = OBContext.getOBContext()
        .getOrganizationStructureProvider(clientId);
    StringBuilder hql = new StringBuilder();
    hql.append("select e.id ");
    hql.append(" from Warehouse as e ");
    hql.append(" where e.organization.id in (:orgIds) ");
    hql.append(" and e.client.id = :clientId ");

    return OBDal.getReadOnlyInstance()
        .getSession()
        .createQuery(hql.toString(), String.class)
        .setParameterList("orgIds", osp.getNaturalTree(orgId))
        .setParameter("clientId", clientId)
        .list();
  }

  private FieldProvider[] getSummaryProductCategories(ReportValuationStockData[] data) {
    List<HashMap<String, String>> summaryProductCategories = new ArrayList<>();

    Arrays.stream(data).forEach(row -> {
      HashMap<String, String> summaryData = new HashMap<>();
      if (!containsSummaryData(summaryProductCategories, row.categoryName)) {
        summaryData.put(CATEGORY, row.categoryName);
        summaryData.put("cost", "0");
      } else {
        summaryData = getSummaryData(summaryProductCategories, row.categoryName);
      }
      if (StringUtils.isNotBlank(row.totalCost)) {
        BigDecimal cost = new BigDecimal(row.totalCost);
        BigDecimal totalCost = new BigDecimal(summaryData.get("cost"));
        summaryData.put("cost", (totalCost.add(cost)).toString());
      }
      if (!summaryProductCategories.contains(summaryData)) {
        summaryProductCategories.add(summaryData);
      }
    });
    return FieldProviderFactory.getFieldProviderArray(summaryProductCategories);
  }

  private boolean containsSummaryData(List<HashMap<String, String>> summaryProductCategories, String categoryName) {
    return summaryProductCategories.stream().anyMatch(
        summaryData -> StringUtils.equals(summaryData.get(CATEGORY), categoryName));
  }

  private HashMap<String, String> getSummaryData(List<HashMap<String, String>> summaryProductCategories,
      String categoryName) {
    return summaryProductCategories.stream().filter(
        summaryData -> StringUtils.equals(summaryData.get(CATEGORY), categoryName)).findFirst().orElse(null);
  }

  /**
   * Checks if there is at least one material transaction without calculated cost
   * before the given date, optionally filtered by warehouse and product category.
   *
   * @param strDate
   *     Base date (format: yyyy-MM-dd). Transactions before this date + 1 day are considered.
   * @param orgs
   *     Set of organization IDs to filter by.
   * @param strWarehouse
   *     (Optional) Warehouse ID to filter by.
   * @param strCategoryProduct
   *     (Optional) Product category ID to filter by.
   * @return {@code true} if at least one matching transaction exists; {@code false} otherwise.
   */
  protected boolean hasTrxWithNoCost(String strDate, Set<String> orgs, String strWarehouse, String strCategoryProduct) {
    try {
      final OBCriteria<MaterialTransaction> criteria = OBDal.getReadOnlyInstance().createCriteria(
          MaterialTransaction.class);
      criteria.setFilterOnReadableClients(false);
      criteria.setFilterOnReadableOrganization(false);

      try {
        ConnectionProvider readOnlyCP = DalConnectionProvider.getReadOnlyConnectionProvider();
        Date maxDate = OBDateUtils.getDate(DateTimeData.nDaysAfter(readOnlyCP, strDate, "1"));
        criteria.add(Restrictions.lt(MaterialTransaction.PROPERTY_MOVEMENTDATE, maxDate));
      } catch (Exception e) {
        log4j.error("Error parsing date: " + strDate, e);
      }

      criteria.add(Restrictions.eq(MaterialTransaction.PROPERTY_ISCOSTCALCULATED, false));
      criteria.add(Restrictions.in(MaterialTransaction.PROPERTY_ORGANIZATION + ID, orgs));
      criteria.createAlias(MaterialTransaction.PROPERTY_PRODUCT, "p");
      criteria.add(Restrictions.eq("p." + Product.PROPERTY_STOCKED, true));

      if (StringUtils.isNotBlank(strWarehouse)) {
        criteria.createAlias(MaterialTransaction.PROPERTY_STORAGEBIN, "loc");
        criteria.add(Restrictions.eq("loc." + Locator.PROPERTY_WAREHOUSE + ID, strWarehouse));
      }

      if (StringUtils.isNotBlank(strCategoryProduct)) {
        criteria.add(Restrictions.eq("p." + Product.PROPERTY_PRODUCTCATEGORY + ID, strCategoryProduct));
      }

      criteria.setMaxResults(1);
      return criteria.uniqueResult() != null;
    } catch (Exception e) {
      log4j.error("Error in hasTrxWithNoCost", e);
      return false;
    }
  }
}
