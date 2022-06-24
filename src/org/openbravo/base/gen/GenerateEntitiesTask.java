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
 * All portions are Copyright (C) 2008-2021 Openbravo SLU
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.base.gen;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.TemplateException;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.WordUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tools.ant.Task;
import org.openbravo.base.AntExecutor;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.base.session.OBPropertiesProvider;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Task generates the entities using the freemarker template engine.
 *
 * @author Martin Taal
 * @author Stefan Huehner
 */
public class GenerateEntitiesTask extends Task {
  public static final String COM_ETENDOERP_SEQUENCES_SERVICES_NON_TRXMETADATA_CONTRIBUTOR = "com.etendoerp.sequences.services.NonTRXMetadataContributor\n";
  private static final Logger log = LogManager.getLogger();
  public static final String ERROR_GENERATING_FILE = "Error generating file: ";
  public static final String GENERATING_FILE = "Generating file: ";
  public static final String COM_ETENDOERP_SEQUENCES_SERVICES_NON_TRXSERVICE_CONTRIBUTOR = "com.etendoerp.sequences.services.NonTRXServiceContributor\n";

  private String basePath;
  private String srcGenPath;
  private String propertiesFile;
  boolean generateAllChildProperties;
  boolean generateDeprecatedProperties;

  public final static String GENERATED_DIR = "/../build/tmp/generated";

  public static void main(String[] args) {
    final String srcPath = args[0];
    String friendlyWarnings = "false";
    if (args.length >= 2) {
      friendlyWarnings = args[0];
    }
    final File srcDir = new File(srcPath);
    final File baseDir = srcDir.getParentFile();
    try {
      final AntExecutor antExecutor = new AntExecutor(baseDir.getAbsolutePath());
      antExecutor.setProperty("friendlyWarnings", friendlyWarnings);
      antExecutor.runTask("generate.entities.quick.forked");
    } catch (final Exception e) {
      throw new OBException(e);
    }
  }

  public String getBasePath() {
    return basePath;
  }

  public void setBasePath(String basePath) {
    this.basePath = basePath;
  }

  public boolean getFriendlyWarnings() {
    return OBPropertiesProvider.isFriendlyWarnings();
  }

  public void setFriendlyWarnings(boolean doFriendlyWarnings) {
    OBPropertiesProvider.setFriendlyWarnings(doFriendlyWarnings);
  }

  public String getPropertiesFile() {
    return propertiesFile;
  }

  public void setPropertiesFile(String propertiesFile) {
    this.propertiesFile = propertiesFile;
  }

  public String getSrcGenPath() {
    return srcGenPath;
  }

  public void setSrcGenPath(String srcGenPath) {
    this.srcGenPath = srcGenPath;
  }

  @Override
  public void execute() {
    if (getBasePath() == null) {
      setBasePath(super.getProject().getBaseDir().getAbsolutePath());
    }

    // the beautifier uses the source.path if it is not set
    log.debug("initializating dal layer, getting properties from " + getPropertiesFile());
    OBPropertiesProvider.getInstance().setProperties(getPropertiesFile());

    if (!hasChanged()) {
      log.info("Model has not changed since last run, not re-generating entities");
      return;
    }

    final Properties obProperties = OBPropertiesProvider.getInstance().getOpenbravoProperties();
    final String pathEntitiesRx = obProperties.getProperty("rx.path.entities");
    final String pathJPARepoRx = obProperties.getProperty("rx.path.jparepo");
    final boolean generateRxCode = Boolean.parseBoolean(obProperties.getProperty("rx.generateCode"));
    log.info("Generate Etendo Rx Code={}", generateRxCode);
    log.info("Path Entities Rx={}", pathEntitiesRx);
    log.info("Path JPA Repo Rx={}", pathJPARepoRx);

    generateAllChildProperties = OBPropertiesProvider.getInstance()
            .getBooleanProperty("hb.generate.all.parent.child.properties");

    generateDeprecatedProperties = OBPropertiesProvider.getInstance()
            .getBooleanProperty("hb.generate.deprecated.properties");

    List<String> sequenceContributors = new ArrayList<>();

    // read and parse template
    String ftlFilename = "org/openbravo/base/gen/entity.ftl";
    File ftlFile = new File(getBasePath(), ftlFilename);
    freemarker.template.Template template = createTemplateImplementation(ftlFile);

    // template for computed columns entities
    String ftlComputedFilename = "org/openbravo/base/gen/entityComputedColumns.ftl";
    File ftlComputedFile = new File(getBasePath(), ftlComputedFilename);
    freemarker.template.Template templateComputed = createTemplateImplementation(ftlComputedFile);

    // template for sequenced columns contributor
    String ftlSequenceContributorFilename = "org/openbravo/base/gen/entitySequenceContributor.ftl";
    File ftlSequenceContributorFile = new File(getBasePath(), ftlSequenceContributorFilename);
    freemarker.template.Template sequenceContributorTemplate = createTemplateImplementation(ftlSequenceContributorFile);

    // template for Etendo RX
    String ftlFileNameRX = "org/openbravo/base/gen/entityRX.ftl";
    File ftlFileRX = new File(getBasePath(), ftlFileNameRX);
    freemarker.template.Template templateRX = createTemplateImplementation(ftlFileRX);

    String ftlFileNameJPARepo = "org/openbravo/base/gen/jpaRepoRX.ftl";
    File ftlFileJPARepoRX = new File(getBasePath(), ftlFileNameJPARepo);
    freemarker.template.Template templateJPARepoRX = createTemplateImplementation(ftlFileJPARepoRX);

    // process template & write file for each entity
    List<Entity> entities = ModelProvider.getInstance().getModel();
    ModelProvider.getInstance().addHelpAndDeprecationToModel(generateDeprecatedProperties);

    Path excludedFilterPath = Paths.get(basePath + GENERATED_DIR);
    try {
      Files.createDirectories(excludedFilterPath.getParent());
    } catch (IOException e) {
      log.error("Error while creating generated filter file.", e);
    }

    try (FileOutputStream excludedFilter = new FileOutputStream(basePath + GENERATED_DIR)) {
      for (Entity entity : entities) {
        // If the entity is associated with a datasource based table or based on an HQL query, do not
        // generate a Java file
        if (entity.isDataSourceBased() || entity.isHQLBased()) {
          continue;
        }

        File outFile;
        File outFileRepo;
        String classfileName;

        if (!entity.isVirtualEntity()) {
          classfileName = entity.getClassName().replaceAll("\\.", "/") + ".java";
          log.debug(GENERATING_FILE + classfileName);
          outFile = new File(srcGenPath, classfileName);
          new File(outFile.getParent()).mkdirs();
          //Adding Class in generated file
          addClassInGenerated(excludedFilter, entity, null);

          try (Writer outWriter = new BufferedWriter(
              new OutputStreamWriter(new FileOutputStream(outFile), StandardCharsets.UTF_8))) {
            Map<String, Object> data = new HashMap<>();
            data.put("entity", entity);
            data.put("util", this);
            processTemplate(template, data, outWriter);
          } catch (IOException e) {
            log.error(ERROR_GENERATING_FILE + classfileName, e);
          }
        }

        if (entity.hasComputedColumns()) {
          classfileName = entity.getPackageName().replaceAll("\\.", "/") + "/"
              + entity.getSimpleClassName() + Entity.COMPUTED_COLUMNS_CLASS_APPENDIX + ".java";
          log.debug(GENERATING_FILE + classfileName);
          outFile = new File(srcGenPath, classfileName);
          new File(outFile.getParent()).mkdirs();

          //Adding the Class in Generated file
          addClassInGenerated(excludedFilter, entity, Entity.COMPUTED_COLUMNS_CLASS_APPENDIX);

          try (Writer outWriter = new BufferedWriter(
              new OutputStreamWriter(new FileOutputStream(outFile), StandardCharsets.UTF_8))) {
            Map<String, Object> data = new HashMap<>();
            data.put("entity", entity);

            List<Property> properties = entity.getComputedColumnProperties();

            if (entity.hasProperty("client")) {
              properties.add(entity.getProperty("client"));
              data.put("implementsClientEnabled", "implements ClientEnabled ");
            } else {
              data.put("implementsClientEnabled", "");
            }
            if (entity.hasProperty("organization")) {
              properties.add(entity.getProperty("organization"));
              if (entity.hasProperty("client")) {
                data.put("implementsOrgEnabled", ", OrganizationEnabled ");
              } else {
                data.put("implementsOrgEnabled", "implements OrganizationEnabled ");
              }
            } else {
              data.put("implementsOrgEnabled", "");
            }

            data.put("properties", properties);
            List<String> imports = entity.getJavaImports(properties);
            imports.remove("import org.openbravo.base.structure.ActiveEnabled;");
            imports.remove("import org.openbravo.base.structure.Traceable;");
            data.put("javaImports", imports);
            processTemplate(templateComputed, data, outWriter);
          } catch (IOException e) {
            log.error(ERROR_GENERATING_FILE + classfileName, e);
          }
        }

        if (entity.hasSequencedColumns()) {
          classfileName = entity.getClassName().replace(".", "/") + Entity.SEQUENCE_CONTRIBUTOR_CLASS_APPENDIX + ".java";
          log.debug("Generating file: {}", classfileName);

          outFile = new File(srcGenPath, classfileName);
          new File(outFile.getParent()).mkdirs();

          try (Writer outWriter = new BufferedWriter(
              new OutputStreamWriter(new FileOutputStream(outFile), StandardCharsets.UTF_8))) {
            Map<String, Object> data = new HashMap<>();
            data.put("entity", entity);
            data.put("util", this);
            processTemplate(sequenceContributorTemplate, data, outWriter);
          } catch (IOException e) {
            log.error("Error generating file: {}", classfileName, e);
          }
          sequenceContributors.add(entity.getClassName() + Entity.SEQUENCE_CONTRIBUTOR_CLASS_APPENDIX);
          addClassInGenerated(excludedFilter, entity, Entity.SEQUENCE_CONTRIBUTOR_CLASS_APPENDIX);
        }

        if (generateRxCode && !entity.isVirtualEntity()) {
            final String packageEntities = pathEntitiesRx.substring(pathEntitiesRx.lastIndexOf('/') + 1);
            final String fullPathEntities = pathEntitiesRx + "/src/main/java/" + packageEntities.replace('.', '/');
            final String className = entity.getClassName().replaceAll("\\.", "/");
            final String onlyClassName = className.substring(className.lastIndexOf('/') + 1);

            final String packageJPARepo = pathJPARepoRx.substring(pathJPARepoRx.lastIndexOf('/') + 1);
            final String fullPathJPARepo = pathJPARepoRx + "/src/main/java/" + packageJPARepo.replace('.', '/');
            final String repositoryClass = Utilities.toCamelCase(entity.getTableName()) + "Repository.java";

            classfileName = className.replace(onlyClassName, Utilities.toCamelCase(entity.getTableName())) + ".java";
            log.debug(GENERATING_FILE + classfileName);
            outFile = new File(fullPathEntities, classfileName);
            outFileRepo = new File(fullPathJPARepo, repositoryClass);
            new File(outFile.getParent()).mkdirs();
            new File(outFileRepo.getParent()).mkdirs();

            Map<String, Object> data = new HashMap<>();
            data.put("newClassName", Utilities.toCamelCase(entity.getTableName()));
            data.put("entity", entity);
            data.put("packageName", packageEntities);
            data.put("packageJPARepo", packageJPARepo);
            data.put("util", this);

            Writer outWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFile), StandardCharsets.UTF_8));
            processTemplate(templateRX, data, outWriter);

            Writer outWriterRepo = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFileRepo), StandardCharsets.UTF_8));
            processTemplate(templateJPARepoRX, data, outWriterRepo);

        }
      }
    } catch (IOException e) {
      log.error(ERROR_GENERATING_FILE + GENERATED_DIR, e);
    }

    if (!sequenceContributors.isEmpty()) {
      // generate services registration in META-INF/services
      File outFile = new File(srcGenPath, "META-INF/services/org.hibernate.boot.spi.MetadataContributor");
      new File(outFile.getParent()).mkdirs();
      try (Writer outWriter = new BufferedWriter(
              new OutputStreamWriter(new FileOutputStream(outFile), StandardCharsets.UTF_8))) {
        for (String contributor : sequenceContributors) {
          outWriter.write(contributor + "\n");


        }
        // This contributor is needed to finish initialization of the NonTransactionalSequenceService class
        outWriter.write(COM_ETENDOERP_SEQUENCES_SERVICES_NON_TRXMETADATA_CONTRIBUTOR);
      } catch (IOException e) {
        log.error("Error generating sevices registration file", e);
      }
    }
    // Generating this service registration here to avoid compilation issues when generating and compiling entities.
    File src = new File(getBasePath());
    File outFile = new File(src.getParentFile(), FilenameUtils.getBaseName(srcGenPath) + "/META-INF/services/org.hibernate.service.spi.ServiceContributor");
    outFile.getParentFile().mkdirs();
    try (Writer outWriter = new BufferedWriter(
            new OutputStreamWriter(new FileOutputStream(outFile), StandardCharsets.UTF_8))) {
      // This contributor is needed to start initialization of the NonTransactionalSequenceService class
      outWriter.write(COM_ETENDOERP_SEQUENCES_SERVICES_NON_TRXSERVICE_CONTRIBUTOR);
    } catch (IOException e) {
      log.error("Error generating sevices registration file", e);
    }

    log.info("Generated " + entities.size() + " entities");
  }

  private void addClassInGenerated(FileOutputStream excludedFilter, Entity entity, String suffix) {
    if (excludedFilter != null) {
      try {
        if(suffix!= null){
          excludedFilter.write(entity.getClassName().concat(suffix).getBytes());
        }
        else{
          excludedFilter.write(entity.getClassName().getBytes());
        }
        excludedFilter.write("\n".getBytes());

      } catch (IOException e) {
        log.error(e.getMessage());
      }
    }
  }

  /**
   * Checks if "hb.generate.deprecated.properties" or "hb.generate.all.parent.child.properties"
   * properties from Openbravo.properties have been set to true. If so, then deprecation should be
   * added.
   * 
   * @return True if deprecation should be added, depending on global properties found in
   *         Openbravo.properties, else false.
   */
  public boolean shouldAddDeprecation() {
    return generateDeprecatedProperties || generateAllChildProperties;
  }

  /**
   * Checks if an entity is set as deprecated
   *
   * @param e
   *          Entity to check deprecation
   * @return True if entity is deprecated, false otherwise
   */
  public boolean isDeprecated(Entity e) {
    return e.isDeprecated() != null && e.isDeprecated();
  }

  /**
   * Checks if a proprerty is deprecated, it can be deprecated in Application Dictionary or the
   * entity it references could be deprecated
   *
   * @param p
   *          Property to check deprecation
   * @return True if property or property target entity are deprecated and generate deprecate
   *         property is set to true in Openbravo.properties, false otherwise
   */
  public boolean isDeprecated(Property p) {
    if ((p.isDeprecated() != null && p.isDeprecated()) || (p.getTargetEntity() != null
        && p.getTargetEntity().isDeprecated() != null && p.getTargetEntity().isDeprecated())) {
      return true;
    }

    Property refPropery = p.getReferencedProperty();
    if (refPropery == null) {
      return false;
    }

    boolean generatedInAnyCase = ModelProvider.getInstance()
            .shouldGenerateChildPropertyInParent(refPropery, false);

    boolean generatedDueToPreference = ModelProvider.getInstance()
            .shouldGenerateChildPropertyInParent(refPropery, true);
    return !generatedInAnyCase && generatedDueToPreference;
  }

  public String getDeprecationMessage(Property p) {
    if (p.isDeprecated() != null && p.isDeprecated()) {
      return "Property marked as deprecated on field Development Status";
    }
    if (p.getTargetEntity() != null && p.getTargetEntity().isDeprecated() != null
            && p.getTargetEntity().isDeprecated()) {
      return "Target entity {@link " + p.getTargetEntity().getSimpleClassName()
              + "} is deprecated.";
    }
    return "Child property in parent entity generated for backward compatibility, it will be removed in future releases.";
  }

  private boolean hasChanged() {
    // first check if there is a directory
    // already in the src-gen
    // if not then regenerate anyhow
    final File modelDir = new File(getSrcGenPath(),
            "org" + File.separator + "openbravo" + File.separator + "model" + File.separator + "ad");
    if (!modelDir.exists()) {
      return true;
    }

    // check if the logic to generate has changed...
    final String sourceDir = getBasePath();
    long lastModifiedPackage = 0;
    lastModifiedPackage = getLastModifiedPackage("org.openbravo.base.model", sourceDir,
            lastModifiedPackage);
    lastModifiedPackage = getLastModifiedPackage("org.openbravo.base.gen", sourceDir,
            lastModifiedPackage);
    lastModifiedPackage = getLastModifiedPackage("org.openbravo.base.structure", sourceDir,
            lastModifiedPackage);

    // check if there is a sourcefile which was updated before the last
    // time the model was created. In this case that sourcefile (and
    // all source files need to be regenerated
    final long lastModelUpdateTime = ModelProvider.getInstance().computeLastUpdateModelTime();
    final long lastModified;
    if (lastModelUpdateTime > lastModifiedPackage) {
      lastModified = lastModelUpdateTime;
    } else {
      lastModified = lastModifiedPackage;
    }
    return isSourceFileUpdatedBeforeModelChange(modelDir, lastModified);
  }

  private boolean isSourceFileUpdatedBeforeModelChange(File file, long modelUpdateTime) {
    if (file.isDirectory()) {
      for (File child : file.listFiles()) {
        if (isSourceFileUpdatedBeforeModelChange(child, modelUpdateTime)) {
          return true;
        }
      }
      return false;
    }
    return file.lastModified() < modelUpdateTime;
  }

  private long getLastModifiedPackage(String pkg, String baseSourcePath, long prevLastModified) {
    final File file = new File(baseSourcePath, pkg.replaceAll("\\.", "/"));
    final long lastModified = getLastModifiedRecursive(file);
    if (lastModified > prevLastModified) {
      return lastModified;
    }
    return prevLastModified;
  }

  private long getLastModifiedRecursive(File file) {
    long lastModified = file.lastModified();
    if (file.isDirectory()) {
      for (File child : file.listFiles()) {
        final long childLastModified = getLastModifiedRecursive(child);
        if (lastModified < childLastModified) {
          lastModified = childLastModified;
        }
      }
    }
    return lastModified;
  }

  private void processTemplate(freemarker.template.Template templateImplementation,
                               Map<String, Object> data, Writer output) {
    try {
      templateImplementation.process(data, output);
    } catch (IOException | TemplateException e) {
      throw new IllegalStateException(e);
    }
  }

  private freemarker.template.Template createTemplateImplementation(File file) {
    try (FileReader reader = new FileReader(file)) {
      return new freemarker.template.Template("template", reader, getNewConfiguration());
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private Configuration getNewConfiguration() {
    final Configuration cfg = new Configuration();
    cfg.setObjectWrapper(new DefaultObjectWrapper());
    return cfg;
  }

  public String formatSqlLogic(String sqlLogic) {
    if (sqlLogic != null) {
      final String sqlLogicEscaped = sqlLogic.replaceAll("\\*/", " ");
      final String wrappedSqlLogic = WordUtils.wrap(sqlLogicEscaped, 100);
      return wrappedSqlLogic.replaceAll("\n", "\n       ");
    } else {
      return sqlLogic;
    }
  }
}
