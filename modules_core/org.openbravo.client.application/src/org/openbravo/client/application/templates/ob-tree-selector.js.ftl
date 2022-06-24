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
 * All portions are Copyright (C) 2013 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
*/
-->

    treeReferenceId: '${data.id}',
    dataSourceId: '${data.dataSourceId}',
    parentSelectionAllowed:  ${data.parentSelectionAllowed?string},
    popupTextMatchStyle: 'substring',
    textMatchStyle: 'substring',
    defaultPopupFilterField : '${data.defaultPopupFilterField}',
    displayField: '${data.displayField?js_string}',
    valueField: '${data.valueField?js_string}',
    referencedTableId: '${data.referencedTableId?js_string}',
    pickListFields: [
    <#list data.pickListFields as pickListField>
<@compress single_line=true>
        {<#list pickListField.properties as property>
        ${property.name}: ${property.value}<#if property_has_next>,</#if>
         </#list>       
        }<#if pickListField_has_next>,</#if>
</@compress>
    </#list>
    ],
    showSelectorGrid: ${data.showSelectorGrid},
    treeGridFields : [
    <#list data.treeGridFields as treeGridField>
<@compress single_line=true>
        {
        <#list treeGridField.properties as property>
        ${property.name}: ${property.value}<#if property_has_next>,</#if>
         </#list>         
         ${treeGridField.filterEditorProperties}         
        }<#if treeGridField_has_next>,</#if>
</@compress>
    </#list>
    ],
    extraSearchFields: [${data.extraSearchFields}],
<#--
    // create the datasource in the init method, this
    // prevents too early creation, it is created when the
    // fields on the form are actually created
-->
    init: function() {
        this.optionDataSource = ${data.dataSourceJavascript};
        this.Super('init', arguments);
    }, 
