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
 * All portions are Copyright (C) 2018-2020 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.client.kernel;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import org.hibernate.dialect.function.NoArgSQLFunction;
import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.type.StandardBasicTypes;
import org.openbravo.dal.core.SQLFunctionRegister;

/**
 * A class in charge of registering core SQL functions in Hibernate.
 */
@ApplicationScoped
public class KernelSQLFunctionRegister implements SQLFunctionRegister {

  @Override
  public Map<String, SQLFunction> getSQLFunctions() {
    Map<String, SQLFunction> sqlFunctions = new HashMap<>();
    sqlFunctions.put("ad_org_getcalendarowner",
        new StandardSQLFunction("ad_org_getcalendarowner", StandardBasicTypes.STRING));
    sqlFunctions.put("ad_org_getperiodcontrolallow",
        new StandardSQLFunction("ad_org_getperiodcontrolallow", StandardBasicTypes.STRING));
    sqlFunctions.put("get_uuid", new StandardSQLFunction("get_uuid", StandardBasicTypes.STRING));
    sqlFunctions.put("m_isparent_ch_value",
        new StandardSQLFunction("m_isparent_ch_value", StandardBasicTypes.STRING));
    sqlFunctions.put("m_getjsondescription",
        new StandardSQLFunction("m_getjsondescription", StandardBasicTypes.STRING));
    sqlFunctions.put("now", new NoArgSQLFunction("now", StandardBasicTypes.TIMESTAMP));
    sqlFunctions.put("to_timestamp",
        new StandardSQLFunction("to_timestamp", StandardBasicTypes.TIMESTAMP));
    sqlFunctions.put("fullTextSearchFilter", new PgFullTextSearchFunction.Filter());
    sqlFunctions.put("fullTextSearchRank", new PgFullTextSearchFunction.Rank());
    return sqlFunctions;
  }
}
