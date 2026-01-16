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
import org.openbravo.service.db.DalConnectionProvider;

import jakarta.enterprise.context.Dependent;

/**
 * A class in charge of registering APRM SQL functions in Hibernate.
 */
@Dependent
public class APRMSQLFunctionRegister implements SQLFunctionRegister {
  private static final String RDBMS = new DalConnectionProvider(false).getRDBMS();

  @Override
  public Map<String, SqmFunctionDescriptor> getSQLFunctions() {
    Map<String, SqmFunctionDescriptor> sqlFunctions = new HashMap<>();
    
    // Standard SQL function for ad_message_get2
    sqlFunctions.put("ad_message_get2", new StandardSqlFunction("ad_message_get2"));
    
    // Template SQL function for hqlagg
    sqlFunctions.put("hqlagg", new TemplateSqlFunction("hqlagg", getAggregationSQL()));
    
    return sqlFunctions;
  }
  
  // Helper class for standard SQL functions
  private static class StandardSqlFunction extends AbstractSqmSelfRenderingFunctionDescriptor {
    private final String functionName;
    
    public StandardSqlFunction(String functionName) {
      super(functionName, 
            StandardArgumentsValidators.exactly(1),
            StandardFunctionReturnTypeResolvers.useFirstNonNull(),
            null);
      this.functionName = functionName;
    }
    
    @Override
    public void render(SqlAppender sqlAppender, List<? extends SqlAstNode> arguments, 
                      ReturnableType<?> returnType, SqlAstTranslator<?> translator) {
      sqlAppender.appendSql(functionName);
      sqlAppender.appendSql("(");
      if (!arguments.isEmpty()) {
        translator.render(arguments.get(0), SqlAstNodeRenderingMode.DEFAULT);
      }
      sqlAppender.appendSql(")");
    }
  }
  
  // Helper class for template SQL functions
  private static class TemplateSqlFunction extends AbstractSqmSelfRenderingFunctionDescriptor {
    private final String template;
    
    public TemplateSqlFunction(String name, String template) {
      super(name, 
            StandardArgumentsValidators.exactly(1),
            StandardFunctionReturnTypeResolvers.useFirstNonNull(),
            null);
      this.template = template;
    }
    
    @Override
    public void render(SqlAppender sqlAppender, List<? extends SqlAstNode> arguments, 
                      ReturnableType<?> returnType, SqlAstTranslator<?> translator) {
      String sql = template;
      for (int i = 0; i < arguments.size(); i++) {
        String placeholder = "?" + (i + 1);
        if (sql.contains(placeholder)) {
          int pos = sql.indexOf(placeholder);
          sqlAppender.appendSql(sql.substring(0, pos));
          translator.render(arguments.get(i), SqlAstNodeRenderingMode.DEFAULT);
          sql = sql.substring(pos + placeholder.length());
        }
      }
      sqlAppender.appendSql(sql);
    }
  }

  private String getAggregationSQL() {
    if ("ORACLE".equals(RDBMS)) {
      return "listagg(to_char(?1), ',') WITHIN GROUP (ORDER BY ?1)";
    } else {
      return "array_to_string(array_agg(?1), ',')";
    }
  }
}
