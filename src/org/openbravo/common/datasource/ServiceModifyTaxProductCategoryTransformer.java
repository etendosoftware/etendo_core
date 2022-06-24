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
 * All portions are Copyright (C) 2018-2019 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.common.datasource;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.openbravo.client.kernel.ComponentProvider;
import org.openbravo.service.datasource.hql.HqlQueryTransformer;

@ComponentProvider.Qualifier("E42E75B452F545469E648BD84C9538E9")
public class ServiceModifyTaxProductCategoryTransformer extends HqlQueryTransformer {
  private static final String MODIFY_TAX_PROD_CAT_PE = "B51960EFD3E04B79917A1277C751232F";

  @Override
  public String transformHqlQuery(String hqlQuery, Map<String, String> requestParameters,
      Map<String, Object> queryNamedParameters) {
    String tabId = requestParameters.get("tabId");
    if (StringUtils.equals(tabId, MODIFY_TAX_PROD_CAT_PE)) {
      StringBuilder replacement = new StringBuilder("");
      replacement.append(" not exists (select sl.id ");
      replacement.append("             from M_PRODUCT_SERVICELINKED sl ");
      replacement.append("             where sl.productCategory.id = cat.id ");
      replacement.append("             and sl.product.id = :productId)");
      queryNamedParameters.put("productId", requestParameters.get("@Product.id@"));
      return hqlQuery.replace("@insertion_point@", replacement);
    } else {
      return hqlQuery;
    }
  }

}
