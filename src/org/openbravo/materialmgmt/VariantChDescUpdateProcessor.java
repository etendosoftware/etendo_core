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
 * All portions are Copyright (C) 2016-2018 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */
package org.openbravo.materialmgmt;

import javax.enterprise.context.ApplicationScoped;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.dal.service.OBDal;
import org.openbravo.service.importprocess.ImportEntry;
import org.openbravo.service.importprocess.ImportEntryManager;
import org.openbravo.service.importprocess.ImportEntryManager.ImportEntryQualifier;
import org.openbravo.service.importprocess.ImportEntryProcessor;

@ImportEntryQualifier(entity = "VariantChDescUpdate")
@ApplicationScoped
public class VariantChDescUpdateProcessor extends ImportEntryProcessor {
  private static final Logger log = LogManager.getLogger();

  @Override
  protected ImportEntryProcessRunnable createImportEntryProcessRunnable() {
    return WeldUtils.getInstanceFromStaticBeanManager(VariantChDescUpdateRunnable.class);
  }

  @Override
  protected boolean canHandleImportEntry(ImportEntry importEntryInformation) {
    return "VariantChDescUpdate".equals(importEntryInformation.getTypeofdata());
  }

  @Override
  protected String getProcessSelectionKey(ImportEntry importEntry) {
    return importEntry.getTypeofdata();
  }

  protected static class VariantChDescUpdateRunnable extends ImportEntryProcessRunnable {

    @Override
    protected void processEntry(ImportEntry importEntry) throws Exception {
      OBContext.setAdminMode(true);
      try {
        JSONObject jsonInfo = new JSONObject(importEntry.getJsonInfo());
        JSONArray productIds = jsonInfo.getJSONArray("productIds");
        for (int i = 0; i < productIds.length(); i++) {
          VariantChDescUpdateProcess process = WeldUtils
              .getInstanceFromStaticBeanManager(VariantChDescUpdateProcess.class);
          try {
            process.update(productIds.getString(i), null);
            OBDal.getInstance().flush();
            OBDal.getInstance().getSession().clear();
          } catch (Exception e) {
            log.error("Error updating product ch description", e);
          }
        }

        ImportEntryManager.getInstance().setImportEntryProcessed(importEntry.getId());
        if (SessionHandler.isSessionHandlerPresent()) {
          OBDal.getInstance().commitAndClose();
        }
      } finally {
        OBContext.restorePreviousMode();
      }
    }
  }
}
