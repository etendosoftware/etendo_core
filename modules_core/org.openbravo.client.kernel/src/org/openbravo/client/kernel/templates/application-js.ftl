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

if (window.isc) {
  // do at the beginning
  isc.setAutoDraw(false);
  // Prevent errors in smartclient for screenreader, is quite new and unstable for now
  isc.screenReader = false;
}

// On logging in, create the OB object from scratch.
// But if we are in an old 2.50 window/process, clone the already existing OB object from the parent
var OBLayoutMDI = window.getFrame && window.getFrame('LayoutMDI');
var OB = (OBLayoutMDI && OBLayoutMDI.OB) ? OBLayoutMDI.shallowClone(OBLayoutMDI.OB) : {
    Application : {
        testEnvironment: ${data.testEnvironment?string},
        contextUrl: '${data.contextUrl}',
        communityBrandingStaticUrl: '${data.communityBrandingStaticUrl?js_string}',
        butlerUtilsUrl: '${data.butlerUtilsUrl?js_string}'
    },

    Format : {
        defaultGroupingSize: 3,
        defaultGroupingSymbol: '${data.defaultGroupingSymbol}',
        defaultDecimalSymbol: '${data.defaultDecimalSymbol}',
        defaultNumericMask: '${data.defaultNumericMask}',
        date: '${data.dateFormat}',
        dateTime: '${data.dateTimeFormat}',
        formats: {
        <#list data.formats?keys as key>
          '${key}': '${data.formats[key]}'<#if key_has_next>,</#if>
        </#list>
        }
    },

    Constants : {
        WINTITLE : 'Etendo',
        IDENTIFIER : '_identifier',
        ID : 'id',
        TITLE : 'title',
        FIELDSEPARATOR : '$',
        WHERE_PARAMETER : '_where',
        SQL_WHERE_PARAMETER : '_sqlWhere',
        ORG_PARAMETER : '_org',
        CALCULATE_ORGS: '_calculateOrgs',
        ORDERBY_PARAMETER : '_orderBy',
        SQL_ORDERBY_PARAMETER : '_sqlOrderBy',
        FILTER_PARAMETER : '_filter',
        SQL_FILTER_PARAMETER : '_sqlFilter',
        SORTBY_PARAMETER : '_sortBy',
        OR_EXPRESSION : '_OrExpression',
        TEXT_MATCH_PARAMETER_OVERRIDE : '_textMatchStyleOverride',
        SUCCESS : 'success',
        DBL_CLICK_DELAY : 300,
        ERROR : 'error',
        IS_PICK_AND_EDIT : '_isPickAndEdit',
        SELECTED_PROPERTIES : '_selectedProperties',
        EXTRA_PROPERTIES : '_extraProperties',
        TREE_DS_DEFAULT_FETCH_LIMIT : 200
    },

    Styles : {
      skinsPath : '${data.contextUrl}' + 'web/org.openbravo.userinterface.smartclient/openbravo/skins/'
    },

    I18N: {}
};