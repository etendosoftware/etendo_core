<#--
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
 * All portions are Copyright (C) 2010-2016 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
*/
-->

isc.OBViewGrid.create({
    view: this,
    uiPattern: '${data.uiPattern}', 
     
    <#if data.orderByClause != "">
        orderByClause: '${data.orderByClause?js_string}',
    </#if>
    <#if data.sortField != "">
        sortField: '${data.sortField?js_string}',
    </#if>
    <#if data.hasFilterClause>
        filterClause: true,
    <#else>
        filterClause: false,
    </#if>
    <#if data.filterName != "">
        filterName: '${data.filterName?js_string}',
    </#if>
    <#if data.lazyFiltering>
        lazyFiltering: ${data.lazyFiltering?string},
    </#if>
    allowSummaryFunctions: ${data.allowSummaryFunctions?string},
    <#if data.alwaysFilterFksByIdentifier>
        alwaysFilterFksByIdentifier: ${data.alwaysFilterFksByIdentifier?string},
    </#if>       
    //List of properties  that must be always included in this grid
    requiredGridProperties: [
    <#list data.requiredGridProperties as property>
        '${property?js_string}'<#if property_has_next>,</#if>
    </#list>],
    // the this is the view instance
    fields: this.gridFields
})
