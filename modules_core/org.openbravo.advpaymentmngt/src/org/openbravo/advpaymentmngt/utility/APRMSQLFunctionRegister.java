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
 * All portions are Copyright (C) 2018 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */

package org.openbravo.advpaymentmngt.utility;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.type.StandardBasicTypes;
import org.openbravo.dal.core.SQLFunctionRegister;
import org.openbravo.service.db.DalConnectionProvider;

/**
 * A class in charge of registering APRM SQL functions in Hibernate.
 */
@ApplicationScoped
public class APRMSQLFunctionRegister implements SQLFunctionRegister {
  private static final String RDBMS = new DalConnectionProvider(false).getRDBMS();

  @Override
  public Map<String, SQLFunction> getSQLFunctions() {
    Map<String, SQLFunction> sqlFunctions = new HashMap<>();
    sqlFunctions.put("ad_message_get2",
        new StandardSQLFunction("ad_message_get2", StandardBasicTypes.STRING));
    sqlFunctions.put("hqlagg",
        new SQLFunctionTemplate(StandardBasicTypes.STRING, getAggregationSQL()));
    return sqlFunctions;
  }

  private String getAggregationSQL() {
    if ("ORACLE".equals(RDBMS)) {
      return "listagg(to_char(?1), ',') WITHIN GROUP (ORDER BY ?1)";
    } else {
      return "array_to_string(array_agg(?1), ',')";
    }
  }
}
