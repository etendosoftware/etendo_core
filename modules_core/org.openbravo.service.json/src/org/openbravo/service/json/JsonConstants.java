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
package org.openbravo.service.json;

/**
 * Defines constants used for parameters in the {@link JsonRestServlet} class.
 * 
 * Many constant names are derived from SmartClient parameter names using in fetch operations of
 * datasources.
 * 
 * @author mtaal
 */
public class JsonConstants {

  public static final String JSON_CONTENT_TYPE = "application/json;charset=UTF-8";

  public static final String JSON_REST_URL_NAME_PARAM = "mapping_name";

  public static final String MAIN_ALIAS = "e";

  // these constants are used as request parameters
  public static final String STARTROW_PARAMETER = "_startRow";
  // note is used hardcoded in WAD generation of the selector
  public static final String ADDITIONAL_PROPERTIES_PARAMETER = "_extraProperties";

  /**
   * Contains the id of the organization, the queried information should be accessible from this
   * organization.
   */
  public static final String ORG_PARAMETER = "_org";
  public static final String CALCULATE_ORGS = "_calculateOrgs";

  public static final String NO_ACTIVE_FILTER = "_noActiveFilter";
  public static final String ONLYCOUNT_PARAMETER = "_onlyCount";
  public static final String NOCOUNT_PARAMETER = "_noCount";
  public static final String OR_EXPRESSION_PARAMETER = "_OrExpression";

  public static final String USE_ALIAS = "_use_alias";

  public static final String FILTERBYPARENTPROPERTY_PARAMETER = "_filterByParentProperty";
  public static final String WHERE_PARAMETER = "_where";
  public static final String FILTER_APPLIED_PARAMETER = "isImplicitFilterApplied";
  public static final String SELECTEDPROPERTIES_PARAMETER = "_selectedProperties";
  public static final String DISTINCT_PARAMETER = "_distinct";
  public static final String SHOW_FK_DROPDOWN_UNFILTERED_PARAMETER = "_showFkDropdownUnfiltered";
  public static final String SUMMARY_PARAMETER = "_summary";
  public static final String ORDERBY_PARAMETER = "_orderBy";
  public static final String FILTER_PARAMETER = "_filter";
  public static final String ENDROW_PARAMETER = "_endRow";
  public static final String SORTBY_PARAMETER = "_sortBy";
  public static final String TARGETRECORDID_PARAMETER = "_targetRecordId";
  public static final String TAB_PARAMETER = "tabId";
  public static final String DATASOURCE_PARAMETER = "_dataSource";
  public static final String TEXTMATCH_PARAMETER = "_textMatchStyle";
  public static final String TEXTMATCH_PARAMETER_OVERRIDE = "_textMatchStyleOverride";
  public static final String TEXTMATCH_EXACT = "exact";
  public static final String TEXTMATCH_STARTSWITH = "startsWith";
  public static final String TEXTMATCH_SUBSTRING = "substring";
  public static final String DISPLAYFIELD_PARAMETER = "displayProperty";
  public static final String CSRF_TOKEN_PARAMETER = "csrfToken";

  // if this parameter is passed then if a new object already has an id then
  // that id is set back in the json which is returned together with the
  // the new id value. The client/user can then determine himself how to handle
  // changes in id values
  public static final String SEND_ORIGINAL_ID_BACK = "sendOriginalIDBack";
  public static final String ORIGINAL_ID = "_originalId";

  // these constants are used in response
  public static final String RESPONSE_STATUS = "status";

  /**
   * @deprecated has been replaced by {@link #RESPONSE_STARTROW}
   */
  @Deprecated
  public static final String RESPONSE_STARTROWS = "startRows";

  public static final String RESPONSE_STARTROW = "startRow";
  public static final String RESPONSE_ENDROW = "endRow";
  public static final String RESPONSE_TOTALROWS = "totalRows";
  public static final String RESPONSE_ERROR = "error";
  public static final String RESPONSE_ERRORS = "errors";
  public static final String RESPONSE_ERRORMESSAGE = "errorMessage";
  public static final String RESPONSE_DATA = "data";
  public static final String RESPONSE_RESPONSE = "response";

  public static final int RPCREQUEST_STATUS_SUCCESS = 0;
  public static final int RPCREQUEST_STATUS_FAILURE = -1;
  public static final int RPCREQUEST_STATUS_VALIDATION_ERROR = -4;
  public static final int RPCREQUEST_STATUS_LOGIN_INCORRECT = -5;
  public static final int RPCREQUEST_STATUS_MAX_LOGIN_ATTEMPTS_EXCEEDED = -6;
  public static final int RPCREQUEST_STATUS_LOGIN_REQUIRED = -7;
  public static final int RPCREQUEST_STATUS_LOGIN_SUCCESS = -8;

  public static final String IDENTIFIER = "_identifier";
  public static final String ENTITYNAME = "_entityName";
  public static final String ID = "id";
  public static final String REF = "$ref";
  public static final String ACTIVE = "active";
  public static final String DATA = "data";
  public static final String NEW_INDICATOR = "_new";

  public static final String IN_PARAMETER_SEPARATOR = "__;__";

  public static final String SESSION_PARAM_TRANSACTIONALRANGE = "Transactional$Range".toUpperCase();

  public static final String QUERY_PARAM_CLIENT = "@client@";
  public static final String QUERY_PARAM_USER = "@user@";
  public static final String QUERY_PARAM_TRANSACTIONAL_RANGE = "@transactionalRange@";

  public static final String IS_WS_CALL = "_isWsCall";
  public static final String IS_PICK_AND_EDIT = "_isPickAndEdit";
  public static final String WHERE_AND_FILTER_CLAUSE = "whereAndFilterClause";
  public static final String TABLE_ID = "tableId";
  public static final String WHERE_CLAUSE_HAS_BEEN_CHECKED = "whereClauseHasBeenChecked";
  public static final String WINDOW_ID = "windowId";
  public static final String DATASOURCE_NAME = "dataSourceName";

  public static final String IS_MULTI_SELECTOR = "isMultiSelector";

  public static final String UNDEFINED = "undefined";
  public static final String NULL = "null";
  public static final String FIELD_SEPARATOR = "$";

  public static final int PAE_DATA_PAGE_SIZE = 100;
  public static final int PAE_MAX_PAGE_SIZE = 300;
}
