<#assign SEARCH_VECTOR_REF_ID = "81FCDA657A5540F69B0AE57B4E0F8A51" >
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
 * All portions are Copyright (C) 2008-2020 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
*/
package ${entity.packageName};
<#list entity.javaImports as i>
${i}
</#list>
/**
 * Entity class for entity ${entity.name} (stored in table ${entity.tableName}).<#if entity.help??>
 * <br>
 * Help: ${entity.help}</#if>
 * <br>
 * NOTE: This class should not be instantiated directly. To instantiate this
 * class the {@link org.openbravo.base.provider.OBProvider} should be used.<#if util.isDeprecated(entity)>
 * @deprecated Table entity has been marked as deprecated on Development Status field.</#if>
 */
<#if util.isDeprecated(entity)>
@Deprecated
</#if>
public class ${entity.simpleClassName} extends BaseOBObject ${entity.implementsStatement} {
    private static final long serialVersionUID = 1L;
    public static final String TABLE_NAME = "${entity.tableName}";
    public static final String ENTITY_NAME = "${entity.name}";

    <#list entity.properties as p>
    <#if !p.computedColumn>
    <#if !(p.domainType?? && p.domainType.reference?? && p.domainType.reference.id?matches(SEARCH_VECTOR_REF_ID))>
    <#if p.allowDerivedRead() && !p.isBeingReferenced()>
    /**
     * Property ${p.name} stored <#if p.columnName??>in column ${p.columnName} </#if>in table ${entity.tableName} <@addSeeTag p/>     * <@addDeprecationMessageIfNeeded property=p />
     */
    <#else>
    /**
     * Property ${p.name} stored <#if p.columnName??>in column ${p.columnName} </#if>in table ${entity.tableName}<#if p.help??><br>
     * Help: ${p.help}</#if>
     * <@addDeprecationMessageIfNeeded property=p />
     */
    </#if>
    <@addDeprecationTagIfNeeded property=p />
    public static final String PROPERTY_${p.name?upper_case} = "${p.name}";
    </#if>
    </#if>

    </#list>

    <#if entity.hasComputedColumns()>

    /**
     * Computed columns properties, these properties cannot be directly accessed, they need
     * to be read through _computedColumns proxy. They cannot be directly used in HQL, OBQuery
     * nor OBCriteria.
     */
    <#list entity.computedColumnProperties as p>

    /**
     * Computed column for property ${p.name}<br>
     * <#if p.help??>
     * Help: ${p.help}</#if><br>
     * Computed from: <br>
     * {@code ${util.formatSqlLogic(p.sqlLogic)}}
     */
    public static final String COMPUTED_COLUMN_${p.name?upper_case} = "${p.name}";
    </#list>

    </#if>
    public ${entity.simpleClassName}() {
    <#list entity.properties as p>
        <#if p.hasDefaultValue() && !p.computedColumn>
        setDefaultValue(PROPERTY_${p.name?upper_case}, ${p.formattedDefaultValue});
        </#if>
    </#list>
    }

    @Override
    public String getEntityName() {
        return ENTITY_NAME;
    }

    <#list entity.properties as p>
    <#if !p.oneToMany>
    <#if !(p.domainType?? && p.domainType.reference.id?matches(SEARCH_VECTOR_REF_ID))>
    /**
     * @see ${entity.simpleClassName}#<#if p.computedColumn>COMPUTED_COLUMN<#else>PROPERTY</#if>_${p.name?upper_case}
     * <@addDeprecationMessageIfNeeded property=p />
     */
    <@addDeprecationTagIfNeeded property=p />
    <#if p.name?matches("Id")>
    @Override
    </#if>
    public ${p.shorterTypeName} ${getter(p)}() {
    <#if p.partOfCompositeId>
        return ((Id)getId()).«getter((Property)p)»();
    <#else>
      <#if !p.computedColumn>
        return (${p.shorterTypeName}) get(PROPERTY_${p.name?upper_case});
      <#else>
        return (${p.shorterTypeName}) get(COMPUTED_COLUMN_${p.name?upper_case});
      </#if>
    </#if>
    }
    </#if>
    <#if !(p.domainType?? && p.domainType.reference.id?matches(SEARCH_VECTOR_REF_ID))>
    /**
     * @see ${entity.simpleClassName}#<#if p.computedColumn>COMPUTED_COLUMN<#else>PROPERTY</#if>_${p.name?upper_case}
     * <@addDeprecationMessageIfNeeded property=p />
     */
     <@addDeprecationTagIfNeeded property=p />
    <#if p.name?matches("Id")>
    @Override
    </#if>
    public void set${p.getterSetterName?cap_first}(${p.shorterTypeName} ${p.javaName}) {
    <#if p.partOfCompositeId>
	    ((Id)getId()).set${p.getterSetterName?cap_first}(${p.javaName});
	<#else>
      <#if !p.computedColumn>
        set(PROPERTY_${p.name?upper_case}, ${p.javaName});
      <#else>
        set(COMPUTED_COLUMN_${p.name?upper_case}, ${p.javaName});
      </#if>
	</#if>
    }
    </#if>

    </#if>
	</#list>
	<#list entity.properties as p>
	<#if p.oneToMany>
    /**<#if p.targetEntity.help??>
     * Help: ${p.targetEntity.help}<br></#if>
     * @see ${p.shorterNameTargetEntity}
     * <@addDeprecationMessageIfNeeded property=p/>
     */
	<@addDeprecationTagIfNeeded property=p />
    @SuppressWarnings("unchecked")
    public ${theList(entity)}<${p.shorterNameTargetEntity}> get${p.name?cap_first}() {
      <#if !p.computedColumn>
      return (${theList(entity)}<${p.shorterNameTargetEntity}>) get(PROPERTY_${p.name?upper_case});
      <#else>
      return (${theList(entity)}<${p.shorterNameTargetEntity}>) get(COMPUTED_COLUMN_${p.name?upper_case});
      </#if>
    }

    /**<#if p.targetEntity.help??>
     * Help: ${p.targetEntity.help}<br></#if>
     * @see ${p.shorterNameTargetEntity}
     * <@addDeprecationMessageIfNeeded property=p/>
     */
    <@addDeprecationTagIfNeeded property=p />
    public void set${p.getterSetterName?cap_first}(${theList(entity)}<${p.shorterNameTargetEntity}> ${p.name}) {
        set(PROPERTY_${p.name?upper_case}, ${p.name});
    }

    </#if>
    </#list>
    <#if entity.hasCompositeId()>
	public static class Id implements java.io.Serializable {
	    private static final long serialVersionUID = 1L;

		<#list entity.properties as p>
		<#if p.partOfCompositeId>
		<#if p.hasDefaultValue()>
		private ${p.typeName} ${p.javaName} = ${p.formattedDefaultValue};
		<#else>
		private ${p.typeName} ${p.javaName};
		</#if>
		</#if>
		</#list>

		<#list entity.properties as p>
		<#if p.partOfCompositeId>
		public ${p.typeName} «getter((Property)p)»() {
			return ${p.javaName};
		}

		public void set${p.getterSetterName?cap_first}(${p.typeName} ${p.javaName}) {
			this.${p.javaName} = ${p.javaName};
		}
		</#if>
		</#list>

	    public boolean equals(Object obj) {
			if (this == obj) {
    			return true;
			}
			if (!(obj instanceof Id)) {
				return false;
			}
			final Id otherId = (Id)obj;
		<#list entity.properties as p>
		<#if p.partOfCompositeId>
			if (!areEqual(«getter((Property)p)»(), otherId.«getter((Property)p)»())) {
				return false;
			}
		</#if>
		</#list>
			return true;
		}

		// hashCode assumes that keys can not change!
    	public int hashCode() {
    		int result = 0;
    		<#list entity.properties as p>
    		<#if p.partOfCompositeId>
			if («getter((Property)p)»() != null) {
				result +=«getter((Property)p)»().hashCode();
			}
			</#if>
			</#list>

    		if (result == 0) {
    			return super.hashCode();
    		}
    		return result;
    	}

		private boolean areEqual(Object v1, Object v2) {
			if (v1 == null || v2 == null) {
				return v1 == v2;
			}
			return v1.equals(v2);
		}
	}
	</#if>
	<#if entity.hasComputedColumns()>

    @Override
    public Object get(String propName) {
      <#list entity.computedColumnProperties as p>
      if (COMPUTED_COLUMN_${p.name?upper_case}.equals(propName)) {
        if (get_computedColumns() == null) {
          return null;
        }
        return get_computedColumns().${getter(p)}();
      }
      </#list>

      return super.get(propName);
    }
    </#if>
}
<#macro addDeprecationMessageIfNeeded property>
    <#if util.isDeprecated(property)>
@deprecated ${util.getDeprecationMessage(property)}
    </#if>
</#macro>

<#macro addDeprecationTagIfNeeded property>
    <#if util.isDeprecated(property)>
    @Deprecated
    </#if>
</#macro>

<#macro addSeeTag property>
    <#if property.entity.isTraceable() && property.isAuditInfo()>

     * @see Traceable <#elseif property.isClientOrOrganization() && property.getColumnName() == "AD_Org_ID">
     * @see OrganizationEnabled <#elseif property.isClientOrOrganization() && property.getColumnName() != "AD_Org_ID">
     * @see ClientEnabled <#elseif property.isActiveColumn()>
     * @see ActiveEnabled </#if>
</#macro>
