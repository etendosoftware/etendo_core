//Sqlc generated V1.O00-1
package org.openbravo.buildvalidation;

import java.sql.*;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import javax.servlet.ServletException;

import org.openbravo.data.FieldProvider;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.data.UtilSql;
import java.util.*;

@SuppressWarnings("serial")
class DuplicatedJavaPackageData implements FieldProvider {
static Logger log4j = LogManager.getLogger();
  private String InitRecordNumber="0";
  public String javapackage;
  public String name;

  public String getInitRecordNumber() {
    return InitRecordNumber;
  }

  public String getField(String fieldName) {
    if (fieldName.equalsIgnoreCase("javapackage"))
      return javapackage;
    else if (fieldName.equalsIgnoreCase("name"))
      return name;
   else {
     log4j.debug("Field does not exist: " + fieldName);
     return null;
   }
 }

  public static DuplicatedJavaPackageData[] duplicatedPackages(ConnectionProvider connectionProvider)    throws ServletException {
    return duplicatedPackages(connectionProvider, 0, 0);
  }

  public static DuplicatedJavaPackageData[] duplicatedPackages(ConnectionProvider connectionProvider, int firstRegister, int numberRegisters)    throws ServletException {
    String strSql = "";
    strSql = strSql + 
      "         select javapackage, name" +
      "           from ad_module m" +
      "          where exists (select 1 " +
      "                          from ad_module m2 " +
      "                         where m2.javapackage=m.javapackage " +
      "                           and m2.ad_module_id != m.ad_module_id)" +
      "          order by 1,2";

    ResultSet result;
    Vector<DuplicatedJavaPackageData> vector = new Vector<DuplicatedJavaPackageData>(0);
    PreparedStatement st = null;

    try {
    st = connectionProvider.getPreparedStatement(strSql);

      result = st.executeQuery();
      long countRecord = 0;
      long countRecordSkip = 1;
      boolean continueResult = true;
      while(countRecordSkip < firstRegister && continueResult) {
        continueResult = result.next();
        countRecordSkip++;
      }
      while(continueResult && result.next()) {
        countRecord++;
        DuplicatedJavaPackageData objectDuplicatedJavaPackageData = new DuplicatedJavaPackageData();
        objectDuplicatedJavaPackageData.javapackage = UtilSql.getValue(result, "javapackage");
        objectDuplicatedJavaPackageData.name = UtilSql.getValue(result, "name");
        objectDuplicatedJavaPackageData.InitRecordNumber = Integer.toString(firstRegister);
        vector.addElement(objectDuplicatedJavaPackageData);
        if (countRecord >= numberRegisters && numberRegisters != 0) {
          continueResult = false;
        }
      }
      result.close();
    } catch(SQLException e){
      if (log4j.isDebugEnabled()) {
        log4j.error("SQL error in query: " + strSql, e);
      } else {
        log4j.error("SQL error in query: " + strSql + " :" + e);
      }
      throw new ServletException("@CODE=" + Integer.toString(e.getErrorCode()) + "@" + e.getMessage());
    } catch(Exception ex){
      if (log4j.isDebugEnabled()) {
        log4j.error("Exception in query: " + strSql, ex);
      } else {
        log4j.error("Exception in query: " + strSql + " :" + ex);
      }
      throw new ServletException("@CODE=@" + ex.getMessage());
    } finally {
      try {
        connectionProvider.releasePreparedStatement(st);
      } catch(Exception e){
        log4j.error("Error during release*Statement of query: " + strSql, e);
      }
    }
    DuplicatedJavaPackageData objectDuplicatedJavaPackageData[] = new DuplicatedJavaPackageData[vector.size()];
    vector.copyInto(objectDuplicatedJavaPackageData);
    return(objectDuplicatedJavaPackageData);
  }
}
