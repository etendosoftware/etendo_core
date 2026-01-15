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
import java.util.List;
import java.util.Map;

import org.hibernate.query.ReturnableType;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.openbravo.dal.core.SQLFunctionRegister;

import jakarta.enterprise.context.Dependent;

/**
 * A class in charge of registering core SQL functions in Hibernate.
 */
@Dependent
public class KernelSQLFunctionRegister implements SQLFunctionRegister {

  @Override
  public Map<String, SqmFunctionDescriptor> getSQLFunctions() {
    Map<String, SqmFunctionDescriptor> sqlFunctions = new HashMap<>();
    
    // Standard SQL functions with arguments
    sqlFunctions.put("ad_org_getcalendarowner", new StandardSqlFunction("ad_org_getcalendarowner"));
    sqlFunctions.put("ad_org_getperiodcontrolallow", new StandardSqlFunction("ad_org_getperiodcontrolallow"));
    sqlFunctions.put("get_uuid", new StandardSqlFunction("get_uuid"));
    sqlFunctions.put("m_isparent_ch_value", new StandardSqlFunction("m_isparent_ch_value"));
    sqlFunctions.put("m_getjsondescription", new StandardSqlFunction("m_getjsondescription"));
    sqlFunctions.put("to_timestamp", new StandardSqlFunction("to_timestamp"));
    
    // No-argument SQL function
    sqlFunctions.put("now", new NoArgSqlFunction("now"));
    
    // Full text search functions (commenting out for now as they need special migration)
    // sqlFunctions.put("fullTextSearchFilter", new PgFullTextSearchFunction.Filter());
    // sqlFunctions.put("fullTextSearchRank", new PgFullTextSearchFunction.Rank());
    
    return sqlFunctions;
  }
  
  // Helper class for standard SQL functions with arguments
  private static class StandardSqlFunction extends AbstractSqmSelfRenderingFunctionDescriptor {
    private final String functionName;
    
    public StandardSqlFunction(String functionName) {
      super(functionName, 
            StandardArgumentsValidators.min(0),
            StandardFunctionReturnTypeResolvers.useFirstNonNull(),
            null);
      this.functionName = functionName;
    }
    
    @Override
    public void render(SqlAppender sqlAppender, List<? extends SqlAstNode> arguments, 
                      ReturnableType<?> returnType, SqlAstTranslator<?> translator) {
      sqlAppender.appendSql(functionName);
      sqlAppender.appendSql("(");
      for (int i = 0; i < arguments.size(); i++) {
        if (i > 0) {
          sqlAppender.appendSql(", ");
        }
        translator.render(arguments.get(i), SqlAstNodeRenderingMode.DEFAULT);
      }
      sqlAppender.appendSql(")");
    }
  }
  
  // Helper class for no-argument SQL functions
  private static class NoArgSqlFunction extends AbstractSqmSelfRenderingFunctionDescriptor {
    private final String functionName;
    
    public NoArgSqlFunction(String functionName) {
      super(functionName, 
            StandardArgumentsValidators.exactly(0),
            StandardFunctionReturnTypeResolvers.useFirstNonNull(),
            null);
      this.functionName = functionName;
    }
    
    @Override
    public void render(SqlAppender sqlAppender, List<? extends SqlAstNode> arguments, 
                      ReturnableType<?> returnType, SqlAstTranslator<?> translator) {
      sqlAppender.appendSql(functionName);
      sqlAppender.appendSql("()");
    }
  }
}
