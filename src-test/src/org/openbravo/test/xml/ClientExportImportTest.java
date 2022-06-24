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
 * All portions are Copyright (C) 2008-2018 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import org.apache.ddlutils.Platform;
import org.apache.ddlutils.model.Database;
import org.apache.ddlutils.model.Table;
import org.apache.ddlutils.platform.ExcludeFilter;
import org.junit.Test;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.base.structure.ClientEnabled;
import org.openbravo.dal.service.OBDal;
import org.openbravo.ddlutils.util.DBSMOBUtil;
import org.openbravo.model.ad.system.Client;
import org.openbravo.service.db.ClientImportProcessor;
import org.openbravo.service.db.DataExportService;
import org.openbravo.service.db.DataImportService;
import org.openbravo.service.db.ImportResult;
import org.openbravo.service.system.SystemService;

/**
 * Tests export and import of client dataset.
 * 
 * <b>NOTE: this test has as side effect that new clients are created in the database with all their
 * data. These clients are not removed after the tests.</b>
 * 
 * @author mtaal
 */
// @FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ClientExportImportTest extends XMLBaseTest {

  /**
   * Test which copies a client, then deletes it, and then tests that the foreign keys are still
   * activated
   */
  @Test
  public void testZDeleteClient() {
    Platform platform = SystemService.getInstance().getPlatform();
    ExcludeFilter excludeFilter = DBSMOBUtil.getInstance()
        .getExcludeFilter(new File(OBPropertiesProvider.getInstance()
            .getOpenbravoProperties()
            .getProperty("source.path")));
    Database dbBefore = platform.loadTablesFromDatabase(excludeFilter);
    String newClientId = exportImport(QA_TEST_CLIENT_ID);
    Client client = OBDal.getInstance().get(Client.class, newClientId);

    SystemService.getInstance().deleteClient(client);
    Database dbAfter = platform.loadTablesFromDatabase(excludeFilter);
    for (int i = 0; i < dbBefore.getTableCount(); i++) {
      Table table1 = dbBefore.getTable(i);
      Table table2 = dbAfter.getTable(i);
      for (int j = 0; j < table1.getForeignKeyCount(); j++) {
        assertTrue(table1.getForeignKey(j).equals(table2.getForeignKey(j)));
      }
    }
  }

  private String exportImport(String clientId) {
    setSystemAdministratorContext();
    final Map<String, Object> parameters = new HashMap<String, Object>();
    parameters.put(DataExportService.CLIENT_ID_PARAMETER_NAME, clientId);

    final StringWriter sw = new StringWriter();
    DataExportService.getInstance().exportClientToXML(parameters, false, sw);
    String xml = sw.toString();
    try {
      final String sourcePath = (String) OBPropertiesProvider.getInstance()
          .getOpenbravoProperties()
          .get("source.path");
      final File dir = new File(sourcePath + File.separator + "temp");
      if (!dir.exists()) {
        dir.mkdir();
      }
      final File f = new File(dir, "export.xml");
      if (f.exists()) {
        f.delete();
      }
      f.createNewFile();
      final FileWriter fw = new FileWriter(f);
      fw.write(xml);
      fw.close();
    } catch (final Exception e) {
      throw new OBException(e);
    }

    final ClientImportProcessor importProcessor = new ClientImportProcessor();
    importProcessor.setNewName("" + System.currentTimeMillis());
    try {
      final ImportResult ir = DataImportService.getInstance()
          .importClientData(importProcessor, false, new StringReader(xml));
      xml = null;
      if (ir.getException() != null) {
        throw new OBException(ir.getException());
      }
      if (ir.getErrorMessages() != null) {
        fail(ir.getErrorMessages());
      }
      // none should be updated!
      assertEquals(0, ir.getUpdatedObjects().size());

      String newClientId = null;

      // and never insert anything in client 0
      for (final BaseOBObject bob : ir.getInsertedObjects()) {
        if (bob instanceof ClientEnabled) {
          final ClientEnabled ce = (ClientEnabled) bob;
          assertNotNull(ce.getClient());
          assertTrue(!ce.getClient().getId().equals("0"));
          newClientId = ce.getClient().getId();
        }
      }
      assertTrue(newClientId != null);
      assertTrue(!clientId.equals(newClientId));
      commitTransaction();
      return newClientId;
    } catch (final Exception e) {
      throw new OBException(e);
    }
  }

}
