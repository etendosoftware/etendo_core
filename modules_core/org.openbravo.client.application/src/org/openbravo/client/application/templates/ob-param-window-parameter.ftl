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
 * All portions are Copyright (C) 2012-2015 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
*/

-->

<#macro createParameter param>
{
  
  <#if param.grid>
    type:'OBPickEditGridItem',      
  <#else>
    type: '${param.type}',
  </#if>
  title: '${param.title?js_string}',
  name: '${param.name?js_string}',
  <#if param.type != "OBSectionItem">
    paramId: '${param.id}',
    width: '${param.width?js_string}',
    <#if param.targetEntity != ''>
      targetEntity: '${param.targetEntity?string}',
    </#if>
    <#if param.valueMapPresent>
    valueMap: {
    <#list param.valueMap as valueMapValue>
      '${valueMapValue.key?js_string}': '${valueMapValue.value?js_string}'<#if valueMapValue_has_next>,</#if>
    </#list>
    },
    </#if>
    <#if param.onChangeFunction?? && param.onChangeFunction != "" > <#-- TODO: Check why "param.onChangeFunction??" is needed -->
        onChangeFunction: ${param.onChangeFunction?js_string},
    </#if>
    <#if param.showIf != "" >
      showIf: function(item, value, form, currentValues, context) {
        return (${param.showIf});
      },
    </#if>
    <#if param.readOnlyIf != "" >
      readOnlyIf: function(currentValues,context) {
        return (${param.readOnlyIf});
      },
    </#if>
    <#if param.length != -1>
      length: ${param.length?c},
    </#if>
    required: ${param.required?string}
    ${param.parameterProperties}
  <#else>
    defaultValue: '${param.title?js_string}',
    sectionExpanded: ${param.expanded?string},
    itemIds: [
    <#list param.children as childParam>
      '${childParam.dBColumnName?js_string}'<#if childParam_has_next>,</#if>
    </#list>
    ]
  </#if>
  <#if param.grid> 
    , displayedRowsNumber: ${param.numberOfDisplayedRows}
    , showTitle: ${param.showTitle?string}
    <#if param.onGridLoadFunction?? && param.onGridLoadFunction != "" >
      ,onGridLoadFunction: ${param.onGridLoadFunction?js_string}
    </#if>
    ,viewProperties: {
    ${param.tabView}
    }
   </#if>
}
</#macro>