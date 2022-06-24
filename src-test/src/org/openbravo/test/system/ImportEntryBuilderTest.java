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
package org.openbravo.test.system;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Hibernate;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.service.importprocess.ImportEntry;
import org.openbravo.service.importprocess.ImportEntryAlreadyExistsException;
import org.openbravo.service.importprocess.ImportEntryArchive;
import org.openbravo.service.importprocess.ImportEntryBuilder;

/**
 * Test the ImportEntryBuilder under various scenarios
 *
 * @author jarmendariz
 */
public class ImportEntryBuilderTest extends WeldBaseTest {

  private static final Organization SPAIN_ORG = OBDal.getInstance()
      .getProxy(Organization.class, "357947E87C284935AD1D783CF6F099A1");
  private static final Client QA_TESTING_CLIENT = OBDal.getInstance()
      .getProxy(Client.class, "4028E6C72959682B01295A070852010D");
  private static final Role QA_TESTING_ADMIN_ROLE = OBDal.getInstance()
      .getProxy(Role.class, "4028E6C72959682B01295A071429011E");
  private static final String JSON_DATA = "{\"custom\":\"data\"}";
  private static final String IMPORT_STATUS = "Initial";
  private static final String TYPE_OF_DATA = "Order";

  private ImportEntry createdEntry;
  private static ImportEntry existingImportEntry;
  private static ImportEntryArchive existingImportEntryArchive;

  /**
   * Create two sample import entries. One for ImportEntry table and the other for
   * ImportEntryArchive Those entries will be removed in @AfterClass
   */
  @BeforeClass
  public static void createImportEntries() {
    OBContext.setOBContext("100", "0", TEST_CLIENT_ID, TEST_ORG_ID);

    existingImportEntry = createImportEntry();
    existingImportEntryArchive = createImportEntryArchive();

    Hibernate.initialize(SPAIN_ORG);
    Hibernate.initialize(QA_TESTING_CLIENT);
    Hibernate.initialize(QA_TESTING_ADMIN_ROLE);

    OBDal.getInstance().save(existingImportEntry);
    OBDal.getInstance().save(existingImportEntryArchive);
    OBDal.getInstance().commitAndClose();
  }

  private static ImportEntryArchive createImportEntryArchive() {
    ImportEntryArchive importEntryArchive = OBProvider.getInstance().get(ImportEntryArchive.class);
    importEntryArchive.setNewOBObject(true);
    importEntryArchive.setJsonInfo(JSON_DATA);
    importEntryArchive.setImported(new Date());
    importEntryArchive.setImportStatus(IMPORT_STATUS);
    importEntryArchive.setTypeofdata(TYPE_OF_DATA);

    return importEntryArchive;
  }

  private static ImportEntry createImportEntry() {
    ImportEntry importEntry = OBProvider.getInstance().get(ImportEntry.class);
    importEntry.setNewOBObject(true);
    importEntry.setJsonInfo(JSON_DATA);
    importEntry.setImported(new Date());
    importEntry.setImportStatus(IMPORT_STATUS);
    importEntry.setTypeofdata(TYPE_OF_DATA);

    return importEntry;
  }

  /**
   * Check that the new import entry returned contains the inserted data
   */
  @Test
  public void whenNoIdIsProvidedANewEntryIsCreated() {
    createdEntry = ImportEntryBuilder.newInstance(TYPE_OF_DATA, JSON_DATA) //
        .create();

    assertThat("Created object has JSON data", createdEntry.getJsonInfo(), is(JSON_DATA));
  }

  /**
   * Before creating an ImportEntry, if ID is provided, builder will check whether this entry exists
   * in ImportEntry table
   */
  @Test(expected = ImportEntryAlreadyExistsException.class)
  public void whenGivenIdExistsImportEntryBuilderThrowsException() {
    createdEntry = ImportEntryBuilder.newInstance(TYPE_OF_DATA, JSON_DATA) //
        .setId(existingImportEntry.getId()) //
        .create();
  }

  /**
   * Before creating an ImportEntry, if ID is provided, builder will check whether this entry exists
   * in ImportEntryArchive table
   */
  @Test(expected = ImportEntryAlreadyExistsException.class)
  public void whenGivenIdExistsAsArchivedImportEntryBuilderThrowsException() {
    createdEntry = ImportEntryBuilder.newInstance(TYPE_OF_DATA, JSON_DATA) //
        .setId(existingImportEntryArchive.getId()) //
        .create();
  }

  /**
   * Any field can be filled using .setProperty method. Test that active field can be set using this
   * method
   */
  @Test
  public void customPropertiesCanBeInserted() {
    createdEntry = ImportEntryBuilder.newInstance(TYPE_OF_DATA, JSON_DATA) //
        .setProperty("active", false) //
        .create();

    assertFalse("Created object should be inactive", createdEntry.isActive());
  }

  /**
   * Check that the builder throws an exception when attempting to set a non existing field in
   * setProperties
   */
  @Test(expected = OBException.class)
  public void whenCustomPropertyToInsertDoesNotExistImportEntryCreationFails() {
    createdEntry = ImportEntryBuilder.newInstance(TYPE_OF_DATA, JSON_DATA) //
        .setProperty("customData", "") //
        .create();
  }

  /**
   * Test that default values are set to the ImportEntry
   */
  @Test
  public void whenNoParametersAreSetImportEntryIsCreatedWithDefaultValues() {
    createdEntry = ImportEntryBuilder.newInstance(TYPE_OF_DATA, JSON_DATA) //
        .create();

    Client client = OBContext.getOBContext().getCurrentClient();
    Organization organization = OBContext.getOBContext().getCurrentOrganization();
    Role role = OBContext.getOBContext().getRole();
    assertTrue("Created entry values matches default values",
        checkImportEntryValues(createdEntry, client, organization, role, TYPE_OF_DATA, JSON_DATA));
  }

  /**
   * Test that defined parameters are actually in the created object
   */
  @Test
  public void whenParametersAreSetImportEntryIsCreatedUsingThoseValues() {
    createdEntry = ImportEntryBuilder.newInstance(TYPE_OF_DATA, JSON_DATA) //
        .setClient(QA_TESTING_CLIENT) //
        .setOrganization(SPAIN_ORG) //
        .setRole(QA_TESTING_ADMIN_ROLE) //
        .create();

    assertTrue("Created entry values matches defined values", checkImportEntryValues(createdEntry,
        QA_TESTING_CLIENT, SPAIN_ORG, QA_TESTING_ADMIN_ROLE, TYPE_OF_DATA, JSON_DATA));
  }

  private boolean checkImportEntryValues(ImportEntry entry, Client client,
      Organization organization, Role role, String typeOfData, String jsonData) {
    return (client.getId().equals(entry.getClient().getId()))
        && (organization.getId().equals(entry.getOrganization().getId()))
        && (role.getId().equals(entry.getRole().getId()))
        && (StringUtils.equals(typeOfData, entry.getTypeofdata()))
        && (StringUtils.equals(jsonData, entry.getJsonInfo()));
  }

  @After
  public void removeImportEntryWhenCreated() {
    OBContext.setAdminMode(false);
    try {
      if (createdEntry != null) {
        OBDal.getInstance().remove(createdEntry);
        OBDal.getInstance().flush();
      }
      createdEntry = null;
    } finally {
      OBContext.restorePreviousMode();
    }

  }

  @AfterClass
  public static void disposeImportEntries() {
    OBContext.setOBContext("100", "0", TEST_CLIENT_ID, TEST_ORG_ID);

    OBDal.getInstance().remove(existingImportEntry);
    OBDal.getInstance().remove(existingImportEntryArchive);
    OBDal.getInstance().flush();
    OBDal.getInstance().commitAndClose();
  }

}
