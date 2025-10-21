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
 * All portions are Copyright (C) 2009-2020 Openbravo SLU
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.service.system;

/**
 * MIGRATED TO HIBERNATE 6
 * - Replaced org.hibernate.criterion.* with jakarta.persistence.criteria.*
 * - This file was automatically migrated from Criteria API to JPA Criteria API
 * - Review and test thoroughly before committing
 */


import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ddlutils.model.Check;
import org.apache.ddlutils.model.Database;
import org.apache.ddlutils.model.ForeignKey;
import org.apache.ddlutils.model.Index;
import org.apache.ddlutils.model.Reference;
import org.apache.ddlutils.model.Unique;
import org.apache.ddlutils.model.View;
// TODO: Migrado a CriteriaBuilder - import org.hibernate.criterion.LogicalExpression;
import jakarta.persistence.criteria.CriteriaBuilder;
// TODO: Migrado a CriteriaBuilder - import org.hibernate.criterion.SimpleExpression;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.base.model.domaintype.ButtonDomainType;
import org.openbravo.client.application.ApplicationConstants;
import org.openbravo.dal.service.OBCriteria;
import jakarta.persistence.criteria.CriteriaBuilder;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.datamodel.Column;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.model.ad.module.Module;
import org.openbravo.model.ad.module.ModuleDBPrefix;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.ad.ui.Field;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.model.ad.utility.DataSet;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.scheduling.KillableProcess;
import org.openbravo.service.system.SystemValidationResult.SystemValidationType;

/**
 * Validates the database model against the application dictionary and checks that columns are
 * inline with the application dictionary.
 * 
 * @author mtaal
 */

// check naming rule of a table, use ad_exceptions table
public class DatabaseValidator implements SystemValidator {
  private Database database;
  private boolean dbsmExecution = false;

  private static int MAX_OBJECT_NAME_LENGTH = 30;
  private static final String FRENCHFISCAL_MODULE = "BA750E79C7AA4AA2860B99B415038E37";

  private StringBuilder updateSql = new StringBuilder();

  // if this is set then only module specific things are checked.
  private Module validateModule;

  @Override
  public String getCategory() {
    return "Database Model";
  }

  @Override
  public SystemValidationResult validate() {
    boolean checkAD = true;
    return validate(checkAD);
  }

  @Override
  public SystemValidationResult validate(boolean checkAD) {
    final SystemValidationResult result = new SystemValidationResult();

    // read the tables
    Database db = getDatabase();
    for (int i = 0; i < db.getFunctionCount(); i++) {
      if (db.getFunction(i).getName().length() > 30) {
        result.addError(SystemValidationResult.SystemValidationType.INCORRECT_NAME_LENGTH,
            "Name of Function " + db.getFunction(i).getName()
                + " is too long. Only 30 characters allowed.");
      }
    }
    for (int i = 0; i < db.getTriggerCount(); i++) {
      if (db.getTrigger(i).getName().length() > 30) {
        result.addError(SystemValidationResult.SystemValidationType.INCORRECT_NAME_LENGTH,
            "Name of Trigger " + db.getTrigger(i).getName()
                + " is too long. Only 30 characters allowed.");
      }
    }
    for (int i = 0; i < db.getViewCount(); i++) {
      if (db.getView(i).getName().length() > 30) {
        result.addError(SystemValidationResult.SystemValidationType.INCORRECT_NAME_LENGTH,
            "Name of View " + db.getView(i).getName()
                + " is too long. Only 30 characters allowed.");
      }
    }

    // We need to check both in the full tables added by the module, and in the objects which are
    // added by this module to tables which belong to a different module
    ArrayList<org.apache.ddlutils.model.Table> tables = new ArrayList<org.apache.ddlutils.model.Table>();
    for (int j = 0; j < db.getTableCount(); j++) {
      tables.add(db.getTable(j));
    }
    for (int j = 0; j < db.getModifiedTableCount(); j++) {
      tables.add(db.getModifiedTable(j));
    }
    for (org.apache.ddlutils.model.Table dbTable : tables) {
      if (dbTable.getName().length() > 30) {
        result.addError(SystemValidationResult.SystemValidationType.INCORRECT_NAME_LENGTH,
            "Name of table " + dbTable.getName() + " is too long. Only 30 characters allowed.");
      }
      for (int i = 0; i < dbTable.getColumnCount(); i++) {
        if (dbTable.getColumn(i).getName().length() > 30) {
          result.addError(SystemValidationResult.SystemValidationType.INCORRECT_NAME_LENGTH,
              "Name of column " + dbTable.getColumn(i).getName() + "for table " + dbTable.getName()
                  + " is too long. Only 30 characters allowed.");
        }
      }
      for (int i = 0; i < dbTable.getCheckCount(); i++) {
        if (dbTable.getCheck(i).getName().length() > 30) {
          result.addError(SystemValidationResult.SystemValidationType.INCORRECT_NAME_LENGTH,
              "Name of Check " + dbTable.getCheck(i).getName() + "for table " + dbTable.getName()
                  + " is too long. Only 30 characters allowed.");
        }
      }
      for (int i = 0; i < dbTable.getForeignKeyCount(); i++) {
        if (dbTable.getForeignKey(i).getName().length() > 30) {
          result.addError(SystemValidationResult.SystemValidationType.INCORRECT_NAME_LENGTH,
              "Name of ForeignKey " + dbTable.getForeignKey(i).getName() + "for table "
                  + dbTable.getName() + " is too long. Only 30 characters allowed.");
        }
      }
      for (int i = 0; i < dbTable.getIndexCount(); i++) {
        if (dbTable.getIndex(i).getName().length() > 30) {
          result.addError(SystemValidationResult.SystemValidationType.INCORRECT_NAME_LENGTH,
              "Name of Index " + dbTable.getIndex(i).getName() + "for table " + dbTable.getName()
                  + " is too long. Only 30 characters allowed.");
        }
      }
      for (int i = 0; i < dbTable.getUniqueCount(); i++) {
        if (dbTable.getUnique(i).getName().length() > 30) {
          result.addError(SystemValidationResult.SystemValidationType.INCORRECT_NAME_LENGTH,
              "Name of Unique " + dbTable.getUnique(i).getName() + "for table " + dbTable.getName()
                  + " is too long. Only 30 characters allowed.");
        }
      }
    }

    if (checkAD) {
      // the following cases are checked:
      // 1) table present in ad, but not in db
      // 2) table present in db, not in ad
      // 3) table present on both sides, column match check

      final Map<String, org.apache.ddlutils.model.Table> dbTablesByName = new HashMap<String, org.apache.ddlutils.model.Table>();

      final org.apache.ddlutils.model.Table[] dbTables = getDatabase().getTables();
      for (org.apache.ddlutils.model.Table dbTable : dbTables) {
        dbTablesByName.put(dbTable.getName().toUpperCase(), dbTable);
      }

      final org.apache.ddlutils.model.Table[] dbModifiedTables = getDatabase().getModifiedTables();
      for (org.apache.ddlutils.model.Table dbModifiedTable : dbModifiedTables) {
        dbTablesByName.put(dbModifiedTable.getName().toUpperCase(), dbModifiedTable);
      }
      final Map<String, org.apache.ddlutils.model.Table> tmpDBTablesByName = new HashMap<String, org.apache.ddlutils.model.Table>(
          dbTablesByName);

      final Map<String, View> dbViews = new HashMap<String, View>();
      final View[] views = getDatabase().getViews();
      for (View view : views) {
        dbViews.put(view.getName().toUpperCase(), view);
      }

      final List<Table> adTables = OBDal.getInstance()
          .createCriteria(Table.class)
          .addEqual(Table.PROPERTY_VIEW, false)
          .addEqual(Table.PROPERTY_DATAORIGINTYPE, ApplicationConstants.TABLEBASEDTABLE)
          .list();

      final String moduleId = (getValidateModule() == null ? null : getValidateModule().getId());
      for (Table adTable : adTables) {
        final org.apache.ddlutils.model.Table dbTable = dbTablesByName
            .get(adTable.getDBTableName().toUpperCase());
        final View view = dbViews.get(adTable.getDBTableName().toUpperCase());

        if (view == null && dbTable == null) {
          // in Application Dictionary not in Physical Table
          if (moduleId == null || (adTable.getDataPackage().getModule() != null
              && adTable.getDataPackage().getModule().getId().equals(moduleId))) {
            result.addError(SystemValidationResult.SystemValidationType.NOT_EXIST_IN_DB,
                "Table " + adTable.getDBTableName() + " defined in the Application Dictionary"
                    + " but is not present in the database");
          }
        } else if (view != null) {
          dbViews.remove(view.getName().toUpperCase());
        } else {
          if (moduleId == null || (adTable.getDataPackage().getModule() != null
              && adTable.getDataPackage().getModule().getId().equals(moduleId))) {
            checkTableWithoutPrimaryKey(dbTable, result);
            checkMaxObjectNameLength(dbTable, result);
          }
          matchColumns(adTable, dbTable, result);
          tmpDBTablesByName.remove(dbTable.getName().toUpperCase());
          checkFieldsInGridView(adTable, result);
        }
      }
      for (int i = 0; i < database.getTableCount(); i++) {
        checkForeignKeys(validateModule, database.getTable(i), result);
      }
      for (int i = 0; i < database.getModifiedTableCount(); i++) {
        checkForeignKeys(validateModule, database.getModifiedTable(i), result);
      }

      // only check this one if the global validate check is done
      for (org.apache.ddlutils.model.Table dbTable : tmpDBTablesByName.values()) {
        // ignore errors related to C_TEMP_SELECTION and AD_CONTEXT_INFO
        if (!dbTable.getName().toUpperCase().startsWith("C_TEMP_SELECTION")
            && !dbTable.getName().toUpperCase().startsWith("AD_CONTEXT_INFO")) {
          result.addWarning(SystemValidationResult.SystemValidationType.NOT_EXIST_IN_AD,
              "Table " + dbTable.getName() + " present in the database "
                  + " but not defined in the Application Dictionary");
        }
      }

      if (getValidateModule() == null) {
        for (View view : dbViews.values()) {
          // ignore errors related to C_TEMP_SELECTION
          if (!view.getName().toUpperCase().startsWith("C_TEMP_SELECTION")) {
            result.addWarning(SystemValidationResult.SystemValidationType.NOT_EXIST_IN_AD,
                "View " + view.getName() + " present in the database "
                    + " but not defined in the Application Dictionary");
          }
        }
      }

      checkIncorrectClientOrganizationName(result);

      checkDBObjectsName(result);

      checkDataSetName(result);

      checkPasswordColumns(result);

      checkKillableImplementation(result);

    }

    OBDal.getInstance().getSession().clear();
    return result;
  }

  /**
   * Checks if old-style password columns exist which should be updated to use the new references.
   * 
   */
  private void checkPasswordColumns(SystemValidationResult result) {
    org.openbravo.model.ad.domain.Reference hashed = OBDal.getInstance()
        .get(org.openbravo.model.ad.domain.Reference.class, "C5C21C28B39E4683A91779F16C112E40");
    org.openbravo.model.ad.domain.Reference encrypted = OBDal.getInstance()
        .get(org.openbravo.model.ad.domain.Reference.class, "16EC6DF4A59747749FDF256B7FBBB058");

    // if one of the old-booleans is set, but not using new reference-id's -> report as warning
    OBCriteria<Column> colQuery = OBDal.getInstance().createCriteria(Column.class);
    colQuery.addAnd((cb, obc) -> cb.equal(obc.getPath(Column.PROPERTY_DISPLAYENCRIPTION), Boolean.TRUE),
                    (cb, obc) -> cb.not(cb.or(cb.equal(obc.getPath(Column.PROPERTY_REFERENCE), hashed),
                                              cb.equal(obc.getPath(Column.PROPERTY_REFERENCE), encrypted))));

    // only validate given module (if given)
    if (validateModule != null) {
      colQuery.addEqual(Column.PROPERTY_MODULE, validateModule);
    }
    if (colQuery.count() > 0) {
      List<Column> columns = colQuery.list();
      for (Column column : columns) {
        result.addWarning(SystemValidationType.OLDSTYLE_PASSWORD_COLUMNS, "The column '"
            + column.getTable().getName() + "'.'" + column.getName()
            + "' is using old-style config for password-type columns. It should be changed to use one of the new references '"
            + hashed.getName() + "' or '" + encrypted.getName() + "'");
      }
    }
  }

  /**
   * Checks dataset name against allowed characters.
   * 
   * Background is that dataset name is directly used to derive the exported filename for the
   * dataset xml-file and this name should not contain any special characters.
   */
  private void checkDataSetName(SystemValidationResult result) {
    OBCriteria<DataSet> obc = OBDal.getInstance().createCriteria(DataSet.class);
    if (validateModule != null) {
      obc.addEqual(DataSet.PROPERTY_MODULE, validateModule);
    }
    List<DataSet> dsList = obc.list();
    for (DataSet ds : dsList) {
      String dsName = ds.getName();
      if (!dsName.matches("[a-zA-Z0-9 _\\-]+")) {
        result.addWarning(SystemValidationResult.SystemValidationType.INCORRECT_DATASET_NAME,
            "The name of the dataset \"" + dsName
                + "\" contains illegal characters. It is only allowed to contain 'a'..'z', 'A'..'Z', '0'..'9', whitespace, '-' and '_'");
      }
    }
  }

  private void checkMaxObjectNameLength(org.apache.ddlutils.model.Table dbTable,
      SystemValidationResult result) {
    checkNameLength("Table", dbTable.getName(), result);
    for (org.apache.ddlutils.model.Column dbColumn : dbTable.getColumns()) {
      checkNameLength("(table: " + dbTable.getName() + ") Column ", dbColumn.getName(), result);
    }
    for (ForeignKey fk : dbTable.getForeignKeys()) {
      checkNameLength("(table: " + dbTable.getName() + ") Foreign Key ", fk.getName(), result);
    }
    for (Unique unique : dbTable.getuniques()) {
      checkNameLength("(table: " + dbTable.getName() + ") Unique Constraint ", unique.getName(),
          result);
    }
    for (Index index : dbTable.getIndices()) {
      checkNameLength("(table: " + dbTable.getName() + ") Index ", index.getName(), result);
    }
  }

  private void checkNameLength(String type, String name, SystemValidationResult result) {
    if (name.length() > MAX_OBJECT_NAME_LENGTH) {
      result.addError(SystemValidationResult.SystemValidationType.NAME_TOO_LONG,
          "The name of " + type + " " + name + " is too long, the maximum allowed length is: "
              + MAX_OBJECT_NAME_LENGTH);
    }
  }

  private void checkTableWithoutPrimaryKey(org.apache.ddlutils.model.Table dbTable,
      SystemValidationResult result) {
    if (dbTable.getPrimaryKeyColumns().length == 0) {
      result.addError(SystemValidationResult.SystemValidationType.NO_PRIMARY_KEY_COLUMNS,
          "Table " + dbTable.getName() + " has no primary key columns.");
    }
  }

  private Property getProperty(String tableName, String columnName) {
    final Entity entity = ModelProvider.getInstance().getEntityByTableName(tableName);
    if (entity == null) {
      // can happen with mismatches
      return null;
    }
    for (Property property : entity.getProperties()) {
      if (property.getColumnName() != null
          && property.getColumnName().equalsIgnoreCase(columnName)) {
        return property;
      }
    }
    return null;
  }

  private void checkForeignKeys(Module module, org.apache.ddlutils.model.Table table,
      SystemValidationResult result) {
    final Entity entity = ModelProvider.getInstance().getEntityByTableName(table.getName());
    List<String> fkWhiteList = getFkWhiteList();
    List<String> fKTblWhiteList = getFkTblWhiteList();
    if (entity == null) {
      // can happen with mismatches
      return;
    }

    if (fKTblWhiteList.contains(entity.getTableName().toLowerCase())) {
      // ignore fk check for the tables in the tableFKWhiteList
      return;
    }

    for (Property property : entity.getProperties()) {

      // ignore computed columns
      if (property.getSqlLogic() != null) {
        continue;
      }

      if (!property.isPrimitive() && !property.isOneToMany() && !property.isAuditInfo()) {
        // check if the property column is present in a foreign key

        // special case that a property does not have a column, if it is
        // like a virtual property see ClientInformation.client
        if (property.getColumnName() == null) {
          continue;
        }

        final String colName = property.getColumnName().toUpperCase();

        // ignore this specific case
        if (fkWhiteList
            .contains(entity.getTableName().toLowerCase() + "." + colName.toLowerCase())) {
          continue;
        }

        // ignore ad_audit_trail as fk's are omitted on purpose

        if (entity.getTableName().equalsIgnoreCase("ad_audit_trail")) {
          continue;
        }

        // if the property doesn't belong to the module, then it shouldn't be checked, as the
        // physical model will not contain it
        if (module != null && (property.getModule() == null
            || !module.getId().equals(property.getModule().getId()))) {
          continue;
        }

        boolean found = false;
        for (ForeignKey fk : table.getForeignKeys()) {
          for (Reference reference : fk.getReferences()) {
            if (reference.getLocalColumnName().toUpperCase().equals(colName)) {
              found = true;
              break;
            }
          }
          if (found) {
            break;
          }
        }
        if (!found) {
          result.addWarning(SystemValidationResult.SystemValidationType.NOT_PART_OF_FOREIGN_KEY,
              "Foreign Key Column " + table.getName() + "." + property.getColumnName()
                  + " is not part of a foreign key constraint.");
        }
      }
    }
  }

  // Get List of Foreign Keys to be skipped in the validation while exporting database.
  private List<String> getFkWhiteList() {
    List<String> fkWhiteList = new ArrayList<String>();
    fkWhiteList.add("ad_module_log.ad_module_id");
    fkWhiteList.add("c_order.cancelledorder_id");
    fkWhiteList.add("c_order.replacedorder_id");
    fkWhiteList.add("c_order.replacementorder_id");
    fkWhiteList.add("c_orderline.replacedorderline_id");
    fkWhiteList.add("obuiapp_data_pool_sel.obuiapp_pool_report_v_id");
    return fkWhiteList;
  }

  // Get List of Tables for which Foreign Keys to be skipped in the validation while exporting
  // database.
  private List<String> getFkTblWhiteList() {
    List<String> fkTblWhiteList = new ArrayList<String>();
    fkTblWhiteList.add("ad_module_install");
    fkTblWhiteList.add("obanaly_fact_discounts");
    fkTblWhiteList.add("obanaly_fact_order");
    fkTblWhiteList.add("obanaly_fact_salesordheader");
    fkTblWhiteList.add("obretan_fact_sessions");
    fkTblWhiteList.add("obdpl_fact_acct_pl");
    return fkTblWhiteList;
  }

  private void matchColumns(Table adTable, org.apache.ddlutils.model.Table dbTable,
      SystemValidationResult result) {

    final Map<String, org.apache.ddlutils.model.Column> dbColumnsByName = new HashMap<String, org.apache.ddlutils.model.Column>();
    for (org.apache.ddlutils.model.Column dbColumn : dbTable.getColumns()) {
      dbColumnsByName.put(dbColumn.getName().toUpperCase(), dbColumn);
    }

    final String moduleId = (getValidateModule() == null ? null : getValidateModule().getId());
    for (Column column : adTable.getADColumnList()) {
      boolean hasSqlLogic;
      if (column.getSqllogic() != null && !column.getSqllogic().isEmpty()) {
        hasSqlLogic = true;
      } else {
        hasSqlLogic = false;
      }
      // If the column has SQL logic, it does not need to be present in the database
      final boolean checkColumn = (moduleId == null
          || (column.getModule().getId().equals(moduleId))) && !hasSqlLogic;
      if (!checkColumn) {
        dbColumnsByName.remove(column.getDBColumnName().toUpperCase());
        continue;
      }
      final org.apache.ddlutils.model.Column dbColumn = dbColumnsByName
          .get(column.getDBColumnName().toUpperCase());
      if (dbColumn == null) {
        String message = "";
        if (column.getDBColumnName().substring(0, 3).equalsIgnoreCase("EM_")) {
          message = " The problem could also be that the column name has a dbprefix of a non existant module or that it starts with the dbprefix of the module which owns the table. In this second case, the column doesn't need the 'EM_DBPREFIX' prefix, you can use the name you want.";
        }
        result.addError(SystemValidationResult.SystemValidationType.NOT_EXIST_IN_DB,
            "Column " + adTable.getDBTableName() + "." + column.getDBColumnName()
                + " defined in the Application Dictionary but not present in the database."
                + message);

      } else {
        checkDataType(column, dbColumn, result, dbTable);

        checkNameLength("(table: " + dbTable.getName() + ") Column ", dbColumn.getName(), result);

        dbColumnsByName.remove(column.getDBColumnName().toUpperCase());

      }
    }

    // The columns in dbColumnsByName belong either to the tables defined in the module being
    // validated, or
    // to its modified tables, so they have to be checked always at this point

    for (org.apache.ddlutils.model.Column dbColumn : dbColumnsByName.values()) {
      result.addError(SystemValidationResult.SystemValidationType.NOT_EXIST_IN_AD,
          "Column " + dbTable.getName() + "." + dbColumn.getName() + " present in the database "
              + " but not defined in the Application Dictionary.");
    }
  }

  private void checkDataType(Column adColumn, org.apache.ddlutils.model.Column dbColumn,
      SystemValidationResult result, org.apache.ddlutils.model.Table dbTable) {

    final Property property = getProperty(dbTable.getName(), dbColumn.getName());

    // disabled because mandatory is always false
    // if (property != null && !property.isMandatory() && dbColumn.isRequired()) {
    // result.addError(
    // SystemValidationResult.SystemValidationType.NOT_NULL_IN_DB_NOT_MANDATORY_IN_AD, "Column "
    // + dbTable.getName() + "." + dbColumn.getName()
    // + " is required (not-null) but in the Application Dictonary"
    // + " it is set as non-mandatory");
    //
    // final Property p = getProperty(dbTable.getName(), dbColumn.getName());
    // updateSql
    // .append("update ad_column set ismandatory='Y' where ad_column_id in (select c.ad_column_id
    // from ad_column c, ad_table t "
    // + "where c.ad_table_id=t.ad_table_id and t.tablename='"
    // + p.getEntity().getTableName() + "' and c.columnname='" + p.getColumnName() + "');\n");
    //
    // }

    // disabled this check, will be done in 2.60
    // if (property != null && property.isMandatory() && !dbColumn.isRequired()) {
    // result.addError(SystemValidationType.MANDATORY_IN_AD_NULLABLE_IN_DB, "Column "
    // + dbTable.getName() + "." + dbColumn.getName()
    // + " is not-required (null-allowed) but in the Application Dictonary"
    // + " it is set as mandatory");
    // }

    if ("VARCHAR".equals(dbColumn.getType()) || "NVARCHAR".equals(dbColumn.getType())) {
      checkMaxLength(dbColumn, dbTable, result, 4000);
    }

    // check the default value
    if (property != null && property.getActualDefaultValue() != null) {
      try {
        property.checkIsValidValue(property.getActualDefaultValue());
      } catch (Exception e) {
        // actually a ValidationException is thrown but this is not
        // accepted by the compiler
        result.addError(SystemValidationType.INCORRECT_DEFAULT_VALUE, e.getMessage());
      }
    }

    if (dbColumn.isPrimaryKey()) {
      // there is a special case, the ad_script_sql has a
      // seqno has key
      if (!dbTable.getName().equalsIgnoreCase("ad_script_sql")) {
        checkType(dbColumn, dbTable, result, "VARCHAR");
        checkLength(dbColumn, dbTable, result, 32);
      }
    } else if (property != null && property.getAllowedValues().size() > 0) {
      checkType(dbColumn, dbTable, result, "VARCHAR");
      checkLength(dbColumn, dbTable, result, 60);
    } else if (property != null && property.isOneToMany()) {
      // ignore those
    } else if (property != null && !property.isPrimitive()) {

      checkType(dbColumn, dbTable, result, "VARCHAR");
      if (property.getReferencedProperty() != null) {
        checkLength(dbColumn, dbTable, result, property.getReferencedProperty().getFieldLength());
      } else {
        checkLength(dbColumn, dbTable, result, 32);
      }
    } else if (property != null && property.getPrimitiveObjectType() != null) {
      final Class<?> prim = property.getPrimitiveObjectType();
      if (prim == String.class || property.getDomainType() instanceof ButtonDomainType) {
        checkType(dbColumn, dbTable, result,
            new String[] { "VARCHAR", "NVARCHAR", "CHAR", "NCHAR", "CLOB", "TSVECTOR" });
        // there are too many differences which make this check not relevant/practical at the moment
        // checkLength(dbColumn, dbTable, result, property.getFieldLength());
      } else if (prim == Long.class) {
        checkType(dbColumn, dbTable, result, "DECIMAL");
      } else if (prim == Integer.class) {
        checkType(dbColumn, dbTable, result, "DECIMAL");
      } else if (prim == BigDecimal.class) {
        checkType(dbColumn, dbTable, result, "DECIMAL");
      } else if (prim == Date.class) {
        checkType(dbColumn, dbTable, result, "TIMESTAMP");
      } else if (prim == Boolean.class) {
        checkType(dbColumn, dbTable, result, "CHAR");
        checkLength(dbColumn, dbTable, result, 1);
      } else if (prim == Float.class) {
        checkType(dbColumn, dbTable, result, "DECIMAL");
      } else if (prim == Object.class) {
        // nothing to check...
      } else if (prim == Timestamp.class) {
        checkType(dbColumn, dbTable, result, "TIMESTAMP");
      }
    }
  }

  private void checkType(org.apache.ddlutils.model.Column dbColumn,
      org.apache.ddlutils.model.Table dbTable, SystemValidationResult result,
      String[] expectedTypes) {
    boolean found = false;
    final StringBuilder sb = new StringBuilder();
    for (String expectedType : expectedTypes) {
      sb.append(expectedType + " ");
      found = dbColumn.getType().equals(expectedType);
      if (found) {
        break;
      }
    }
    if (!found) {
      result.addError(SystemValidationType.WRONG_TYPE,
          "Column " + dbTable.getName() + "." + dbColumn.getName()
              + " has incorrect type, expecting " + sb.toString() + "but was "
              + dbColumn.getType());
    }
  }

  private void checkType(org.apache.ddlutils.model.Column dbColumn,
      org.apache.ddlutils.model.Table dbTable, SystemValidationResult result, String expectedType) {
    if (!dbColumn.getType().equals(expectedType)) {
      if (dbColumn.getName().toUpperCase().equals("USER1_ID")
          || dbColumn.getName().toUpperCase().equals("USER2_ID")) {
        final Property p = getProperty(dbTable.getName(), dbColumn.getName());
        updateSql.append(
            "update ad_column set ad_reference_id='10', ad_reference_value_id=NULL where ad_column_id in (select c.ad_column_id from ad_column c, ad_table t "
                + "where c.ad_table_id=t.ad_table_id and t.tablename='"
                + p.getEntity().getTableName() + "' and c.columnname='" + p.getColumnName()
                + "');\n");
      }

      result.addError(SystemValidationType.WRONG_TYPE,
          "Column " + dbTable.getName() + "." + dbColumn.getName()
              + " has incorrect type, expecting " + expectedType + " but was "
              + dbColumn.getType());
    }
  }

  private void checkLength(org.apache.ddlutils.model.Column dbColumn,
      org.apache.ddlutils.model.Table dbTable, SystemValidationResult result, int expectedLength) {
    // special case no length check
    if ("AD_SCRIPT_SQL.SEQNO".equalsIgnoreCase(dbTable.getName() + "." + dbColumn.getName())) {
      return;
    }

    if (dbColumn.getSizeAsInt() != expectedLength) {
      result.addError(SystemValidationType.WRONG_LENGTH,
          "Column " + dbTable.getName() + "." + dbColumn.getName()
              + " has incorrect length, expecting " + expectedLength + " but was "
              + dbColumn.getSizeAsInt() + ". If this a foreign key column then the expected "
              + "length is either 32 (a uuid) or based on"
              + " the fieldLength (so not the db columnlength) of the referenced column"
              + ", as defined in AD_COLUMN.");
    }
  }

  private void checkMaxLength(org.apache.ddlutils.model.Column dbColumn,
      org.apache.ddlutils.model.Table dbTable, SystemValidationResult result, int maxLength) {
    if (dbColumn.getSizeAsInt() > maxLength) {
      result.addError(SystemValidationType.WRONG_LENGTH,
          "Column " + dbTable.getName() + "." + dbColumn.getName()
              + " has incorrect length, it is  " + dbColumn.getSizeAsInt()
              + " but the maximum size allowed for that type of column is " + maxLength + ".");
    }
  }

  // checks for all entities if the reference to the client indeed has the name client
  // and the same for the organization property
  private void checkIncorrectClientOrganizationName(SystemValidationResult result) {
    final Entity orgEntity = ModelProvider.getInstance().getEntity(Organization.ENTITY_NAME);
    final Entity clientEntity = ModelProvider.getInstance().getEntity(Client.ENTITY_NAME);
    for (Entity entity : ModelProvider.getInstance().getModel()) {
      boolean hasClientReference = false;
      boolean hasOrgReference = false;
      boolean hasValidOrg = false;
      String invalidOrgName = null;
      boolean hasValidClient = false;
      String invalidClientName = null;
      for (Property p : entity.getProperties()) {
        if (!p.isPrimitive() && p.getTargetEntity() == orgEntity && !hasValidOrg) {
          hasOrgReference = true;
          hasValidOrg = p.getName().equals(Client.PROPERTY_ORGANIZATION);
          if (!hasValidOrg) {
            invalidOrgName = p.getName();
          }
        }
        if (!p.isPrimitive() && p.getTargetEntity() == clientEntity && !hasValidClient) {
          hasClientReference = true;
          hasValidClient = p.getName().equals(Organization.PROPERTY_CLIENT);
          if (!hasValidClient) {
            invalidClientName = p.getName();
          }
        }
        if (p.isPrimitive() && p.getPrimitiveObjectType() != null
            && Date.class.isAssignableFrom(p.getPrimitiveObjectType())
            && p.getName().toLowerCase().equals("created") && !entity.isTraceable()) {
          result.addWarning(SystemValidationType.WRONG_NAME, "The table " + entity.getTableName()
              + " has a column 'created', Note that the audit column which stores the creation time MUST be called: creation Date");
        }
      }
      // can this ever be false?
      if (hasClientReference && !hasValidClient) {
        result.addError(SystemValidationType.INCORRECT_CLIENT_ORG_PROPERTY_NAME,
            "Table  " + entity.getTableName() + " has a column referencing AD_Client. "
                + " The AD_Column.name (note: different from AD_Column.columnname!) of this column "
                + "should have the value " + Organization.PROPERTY_CLIENT + ", it currently has "
                + invalidClientName);
      }
      if (hasOrgReference && !hasValidOrg && !entity.getName().equals("Organization")) {
        result.addError(SystemValidationType.INCORRECT_CLIENT_ORG_PROPERTY_NAME,
            "Table  " + entity.getTableName() + " has a column referencing AD_Org. "
                + " The AD_Column.name (note: different from AD_Column.columnname!) of this column "
                + "should have the value " + Client.PROPERTY_ORGANIZATION + ", it currently has "
                + invalidOrgName);
      }
    }
  }

  /**
   * Checks DB objects naming rules for:
   * <li>Primary Keys
   * <li>Foreign Keys
   * <li>Check Constraints
   * <li>Unique Constraints
   * <li>Indexes
   * 
   */
  private void checkDBObjectsName(SystemValidationResult result) {
    if (getValidateModule() == null) {
      return;
    }

    for (org.apache.ddlutils.model.Table table : getDatabase().getTables()) {
      // Primary Key
      if (table.getPrimaryKey() != null && !table.getPrimaryKey().equals("")
          && !nameStartsByDBPrefix(table.getPrimaryKey())) {
        String errorMsg = "Table  " + table.getName() + " has primary key named "
            + table.getPrimaryKey()
            + ", which does not start with the database prefix of the module.";
        if (isDbsmExecution()) {
          result.addWarning(SystemValidationType.INCORRECT_PK_NAME, errorMsg);
        } else {
          result.addError(SystemValidationType.INCORRECT_PK_NAME, errorMsg);
        }
      }

      // Foreign Key
      for (ForeignKey fk : table.getForeignKeys()) {
        if (!nameStartsByDBPrefix(fk.getName())) {
          String errorMsg = "Table  " + table.getName() + " has foreign key named " + fk.getName()
              + ", which does not starts by module's DBPrefix.";
          if (isDbsmExecution()) {
            result.addWarning(SystemValidationType.INCORRECT_FK_NAME, errorMsg);
          } else {
            result.addError(SystemValidationType.INCORRECT_FK_NAME, errorMsg);
          }
        }
      }

      // Check constraints
      for (Check check : table.getChecks()) {
        if (!nameStartsByDBPrefix(check.getName())) {
          String errorMsg = "Table  " + table.getName() + " has check constraint key named "
              + check.getName() + ", which does not starts by module's DBPrefix.";
          if (isDbsmExecution()) {
            result.addWarning(SystemValidationType.INCORRECT_CHECK_NAME, errorMsg);
          } else {
            result.addError(SystemValidationType.INCORRECT_CHECK_NAME, errorMsg);
          }
        }
      }

      // Unique constraints
      for (Unique unique : table.getuniques()) {
        if (!nameStartsByDBPrefix(unique.getName())) {
          String errorMsg = "Table  " + table.getName() + " has unique constraint key named "
              + unique.getName() + ", which does not starts by module's DBPrefix.";
          if (isDbsmExecution()) {
            result.addWarning(SystemValidationType.INCORRECT_UNIQUE_NAME, errorMsg);
          } else {
            result.addError(SystemValidationType.INCORRECT_UNIQUE_NAME, errorMsg);
          }
        }
      }

      // Indexes
      for (Index index : table.getIndices()) {
        if (!nameStartsByDBPrefix(index.getName())) {
          String errorMsg = "Table  " + table.getName() + " has index named " + index.getName()
              + ", which does not starts by module's DBPrefix.";
          if (isDbsmExecution()) {
            result.addWarning(SystemValidationType.INCORRECT_INDEX_NAME, errorMsg);
          } else {
            result.addError(SystemValidationType.INCORRECT_INDEX_NAME, errorMsg);
          }
        }
      }
    }
  }

  private boolean nameStartsByDBPrefix(String name) {
    if (name.toUpperCase().startsWith("EM_")) {
      // belongs to another module
      return true;
    }
    for (ModuleDBPrefix dbprefix : getValidateModule().getModuleDBPrefixList()) {
      if (name.toUpperCase().startsWith(dbprefix.getName().toUpperCase() + "_")) {
        return true;
      }
    }
    return false;
  }

  public Database getDatabase() {
    return database;
  }

  public void setDatabase(Database database) {
    this.database = database;
  }

  public Module getValidateModule() {
    return validateModule;
  }

  public void setValidateModule(Module module) {
    this.validateModule = module;
  }

  public boolean isDbsmExecution() {
    return dbsmExecution;
  }

  public void setDbsmExecution(boolean dbsmExecution) {
    this.dbsmExecution = dbsmExecution;
  }

  /*
   * Check at least one field is visible in grid view
   */
  public void checkFieldsInGridView(Table adTable, SystemValidationResult result) {
    OBCriteria<Tab> tabCriteria = OBDal.getInstance().createCriteria(Tab.class);
    tabCriteria.addEqual(Tab.PROPERTY_TABLE, adTable);
    for (Tab tab : tabCriteria.list()) {
      if ("Field Sequence".equals(tab.getName()) || ("Grid Sequence".equals(tab.getName()))) {
        continue;
      }
      OBCriteria<Field> fieldCriteria = OBDal.getInstance().createCriteria(Field.class);
      fieldCriteria.addEqual(Field.PROPERTY_TAB, tab);
      fieldCriteria.addEqual(Field.PROPERTY_SHOWINGRIDVIEW, true);
      if (fieldCriteria.count() == 0) {
        result.addError(SystemValidationType.NOFIELDSINGRIDVIEW,
            "No Fields are visible in grid view for Tab " + tab.getWindow().getName() + " - "
                + tab.getName());
      }
    }

  }

  /**
   * Checks that java background killable process are truly killable.
   * 
   * If a background process has the killable check-box marked the java process associated must
   * implement the KillableProcess interface.
   */
  private void checkKillableImplementation(SystemValidationResult result) {
    OBCriteria<org.openbravo.model.ad.ui.Process> obc = OBDal.getInstance()
        .createCriteria(org.openbravo.model.ad.ui.Process.class);
    obc.addEqual(org.openbravo.model.ad.ui.Process.PROPERTY_KILLABLE, true);
    // FIXME: Remove when https://issues.openbravo.com/view.php?id=41753 is fixed
    obc.addNotEqual(org.openbravo.model.ad.ui.Process.PROPERTY_MODULE + ".id",
        FRENCHFISCAL_MODULE);
    if (validateModule != null) {
      obc.addEqual(org.openbravo.model.ad.ui.Process.PROPERTY_MODULE + ".id",
          validateModule.getId());
    }
    List<org.openbravo.model.ad.ui.Process> processList = obc.list();
    for (org.openbravo.model.ad.ui.Process process : processList) {
      try {
        Class<?> processClass = Class.forName(process.getJavaClassName(), false,
            this.getClass().getClassLoader());
        if (!KillableProcess.class.isAssignableFrom(processClass)) {
          result.addWarning(SystemValidationType.KILLABLENOTIMPLEMENTED, "The process "
              + process.getIdentifier()
              + " is marked as killable so the javaclass associated must implement the KillableProcess interface");
        }
      } catch (ClassNotFoundException e) {
        result.addWarning(SystemValidationType.KILLABLECLASSNOTFOUND,
            "Error trying to obtain the class for process " + process.getIdentifier());
      }

    }
  }
}
