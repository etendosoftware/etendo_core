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

package org.openbravo.dal.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.type.YesNoType;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.provider.OBSingleton;
import org.openbravo.base.session.DalUUIDGenerator;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.base.util.Check;

/**
 * This class is responsible for generating the Hibernate mapping for the tables and entities within
 * OpenBravo. It uses the runtime model provided by {@link ModelProvider ModelProvider}.
 * 
 * @author mtaal
 */

public class DalMappingGenerator implements OBSingleton {
  private static final Logger log = LogManager.getLogger();

  private static final String HIBERNATE_FILE_PROPERTY = "hibernate.hbm.file";
  private static final String HIBERNATE_READ_FILE_PROPERTY = "hibernate.hbm.readFile";

  private static final String TEMPLATE_FILE = "template.hbm.xml";
  private static final String MAIN_TEMPLATE_FILE = "template_main.hbm.xml";
  private static final String TAB2 = "\t\t";
  private static final String TAB3 = "\t\t\t";
  private static final char NL = '\n';

  private static DalMappingGenerator instance = new DalMappingGenerator();

  public static synchronized DalMappingGenerator getInstance() {
    if (instance == null) {
      instance = OBProvider.getInstance().get(DalMappingGenerator.class);
    }
    return instance;
  }

  public static synchronized void setInstance(DalMappingGenerator dalMappingGenerator) {
    instance = dalMappingGenerator;
  }

  private String templateContents;

  /**
   * Generates the Hibernate mapping for {@link Entity Entities} in the system. The generated
   * Hibernate mapping is returned as a String.
   * 
   * @return the generated Hibernate mapping (corresponds to what is found in a hbm.xml file)
   */
  public String generateMapping() {
    final String hibernateFileLocation = getHibernateFileLocation();

    // If readMappingFromFile is true and the mapping is already generated to a file, this file will
    // be read instead of generating a new one. Useful while developing changes in mapping to edit
    // the file before generating it.
    final String readMappingFromFile = OBPropertiesProvider.getInstance()
        .getOpenbravoProperties()
        .getProperty(HIBERNATE_READ_FILE_PROPERTY);

    if (hibernateFileLocation != null && readMappingFromFile != null
        && Boolean.parseBoolean(readMappingFromFile)) {
      try {
        File hbm = new File(hibernateFileLocation);
        if (hbm.exists()) {
          log.info("Reading mapping from " + hibernateFileLocation);
          FileInputStream fis = new FileInputStream(hbm);
          return readFile(fis);
        }
      } catch (Exception e) {
        log.error("Error reading mapping file, generating it instead", e);
      }
    }

    final ModelProvider mp = ModelProvider.getInstance();
    final StringBuilder sb = new StringBuilder();
    for (final Entity e : mp.getModel()) {
      // Do not map datasource based tables
      if (!e.isDataSourceBased() && !e.isVirtualEntity() && e.getMappingClass() != null) {
        final String entityMapping = generateMapping(e);
        sb.append(entityMapping);
      }
    }
    final String mainTemplate = readFile(MAIN_TEMPLATE_FILE);
    final String result = mainTemplate.replace("contentPlaceholder", sb.toString());

    if (log.isDebugEnabled()) {
      log.debug(result);
    }

    if (hibernateFileLocation != null) {
      try {
        final File f = new File(hibernateFileLocation);
        if (f.exists()) {
          f.delete();
        }
        f.createNewFile();
        final FileWriter fw = new FileWriter(f);
        fw.write(result);
        fw.close();
      } catch (final Exception e) {
        // ignoring exception for the rest
        log.error("Exception when saving hibernate mapping in " + hibernateFileLocation, e);
      }
    }
    return result;
  }

  String getHibernateFileLocation() {
    return OBPropertiesProvider.getInstance()
        .getOpenbravoProperties()
        .getProperty(HIBERNATE_FILE_PROPERTY);
  }

  private String generateMapping(Entity entity) {
    String hbm = getClassTemplateContents();
    hbm = hbm.replaceAll("mappingName", entity.getName());
    hbm = hbm.replaceAll("tableName", entity.getTableName());
    hbm = hbm.replaceAll("ismutable", Boolean.toString(entity.isMutable()));

    if (entity.getMappingClass() != null) {
      hbm = hbm.replaceAll("<class", "<class name=\"" + entity.getClassName() + "\" ");
    }

    // create the content by first getting the id
    final StringBuilder content = new StringBuilder();

    content.append(TAB2);

    if (entity.hasCompositeId()) {
      content.append(generateCompositeID(entity));
    } else {
      content.append(generateStandardID(entity));
    }
    content.append(NL);

    List<Property> computedColumns = new ArrayList<>();
    // now handle the standard columns
    for (final Property p : entity.getProperties()) {
      if (p.isId()) { // && p.isPrimitive()) { // handled separately
        continue;
      }

      if (p.isPartOfCompositeId()) {
        continue;
      }

      if (p.getDomainType() != null && p.getDomainType().getReference() != null
          && Entity.SEARCH_VECTOR_REF_ID.equals(p.getDomainType().getReference().getId())) {
        continue;
      }

      if (p.isOneToMany()) {
        content.append(generateOneToMany(p));
      } else {
        if (p.getSqlLogic() != null) {
          computedColumns.add(p);
        } else if (p.isPrimitive()) {
          content.append(generatePrimitiveMapping(p));
        } else {
          content.append(generateReferenceMapping(p));
        }
      }
    }

    if (!computedColumns.isEmpty()) {
      // create a proxy property for all computed columns
      content.append(generateComputedColumnsMapping(entity));
    }

    if (entity.isActiveEnabled()) {
      content.append(TAB2 + getActiveFilter());
    }

    hbm = hbm.replace("content", content.toString());

    if (!computedColumns.isEmpty()) {
      hbm = hbm + generateComputedColumnsClassMapping(entity, computedColumns);
    }
    return hbm;
  }

  private String generateComputedColumnsClassMapping(Entity entity,
      List<Property> computedColumns) {
    String hbm = getClassTemplateContents();
    String entityName = getComputedColumnsEntityName(entity);
    hbm = hbm.replaceAll("<class",
        "<class name=\"" + entity.getPackageName() + "." + entityName + "\" ");
    hbm = hbm.replaceAll("mappingName", entityName);
    hbm = hbm.replaceAll("tableName", entity.getTableName());
    hbm = hbm.replaceAll("ismutable", "false");

    final StringBuilder content = new StringBuilder();
    content.append(generateStandardID(entity) + NL);

    if (entity.isClientEnabled()) {
      content.append(TAB2
          + "<many-to-one name=\"client\" column=\"AD_Client_ID\" not-null=\"true\" update=\"false\" insert=\"false\" entity-name=\"ADClient\" access=\"org.openbravo.dal.core.DalPropertyAccessStrategy\"/>"
          + NL);
    }
    if (entity.isOrganizationEnabled()) {
      content.append(TAB2
          + "<many-to-one name=\"organization\" column=\"AD_Org_ID\" not-null=\"true\" update=\"false\" insert=\"false\" entity-name=\"Organization\" access=\"org.openbravo.dal.core.DalPropertyAccessStrategy\"/>"
          + NL);
    }
    content.append(NL);
    for (Property p : computedColumns) {
      if (p.isPrimitive()) {
        content.append(generatePrimitiveMapping(p));
      } else {
        content.append(generateReferenceMapping(p));
      }
    }
    hbm = hbm.replace("content", content.toString());
    return hbm;
  }

  private String generateComputedColumnsMapping(Entity entity) {
    Check.isTrue(entity.getIdProperties().size() == 1,
        "Computed columns are not supported in entities with composited ID");
    StringBuilder sb = new StringBuilder();
    final Property p = entity.getIdProperties().get(0);
    sb.append(TAB2
        + "<many-to-one name=\"_computedColumns\" update=\"false\" insert=\"false\" access=\"org.openbravo.dal.core.DalPropertyAccessStrategy\" ");
    sb.append("column=\"" + p.getColumnName() + "\" ");
    sb.append("entity-name=\"" + getComputedColumnsEntityName(entity) + "\"/>" + NL);
    return sb.toString();
  }

  private String getComputedColumnsEntityName(Entity entity) {
    return entity.getSimpleClassName() + "_ComputedColumns";
  }

  private String getActiveFilter() {
    return "<filter name=\"activeFilter\" condition=\":activeParam = isActive\"/>\n";
  }

  private String generatePrimitiveMapping(Property p) {
    if (p.getHibernateType() == Object.class) {
      return "";
    }
    final StringBuilder sb = new StringBuilder();
    sb.append(TAB2 + "<property name=\"" + p.getName() + "\"");
    sb.append(getAccessorAttribute());
    String type;
    if (p.getHibernateType().isArray()) {
      type = p.getHibernateType().getComponentType().getName() + "[]";
    } else {
      type = p.getHibernateType().getName();
    }
    if (p.isBoolean()) {
      type = YesNoType.class.getName();
    }
    sb.append(" type=\"" + type + "\"");

    if (p.getSqlLogic() != null) {
      sb.append(
          " formula=\"" + StringEscapeUtils.escapeHtml(processSqlLogic(p.getSqlLogic())) + "\"");
    } else {
      sb.append(" column=\"" + p.getColumnName() + "\"");
    }

    if (p.isMandatory()) {
      sb.append(" not-null=\"true\"");
    }

    // ignoring p.isUpdatable() for now as this is primarily used
    // for ui and not for background processes

    if (p.isInactive() || p.getEntity().isView() || p.getSqlLogic() != null) {
      sb.append(" update=\"false\"");
      sb.append(" insert=\"false\"");
    }

    sb.append("/>" + NL);
    return sb.toString();
  }

  private String generateReferenceMapping(Property p) {
    if (p.getTargetEntity() == null) {
      if (p.isProxy()) {
        return "";
      } else {
        return "<!-- Unsupported reference type " + p.getName() + " of entity "
            + p.getEntity().getName() + "-->" + NL;
      }
    }
    final StringBuilder sb = new StringBuilder();
    if (p.isOneToOne()) {
      final String name = p.getSimpleTypeName().substring(0, 1).toLowerCase()
          + p.getSimpleTypeName().substring(1);
      sb.append(TAB2 + "<one-to-one name=\"" + name + "\"");
      sb.append(" constrained=\"true\"");
    } else {
      sb.append(TAB2 + "<many-to-one name=\"" + p.getName() + "\" ");
      if (p.getSqlLogic() != null) {
        sb.append(
            "formula=\"" + StringEscapeUtils.escapeHtml(processSqlLogic(p.getSqlLogic())) + "\"");
      } else {
        sb.append("column=\"" + p.getColumnName() + "\"");
      }

      // cascade=\
      // "save-update\"
      if (p.isMandatory()) {
        sb.append(" not-null=\"true\"");
      }

      // language is always loaded explicitly by Hibernate because it is a non-pk
      // association, eager fetch with the parent then..
      // after some more thought, normally only a limited number of languages are used
      // resulting in a few extra queries in the beginning of the transaction, the rest is loaded
      // from the first level cache, so the current approach is fine, keep the following
      // lines commented
      // if (p.getTargetEntity() != null && p.getTargetEntity().getName().equals("ADLanguage")) {
      // sb.append(" fetch=\"join\"");
      // }

      if (p.isInactive() || p.getEntity().isView()) {
        sb.append(" update=\"false\"");
      }
      if (p.isInactive() || p.getEntity().isView()) {
        sb.append(" insert=\"false\"");
      }
    }

    // to prevent cascade errors that the parent is saved after the child
    // this is handled by the DataImportService.insertObjectGraph
    // but other specific code needs it
    if (p.isParent() && p.isMandatory()) {
      sb.append(" cascade=\"persist\"");
    }

    sb.append(" entity-name=\"" + p.getTargetEntity().getName() + "\"");

    sb.append(getAccessorAttribute());

    if (p.getReferencedProperty() != null && !p.getReferencedProperty().isId()) {
      sb.append(" property-ref=\"" + p.getReferencedProperty().getName() + "\"");
    }

    sb.append("/>" + NL);
    return sb.toString();
  }

  private String generateOneToMany(Property p) {
    final StringBuilder sb = new StringBuilder();
    StringBuilder order = new StringBuilder();
    if (p.isOneToMany()) {
      if (!p.getTargetEntity().getOrderByProperties().isEmpty()) {
        order.append("order-by=\"");
        for (final Property po : p.getTargetEntity().getOrderByProperties()) {
          order.append(po.getColumnName() + " ASC,");
        }
        order = order.replace(order.length() - 1, order.length(), "");
        order.append("\"");
      }

      String mutable = "";
      String cascade = "";
      if (p.isChild()) {
        cascade = " cascade=\"all,delete-orphan\" ";
      }
      if (p.getEntity().isView() || p.getTargetEntity().isView()) {
        mutable = " mutable=\"false\" ";
        cascade = "";
      }

      sb.append(TAB2 + "<bag name=\"" + p.getName() + "\" " + cascade + order
          + getAccessorAttribute() + mutable + " inverse=\"true\">" + NL);
      sb.append(TAB3 + "<key column=\"" + p.getReferencedProperty().getColumnName() + "\""
          + (p.getReferencedProperty().isMandatory() ? " not-null=\"true\"" : "") + "/>" + NL);
      sb.append(TAB3 + "<one-to-many entity-name=\"" + p.getTargetEntity().getName() + "\"/>" + NL);

      if (p.getTargetEntity().isActiveEnabled()) {
        sb.append(TAB3 + getActiveFilter());
      }
      sb.append(TAB2 + "</bag>" + NL);

    }
    return sb.toString();
  }

  // assumes one primary key column
  private String generateStandardID(Entity entity) {
    Check.isTrue(entity.getIdProperties().size() == 1,
        "Method can only handle primary keys with one column in entity " + entity.getName()
            + ". It has " + entity.getIdProperties().size());
    final Property p = entity.getIdProperties().get(0);
    final StringBuilder sb = new StringBuilder();
    sb.append(TAB2 + "<id name=\"" + p.getName() + "\" type=\"string\" " + getAccessorAttribute()
        + " column=\"" + p.getColumnName() + "\" unsaved-value=\"null\">" + NL);
    if (p.getIdBasedOnProperty() != null) {
      sb.append(TAB3 + "<generator class=\"foreign\">" + NL);
      sb.append(TAB2 + TAB2 + "<param name=\"property\">" + p.getIdBasedOnProperty().getName()
          + "</param>" + NL);
      sb.append(TAB3 + "</generator>" + NL);
    } else if (p.isUuid()) {
      sb.append(TAB3 + "<generator class=\"" + DalUUIDGenerator.class.getName() + "\"/>" + NL);
    }
    sb.append(TAB2 + "</id>" + NL);
    return sb.toString();
  }

  private String getAccessorAttribute() {
    return " access=\"" + DalPropertyAccessStrategy.class.getName() + "\"";
  }

  private String generateCompositeID(Entity e) {
    Check.isTrue(e.hasCompositeId(),
        "Method can only handle primary keys with more than one column");
    final StringBuilder sb = new StringBuilder();
    sb.append(TAB2 + "<composite-id name=\"id\" class=\"" + e.getClassName() + "$Id\""
        + getAccessorAttribute() + ">" + NL);
    final Property compId = e.getIdProperties().get(0);
    Check.isTrue(compId.isCompositeId(),
        "Property " + compId + " is expected to be a composite Id");
    for (final Property p : compId.getIdParts()) {
      if (p.isPrimitive()) {
        String type = p.getHibernateType().getName();
        if (boolean.class.isAssignableFrom(p.getHibernateType().getClass())
            || Boolean.class == p.getHibernateType()) {
          type = "yes_no";
        }
        sb.append(TAB3 + "<key-property name=\"" + p.getName() + "\" column=\"" + p.getColumnName()
            + "\" type=\"" + type + "\"/>" + NL);
      } else {
        sb.append(TAB3 + "<key-many-to-one name=\"" + p.getName() + "\" column=\""
            + p.getColumnName() + "\"");
        sb.append(" entity-name=\"" + p.getTargetEntity().getName() + "\"");
        sb.append("/>" + NL);
      }
    }
    sb.append(TAB2 + "</composite-id>" + NL);
    return sb.toString();
  }

  private String getClassTemplateContents() {
    if (templateContents == null) {
      templateContents = readFile(TEMPLATE_FILE);
    }
    return templateContents;
  }

  private String readFile(String fileName) {
    return readFile(getClass().getResourceAsStream(fileName));
  }

  private String readFile(InputStream is) {
    try {
      final InputStreamReader fr = new InputStreamReader(is);
      final BufferedReader br = new BufferedReader(fr);
      try {
        String line;
        final StringBuilder sb = new StringBuilder();
        while ((line = br.readLine()) != null) {
          sb.append(line + "\n");
        }
        return sb.toString();
      } finally {
        br.close();
        fr.close();
      }
    } catch (final IOException e) {
      throw new OBException(e);
    }
  }

  private String processSqlLogic(String val) {
    String localVal = val.trim();
    if (val.contains("\"")) {
      localVal = localVal.replace("\"", "");
    }
    if (!localVal.startsWith("(")) {
      localVal = "(" + localVal;
    }
    if (!localVal.endsWith(")")) {
      localVal = localVal + ")";
    }
    return localVal;
  }
}
