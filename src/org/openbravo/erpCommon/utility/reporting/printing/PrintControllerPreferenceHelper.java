package org.openbravo.erpCommon.utility.reporting.printing; //NOSONAR

import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.PropertyException;
import org.openbravo.model.ad.ui.Tab;

@SuppressWarnings("java:S00120")
final class PrintControllerPreferenceHelper {
  private PrintControllerPreferenceHelper() {
  }

  static boolean isDirectPrint(VariablesSecureApp vars) {
    OBContext context = OBContext.getOBContext();
    String preferenceValue = "";
    try {
      OBContext.setAdminMode(true);
      String tabId = vars.getSessionValue(PrintController.INP_TAB_ID);
      Tab tab = OBDal.getInstance().get(Tab.class, tabId);
      if (tab == null) {
        return false;
      }
      try {
        preferenceValue = Preferences.getPreferenceValue("DirectPrint", true,
            context.getCurrentClient(), context.getCurrentOrganization(), context.getUser(),
            context.getRole(), tab.getWindow());
      } catch (PropertyException exception) {
        return false;
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return Preferences.YES.equals(preferenceValue);
  }
}
