/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.0  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2013-2021 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */
package org.openbravo.materialmgmt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.exception.OBSecurityException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.reference.UIDefinition;
import org.openbravo.client.kernel.reference.UIDefinitionController;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.model.ad.domain.Reference;
import org.openbravo.model.common.plm.Characteristic;
import org.openbravo.model.common.plm.CharacteristicValue;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.plm.ProductCharacteristic;
import org.openbravo.model.common.plm.ProductCharacteristicConf;
import org.openbravo.service.datasource.DataSourceProperty;
import org.openbravo.service.datasource.ReadOnlyDataSourceService;
import org.openbravo.service.json.JsonUtils;

public class ManageVariantsDS extends ReadOnlyDataSourceService {

  private static final Logger log = LogManager.getLogger();
  private static final int SEARCH_KEY_LENGTH = getSearchKeyColumnLength();
  private static final String MANAGE_VARIANTS_TABLE_ID = "147D4D709FAC4AF0B611ABFED328FA12";
  private static final String ID_REFERENCE_ID = "13";

  @Override
  public void checkFetchDatasourceAccess(Map<String, String> parameter) {
    final OBContext obContext = OBContext.getOBContext();
    Entity entityManageVariants = ModelProvider.getInstance()
        .getEntityByTableId(MANAGE_VARIANTS_TABLE_ID);
    try {
      obContext.getEntityAccessChecker().checkReadableAccess(entityManageVariants);
    } catch (OBSecurityException e) {
      handleExceptionUnsecuredDSAccess(e);
    }
  }

  @Override
  protected int getCount(Map<String, String> parameters) {
    return getData(parameters, 0, Integer.MAX_VALUE).size();
  }

  @Override
  protected List<Map<String, Object>> getData(Map<String, String> parameters, int startRow,
      int endRow) {
    OBContext.setAdminMode(true);
    final List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
    try {
      ProductChSelectedFilters selectedFilters = readCriteria(parameters);
      final String strProductId = parameters.get("@Product.id@");
      final Product product = OBDal.getInstance().get(Product.class, strProductId);

      int totalMaxLength = product.getSearchKey().length();
      long variantNumber = 1;
      Map<ProductCharacteristic, ProductCharacteristicAux> prChUseCode = new HashMap<ProductCharacteristic, ProductCharacteristicAux>();

      OBCriteria<ProductCharacteristic> prChCrit = OBDal.getInstance()
          .createCriteria(ProductCharacteristic.class);
      prChCrit.add(Restrictions.eq(ProductCharacteristic.PROPERTY_PRODUCT, product));
      prChCrit.add(Restrictions.eq(ProductCharacteristic.PROPERTY_VARIANT, true));
      prChCrit.addOrderBy(ProductCharacteristic.PROPERTY_SEQUENCENUMBER, true);
      List<ProductCharacteristic> prChs = prChCrit.list();
      int chNumber = prChs.size();
      if (chNumber == 0) {
        return result;
      }
      ProductCharacteristicConf[] currentValues = new ProductCharacteristicConf[chNumber];
      boolean includeInResult = true;

      int i = 0;
      for (ProductCharacteristic prCh : prChs) {
        OBCriteria<ProductCharacteristicConf> prChConfCrit = OBDal.getInstance()
            .createCriteria(ProductCharacteristicConf.class);
        prChConfCrit
            .add(Restrictions.eq(ProductCharacteristicConf.PROPERTY_CHARACTERISTICOFPRODUCT, prCh));
        List<ProductCharacteristicConf> prChConfs = prChConfCrit.list();
        long valuesCount = prChConfs.size();

        boolean useCode = true;
        int maxLength = 0;
        for (ProductCharacteristicConf prChConf : prChConfs) {
          if (StringUtils.isBlank(prChConf.getCode())) {
            useCode = false;
            break;
          }
          if (prChConf.getCode().length() > maxLength) {
            maxLength = prChConf.getCode().length();
          }
        }

        variantNumber = variantNumber * valuesCount;
        if (useCode) {
          totalMaxLength += maxLength;
        }
        List<CharacteristicValue> filteredValues = selectedFilters.getSelectedChValues()
            .get(prCh.getCharacteristic().getId());
        ProductCharacteristicAux prChAux = new ProductCharacteristicAux(useCode, prChConfs,
            filteredValues);
        currentValues[i] = prChAux.getNextValue();
        if (filteredValues != null) {
          includeInResult = includeInResult
              && filteredValues.contains(currentValues[i].getCharacteristicValue());
        }

        prChUseCode.put(prCh, prChAux);
        i++;
      }

      throwExceptionIfVariantNumberIsTooHigh(variantNumber);

      totalMaxLength += Long.toString(variantNumber).length();
      boolean useCodes = totalMaxLength <= SEARCH_KEY_LENGTH;

      boolean hasNext = true;
      int productNo = 0;
      do {
        // reset boolean value.
        includeInResult = true;
        // Create variant product
        Map<String, Object> variantMap = new HashMap<String, Object>();
        variantMap.put("Client", product.getClient());
        variantMap.put("Organization", product.getOrganization());
        variantMap.put("Active", "Y");
        variantMap.put("creationDate", new Date());
        variantMap.put("createdBy", OBContext.getOBContext().getUser());
        variantMap.put("updated", new Date());
        variantMap.put("updatedBy", OBContext.getOBContext().getUser());
        variantMap.put("name", product.getName());
        variantMap.put("variantCreated", false);
        variantMap.put("obSelected", false);

        StringBuilder searchKey = new StringBuilder().append(product.getSearchKey());
        for (i = 0; i < chNumber; i++) {
          ProductCharacteristicConf prChConf = currentValues[i];
          ProductCharacteristicAux prChConfAux = prChUseCode.get(prChs.get(i));
          List<CharacteristicValue> filteredValues = prChConfAux.getFilteredValues();
          if (filteredValues != null) {
            includeInResult = includeInResult
                && filteredValues.contains(currentValues[i].getCharacteristicValue());
          }

          if (useCodes && prChConfAux.isUseCode() && prChConf != null
              && StringUtils.isNotBlank(prChConf.getCode())) {
            searchKey.append("_" + prChConf.getCode() + "_");
          }
        }
        for (int j = 0; j < (Long.toString(variantNumber).length()
            - Integer.toString(productNo).length()); j++) {
          searchKey.append("0");
        }
        searchKey.append(productNo);
        variantMap.put("searchKey", searchKey);
        //@formatter:off
        String hql = " as p "
                   + " where p.genericProduct.id = :productId ";
        //@formatter:on

        StringBuilder strChDesc = new StringBuilder();
        StringBuilder strKeyId = new StringBuilder();
        JSONArray valuesArray = new JSONArray();
        for (i = 0; i < chNumber; i++) {
          ProductCharacteristicConf prChConf = currentValues[i];
          Characteristic characteristic = prChConf.getCharacteristicOfProduct().getCharacteristic();
          hql += buildExistsClause(i);
          if (StringUtils.isNotBlank(strChDesc.toString())) {
            strChDesc.append(", ");
          }
          strChDesc.append(characteristic.getName() + ":");
          strChDesc.append(" " + prChConf.getCharacteristicValue().getName());
          strKeyId.append(prChConf.getCharacteristicValue().getId());
          JSONObject value = new JSONObject();
          value.put("characteristic", characteristic.getId());
          value.put("characteristicValue", prChConf.getCharacteristicValue().getId());
          value.put("characteristicConf", prChConf.getId());
          valuesArray.put(value);
        }
        variantMap.put("characteristicArray", valuesArray);
        variantMap.put("characteristicDescription", strChDesc);
        variantMap.put("id", strKeyId);

        OBQuery<Product> variantQry = OBDal.getInstance()
            .createQuery(Product.class, hql)
            .setNamedParameter("productId", product.getId());
        for (i = 0; i < chNumber; i++) {
          ProductCharacteristicConf prChConf = currentValues[i];
          Characteristic characteristic = prChConf.getCharacteristicOfProduct().getCharacteristic();
          variantQry.setNamedParameter("ch" + i, characteristic.getId())
              .setNamedParameter("chvalue" + i, prChConf.getCharacteristicValue().getId());
        }
        Product existingProduct = variantQry.uniqueResult();
        if (existingProduct != null) {
          variantMap.put("name", existingProduct.getName());
          variantMap.put("searchKey", existingProduct.getSearchKey());
          variantMap.put("variantCreated", true);
          variantMap.put("variantId", existingProduct.getId());
          variantMap.put("id", existingProduct.getId());
        }
        if (StringUtils.isNotEmpty(selectedFilters.getSearchKey())) {
          StringBuilder matchedRegex = new StringBuilder(".*");
          matchedRegex
              .append(selectedFilters.getSearchKey().trim().toLowerCase().replace(" ", ".*"));
          matchedRegex.append(".*");
          includeInResult = includeInResult && ((String) variantMap.get("searchKey")).trim()
              .toLowerCase()
              .matches(matchedRegex.toString());
        }
        if (StringUtils.isNotEmpty(selectedFilters.getName())) {
          StringBuilder matchedRegex = new StringBuilder(".*");
          matchedRegex.append(selectedFilters.getName().trim().toLowerCase().replace(" ", ".*"));
          matchedRegex.append(".*");
          includeInResult = includeInResult && ((String) variantMap.get("name")).trim()
              .toLowerCase()
              .matches(matchedRegex.toString());
        }
        if (selectedFilters.isVariantCreated() != null) {
          includeInResult = includeInResult
              && selectedFilters.isVariantCreated() == (existingProduct != null);
        }
        if (!selectedFilters.getSelectedIds().isEmpty()) {
          includeInResult = includeInResult
              || selectedFilters.getSelectedIds().contains(variantMap.get("id"));
        }

        if (includeInResult) {
          result.add(variantMap);
        }

        for (i = 0; i < chNumber; i++) {
          ProductCharacteristicAux prChConfAux = prChUseCode.get(prChs.get(i));
          currentValues[i] = prChConfAux.getNextValue();
          if (!prChConfAux.isIteratorReset()) {
            break;
          } else if (i + 1 == chNumber) {
            hasNext = false;
          }
        }
        productNo++;
      } while (hasNext);
      String strSortBy = parameters.get("_sortBy");
      if (strSortBy == null) {
        strSortBy = "characteristicDescription";
      }
      boolean ascending = true;
      if (strSortBy.startsWith("-")) {
        ascending = false;
        strSortBy = strSortBy.substring(1);
      }

      Collections.sort(result, new ResultComparator(strSortBy, ascending));

    } catch (JSONException e) {
      log.error("Error while managing the variant characteristics", e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return result;
  }

  private void throwExceptionIfVariantNumberIsTooHigh(long variantNumber) {
    if (variantNumber > getManageVariantsLimitPrefValue()) {
      throw new OBException("ManageVariantsLimitReached");
    }
  }

  private int getManageVariantsLimitPrefValue() {
    try {
      OBContext context = OBContext.getOBContext();
      return Integer.parseInt(
          Preferences.getPreferenceValue("ManageVariantsLimit", false, context.getCurrentClient(),
              context.getCurrentOrganization(), context.getUser(), context.getRole(), null));
    } catch (Exception e) {
      return 1000;
    }
  }

  private ProductChSelectedFilters readCriteria(Map<String, String> parameters)
      throws JSONException {
    ProductChSelectedFilters selectedFilters = new ProductChSelectedFilters();
    JSONArray criteriaArray = (JSONArray) JsonUtils.buildCriteria(parameters).get("criteria");

    // special case: only column filter on a product characteristics field, and selecting several
    // product characteristics. In that case the inner criteria is selected, otherwise only the
    // first product characteristic would be used in the filter
    // see issue: https://issues.openbravo.com/view.php?id=41900
    if (criteriaArray.length() == 1) {
      JSONObject criteria = criteriaArray.getJSONObject(0);
      if (criteria.has("isProductCharacteristicsCriteria")
          && criteria.getBoolean("isProductCharacteristicsCriteria") && criteria.has("criteria")) {
        criteriaArray = criteria.getJSONArray("criteria");
      }
    }

    for (int i = 0; i < criteriaArray.length(); i++) {
      String value = "";
      JSONObject criteria = criteriaArray.getJSONObject(i);
      // Basic advanced criteria handling
      if (criteria.has("_constructor")
          && "AdvancedCriteria".equals(criteria.getString("_constructor"))
          && criteria.has("criteria")) {
        JSONArray innerCriteriaArray = new JSONArray(criteria.getString("criteria"));
        criteria = innerCriteriaArray.getJSONObject(0);
      }
      String fieldName = criteria.getString("fieldName");
      String operatorName = criteria.getString("operator");
      if (criteria.has("value")) {
        value = criteria.getString("value");
      }
      if (fieldName.equals("id") && operatorName.equals("notNull")) {
        // In the case of having the criteria
        // "fieldName":"id","operator":"notNull" don't do anything.
        // This case is the one which should return every record.
        continue;
      }
      if (fieldName.equals("name")) {
        selectedFilters.setName(value);
      } else if (fieldName.equals("searchKey")) {
        selectedFilters.setSearchKey(value);
      } else if (fieldName.equals("id")) {
        selectedFilters.addSelectedID(value);
      } else if (fieldName.equals("variantCreated")) {
        selectedFilters.setIsVariantCreated(criteria.getBoolean("value"));
      } else if (fieldName.equals("characteristicDescription")) {
        JSONArray values = new JSONArray(value);
        // All values belong to the same characteristicId, get the first one.
        String strCharacteristicId = null;
        List<CharacteristicValue> chValueIds = new ArrayList<CharacteristicValue>();
        for (int j = 0; j < values.length(); j++) {
          CharacteristicValue chValue = OBDal.getInstance()
              .get(CharacteristicValue.class, values.getString(j));
          chValueIds.add(chValue);
          if (strCharacteristicId == null) {
            strCharacteristicId = chValue.getCharacteristic().getId();
          }
        }
        selectedFilters.addSelectedChValues(strCharacteristicId, chValueIds);
      }

    }
    return selectedFilters;
  }

  private String buildExistsClause(int i) {
    //@formatter:off
    return " and exists (select 1 "
        + "             from ProductCharacteristicValue as pcv "
        + "             where pcv.product.id = p.id "
        + "             and pcv.characteristic.id = :ch" + i
        + "             and pcv.characteristicValue.id = :chvalue" + i + ") ";
    //@formatter:on
  }

  private static int getSearchKeyColumnLength() {
    final Entity prodEntity = ModelProvider.getInstance().getEntity(Product.ENTITY_NAME);

    final Property searchKeyProperty = prodEntity.getProperty(Product.PROPERTY_SEARCHKEY);
    return searchKeyProperty.getFieldLength();
  }

  private static class ResultComparator implements Comparator<Map<String, Object>> {
    private String sortByField;
    private boolean ascending;

    public ResultComparator(String sortbyfield, boolean isascending) {
      sortByField = sortbyfield;
      ascending = isascending;
    }

    @Override
    public int compare(Map<String, Object> map1, Map<String, Object> map2) {
      boolean sortByChanged = false;
      if ("variantCreated".equals(sortByField)) {
        boolean o1 = (boolean) map1.get(sortByField);
        boolean o2 = (boolean) map2.get(sortByField);
        if (o1 == o2) {
          sortByField = "characteristicDescription";
          sortByChanged = true;
        } else if (ascending) {
          return o1 ? -1 : 1;
        } else {
          return o2 ? -1 : 1;
        }
      }
      // previous if might have changed the value of sortByField
      if (!"variantCreated".equals(sortByField)) {
        String str1 = map1.get(sortByField).toString();
        String str2 = map2.get(sortByField).toString();
        if (sortByChanged) {
          sortByField = "variantCreated";
        }
        if (ascending) {
          return str1.compareTo(str2);
        } else {
          return str2.compareTo(str1);
        }
      }
      // returning 0 but should never reach this point.
      return 0;
    }
  }

  private class ProductCharacteristicAux {
    private boolean useCode;
    private boolean isIteratorReset;
    private List<ProductCharacteristicConf> values;
    private List<CharacteristicValue> filteredValues;
    private Iterator<ProductCharacteristicConf> iterator;

    ProductCharacteristicAux(boolean usecode, List<ProductCharacteristicConf> valueslist,
        List<CharacteristicValue> filteredvalues) {
      useCode = usecode;
      values = valueslist;
      filteredValues = filteredvalues;
    }

    public boolean isUseCode() {
      return useCode;
    }

    public boolean isIteratorReset() {
      return isIteratorReset;
    }

    public List<CharacteristicValue> getFilteredValues() {
      return filteredValues;
    }

    public ProductCharacteristicConf getNextValue() {
      ProductCharacteristicConf prChConf;
      if (iterator == null || !iterator.hasNext()) {
        iterator = values.iterator();
        isIteratorReset = true;
      } else {
        isIteratorReset = false;
      }
      prChConf = iterator.next();
      return prChConf;
    }
  }

  /**
   * Private class that groups all the values of the filters introduced by the user
   */
  private class ProductChSelectedFilters {
    private List<String> selectedIds;
    private HashMap<String, List<CharacteristicValue>> selectedChValues;
    private String name;
    private String searchKey;
    private Boolean isVariantCreated;

    ProductChSelectedFilters() {
      selectedIds = new ArrayList<String>();
      selectedChValues = new HashMap<String, List<CharacteristicValue>>();
      name = null;
      searchKey = null;
      isVariantCreated = null;
    }

    public void addSelectedID(String id) {
      selectedIds.add(id);
    }

    public void addSelectedChValues(String characteristicId, List<CharacteristicValue> values) {
      selectedChValues.put(characteristicId, values);
    }

    public void setName(String nameFilterParam) {
      name = nameFilterParam;
    }

    public void setSearchKey(String searchKeyFilterParam) {
      searchKey = searchKeyFilterParam;
    }

    public void setIsVariantCreated(boolean variantCreatedParam) {
      isVariantCreated = variantCreatedParam;
    }

    public List<String> getSelectedIds() {
      return selectedIds;
    }

    public Map<String, List<CharacteristicValue>> getSelectedChValues() {
      return selectedChValues;
    }

    public String getName() {
      return name;
    }

    public String getSearchKey() {
      return searchKey;
    }

    public Boolean isVariantCreated() {
      return isVariantCreated;
    }

  }

  @Override
  public List<DataSourceProperty> getDataSourceProperties(Map<String, Object> parameters) {
    List<DataSourceProperty> dataSourceProperties = new ArrayList<DataSourceProperty>();
    final DataSourceProperty dsProperty = new DataSourceProperty();
    Reference idReference = OBDal.getInstance().get(Reference.class, ID_REFERENCE_ID);
    UIDefinition uiDefinition = UIDefinitionController.getInstance().getUIDefinition(idReference);
    dsProperty.setId(true);
    dsProperty.setName("id");
    dsProperty.setUIDefinition(uiDefinition);
    dataSourceProperties.add(dsProperty);
    return dataSourceProperties;
  }

}
