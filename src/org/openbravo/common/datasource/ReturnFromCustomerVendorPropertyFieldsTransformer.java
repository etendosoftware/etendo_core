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
 * All portions are Copyright (C) 2016 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */

package org.openbravo.common.datasource;

import java.util.List;
import java.util.Map;

import org.hibernate.criterion.Restrictions;
import org.openbravo.client.kernel.ComponentProvider;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.ui.Field;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.service.datasource.HQLDataSourceService;
import org.openbravo.service.json.JsonConstants;

/**
 * Transformer to enable Property Fields in Return to Customer and Return to Vendor P&amp;E grids
 */
@ComponentProvider.Qualifier("CDB9DC9655F24DF8AB41AA0ADBD04390")
public class ReturnFromCustomerVendorPropertyFieldsTransformer
    extends ReturnToFromCustomerVendorHQLTransformer {
  private static final String COMMA = ",";

  @Override
  public int getPriority(Map<String, String> parameters) {
    return 90;
  }

  @Override
  public String transformHqlQuery(String hqlQuery, Map<String, String> requestParameters,
      Map<String, Object> queryNamedParameters) {
    String modifiedQuery = hqlQuery;
    if (requestParameters.containsKey(JsonConstants.ADDITIONAL_PROPERTIES_PARAMETER)) {
      String extraProperties = requestParameters.get(JsonConstants.ADDITIONAL_PROPERTIES_PARAMETER);
      String[] properties = extraProperties.split(COMMA);
      if (properties.length > 0) {
        String tabId = requestParameters.get(JsonConstants.TAB_PARAMETER);
        Tab currentTab = OBDal.getInstance().getProxy(Tab.class, tabId);
        OBCriteria<Field> obCriteria = OBDal.getInstance().createCriteria(Field.class);
        obCriteria.add(Restrictions.eq(Field.PROPERTY_TAB, currentTab));
        obCriteria.add(Restrictions.isNotNull(Field.PROPERTY_PROPERTY));
        List<Field> propertyFieldList = obCriteria.list();

        for (Field field : propertyFieldList) {
          String propertyName = field.getProperty();
          if (extraProperties.contains(propertyName)) {
            modifiedQuery = modifiedQuery.replace("MAINFROM", ", iol." + propertyName + " as "
                + propertyName.replace(DalUtil.DOT, HQLDataSourceService.PROPERTY_FIELD_SEPARATOR)
                + " MAINFROM");
          }
        }
      }
    }
    return super.transformHqlQuery(modifiedQuery, requestParameters, queryNamedParameters);
  }
}
