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
 * All portions are Copyright (C) 2017-2019 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
*/

OB.User.userInfo = {
  language: {
    value: '${data.contextLanguageId}',
    valueMap: [
      <#list data.languages as language>
        {
          id: '${language.id}',
          _identifier: '${language.identifier?js_string}'
        } <#if language_has_next>,</#if>
      </#list>
    ]
  },
  initialValues: {
    language: '${data.contextLanguageId}',
    role: '${data.contextRoleId}',
    client: '${data.contextClientId?js_string}',
    organization: '${data.contextOrganizationId}'<#if data.contextWarehouseId != "">,
    warehouse: '${data.contextWarehouseId}'
    </#if>
  },
  role: {
    value: '${data.contextRoleId}',
    valueMap: [
      <#list data.userRolesInfo as roleInfo>
        {
          id: '${roleInfo.roleId}',
          _identifier: '${roleInfo.roleName?js_string} - ${roleInfo.clientName?js_string}'
        } <#if roleInfo_has_next>,</#if>
      </#list>
    ],
    roles: [
      <#list data.userRolesInfo as roleInfo>
      {
        id: '${roleInfo.roleId}',
        client: '${roleInfo.clientName?js_string}',
        organizationValueMap: [
        <#list roleInfo.organizations?keys as organizationId>
          {
            id: '${organizationId}',
            _identifier: '${roleInfo.organizations[organizationId]?js_string}'
          } <#if organizationId_has_next>,</#if>
          </#list>
        ].sortByProperty('_identifier', true),
        warehouseOrgMap: [
          <#list roleInfo.organizationWarehouses?keys as key>
          {
            orgId: '${key}',
            warehouseMap: [
            <#list roleInfo.organizationWarehouses[key] as warehouse>
              {
                id: '${warehouse.warehouseId}',
                _identifier: '${warehouse.warehouseName?js_string}'
              } <#if warehouse_has_next>,</#if>
              </#list>
            ].sortByProperty('_identifier', true)
          } <#if key_has_next>,</#if>
          </#list>
        ]
      } <#if roleInfo_has_next>,</#if>
      </#list>
    ]
  }
};
