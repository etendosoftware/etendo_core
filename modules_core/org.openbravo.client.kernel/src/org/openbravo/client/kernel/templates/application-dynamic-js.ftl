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

OB.User = {
        id : '${data.user.id}',
        firstName : '${(data.user.firstName!'')?js_string}',
        lastName : '${(data.user.lastName!'')?js_string}',
        userName : '${(data.user.username!'')?js_string}',
        name : '${(data.user.name!'')?js_string}',
        email : '${(data.user.email!'')?js_string}',
        roleId: '${data.role.id}',
        roleName: '${data.role.name?js_string}',
        <#if data.role.forPortalUsers>
          isPortal: true,
        <#else>
          isPortal: false,
        </#if>
        <#if data.role.portalAdmin>
          isPortalAdmin: true,
        <#else>
          isPortalAdmin: false,
        </#if>
        <#if data.user.businessPartner??>
          businessPartnerId: '${data.user.businessPartner.id}',
          businessPartnerName: '${data.user.businessPartner.name?js_string}',
        <#else>
          businessPartnerId: null,
          businessPartnerName: null,
        </#if>
        clientId: '${data.client.id}',
        clientName: '${data.client.name?js_string}',
        organizationId: '${data.organization.id}',
        organizationName: '${data.organization.name?js_string}',
        writableOrganizations: [
        <#list data.writableOrganizations as property>
            '${property?js_string}'<#if property_has_next>,</#if>
        </#list>],
        csrfToken: '${data.csrfToken?js_string}'
};

OB.AccessibleEntities = {
    <#list data.accessibleEntities as entity>
    '${entity.name?js_string}':  true<#if entity_has_next>,</#if>
    </#list>
};

OB.Application.language = '${data.languageId?js_string}';
OB.Application.language_string = '${data.language?js_string}';
OB.Application.systemVersion = '${data.systemVersion?js_string}'; // global version used in all hyperlinks
OB.Application.purpose = '${data.instancePurpose?js_string}';
OB.Application.licenseType = '${data.licenseType?js_string}';
OB.Application.isTrial = ${data.trialStringValue};
OB.Application.isGolden = ${data.goldenStringValue};
OB.Application.isActiveInstance = ${data.activeInstanceStringValue};
OB.Application.versionDescription = '${data.versionDescription?js_string}';
OB.Application.companyImage = {
  <#list data.companyImageLogoData?keys as key>
    '${key}': '${data.companyImageLogoData[key]}'<#if key_has_next>,</#if>
  </#list>
};
OB.Application.communityBrandingUrl = '${data.communityBrandingUrl?js_string}'
