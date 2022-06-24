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
 * All portions are Copyright (C) 2013-2021 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.client.kernel.reference;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.data.Sqlc;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.PropertyNotFoundException;
import org.openbravo.model.ad.ui.Field;
import org.openbravo.model.ad.utility.Tree;
import org.openbravo.model.ad.utility.TreeNode;
import org.openbravo.model.common.plm.CharacteristicValue;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.plm.ProductCharacteristicValue;

public class CharacteristicsUIDefinition extends TextUIDefinition {

  private static final Logger log4j = LogManager.getLogger();

  @Override
  public String getFormEditorType() {
    return "OBCharacteristicsItem";
  }

  @Override
  public String getFilterEditorType() {
    return "OBCharacteristicsFilterItem";
  }

  @Override
  public String getGridEditorType() {
    return "OBCharacteristicsGridItem";
  }

  @Override
  public String getReadOnlyEditorType() {
    return "OBCharacteristicsGridItem";
  }

  @Override
  public String getFieldProperties(Field field, boolean getValueFromSession) {
    String result = super.getFieldProperties(field, getValueFromSession);
    OBContext.setAdminMode(true);
    try {
      JSONObject jsnobject = new JSONObject(result);

      RequestContext rq = RequestContext.get();
      String fieldId;
      if (field.getProperty() != null && !field.getProperty().isEmpty()) {
        fieldId = "inp" + "_propertyField_"
            + Sqlc.TransformaNombreColumna(field.getName()).replace(" ", "") + "_"
            + field.getColumn().getDBColumnName();
      } else {
        fieldId = "inp" + Sqlc.TransformaNombreColumna(field.getColumn().getDBColumnName());
      }
      String columnValue = rq.getRequestParameter(fieldId);
      if (StringUtils.isEmpty(columnValue)) {
        return result;
      }
      String productId = rq.getRequestParameter("inpmProductId");
      Product product = null;
      if (!StringUtils.isEmpty(productId)) {
        product = OBDal.getInstance().get(Product.class, productId);
      }
      if (product == null) {
        return result;
      }

      JSONObject value = new JSONObject();
      value.put("dbValue", columnValue);

      JSONObject jsonValue = new JSONObject();
      for (ProductCharacteristicValue charValue : getProductCharacteristicValueListOrderedByCharacteristicType(
          product)) {
        jsonValue.put(charValue.getCharacteristic().getIdentifier(),
            getValue(charValue.getCharacteristicValue()));
      }

      value.put("characteristics", jsonValue);

      jsnobject.put("value", value);
      jsnobject.put("classicValue", columnValue);
      return jsnobject.toString();
    } catch (JSONException e) {
      throw new OBException("Exception when parsing date ", e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private List<ProductCharacteristicValue> getProductCharacteristicValueListOrderedByCharacteristicType(
      final Product product) {
    //@formatter:off
    final String hql = 
        "  as pcv "
        + "where pcv.product.id = :productId "
        + "order by pcv.characteristic.characteristicType ";
    //@formatter:on

    return OBDal.getInstance()
        .createQuery(ProductCharacteristicValue.class, hql)
        .setNamedParameter("productId", product.getId())
        .list();
  }

  @Override
  public String getFilterEditorPropertiesProperty(Field field) {
    String append = "";
    Boolean showFkDropdownUnfiltered = (Boolean) readGridConfigurationSetting(
        "showFkDropdownUnfiltered");
    if (Boolean.TRUE.equals(showFkDropdownUnfiltered)) {
      append = append + ", showFkDropdownUnfiltered: " + showFkDropdownUnfiltered.toString();
    }
    return super.getFilterEditorPropertiesProperty(field) + append;
  }

  private String getValue(CharacteristicValue characValue) {
    int levels = 0;

    try {
      String levelsPreference = Preferences.getPreferenceValue("ShowProductCharacteristicsParents",
          true, OBContext.getOBContext().getCurrentClient(),
          OBContext.getOBContext().getCurrentOrganization(), OBContext.getOBContext().getUser(),
          OBContext.getOBContext().getRole(), null);
      levels = Integer.parseInt(levelsPreference);
    } catch (PropertyNotFoundException ignore) {
    } catch (Exception e) {
      if (e instanceof NumberFormatException) {
        log4j.error("ShowProductCharacteristicsParents preference is not a number", e);
      } else {
        log4j.error("Error finding ShowProductCharacteristicsParents preference", e);
      }
    }

    String value = characValue.getIdentifier();
    if (levels > 0) {
      CharacteristicValue currentCharValue = characValue;
      int i = 0;
      boolean existsParent = true;

      while (i < levels && existsParent) {
        existsParent = false;
        // Find parent
        OBCriteria<TreeNode> treeNodeCri = OBDal.getInstance().createCriteria(TreeNode.class);
        treeNodeCri.createAlias(TreeNode.PROPERTY_TREE, "tree");
        treeNodeCri.add(Restrictions.eq("tree." + Tree.PROPERTY_CLIENT,
            OBContext.getOBContext().getCurrentClient()));
        treeNodeCri.add(Restrictions.eq("tree." + Tree.PROPERTY_TYPEAREA, "CH"));
        treeNodeCri.add(Restrictions.eq(TreeNode.PROPERTY_NODE, currentCharValue.getId()));
        List<TreeNode> treeNodeList = treeNodeCri.list();
        if (treeNodeList.size() == 1 && !treeNodeList.get(0).getReportSet().equals("0")) {
          // Parent Exists
          currentCharValue = OBDal.getInstance()
              .get(CharacteristicValue.class, treeNodeList.get(0).getReportSet());
          value = currentCharValue.getIdentifier() + " / " + value;
          existsParent = true;
        }
        i++;
      }

    }

    return value;
  }

}
