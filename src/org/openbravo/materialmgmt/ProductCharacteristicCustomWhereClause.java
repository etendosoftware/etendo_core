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
 * All portions are Copyright (C) 2019 Openbravo SLU 
 * All Rights Reserved. 
 ************************************************************************
 */
package org.openbravo.materialmgmt;

import java.util.Map;

/**
 * Allows to define a where clause that will be used in the ProductCharacteristicsDS datasource.
 * 
 * Classes implementing this interfaces with this interface must declare
 * the @ComponentProvider.Qualifier annotation, passing as parameter the ID of the Process
 * Definition this ProductCharacteristicCustomWhereClause must be applied to. Subclasses must also
 * declare the @ApplicationScoped annotation
 */
public interface ProductCharacteristicCustomWhereClause {

  /**
   * Method that will be invoked by the ProductCharacteristicsDS datasource, to obtain a where
   * clause that will be added to the product characteristics query.
   * 
   * The returned where clause must not include the WHERE keyword
   * 
   * @param requestParameters
   *          the map of parameters included in the http request. It will include a
   *          _buttonOwnerContextInfo that will contain the context information of the tab where the
   *          process definition button is defined
   * @param queryNamedParameters
   *          map where the query parameters should be included, if any
   * @return an HQL where clause, omitting the WHERE keyword
   */
  public String getCustomWhereClause(Map<String, String> requestParameters,
      Map<String, Object> queryNamedParameters);
}
