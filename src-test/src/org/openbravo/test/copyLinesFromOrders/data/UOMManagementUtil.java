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
 * All portions are Copyright (C) 2017 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.copyLinesFromOrders.data;

import static org.junit.Assert.assertFalse;

import java.util.List;

import org.hibernate.criterion.Restrictions;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.materialmgmt.UOMUtil;
import org.openbravo.model.ad.domain.Preference;
import org.openbravo.model.ad.module.Module;

/**
 * Class used for the UomManagement preference
 * 
 * @author Mark
 *
 */
public class UOMManagementUtil {

  // UOM preference
  private boolean uOMPreferenceCreatedBeforeExecuteProcess;
  private boolean uOMPreferenceEnabledBeforeExecuteProcess;

  public UOMManagementUtil() {
  }

  public void saveUOMPreferenceStatusBeforeExecuteProcess() {
    setUOMPreferenceCreatedBeforeExecuteProcess(existsUOMPreference());
    setUOMPreferenceEnabledBeforeExecuteProcess(UOMUtil.isUomManagementEnabled());
  }

  public boolean existsUOMPreference() {
    return Preferences.existsPreference("UomManagement", true, null, null, null, null, null);
  }

  public void restoreUOMPreferenceStatus() {
    if (isUOMPreferenceCreatedBeforeExecuteProcess()) {
      setUOMPreference(isUOMPreferenceEnabledBeforeExecuteProcess() ? CLFOTestConstants.ENABLE_AUM
          : CLFOTestConstants.DISABLE_AUM, true);
    } else {
      removeUOMPreference();
    }
  }

  public void removeUOMPreference() {
    List<Preference> prefs = Preferences.getPreferences("UomManagement", true, null, null, null,
        null, null);
    if (!prefs.isEmpty() && prefs.size() == 1) {
      Preference uomPref = prefs.get(0);
      OBDal.getInstance().remove(uomPref);
      OBDal.getInstance().flush();
    }
  }

  public void setUOMPreference(final String value, final boolean doCommit) {
    final Module uomPreferenceModule = getModuleOfUOMPreference();
    setModuleInDevelopmentFlag(uomPreferenceModule, true);

    Preferences.setPreferenceValue("UomManagement", value, true, null, null, null, null, null,
        null);
    OBDal.getInstance().flush();

    OBCriteria<Preference> qPref = OBDal.getInstance().createCriteria(Preference.class);
    qPref.add(Restrictions.eq(Preference.PROPERTY_PROPERTY, "UomManagement"));

    assertFalse("No property has been set", qPref.list().isEmpty());

    setModuleInDevelopmentFlag(uomPreferenceModule, false);
    if (doCommit) {
      OBDal.getInstance().commitAndClose();
    }
  }

  private Module getModuleOfUOMPreference() {
    try {
      return Preferences.getPreferences("UomManagement", true, null, null, null, null, null)
          .get(0)
          .getModule();
    } catch (Exception notFound) {
      return null;
    }
  }

  private void setModuleInDevelopmentFlag(Module module, boolean newStatus) {
    if (module != null && (boolean) module.get(Module.PROPERTY_INDEVELOPMENT) != newStatus) {
      module.set(Module.PROPERTY_INDEVELOPMENT, newStatus);
      OBDal.getInstance().save(module);
      OBDal.getInstance().flush();
    }
  }

  public boolean isUOMPreferenceCreatedBeforeExecuteProcess() {
    return uOMPreferenceCreatedBeforeExecuteProcess;
  }

  public void setUOMPreferenceCreatedBeforeExecuteProcess(
      boolean uOMPreferenceCreatedBeforeExecuteProcess) {
    this.uOMPreferenceCreatedBeforeExecuteProcess = uOMPreferenceCreatedBeforeExecuteProcess;
  }

  public boolean isUOMPreferenceEnabledBeforeExecuteProcess() {
    return uOMPreferenceEnabledBeforeExecuteProcess;
  }

  public void setUOMPreferenceEnabledBeforeExecuteProcess(
      boolean uOMPreferenceEnabledBeforeExecuteProcess) {
    this.uOMPreferenceEnabledBeforeExecuteProcess = uOMPreferenceEnabledBeforeExecuteProcess;
  }
}
