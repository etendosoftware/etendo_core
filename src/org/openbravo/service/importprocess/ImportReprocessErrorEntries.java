/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.0  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2015-2018 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */
package org.openbravo.service.importprocess;

import org.hibernate.query.Query;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.DalBaseProcess;

/**
 * For all {@link ImportEntry} with importstatus Error sets it back to Initial and notifies the
 * ImportEntryManager to process them.
 * 
 * @author mtaal
 */
public class ImportReprocessErrorEntries extends DalBaseProcess {
  private ProcessLogger logger;

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {
    super.setDoCommit(false);
    logger = bundle.getLogger();
    // don't do client/org check on resetting import error status
    OBContext.setAdminMode(false);
    try {
      @SuppressWarnings("rawtypes")
      final Query qry = OBDal.getInstance()
          .getSession()
          .createQuery(
              "update " + ImportEntry.ENTITY_NAME + " e set e." + ImportEntry.PROPERTY_IMPORTSTATUS
                  + "='Initial' where e." + ImportEntry.PROPERTY_IMPORTSTATUS + "='Error'");
      final int count = qry.executeUpdate();
      logger.logln(count
          + " import entries are set back to initialized, notifying import thread to process them");
      logger.logln(OBMessageUtils.messageBD("Success"));
    } catch (Exception e) {// won't' happen
      logger.logln(OBMessageUtils.messageBD("Error"));
    } finally {
      OBDal.getInstance().commitAndClose();
      ImportEntryManager entryManager = WeldUtils
          .getInstanceFromStaticBeanManager(ImportEntryManager.class);

      entryManager.notifyNewImportEntryCreated();
    }
  }
}
