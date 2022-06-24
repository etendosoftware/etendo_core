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
 * All portions are Copyright (C) 2019 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.service.importprocess;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.query.Query;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;

/**
 * Builder class used to easily create a new ImportEntry instance
 */
public class ImportEntryBuilder {

  private static final Logger log = LogManager.getLogger();

  private static final String INITIAL_IMPORT_STATUS = "Initial";

  private List<ImportEntryPreProcessor> entryPreProcessors;

  private String id;
  private Client client;
  private Organization organization;
  private Role role;
  private String typeOfData;
  private String jsonData;
  private Map<String, Object> customProperties;
  private boolean notifyManager;

  /**
   * Create a new instance of the Builder to create a new ImportEntry. Both the typeOfData and
   * jsonData values are mandatory.
   *
   * @param typeOfData
   *          Type of data. It must be a valid list reference value within the reference: 'Type of
   *          Import Data'
   * @param jsonData
   *          String representation of a JSONObject with the content to distribute
   * @return A new ImportEntryBuilder instance with typeOfData and jsonData values set
   */
  public static ImportEntryBuilder newInstance(String typeOfData, String jsonData) {
    return new ImportEntryBuilder(typeOfData, jsonData);
  }

  private ImportEntryBuilder(String typeOfData, String jsonData) {
    this.client = OBContext.getOBContext().getCurrentClient();
    this.organization = OBContext.getOBContext().getCurrentOrganization();
    this.role = OBContext.getOBContext().getRole();
    this.customProperties = new HashMap<>();
    this.notifyManager = false;
    this.typeOfData = typeOfData;
    this.jsonData = jsonData;
  }

  public ImportEntryBuilder setId(String id) {
    this.id = id;
    return this;
  }

  public ImportEntryBuilder setOrganization(Organization organization) {
    this.organization = organization;
    return this;
  }

  public ImportEntryBuilder setClient(Client client) {
    this.client = client;
    return this;
  }

  public ImportEntryBuilder setRole(Role role) {
    this.role = role;
    return this;
  }

  /**
   * This method let initialize any ImportEntry property using its property name. Note that if
   * propertyName does not exist for ImportEntry, create() will throw an OBException. Also, if
   * propertyValue cannot be assigned to the property, a ValidationException will be thrown.
   *
   * @param propertyName
   *          the name of the property
   * @param propertyValue
   *          the value of the property
   * @return The ImportEntryBuilder instance to chain another method
   */
  public ImportEntryBuilder setProperty(String propertyName, Object propertyValue) {
    this.customProperties.put(propertyName, propertyValue);
    return this;
  }

  /**
   * Set this flag to true to notify the ImportEntryManager there is a new ImportEntry to process
   * 
   * @param notifyManager
   *          whether ImportEntryManager.notifyNewImportEntryCreated() will be called after
   *          ImportEntry creation
   * @return The ImportEntryBuilder instance to chain another method
   */
  public ImportEntryBuilder setNotifyManager(boolean notifyManager) {
    this.notifyManager = notifyManager;
    return this;
  }

  /**
   * Creates a ImportEntry instance, checks that this instance is not already created or archived
   * and then saves it. If setNotifyManager(true) is called, the transaction is commited, the
   * connection closed and the ImportEntryManager will be notified of the change.
   *
   * @throws ImportEntryAlreadyExistsException
   *           when import entry already exists either in c_import_entry or c_import_entry_archive
   *           tables
   * @throws OBException
   *           when data provided is not valid. This could be caused because: Client/Organization
   *           does not match, role is not part of client or attempting to insert a non-existing
   *           custom property
   */
  public ImportEntry create() {
    OBContext.setAdminMode(false);
    try {
      validateImportEntryData();
      ImportEntry importEntry = createImportEntry();
      preprocessAndSave(importEntry, notifyManager);
      return importEntry;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private void validateImportEntryData() {
    if (importEntryAlreadyExists(id)) {
      log.debug("Entry already exists, ignoring it, id/typeofdata {}/{} json {}", id, typeOfData,
          jsonData);
      throw new ImportEntryAlreadyExistsException("ImportEntry already exists");
    }

    if (importEntryIsAlreadyArchived(id)) {
      log.debug("Entry already archived, ignoring it, id/typeofdata {}/{} json {}", id, typeOfData,
          jsonData);
      throw new ImportEntryAlreadyExistsException(
          "ImportEntry already exists in ImportEntryArchive");
    }

    Entity entity = ModelProvider.getInstance().getEntity(ImportEntry.class);
    for (String property : customProperties.keySet()) {
      if (!entity.hasProperty(property)) {
        log.debug("Attempting to set non-existing property {} in ImportEntry", property);
        throw new OBException("Property " + property + " cannot be found");
      }
    }
  }

  private ImportEntry createImportEntry() {
    ImportEntry importEntry = OBProvider.getInstance().get(ImportEntry.class);
    if (id != null) {
      importEntry.setId(id);
    }

    importEntry.setNewOBObject(true);
    importEntry.setCreatedtimestamp((new Date()).getTime());
    importEntry.setImportStatus(INITIAL_IMPORT_STATUS);
    importEntry.setClient(client);
    importEntry.setOrganization(organization);
    importEntry.setRole(role);
    importEntry.setJsonInfo(jsonData);
    importEntry.setTypeofdata(typeOfData);

    for (Map.Entry<String, Object> property : customProperties.entrySet()) {
      importEntry.set(property.getKey(), property.getValue());
    }

    return importEntry;
  }

  private boolean importEntryAlreadyExists(String entryId) {
    if (StringUtils.isEmpty(entryId)) {
      return false;
    }

    final Query<Number> qry = SessionHandler.getInstance()
        .getSession()
        .createQuery("select count(*) from C_IMPORT_ENTRY where id=:id", Number.class);
    qry.setParameter("id", entryId);

    return qry.uniqueResult().intValue() > 0;
  }

  private boolean importEntryIsAlreadyArchived(String entryId) {
    if (StringUtils.isEmpty(entryId)) {
      return false;
    }

    final Query<Number> qry = SessionHandler.getInstance()
        .getSession()
        .createQuery("select count(*) from C_Import_Entry_Archive where id=:id", Number.class);
    qry.setParameter("id", entryId);

    return qry.uniqueResult().intValue() > 0;
  }

  private void preprocessAndSave(ImportEntry importEntry, boolean notify) {
    for (ImportEntryPreProcessor processor : getEntryPreProcessors()) {
      processor.beforeCreate(importEntry);
    }
    OBDal.getInstance().save(importEntry);
    if (notify) {
      OBDal.getInstance().commitAndClose();
      ImportEntryManager.getInstance().notifyNewImportEntryCreated();
    }
  }

  private List<ImportEntryPreProcessor> getEntryPreProcessors() {
    if (entryPreProcessors == null) {
      entryPreProcessors = WeldUtils.getInstances(ImportEntryPreProcessor.class);
    }
    return entryPreProcessors;
  }
}
