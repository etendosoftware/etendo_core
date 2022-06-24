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

package org.openbravo.test.system;

import java.util.Date;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.service.OBDal;
import org.openbravo.service.importprocess.ImportEntry;
import org.openbravo.service.importprocess.ImportEntryArchive;
import org.openbravo.test.base.OBBaseTest;

/**
 * Test the {@link ImportEntry} and {@link ImportEntryArchive} tables to see that large text fit in
 * there.
 * 
 * @author mtaal
 */

public class ImportEntrySizeTest extends OBBaseTest {
  private static final long SIZE = 1000 * 1000 * 10;

  @Test
  public void doTest() {
    setTestAdminContext();

    // create a random string
    StringBuilder sb = new StringBuilder();
    // do -100 as the new UUID will maybe get it over the max length which is 10 million
    while (sb.length() <= (SIZE - 100)) {
      sb.append(UUID.randomUUID().toString());
    }
    String json = sb.toString();

    StringBuilder sb2 = new StringBuilder();
    // do -100 as the new UUID will maybe get it over the max length which is 10 million
    while (sb2.length() <= (SIZE - 100)) {
      sb2.append(UUID.randomUUID().toString());
    }
    String error = sb2.toString();

    String importEntryId = null;
    {
      final ImportEntry importEntry = OBProvider.getInstance().get(ImportEntry.class);
      importEntry.setJsonInfo(json);
      importEntry.setErrorinfo(error);
      importEntry.setImported(new Date());
      importEntry.setImportStatus("Initial");
      importEntry.setTypeofdata("Order");
      OBDal.getInstance().save(importEntry);
      importEntryId = importEntry.getId();

      final ImportEntryArchive importEntryArchive = OBProvider.getInstance()
          .get(ImportEntryArchive.class);
      importEntryArchive.setId(importEntry.getId());
      importEntryArchive.setNewOBObject(true);
      importEntryArchive.setJsonInfo(json);
      importEntryArchive.setErrorinfo(error);
      importEntryArchive.setImported(new Date());
      importEntryArchive.setImportStatus("Initial");
      importEntryArchive.setTypeofdata("Order");
      OBDal.getInstance().save(importEntryArchive);
      OBDal.getInstance().commitAndClose();
    }

    // read and check
    ImportEntry importEntry = OBDal.getInstance().get(ImportEntry.class, importEntryId);
    ImportEntryArchive importEntryArchive = OBDal.getInstance()
        .get(ImportEntryArchive.class, importEntryId);

    // remove to clean the db before checking
    OBDal.getInstance().remove(importEntryArchive);
    OBDal.getInstance().remove(importEntry);
    OBDal.getInstance().commitAndClose();

    // check the result
    Assert.assertEquals(json, importEntry.getJsonInfo());
    Assert.assertEquals(error, importEntry.getErrorinfo());

    Assert.assertEquals(json, importEntryArchive.getJsonInfo());
    Assert.assertEquals(error, importEntryArchive.getErrorinfo());

    // and check that we actually checked big sizes
    Assert.assertTrue(importEntry.getJsonInfo().length() > (SIZE - 100));
    Assert.assertTrue(importEntry.getErrorinfo().length() > (SIZE - 100));
    Assert.assertTrue(importEntryArchive.getJsonInfo().length() > (SIZE - 100));
    Assert.assertTrue(importEntryArchive.getErrorinfo().length() > (SIZE - 100));
  }
}
