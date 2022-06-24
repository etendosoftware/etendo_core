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
 * All portions are Copyright (C) 2010-2019 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
*/

OB.Application.menu = [
  <#list data.rootMenuOptions as menuOption>
    <@createMenuItem menuOption=menuOption /><#if menuOption_has_next>,</#if>
  </#list>
];

<#macro createMenuItem menuOption>
    {title: '${menuOption.label?js_string}'
    <#if menuOption.window>
        , type: 'window'
        , tabId: '${menuOption.id?js_string}'
        , windowId: '${menuOption.menu.window.id?js_string}'
        , optionType: 'tab'
        , id:'${menuOption.dbId}'
        , viewValue: '${menuOption.id?js_string}'
    <#elseif menuOption.process>
        , type: 'process'
        , optionType: 'process'
        , manualUrl: '${menuOption.id?js_string}'
        , processId: '${menuOption.menu.process.id}'
        , modal: '${menuOption.modal?string}'
        , id:'${menuOption.dbId}'
        , viewValue: '${menuOption.id?js_string}'
    <#elseif menuOption.processManual>
        , type: 'processManual'
        , optionType: 'processManual'
        , manualUrl: '${menuOption.id?js_string}'
        , manualProcessId: '${menuOption.menu.process.id}'
        , id:'${menuOption.dbId}'
        , viewValue: '${menuOption.id?js_string}'
    <#elseif menuOption.report && !menuOption.processDefinition>
        , type: 'report'
        , optionType: 'url'
        , manualUrl: '${menuOption.id?js_string}'
        , manualProcessId: '${menuOption.menu.process.id}'
        , id:'${menuOption.dbId}'
        , viewValue: '${menuOption.id?js_string}'
    <#elseif menuOption.form>
        , type: 'form'
        , optionType: 'url'
        , manualUrl: '${menuOption.id?js_string}'
        , formId: '${menuOption.formId?js_string}'
        , id:'${menuOption.dbId}'
        , viewValue: '${menuOption.id?js_string}'
    <#elseif menuOption.external>
        , type: 'external'
        , optionType: 'external'
        , externalUrl: '${menuOption.id?js_string}'
        , openLinkInBrowser: ${menuOption.menu.openlinkinbrowser?string}
        , id:'${menuOption.dbId}'
        , viewValue: '${menuOption.id?js_string}'
    <#elseif menuOption.view>
        , type: 'view'
        , optionType: 'url'
        , viewId: '${menuOption.id?js_string}'
        , tabTitle: '${menuOption.label?js_string}'
        , id:'${menuOption.dbId}'
        , viewValue: '${menuOption.id?js_string}'
    <#elseif menuOption.processDefinition>
        , type: 'processDefinition'
        , optionType: 'processDefinition'
        , viewId: 'processDefinition_${menuOption.menu.oBUIAPPProcessDefinition.id}'
        , uiPattern: '${menuOption.menu.oBUIAPPProcessDefinition.uIPattern?js_string}'
        , processId: '${menuOption.menu.oBUIAPPProcessDefinition.id}'
        , id:'${menuOption.dbId}'
        , viewValue: 'processDefinition_${menuOption.menu.oBUIAPPProcessDefinition.id}'
    </#if>
    , singleRecord: ${menuOption.singleRecordStringValue}
    , readOnly: ${menuOption.readOnlyStringValue}
    , editOrDeleteOnly: ${menuOption.editOrDeleteOnlyStringValue}
    
    <#list menuOption.parameters as parameter>
        , '${parameter.name?js_string}': '${parameter.parameterValue?js_string}'        
    </#list>
    <#if menuOption.children?size &gt; 0>
    , type: 'folder'
    , submenu: [
    <#list menuOption.children as childMenuOption>
        <@createMenuItem menuOption=childMenuOption /><#if childMenuOption_has_next>,</#if>        
    </#list>]
    </#if>
    }
</#macro>
