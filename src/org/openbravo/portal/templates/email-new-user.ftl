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

<p>Dear ${data.user.firstName} ${data.user.lastName},</p>

<p>
<#if !data.user.grantPortalAccess>
You have been granted access to the ${data.clientName} Online Customer Portal.
<#else>
We have noticed that you have recently updated your password.
</#if> 
Below is your login
information:</p>
<p>Username: ${data.user.username}<br>
Password: ${data.password}</p>
<#if data.url != "">
<p>Please use this information to <a href="${data.url}">log in</a> to your personalized account.</p>
</#if>
<#if data.contactEmail != "">
<p>If you have any questions, comments or concerns, please <a href="mailto:${data.contactEmail}">contact us</a>.</p>
</#if>
<p>
Sincerely,<br>
The ${data.clientName} Crew
</p>