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
 * All portions are Copyright (C) 2011-2019 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
*/
-->

<#if !data.popup>
OB.Layout.ViewManager.loadedWindowClassName = 'processDefinition${data.windowClientClassName?js_string}';
</#if>

isc.ClassFactory.defineClass('<#if !data.popup>processDefinition</#if>${data.windowClientClassName?js_string}', isc.OBParameterWindowView).addProperties({
    processId: '${data.processId?js_string}',
    actionHandler: '${data.actionHandler?js_string}',
    popup: ${data.popup?string}, 
    <#if data.clientSideValidation??>
        clientSideValidation: ${data.clientSideValidation?js_string},
    </#if>
    <#if data.onLoadFunction??>
        onLoadFunction: ${data.onLoadFunction?js_string},
    </#if>
    <#if data.onRefreshFunction??>
        onRefreshFunction: ${data.onRefreshFunction?js_string},
    </#if>
    <#list data.buttonList as button>
    <#if button_index == 0>buttons:{</#if>
    '${button.searchKey?js_string}':'${button.name?js_string}'<#if button_has_next>,<#else>},</#if>
    </#list>
    <#if data.report>
        isReport: true,
        reportId: '${data.reportId?js_string}',
        <#if data.pdfExport>
            pdfExport: true,
        </#if>
        <#if data.xlsExport>
            xlsExport: true,
        </#if>
        <#if data.htmlExport>
            htmlExport: true,
        </#if>
    </#if>
    viewProperties: {
      fields: [
    <#list data.paramHandler.parameters as param>
      <@createParameter param/><#if param_has_next>,</#if>
    </#list>    
     ]
    },
    dynamicColumns: ${data.dynamicColumns}
});

