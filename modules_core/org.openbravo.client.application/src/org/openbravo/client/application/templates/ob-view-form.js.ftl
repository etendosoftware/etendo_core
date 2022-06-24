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
{
    <#if data.fieldHandler.hasStatusBarFields>
    <#--
    // this this is the view    
    -->
    statusBarFields: this.statusBarFields<#if data.fieldHandler.hasFieldsWithReadOnlyIf || data.fieldHandler.hasFieldsWithShowIf>,</#if>
    </#if>
    
<#--
    // except for the fields all other form properties should be added to the formProperties
    // the formProperties are re-used for inline grid editing
-->
   <#if data.fieldHandler.hasFieldsWithReadOnlyIf || data.fieldHandler.hasFieldsWithShowIf>
    obFormProperties: {
      onFieldChanged: function(form, item, value) {
        var f = form || this,
            context = this.view.getContextInfo(false, true),
            currentValues = isc.shallowClone(f.view.getCurrentValues()), otherItem,
            disabledFields, i;
            OB.Utilities.fixNull250(currentValues);
        <#list data.fieldHandler.fields as field>
        <#if !field.readOnly && field.readOnlyIf != "" && field.showIf == "">
           // Applying read only logic.
           f.disableItem('${field.name}', ${field.readOnlyIf});
        <#else>
        <#if !field.readOnly && field.readOnlyIf == "" && field.showIf != "">
        // Applying display logic in grid.
        if (!this.view.isShowingForm) {
        <#if field.showIf == "false">
           f.disableItem('${field.name}', true);
        <#else>
           f.disableItem('${field.name}', (${field.showIf}) === false);
        </#if>
        }
        <#else>
        <#if !field.readOnly && field.readOnlyIf != "" && field.showIf != "">
        // Applying display logic and read only in grid/form.
        if (!this.view.isShowingForm) {
        <#if field.showIf == "false">
           // If display logic has a false value, it is only necessary take into account the read only logic.
           f.disableItem('${field.name}', (${field.readOnlyIf}));
        <#else>
           f.disableItem('${field.name}', (${field.readOnlyIf}) || (${field.showIf}) === false);
        </#if>
        } else {
           f.disableItem('${field.name}', ${field.readOnlyIf});
        }
        </#if>
        </#if>
        </#if>
        </#list>
        // disable forced in case the fields are set as read only per role
        disabledFields = form.view.disabledFields;
        if (disabledFields) {
          for (i=0; i<disabledFields.length; i++){
            f.disableItem(disabledFields[i], true);
          }
        }
      }
    }
    </#if>
}