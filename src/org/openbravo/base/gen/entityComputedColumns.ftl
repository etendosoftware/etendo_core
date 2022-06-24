<#function getter p>
  <#if p.boolean>
    <#return "is" + p.getterSetterName?cap_first>
  <#else>
    <#return "get" + p.getterSetterName?cap_first>
  </#if>
</#function>

<#function theList entity>
  <#if entity.simpleClassName == "List">
    <#return "java.util.List">
  <#else>
    <#return "List">
  </#if>
</#function>
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
 * All portions are Copyright (C) 2013-2017 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
*/
package ${entity.packageName};
<#list javaImports as i>
${i}
</#list>
/**
 * Virtual entity class to hold computed columns for entity ${entity.name}.
 *
 * NOTE: This class should not be instantiated directly.
 */
public class ${entity.simpleClassName}_ComputedColumns extends BaseOBObject ${implementsClientEnabled}${implementsOrgEnabled}{
    private static final long serialVersionUID = 1L;
    public static final String ENTITY_NAME = "${entity.simpleClassName}_ComputedColumns";
    
    <#list properties as p>
    public static final String PROPERTY_${p.name?upper_case} = "${p.name}";
    </#list>

    @Override
    public String getEntityName() {
        return ENTITY_NAME;
    }

    <#list properties as p>
    <#if !p.oneToMany>
    <#if p.name?matches("Id")>
    @Override
    </#if>
    public ${p.shorterTypeName} ${getter(p)}() {
      return (${p.shorterTypeName}) get(PROPERTY_${p.name?upper_case});
    }

    <#if p.name?matches("Id")>
    @Override
    </#if>
    public void set${p.getterSetterName?cap_first}(${p.shorterTypeName} ${p.javaName}) {
      set(PROPERTY_${p.name?upper_case}, ${p.javaName});
    }
    </#if>
	</#list>
}
