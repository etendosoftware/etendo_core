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
 * All portions are Copyright (C) 2015 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.xml;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.service.db.DataImportService;
import org.openbravo.service.db.ImportResult;
import org.openbravo.utility.cleanup.log.LogCleanUpConfig;

/**
 * Test cases to cover default config datasets
 * 
 * @author alostale
 *
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DefaultsDataset extends XMLBaseTest {
  private static final Logger log = LogManager.getLogger();

  private static final String IMPORTED_ELEMENT1_ID = "00000000000000000000000000000001";
  private static final String IMPORTED_ELEMENT2_ID = "00000000000000000000000000000002";

  private static final String DS_FILENAME1 = "dds01.xml";
  private static final String DS_FILENAME2 = "dds02.xml";

  private static final Long NEW_VALUE = 100L;

  @Before
  public void setAdmin() {
    setSystemAdministratorContext();
  }

  /**
   * Imports dataset and checks data is imported
   */
  @Test
  public void dds001DataShouldBeImported() {
    ImportResult result = importDataSet(DS_FILENAME1);
    assertThat(result.getErrorMessages(), result.hasErrorOccured(), is(false));
    assertThat("Config should have been imported", getImportedConfig(IMPORTED_ELEMENT1_ID),
        is(notNullValue()));
  }

  /**
   * Modifies data imported by previous test case, imports dataset again and checks modifications
   * are not overwritten
   */
  @Test
  public void dss002ChangesShouldBePreserved() {
    LogCleanUpConfig config = getImportedConfig(IMPORTED_ELEMENT1_ID);
    config.setOlderThan(NEW_VALUE);
    OBDal.getInstance().flush();

    ImportResult result = importDataSet(DS_FILENAME1);
    assertThat(result.getErrorMessages(), result.hasErrorOccured(), is(false));

    LogCleanUpConfig newConfig = getImportedConfig(IMPORTED_ELEMENT1_ID);
    assertThat(newConfig.getOlderThan(), is(NEW_VALUE));
  }

  /**
   * Imports new version of the dataset, which adds a new record and does not have previous one. New
   * record should be added, whearas old one should be kept
   */
  @Test
  public void dss003NewRowsShouldBeInsertedOldOnesPreserved() {
    ImportResult result = importDataSet(DS_FILENAME2);
    assertThat(result.getErrorMessages(), result.hasErrorOccured(), is(false));

    assertThat("Config should have been imported", getImportedConfig(IMPORTED_ELEMENT1_ID),
        is(notNullValue()));
    assertThat("Config should have been imported", getImportedConfig(IMPORTED_ELEMENT2_ID),
        is(notNullValue()));
  }

  /** Clean ups inserted records */
  @Test
  public void dss999CleanUp() {
    OBDal.getInstance().remove(getImportedConfig(IMPORTED_ELEMENT1_ID));
    OBDal.getInstance().remove(getImportedConfig(IMPORTED_ELEMENT2_ID));
  }

  private ImportResult importDataSet(String xmlFileName) {
    log.info("Importing file {}", xmlFileName);

    String xml = getFileContent(xmlFileName);
    log.debug("xml contents:\n{}", xml);

    return DataImportService.getInstance()
        .importDataFromXML(OBDal.getInstance().get(Client.class, "0"),
            OBDal.getInstance().get(Organization.class, "0"), xml, null);
  }

  private LogCleanUpConfig getImportedConfig(String id) {
    return OBDal.getInstance().get(LogCleanUpConfig.class, id);
  }
}
