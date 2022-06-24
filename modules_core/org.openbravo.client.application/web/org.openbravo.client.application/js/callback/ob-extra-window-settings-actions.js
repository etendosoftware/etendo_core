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
 * All portions are Copyright (C) 2018 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
OB.Utilities = OB.Utilities || {};

OB.Utilities.ExtraWindowSettingsActions = {
  showInfoMessage: function(data) {
    var tab;
    if (
      !data ||
      !data.extraSettings ||
      !data.extraSettings.messageKey ||
      !data.extraSettings.tabId
    ) {
      return;
    }

    tab = OB.MainView.TabSet.tabs.find('tabId', data.extraSettings.tabId);
    if (
      tab &&
      tab.pane &&
      tab.pane.view &&
      tab.pane.view.messageBar &&
      tab.pane.view.messageBar.setMessage
    ) {
      tab.pane.view.messageBar.setMessage(
        isc.OBMessageBar.TYPE_INFO,
        null,
        OB.I18N.getLabel(data.extraSettings.messageKey)
      );
    }
  }
};
