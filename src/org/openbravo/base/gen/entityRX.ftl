package ${packageName}.${entity.packageName};

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@javax.persistence.Entity
@javax.persistence.Table(name = "${entity.tableName?lower_case}")
@javax.persistence.Cacheable
public class ${newClassName} {
    <#list entity.properties as p>
    <#if !p.computedColumn>
    <#if p.isId()>
    @javax.persistence.Id
    @javax.persistence.Column(name = "${p.columnName?lower_case}")
    java.lang.String ${p.javaName};

    </#if>
    <#if p.isPrimitive() && !p.isId()>
    <#if !p.getPrimitiveType().isArray()>
    @javax.persistence.Column(name = "${p.columnName?lower_case}")
    ${p.getObjectTypeName()} ${p.javaName};

    <#else>
    @javax.persistence.Column(name = "${p.columnName?lower_case}")
    ${p.shorterTypeName} ${p.javaName};

    </#if>
    <#else>
    <#if !p.isOneToMany() && !p.isId()>
    <#if p.targetEntity??>
    @javax.persistence.JoinColumn(name = "${p.columnName?lower_case}", insertable=false, updatable=false)
    @javax.persistence.ManyToOne(fetch=javax.persistence.FetchType.LAZY)
    ${packageName}.${p.getTableName(p.getObjectTypeName())} ${p.javaName};

    </#if>
    <#else>
    <#if !p.isId()>
    @javax.persistence.OneToMany(cascade = javax.persistence.CascadeType.ALL, mappedBy = "${p.referencedProperty.name}")
    java.util.List<${packageName}.${p.getTableName(p.getObjectTypeName())}> ${p.name};

    </#if>
    </#if>
    </#if>
    </#if>
    </#list>
}
