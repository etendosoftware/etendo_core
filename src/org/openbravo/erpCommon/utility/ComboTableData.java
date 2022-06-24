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
 * All portions are Copyright (C) 2001-2018 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.utility;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.data.FieldProvider;
import org.openbravo.data.Sqlc;
import org.openbravo.data.UtilSql;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.model.ad.datamodel.Column;
import org.openbravo.model.ad.ui.Field;
import org.openbravo.reference.Reference;
import org.openbravo.reference.ui.UIReference;
import org.openbravo.service.db.DalConnectionProvider;

/**
 * @author Fernando Iriazabal
 * 
 *         This class builds the queries for populating the different kind of combos in the
 *         application.
 */
public class ComboTableData {
  private static Logger log4j = LogManager.getLogger();

  public static final String CLIENT_LIST_PARAM_HOLDER = "__CLIENT_LIST__";
  public static final String ORG_LIST_PARAM_HOLDER = "__ORG_LIST__";

  private static final String INTERNAL_PREFIX = "@@";
  private static final String FIELD_CONCAT = " || ' - ' || ";
  private static final String INACTIVE_DATA = "**";

  private Map<String, String> parameters = new HashMap<>();
  private List<QueryParameterStructure> paramSelect = new ArrayList<>();
  private List<QueryParameterStructure> paramFrom = new ArrayList<>();
  private List<QueryParameterStructure> paramWhere = new ArrayList<>();
  private List<QueryParameterStructure> paramOrderBy = new ArrayList<>();
  private List<QueryFieldStructure> select = new ArrayList<>();
  private List<QueryFieldStructure> from = new ArrayList<>();
  private List<QueryFieldStructure> where = new ArrayList<>();
  private List<QueryFieldStructure> orderBy = new ArrayList<>();
  private boolean canBeCached;
  public int index = 0;
  private String windowId;
  private int accessLevel;
  private boolean allowedCrossOrgReference = false;

  /** Creates a new combo for a given field */
  public static ComboTableData getTableComboDataFor(Field field) throws Exception {
    Column col = field.getColumn();
    String ref = col.getReference().getId();
    String objectReference = "";
    if (col.getReferenceSearchKey() != null) {
      objectReference = col.getReferenceSearchKey().getId();
    }
    String validation = "";
    if (col.getValidation() != null) {
      validation = col.getValidation().getId();
    }
    Entity entity = ModelProvider.getInstance().getEntityByTableId(col.getTable().getId());
    Property property = entity.getPropertyByColumnName(col.getDBColumnName(), false);
    boolean allowedCrossOrgReference = property != null && property.isAllowedCrossOrgReference();

    ComboTableData ctd = new ComboTableData(null, null, ref, col.getDBColumnName(), objectReference,
        validation, null, null, 0, allowedCrossOrgReference);

    ctd.windowId = field.getTab().getWindow().getId();
    ctd.accessLevel = entity.getAccessLevel().getDbValue();

    return ctd;
  }

  /**
   * Constructor
   * 
   * @param _conn
   *          Object with the database connection methods.
   * @param _referenceType
   *          String with the type of reference.
   * @param _name
   *          String with the Object name.
   * @param _objectReference
   *          String with id to the reference value.
   * @param _validation
   *          String with the id to the validation.
   * @param _orgList
   *          String with the list of granted organizations.
   * @param _clientList
   *          String with the list of granted clients.
   * @param _index
   *          String with the id of the default value for the combo.
   * @throws Exception
   */
  public ComboTableData(ConnectionProvider _conn, String _referenceType, String _name,
      String _objectReference, String _validation, String _orgList, String _clientList, int _index)
      throws Exception {
    this(null, _conn, _referenceType, _name, _objectReference, _validation, _orgList, _clientList,
        _index);
  }

  /**
   * Constructor
   * 
   * @param _vars
   *          Object with the session methods.
   * @param _conn
   *          Object with the database connection methods.
   * @param _referenceType
   *          String with the type of reference.
   * @param _name
   *          String with the Object name.
   * @param _objectReference
   *          String with id to the reference value.
   * @param _validation
   *          String with the id to the validation.
   * @param _orgList
   *          String with the list of granted organizations.
   * @param _clientList
   *          String with the list of granted clients.
   * @param _index
   *          String with the id of the default value for the combo.
   * @throws Exception
   */
  public ComboTableData(VariablesSecureApp _vars, ConnectionProvider _conn, String _referenceType,
      String _name, String _objectReference, String _validation, String _orgList,
      String _clientList, int _index) throws Exception {
    this(_vars, _conn, _referenceType, _name, _objectReference, _validation, _orgList, _clientList,
        _index, false);
  }

  private ComboTableData(VariablesSecureApp _vars, ConnectionProvider _conn, String _referenceType,
      String _name, String _objectReference, String _validation, String _orgList,
      String _clientList, int _index, boolean allowedCrossOrgReference) throws Exception {
    this.allowedCrossOrgReference = allowedCrossOrgReference;
    setReferenceType(_referenceType);
    setObjectName(_name);
    setObjectReference(_objectReference);
    setValidation(_validation);
    setOrgList(_orgList);
    setClientList(_clientList);
    setIndex(_index);
    generateSQL();
    parseNames();
  }

  public ComboTableData() {
  }

  /**
   * Getter for the session object.
   * 
   * @return Session object.
   */
  public VariablesSecureApp getVars() {
    return RequestContext.get().getVariablesSecureApp();
  }

  /**
   * Getter for the database handler object.
   * 
   * @return Database handler object.
   */
  public ConnectionProvider getPool() {
    return new DalConnectionProvider(false);
  }

  /**
   * Setter for the reference type id.
   * 
   * @param _reference
   *          String with the new reference
   * @throws Exception
   */
  private void setReferenceType(String _reference) throws Exception {
    String localReference = _reference;
    if (localReference != null && !localReference.equals("")) {
      try {
        Integer.valueOf(localReference).intValue();
      } catch (Exception ignore) {
        if (!Utility.isUUIDString(localReference)) {
          localReference = ComboTableQueryData.getBaseReferenceID(getPool(), localReference);
        }
      }
    }
    setParameter(INTERNAL_PREFIX + "reference", localReference);
  }

  /**
   * Getter for the reference type id.
   * 
   * @return String with the reference type id.
   */
  private String getReferenceType() {
    return getParameter(INTERNAL_PREFIX + "reference");
  }

  /**
   * Setter for the object name.
   * 
   * @param _name
   *          String with the new object name.
   * @throws Exception
   */
  private void setObjectName(String _name) throws Exception {
    setParameter(INTERNAL_PREFIX + "name", _name);
  }

  /**
   * Getter for the object name.
   * 
   * @return String with the object name.
   */
  public String getObjectName() {
    return getParameter(INTERNAL_PREFIX + "name");
  }

  /**
   * Setter for the object reference id.
   * 
   * @param _reference
   *          String with the new object reference id.
   * @throws Exception
   */
  private void setObjectReference(String _reference) throws Exception {
    String localReference = _reference;
    if (localReference != null && !localReference.equals("")) {
      try {
        Integer.valueOf(localReference).intValue();
      } catch (Exception ignore) {
        if (!Utility.isUUIDString(localReference)) {
          // Looking reference by name! This shouldn't be used, name is prone to change. It only
          // looks in core names
          localReference = ComboTableQueryData.getReferenceID(getPool(), localReference,
              getReferenceType());
          if (localReference == null || localReference.equals("")) {
            throw new OBException(
                OBMessageUtils.messageBD("ReferenceNotFound") + " " + localReference);
          }
        }
      }
    }
    setParameter(INTERNAL_PREFIX + "objectReference", localReference);
  }

  /**
   * Getter for the object reference id.
   * 
   * @return String with the object reference id.
   */
  public String getObjectReference() {
    return getParameter(INTERNAL_PREFIX + "objectReference");
  }

  /**
   * Setter for the validation id.
   * 
   * @param _reference
   *          String for the new validation id.
   * @throws Exception
   */
  private void setValidation(String _reference) throws Exception {
    String localReference = _reference;
    if (localReference != null && !localReference.equals("")) {
      try {
        Integer.valueOf(localReference).intValue();
      } catch (Exception ignore) {
        if (!Utility.isUUIDString(localReference)) {
          localReference = ComboTableQueryData.getValidationID(getPool(), localReference);
        }
      }
    }
    setParameter(INTERNAL_PREFIX + "validation", localReference);
  }

  /**
   * Getter for the validation id.
   * 
   * @return String with the validation id.
   */
  private String getValidation() {
    return getParameter(INTERNAL_PREFIX + "validation");
  }

  /**
   * Setter for the granted organizations list.
   * 
   * @param _orgList
   *          String with the new granted organizations list.
   * @throws Exception
   */
  private void setOrgList(String _orgList) throws Exception {
    setParameter(INTERNAL_PREFIX + "orgList", _orgList);
  }

  /**
   * Getter for the granted organizations list.
   * 
   * @return String with the granted organizations list.
   */
  public String getOrgList() {
    String cachedList = getParameter(INTERNAL_PREFIX + "orgList");
    if (cachedList != null) {
      return cachedList;
    } else if ("AD_CLIENT_ID".equalsIgnoreCase(getObjectName())) {
      return null;
    }

    VariablesSecureApp vars = getVars();
    if ("AD_ORG_ID".equalsIgnoreCase(getObjectName())) {
      return Utility.getContext(new DalConnectionProvider(false), vars, "#User_Org", windowId,
          accessLevel);
    } else {
      return Utility.getReferenceableOrg(vars, vars.getStringParameter("inpadOrgId"));
    }
  }

  /**
   * Setter for the granted clients list.
   * 
   * @param _clientList
   *          String with the new granted clients list.
   * @throws Exception
   */
  private void setClientList(String _clientList) throws Exception {
    setParameter(INTERNAL_PREFIX + "clientList", _clientList);
  }

  /**
   * Getter for the granted clients list.
   * 
   * @return String with the granted clients list.
   */
  public String getClientList() {
    String clientList = getParameter(INTERNAL_PREFIX + "clientList");
    if (clientList != null) {
      return clientList;
    }

    VariablesSecureApp vars = getVars();
    if ("AD_CLIENT_ID".equalsIgnoreCase(getObjectName())) {
      clientList = Utility.getContext(new DalConnectionProvider(false), vars, "#User_Client",
          windowId, accessLevel);
      if (clientList == null) {
        clientList = vars.getSessionValue("#User_Client");
      }
    } else {
      clientList = Utility.getContext(new DalConnectionProvider(false), vars, "#User_Client",
          windowId);
    }

    return clientList;
  }

  /**
   * Adds new field to the select section of the query.
   * 
   * @param _field
   *          String with the field.
   * @param _alias
   *          String with the alias for this field.
   */
  public void addSelectField(String _field, String _alias) {
    QueryFieldStructure p = new QueryFieldStructure(_field, " AS ", _alias, "SELECT");
    if (this.select == null) {
      this.select = new ArrayList<QueryFieldStructure>();
    }
    select.add(p);
  }

  private List<QueryFieldStructure> getSelectFields() {
    return this.select;
  }

  /**
   * Adds new field to the from section of the query.
   * 
   * @param _field
   *          String with the field.
   * @param _alias
   *          String with the alias for the field.
   */
  public void addFromField(String _field, String _alias) {
    QueryFieldStructure p = new QueryFieldStructure(_field, " ", _alias, "FROM");
    if (this.from == null) {
      this.from = new ArrayList<QueryFieldStructure>();
    }
    from.add(p);
  }

  private List<QueryFieldStructure> getFromFields() {
    return this.from;
  }

  /**
   * Adds new field to the where section of the query.
   * 
   * @param _field
   *          String with the field.
   * @param _type
   *          String for group fields.
   */
  public void addWhereField(String _field, String _type) {
    QueryFieldStructure p = new QueryFieldStructure(_field, "", "", _type);
    if (this.where == null) {
      this.where = new ArrayList<QueryFieldStructure>();
    }
    where.add(p);
  }

  private List<QueryFieldStructure> getWhereFields() {
    return this.where;
  }

  /**
   * Adds new field to the order by section of the query.
   * 
   * @param _field
   *          String with the field.
   */
  public void addOrderByField(String _field) {
    QueryFieldStructure p = new QueryFieldStructure(_field, "", "", "ORDERBY");
    if (this.orderBy == null) {
      this.orderBy = new ArrayList<QueryFieldStructure>();
    }
    orderBy.add(p);
  }

  private List<QueryFieldStructure> getOrderByFields() {
    return this.orderBy;
  }

  private List<QueryParameterStructure> getSelectParameters() {
    return this.paramSelect;
  }

  /**
   * Adds a new parameter to the from section of the query.
   * 
   * @param _parameter
   *          String with the parameter.
   * @param _fieldName
   *          String with the name od the field.
   */
  public void addFromParameter(String _parameter, String _fieldName) {
    if (this.paramFrom == null) {
      this.paramFrom = new ArrayList<QueryParameterStructure>();
    }
    QueryParameterStructure aux = new QueryParameterStructure(_parameter, _fieldName, "FROM");
    paramFrom.add(aux);
  }

  private List<QueryParameterStructure> getFromParameters() {
    return this.paramFrom;
  }

  /**
   * Adds a new parameter to the where section of the query.
   * 
   * @param _parameter
   *          String with the parameter.
   * @param _fieldName
   *          String with the name of the field.
   * @param _type
   *          String with a group name.
   */
  public void addWhereParameter(String _parameter, String _fieldName, String _type) {
    if (this.paramWhere == null) {
      this.paramWhere = new ArrayList<QueryParameterStructure>();
    }
    QueryParameterStructure aux = new QueryParameterStructure(_parameter, _fieldName, _type);
    paramWhere.add(aux);
  }

  private List<QueryParameterStructure> getWhereParameters() {
    return this.paramWhere;
  }

  /**
   * Adds a new parameter to the order by section of the query.
   * 
   * @param _parameter
   *          String with the parameter.
   * @param _fieldName
   *          String with the name of the field.
   */
  private void addOrderByParameter(String _parameter, String _fieldName) {
    if (this.paramOrderBy == null) {
      this.paramOrderBy = new ArrayList<QueryParameterStructure>();
    }
    QueryParameterStructure aux = new QueryParameterStructure(_parameter, _fieldName, "ORDERBY");
    paramOrderBy.add(aux);
  }

  private List<QueryParameterStructure> getOrderByParameters() {
    return this.paramOrderBy;
  }

  /**
   * Setter for the parameters value.
   * 
   * @param name
   *          The name of the field defined for the parameter.
   * @param value
   *          The value for this parameter.
   * @throws Exception
   */
  public void setParameter(String name, String value) throws Exception {
    if (name == null || name.equals("")) {
      throw new Exception("Invalid parameter name");
    }
    if (this.parameters == null) {
      this.parameters = new HashMap<String, String>();
    }
    if (value == null || value.equals("")) {
      this.parameters.remove(name.toUpperCase());
    } else {
      this.parameters.put(name.toUpperCase(), value);
    }
  }

  /**
   * Getter for the parameters value.
   * 
   * @param name
   *          The name of the field defined for the parameter.
   * @return String with the value.
   */
  private String getParameter(String name) {
    if (name == null || name.equals("")) {
      return "";
    } else if (this.parameters == null) {
      return "";
    } else {
      return this.parameters.get(name.toUpperCase());
    }
  }

  /** Gets the values for all of the defined parameters in the query. */
  private List<String> getParameters() {
    List<String> result = new ArrayList<>();
    if (log4j.isDebugEnabled()) {
      log4j.debug("Obtaining parameters");
    }
    List<QueryParameterStructure> vAux = getSelectParameters();
    if (vAux != null) {
      for (QueryParameterStructure aux : vAux) {
        String strAux = getParameter(aux.getName());
        if (strAux == null || strAux.equals("")) {
          result.add(aux.getName());
        }
      }
    }
    if (log4j.isDebugEnabled()) {
      log4j.debug("Select parameters obtained");
    }
    vAux = getFromParameters();
    if (vAux != null) {
      for (QueryParameterStructure aux : vAux) {
        String strAux = getParameter(aux.getName());
        if (strAux == null || strAux.equals("")) {
          result.add(aux.getName());
        }
      }
    }
    if (log4j.isDebugEnabled()) {
      log4j.debug("From parameters obtained");
    }
    vAux = getWhereParameters();
    if (vAux != null) {
      for (QueryParameterStructure aux : vAux) {
        String strAux = getParameter(aux.getName());
        if (strAux == null || strAux.equals("")) {
          result.add(aux.getName());
        }
      }
    }
    if (log4j.isDebugEnabled()) {
      log4j.debug("Where parameters obtained");
    }
    vAux = getOrderByParameters();
    if (vAux != null) {
      for (QueryParameterStructure aux : vAux) {
        String strAux = getParameter(aux.getName());
        if (strAux == null || strAux.equals("")) {
          result.add(aux.getName());
        }
      }
    }
    if (log4j.isDebugEnabled()) {
      log4j.debug("Order by parameters obtained");
    }
    result.add("#AD_LANGUAGE");
    return result;
  }

  /**
   * Setter for the table alias index.
   * 
   * @param _index
   *          Integer with the new index.
   */
  private void setIndex(int _index) {
    this.index = _index;
  }

  /**
   * Main method to build the query.
   * 
   * @throws Exception
   */
  private void generateSQL() throws Exception {
    if (getPool() == null) {
      throw new Exception("No pool defined for database connection");
    } else if (getReferenceType().equals("")) {
      throw new Exception("No reference type defined");
    }

    identifier("", null);
  }

  /**
   * Method to fix the names of the fields. Searchs all the fields in the where clause and order by
   * clause to change the names with correct aliases. This intends to fix the problem of the names
   * in the whereclauses, filterclauses and orderbyclauses fields of the tab's table, where the user
   * doesn´t know the alias of the referenced field.
   */
  private void parseNames() {
    List<QueryFieldStructure> tables = getFromFields();
    if (tables == null || tables.size() == 0) {
      return;
    }
    if (where != null && where.size() > 0) {
      int i = 0;
      for (QueryFieldStructure auxStructure : where) {
        if (auxStructure.getType().equalsIgnoreCase("FILTER")) {
          String strAux = auxStructure.getField();
          for (QueryFieldStructure auxTable : tables) {
            String strTable = auxTable.getField();
            int p = strTable.indexOf(" ");
            if (p != -1) {
              strTable = strTable.substring(0, p).trim();
            }
            strAux = replaceIgnoreCase(strAux, strTable + ".", auxTable.getAlias() + ".");
          }
          if (!strAux.equalsIgnoreCase(auxStructure.getField())) {
            auxStructure.setField(strAux);
            if (log4j.isDebugEnabled()) {
              log4j.debug("Field replaced: " + strAux);
            }
            where.set(i, auxStructure);
          }
        }
        i++;
      }
    }
    if (orderBy != null && orderBy.size() > 0) {
      int i = 0;
      for (QueryFieldStructure auxStructure : orderBy) {
        String strAux = auxStructure.getField();
        for (QueryFieldStructure auxTable : tables) {
          String strTable = auxTable.getField();
          int p = strTable.indexOf(" ");
          if (p != -1) {
            strTable = strTable.substring(0, p).trim();
          }
          strAux = replaceIgnoreCase(strAux, strTable + ".", auxTable.getAlias() + ".");
        }
        if (!strAux.equalsIgnoreCase(auxStructure.getField())) {
          auxStructure.setField(strAux);
          if (log4j.isDebugEnabled()) {
            log4j.debug("Field replaced: " + strAux);
          }
          orderBy.set(i, auxStructure);
        }
        i++;
      }
    }
  }

  /**
   * Auxiliar method to make a replace ignoring the case.
   * 
   * @param data
   *          String with the text.
   * @param replaceWhat
   *          The string to search.
   * @param replaceWith
   *          The new string to replace with.
   * @return String with the text replaced.
   */
  private String replaceIgnoreCase(String data, String replaceWhat, String replaceWith) {
    String localData = data;
    if (localData == null || localData.equals("")) {
      return "";
    }
    if (log4j.isDebugEnabled()) {
      log4j.debug(
          "parsing data: " + localData + " - replace: " + replaceWhat + " - with: " + replaceWith);
    }
    StringBuilder text = new StringBuilder();
    int i = localData.toUpperCase().indexOf(replaceWhat.toUpperCase());
    while (i != -1) {
      text.append(localData.substring(0, i)).append(replaceWith);
      localData = localData.substring(i + replaceWhat.length());
      i = localData.toUpperCase().indexOf(replaceWhat.toUpperCase());
    }
    text.append(localData);
    return text.toString();
  }

  /**
   * Parse the validation string searching the @ elements and replacing them with the correct
   * values, adding the needed parameters.
   * 
   * @throws Exception
   */
  public void parseValidation() throws Exception {
    if (getValidation() == null || getValidation().equals("")) {
      return;
    }
    if (log4j.isDebugEnabled()) {
      log4j.debug("Validation id: " + getValidation());
    }
    String val = ComboTableQueryData.getValidation(getPool(), getValidation());
    if (log4j.isDebugEnabled()) {
      log4j.debug("Validation text: " + val);
    }
    if (val.indexOf("@") != -1) {
      val = parseContext(val, "WHERE");
    }
    if (!val.equals("")) {
      addWhereField("(" + val + ")", "FILTER");
    }
    if (log4j.isDebugEnabled()) {
      log4j.debug("Validation parsed: " + val);
    }
  }

  /**
   * Auxiliar method to replace the variable sections of the clauses.
   * 
   * @param context
   *          String with the variable.
   * @param type
   *          String with the type of the clause (WHERE, ORDER...)
   * @return String with the text replaced.
   */
  public String parseContext(String context, String type) {
    if (context == null || context.equals("")) {
      return "";
    }
    StringBuilder strOut = new StringBuilder();
    String value = context;
    String token, defStr;
    int i = value.indexOf("@");
    while (i != -1) {
      strOut.append(value.substring(0, i));
      value = value.substring(i + 1);
      int j = value.indexOf("@");
      if (j == -1) {
        strOut.append(value);
        return strOut.toString();
      }
      token = value.substring(0, j);
      if (token.equalsIgnoreCase("#User_Client")) {
        defStr = CLIENT_LIST_PARAM_HOLDER;
      } else if (token.equalsIgnoreCase("#User_Org")) {
        defStr = ORG_LIST_PARAM_HOLDER;
      } else {
        defStr = "?";
      }

      if (defStr.equals("?")) {
        if (type.equalsIgnoreCase("WHERE")) {
          addWhereParameter(token, "FILTER", "FILTER");
        } else if (type.equalsIgnoreCase("ORDERBY")) {
          addOrderByParameter(token, "FILTER");
        }
      }
      strOut.append(defStr);
      value = value.substring(j + 1);
      i = value.indexOf("@");
    }
    strOut.append(value);
    return strOut.toString().replace("'?'", "?");
  }

  /**
   * Support method for the generateSQL method, to build the query.
   * 
   * @param tableName
   *          String with the name of the table.
   * @param field
   *          String with the name of the field.
   * @throws Exception
   */
  public void identifier(String tableName, FieldProvider field) throws Exception {
    UIReference uiref;
    if (field == null) {
      if (getObjectReference() != null && getObjectReference().length() > 0) {
        uiref = Reference.getUIReference(getReferenceType(), getObjectReference());
      } else {
        uiref = Reference.getUIReference(getReferenceType(), null);
      }
    } else {
      uiref = Reference.getUIReference(field.getField("reference"),
          field.getField("referenceValue"));
    }
    uiref.setComboTableDataIdentifier(this, tableName, field);
    canBeCached = uiref.canBeCached();
  }

  /**
   * Returns the generated query.
   * 
   * @param onlyId
   *          Boolean to indicate if the select clause must have only the key field.
   * @param discard
   *          Array of field groups to remove from the query.
   * @param recordId
   *          recordId to be filtered.
   * @param startRow
   *          starting index of the records.
   * @param endRow
   *          end index of the records.
   * @param conn
   *          Connection provider
   * @return String with the query.
   */
  private String getQuery(boolean onlyId, String[] discard, String recordId, Integer startRow,
      Integer endRow, ConnectionProvider conn, boolean applyFilter) {
    StringBuilder text = new StringBuilder();
    List<QueryFieldStructure> aux = getSelectFields();
    String idName = "", nameToCompare = null;
    boolean hasWhere = false;
    boolean applyLimits = (startRow != null && startRow != -1) && (endRow != null && endRow != -1)
        && StringUtils.isEmpty(recordId);
    String rdbms = conn == null ? "" : conn.getRDBMS();
    if (aux != null) {
      StringBuilder name = new StringBuilder();
      String description = "";
      String id = "";
      text.append("SELECT ");
      for (QueryFieldStructure auxStructure : aux) {
        if (!isInArray(discard, auxStructure.getType())) {
          if (auxStructure.getData("alias").equalsIgnoreCase("ID")) {
            if (id.equals("")) {
              id = auxStructure.toString(true);
              idName = auxStructure.toString();
            }
          } else if (auxStructure.getData("alias").equalsIgnoreCase("DESCRIPTION")) {
            if (description.equals("")) {
              description = auxStructure.toString(true);
            }
          } else {
            if (name.toString().equals("")) {
              name.append("(");
            } else {
              name.append(FIELD_CONCAT);
            }
            name.append("COALESCE(TO_CHAR(").append(auxStructure.toString()).append("),'')");
          }
        }
      }
      text.append(id);
      if (!name.toString().equals("")) {
        nameToCompare = name.toString() + ")";
        name.append(") AS NAME");
      } else {
        name.append("'>>No Record Identifier<<' AS NAME");
        log4j.error("Foreign table referenced by '" + idName
            + "' does not have 'Record Identifier' defined");
      }
      text.append(", ").append(name.toString());
      if (description != null && !description.equals("")) {
        text.append(", ").append(description);
      } else {
        text.append(", '' AS DESCRIPTION");
      }
      text.append(" \n");
    }

    aux = getFromFields();
    if (aux != null) {
      StringBuilder txtAux = new StringBuilder();
      text.append("FROM ");
      for (QueryFieldStructure auxStructure : aux) {
        if (!isInArray(discard, auxStructure.getType())) {
          if (!txtAux.toString().equals("")) {
            txtAux.append("left join ");
          }
          txtAux.append(auxStructure.toString()).append(" \n");
        }
      }
      text.append(txtAux.toString());
    }

    aux = getWhereFields();
    String orgList = getOrgList();
    if (aux != null) {
      StringBuilder txtAux = new StringBuilder();
      for (QueryFieldStructure auxStructure : aux) {
        if ("ORG_LIST".equals(auxStructure.getType())
            && StringCollectionUtils.isEmptyCollection(orgList)) {
          continue;
        }
        if (!isInArray(discard, auxStructure.getType())) {
          hasWhere = true;
          if (!txtAux.toString().equals("")) {
            txtAux.append("AND ");
          }
          txtAux.append(auxStructure.toString()).append(" \n");
        }
      }
      if (hasWhere) {
        if (recordId != null) {
          txtAux.append(" AND " + idName + "=(?) ");
        }

        text.append("WHERE ").append(txtAux.toString());
      }
      if (applyFilter && !StringUtils.isEmpty(nameToCompare)) {
        // filtering by value
        text.append(" AND UPPER(" + nameToCompare + ") like UPPER(?)\n");
      }
    }

    if (!onlyId) {
      aux = getOrderByFields();
      if (aux != null) {
        StringBuilder txtAux = new StringBuilder();
        text.append("ORDER BY ");
        for (QueryFieldStructure auxStructure : aux) {
          if (!isInArray(discard, auxStructure.getType())) {
            if (!txtAux.toString().equals("")) {
              txtAux.append(", ");
            }
            txtAux.append(auxStructure.toString());
          }
        }
        text.append(txtAux.toString());
      }
    } else {
      if (!hasWhere) {
        text.append("WHERE ");
      } else {
        text.append("AND ");
      }
      text.append(idName).append(" = ? ");
    }

    if (applyLimits && rdbms.equalsIgnoreCase("POSTGRE")) {
      int numberOfRows = endRow - startRow + 1;
      text.append(" LIMIT " + numberOfRows + " OFFSET " + startRow);
    }

    String query = text.toString().replace(CLIENT_LIST_PARAM_HOLDER, getClientList());
    if (orgList != null && !isAllowedCrossOrgReference()) {
      query = query.replace(ORG_LIST_PARAM_HOLDER, orgList);
    }

    if (applyLimits && rdbms.equalsIgnoreCase("ORACLE")) {
      // in oracle rows are defined from 1, so incrementing startRow and endRow by 1
      String oraQuery = "select * from ( select a.*, ROWNUM rnum from ( " + query
          + ") a where rownum <= " + (endRow + 1) + " ) where rnum >= " + (startRow + 1) + "";
      return oraQuery;
    }
    return query;
  }

  /**
   * Auxiliar method to search a value in an array.
   * 
   * @param data
   *          Array with the data.
   * @param element
   *          String to search in the array.
   * @return Boolean to indicate if the element was found in the array.
   */
  private boolean isInArray(String[] data, String element) {
    if (data == null || data.length == 0 || element == null || element.equals("")) {
      return false;
    }
    for (String d : data) {
      if (d.equalsIgnoreCase(element)) {
        return true;
      }
    }
    return false;
  }

  private int setSQLParameters(PreparedStatement st, Map<String, String> lparameters,
      int iParameter, String[] discard) {
    return setSQLParameters(st, lparameters, iParameter, discard, null);
  }

  private int setSQLParameters(PreparedStatement st, Map<String, String> lparameters,
      int iParameter, String[] discard, String recordId) {
    return setSQLParameters(st, lparameters, iParameter, discard, recordId, null);
  }

  /**
   * Fills the query parameter's values.
   * 
   * @param st
   *          PreparedStatement object.
   * @param iParameter
   *          Index of the parameter.
   * @param discard
   *          Array with the groups to discard.
   * @return Integer with the next parameter's index.
   */
  private int setSQLParameters(PreparedStatement st, Map<String, String> lparameters,
      int iParameter, String[] discard, String recordId, String filter) {
    int localIParameter = iParameter;
    List<QueryParameterStructure> vAux = getSelectParameters();
    if (vAux != null) {
      for (QueryParameterStructure aux : vAux) {
        if (!isInArray(discard, aux.getType())) {
          String strAux = lparameters != null
              ? (aux.getName() == null ? null : lparameters.get(aux.getName().toUpperCase()))
              : getParameter(aux.getName());
          if (log4j.isDebugEnabled()) {
            log4j.debug("Parameter - " + localIParameter + " - " + aux.getName() + ": " + strAux);
          }
          UtilSql.setValue(st, ++localIParameter, 12, null, strAux);
        }
      }
    }
    vAux = getFromParameters();
    if (vAux != null) {
      for (QueryParameterStructure aux : vAux) {
        if (!isInArray(discard, aux.getType())) {
          String strAux = lparameters != null
              ? (aux.getName() == null ? null : lparameters.get(aux.getName().toUpperCase()))
              : getParameter(aux.getName());
          if (log4j.isDebugEnabled()) {
            log4j.debug("Parameter - " + localIParameter + " - " + aux.getName() + ": " + strAux);
          }
          UtilSql.setValue(st, ++localIParameter, 12, null, strAux);
        }
      }
    }
    vAux = getWhereParameters();
    if (vAux != null) {
      for (QueryParameterStructure aux : vAux) {
        if (!isInArray(discard, aux.getType())) {
          String strAux = lparameters != null
              ? (aux.getName() == null ? null : lparameters.get(aux.getName().toUpperCase()))
              : getParameter(aux.getName());
          if (log4j.isDebugEnabled()) {
            log4j.debug("Parameter - " + localIParameter + " - " + aux.getName() + ": " + strAux);
          }
          UtilSql.setValue(st, ++localIParameter, 12, null, strAux);
        }
      }
    }
    if (recordId != null) {
      UtilSql.setValue(st, ++localIParameter, 12, null, recordId);
    }
    if (!StringUtils.isEmpty(filter)) {
      // filtering by value
      UtilSql.setValue(st, ++localIParameter, 12, null, "%" + filter + "%");
    }
    vAux = getOrderByParameters();
    if (vAux != null) {
      for (QueryParameterStructure aux : vAux) {
        if (!isInArray(discard, aux.getType())) {
          String strAux = lparameters != null
              ? (aux.getName() == null ? null : lparameters.get(aux.getName().toUpperCase()))
              : getParameter(aux.getName());
          if (log4j.isDebugEnabled()) {
            log4j.debug("Parameter - " + localIParameter + " - " + aux.getName() + ": " + strAux);
          }
          UtilSql.setValue(st, ++localIParameter, 12, null, strAux);
        }
      }
    }
    return localIParameter;
  }

  /**
   * Executes the query in the database and returns the data.
   * 
   * @param includeActual
   *          Boolean that indicates if the actual selected value must be included in the result,
   *          even if it doesn´t exists in the new query.
   * @return Array of FieldProvider with the data.
   * @throws Exception
   */
  public FieldProvider[] select(boolean includeActual) throws Exception {
    return select(getPool(), null, includeActual);
  }

  public FieldProvider[] select(ConnectionProvider conn, Map<String, String> lparameters,
      boolean includeActual) throws Exception {
    return select(conn, lparameters, includeActual, null, null);
  }

  public FieldProvider[] select(ConnectionProvider conn, Map<String, String> lparameters,
      boolean includeActual, Integer startRow, Integer endRow) throws Exception {
    String actual = lparameters != null ? lparameters.get("@ACTUAL_VALUE@")
        : getParameter("@ACTUAL_VALUE@");
    String filterValue = lparameters != null ? lparameters.get("FILTER_VALUE")
        : getParameter("FILTER_VALUE");
    if (lparameters != null && lparameters.containsKey("@ONLY_ONE_RECORD@")
        && !lparameters.get("@ONLY_ONE_RECORD@").isEmpty()) {
      String strSqlSingleRecord = getQuery(false, null, lparameters.get("@ONLY_ONE_RECORD@"), null,
          null, null, false);
      log4j.debug("Query for single record: " + strSqlSingleRecord);
      PreparedStatement stSingleRecord = conn.getPreparedStatement(strSqlSingleRecord);
      try {
        ResultSet result;
        int iParameter = 0;
        iParameter = setSQLParameters(stSingleRecord, lparameters, iParameter, null,
            lparameters.get("@ONLY_ONE_RECORD@"));
        result = stSingleRecord.executeQuery();
        if (result.next()) {
          SQLReturnObject sqlReturnObject = new SQLReturnObject();
          sqlReturnObject.setData("ID", UtilSql.getValue(result, "ID"));
          sqlReturnObject.setData("NAME", UtilSql.getValue(result, "NAME"));
          sqlReturnObject.setData("DESCRIPTION", UtilSql.getValue(result, "DESCRIPTION"));
          List<Object> vector = new ArrayList<>(1);
          vector.add(sqlReturnObject);
          return vector.toArray(new FieldProvider[vector.size()]);
        }

        if (includeActual && actual != null && !actual.equals("")) {

          String[] discard = { "filter", "orderBy", "CLIENT_LIST", "ORG_LIST" };
          String strSqlDisc = getQuery(true, discard, null, null, null, null, false);
          PreparedStatement stInactive = conn.getPreparedStatement(strSqlDisc);
          iParameter = setSQLParameters(stInactive, lparameters, 0, discard);
          UtilSql.setValue(stInactive, ++iParameter, 12, null, actual);
          ResultSet resultIn = stInactive.executeQuery();
          while (resultIn.next()) {
            SQLReturnObject sqlReturnObject = new SQLReturnObject();
            sqlReturnObject.setData("ID", UtilSql.getValue(resultIn, "ID"));
            String strName = UtilSql.getValue(resultIn, "NAME");
            if (!strName.startsWith(INACTIVE_DATA)) {
              strName = INACTIVE_DATA + strName;
            }
            sqlReturnObject.setData("NAME", strName);
            List<Object> vector = new ArrayList<>(1);
            vector.add(sqlReturnObject);
            return vector.toArray(new FieldProvider[vector.size()]);
          }

        }
      } catch (Exception e) {
        log4j.error("Error in query" + strSqlSingleRecord, e);
        throw e;
      } finally {
        conn.releasePreparedStatement(stSingleRecord);
      }

    }
    String strSql = getQuery(false, null, null, startRow, endRow, conn,
        !StringUtils.isEmpty(filterValue));
    if (log4j.isDebugEnabled()) {
      log4j.debug("SQL: " + strSql);
    }
    PreparedStatement st = conn.getPreparedStatement(strSql);
    ResultSet result;
    List<Object> vector = new ArrayList<>();
    try {
      int iParameter = 0;
      iParameter = setSQLParameters(st, lparameters, iParameter, null, null, filterValue);
      boolean idFound = false;
      result = st.executeQuery();
      while (result.next()) {
        SQLReturnObject sqlReturnObject = new SQLReturnObject();
        sqlReturnObject.setData("ID", UtilSql.getValue(result, "ID"));
        sqlReturnObject.setData("NAME", UtilSql.getValue(result, "NAME"));
        sqlReturnObject.setData("DESCRIPTION", UtilSql.getValue(result, "DESCRIPTION"));
        if (includeActual && actual != null && !actual.equals("")) {
          if (actual.equals(sqlReturnObject.getData("ID"))) {
            if (!idFound) {
              vector.add(sqlReturnObject);
              idFound = true;
            }
          } else {
            vector.add(sqlReturnObject);
          }
        } else {
          vector.add(sqlReturnObject);
        }
        if (lparameters != null && lparameters.containsKey("#ONLY_ONE_RECORD#")) {
          return vector.toArray(new FieldProvider[vector.size()]);
        }
      }
      result.close();

      if (includeActual && actual != null && !actual.equals("") && !idFound) {
        boolean allDataInSinglePage;
        if (startRow != null && endRow != null) {
          allDataInSinglePage = startRow == 0 && vector.size() < endRow - startRow;
        } else {
          // This method is invoked with startRow = endRow = null for lists. Lists always have load
          // all data in a single page
          allDataInSinglePage = true;
        }
        if (!allDataInSinglePage) {
          // retrieved a partial set of data, checking if current id is in a page different that the
          // served applying the same criteria, if so, do not add it again to the list (it will
          // appear in its own page)
          conn.releasePreparedStatement(st);
          strSql = getQuery(true, null, null, 0, 1, conn, !StringUtils.isEmpty(filterValue));
          log4j.debug("SQL to check if actual ID is in another page: " + strSql);
          st = conn.getPreparedStatement(strSql);
          setSQLParameters(st, lparameters, 0, null, actual, filterValue);
          result = st.executeQuery();
          idFound = result.next();
          result.close();
        }
        if (!idFound) {
          conn.releasePreparedStatement(st);
          String[] discard = { "filter", "orderBy", "CLIENT_LIST", "ORG_LIST" };
          strSql = getQuery(true, discard, null, null, null, null, false);
          if (log4j.isDebugEnabled()) {
            log4j.debug("SQL Actual ID: " + strSql);
          }
          st = conn.getPreparedStatement(strSql);
          iParameter = setSQLParameters(st, lparameters, 0, discard);
          UtilSql.setValue(st, ++iParameter, 12, null, actual);
          result = st.executeQuery();
          if (result.next()) {
            SQLReturnObject sqlReturnObject = new SQLReturnObject();
            sqlReturnObject.setData("ID", UtilSql.getValue(result, "ID"));
            String strName = UtilSql.getValue(result, "NAME");
            if (!strName.startsWith(INACTIVE_DATA)) {
              strName = INACTIVE_DATA + strName;
            }
            sqlReturnObject.setData("NAME", strName);
            vector.add(sqlReturnObject);
            idFound = true;
          }
        }
        result.close();
        if (!idFound) {
          SQLReturnObject sqlReturnObject = new SQLReturnObject();
          sqlReturnObject.setData("ID", actual);
          sqlReturnObject.setData("NAME",
              INACTIVE_DATA + Utility.messageBD(conn, "NotFound",
                  lparameters != null ? lparameters.get("#AD_LANGUAGE")
                      : getParameter("#AD_LANGUAGE")));

          vector.add(sqlReturnObject);
        }
      }
    } catch (SQLException e) {
      log4j.error("Error of SQL in query: " + strSql + "Exception:" + e);
      throw new Exception("@CODE=" + Integer.toString(e.getErrorCode()) + "@" + e.getMessage());
    } finally {
      conn.releasePreparedStatement(st);
    }
    return vector.toArray(new FieldProvider[vector.size()]);
  }

  /**
   * Fill the parameters of the sql with the session values or FieldProvider values. Used in the
   * combo fields.
   * 
   * @param data
   *          optional FieldProvider which can be used to get the needed parameter values from. If
   *          the FieldProvider has a filed named after a parameter, then its value will be used if
   *          the value could not be already obtained from the request parameters.
   * @param window
   *          Window id.
   * @param actual_value
   *          actual value for the combo.
   * @throws ServletException
   */
  public void fillParameters(FieldProvider data, String window, String actual_value)
      throws ServletException {
    fillSQLParameters(getPool(), getVars(), data, "", window, actual_value);
  }

  /**
   * Fill the parameters of the sql with the session values or FieldProvider values. Used in the
   * combo fields.
   * 
   * @param conn
   *          Handler for the database connection.
   * @param variables
   *          Handler for the session info.
   * @param data
   *          FieldProvider with the columns values.
   * @param window
   *          Window id.
   * @param actual_value
   *          actual value for the combo.
   * @throws ServletException
   */
  void fillSQLParameters(ConnectionProvider conn, VariablesSecureApp variables, FieldProvider data,
      String tab, String window, String actual_value) throws ServletException {
    final List<String> vAux = getParameters();
    if (vAux != null && vAux.size() > 0) {
      if (log4j.isDebugEnabled()) {
        log4j.debug("Combo Parameters: " + vAux.size());
      }
      for (String strAux : vAux) {
        try {
          final String value = parseParameterValue(conn, variables, data, strAux, tab, window,
              actual_value);
          if (log4j.isDebugEnabled()) {
            log4j.debug("Combo Parameter: " + strAux + " - Value: " + value);
          }
          setParameter(strAux, value);
        } catch (final Exception ex) {
          throw new ServletException(ex);
        }
      }
    }
  }

  public Map<String, String> fillSQLParametersIntoMap(ConnectionProvider conn,
      VariablesSecureApp variables, FieldProvider data, String window, String actual_value)
      throws ServletException {
    final List<String> vAux = getParameters();

    // We first add all current parameters in the combo
    Map<String, String> lparameters = new HashMap<>(parameters);

    if (vAux != null && vAux.size() > 0) {
      if (log4j.isDebugEnabled()) {
        log4j.debug("Combo Parameters: " + vAux.size());
      }
      for (String strAux : vAux) {
        try {
          final String value = parseParameterValue(conn, variables, data, strAux, "", window,
              actual_value);
          if (log4j.isDebugEnabled()) {
            log4j.debug("Combo Parameter: " + strAux + " - Value: " + value);
          }
          if (value == null || value.equals("") || "null".equals(value)) {
            lparameters.remove(strAux.toUpperCase());
          } else {
            lparameters.put(strAux.toUpperCase(), value);
          }
        } catch (final Exception ex) {
          throw new ServletException(ex);
        }
      }
    }
    return lparameters;
  }

  /**
   * Auxiliary method, used by fillSQLParameters and fillTableSQLParameters to get the values for
   * each parameter.
   * 
   * @param conn
   *          Handler for the database connection.
   * @param vars
   *          Handler for the session info.
   * @param data
   *          FieldProvider with the columns values.
   * @param name
   *          Name of the parameter.
   * @param window
   *          Window id.
   * @param actual_value
   *          Actual value.
   * @param fromSearch
   *          If the combo is used from the search popup (servlet). If true, then the pattern for
   *          obtaining the parameter values if changed to conform with the search popup naming.
   * @return String with the parsed parameter.
   * @throws Exception
   */
  private static String parseParameterValue(ConnectionProvider conn, VariablesSecureApp vars,
      FieldProvider data, String name, String tab, String window, String actual_value)
      throws Exception {
    String strAux = null;
    if (name.equalsIgnoreCase("@ACTUAL_VALUE@")) {
      return actual_value;
    }
    if (data != null) {
      strAux = data.getField(name);
    }
    if (strAux == null) {
      strAux = vars.getStringParameter("inp" + Sqlc.TransformaNombreColumna(name));

      if (log4j.isDebugEnabled()) {
        log4j.debug("parseParameterValues - getStringParameter(inp"
            + Sqlc.TransformaNombreColumna(name) + "): " + strAux);
      }

      if ((strAux == null || strAux.equals("")) && name.startsWith("_propertyField_")) {
        // property fields are sent in the request with a different format
        strAux = vars.getStringParameter("inp" + name);
      }

      if (strAux == null || strAux.equals("")) {
        strAux = Utility.getContext(conn, vars, name, window);
      }
    }
    return strAux;
  }

  public boolean canBeCached() {
    return canBeCached;
  }

  /** Returns whether the columns this combo is for allows cross organization references */
  public boolean isAllowedCrossOrgReference() {
    return allowedCrossOrgReference;
  }
}
