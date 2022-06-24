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
 * All portions are Copyright (C) 2016 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.client.application.window;

import javax.servlet.ServletException;

import org.hibernate.criterion.Restrictions;
import org.openbravo.client.application.GCSystem;
import org.openbravo.client.application.GCTab;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.ui.Tab;

/**
 * Checks if there are more than one grid configuration with the same sequence number. If there are,
 * a warning message is shown.
 *
 * @author NaroaIriarte
 */
public class GridConfigurationSequenceNumberCallout extends SimpleCallout {

  private static final String GC_SYSTEM_TAB_ID = "13FE911F7F684A47801DF55525BAD4A1";
  private static final String GC_TAB_TAB_ID = "49B33DC2EDFD45A48EECE139AD5E9AC9";
  private static final String warningMessage = "SameSeqNoForGridConfiguration";

  @Override
  public void execute(CalloutInfo info) throws ServletException {
    Long configurationSequence;
    try {
      String seq = info.getStringParameter("inpseqno", null);
      configurationSequence = Long.parseLong(seq);
    } catch (NumberFormatException ignore) {
      return;
    }

    if (info.getTabId().equals(GC_TAB_TAB_ID)) {
      String tabOfGcTabId = info.getStringParameter("inpadTabId", null);
      Tab myTab = OBDal.getInstance().get(Tab.class, tabOfGcTabId);
      OBCriteria<GCTab> gcTabCriteria = OBDal.getInstance().createCriteria(GCTab.class);
      gcTabCriteria.add(Restrictions.and(Restrictions.eq(GCTab.PROPERTY_TAB, myTab),
          Restrictions.eq(GCTab.PROPERTY_SEQNO, configurationSequence)));
      int countGCTabCriteria = gcTabCriteria.count();
      createWarningMessage(info, countGCTabCriteria);
    } else if (info.getTabId().equals(GC_SYSTEM_TAB_ID)) {
      OBCriteria<GCSystem> gcSystemCriteria = OBDal.getInstance().createCriteria(GCSystem.class);
      gcSystemCriteria.add(Restrictions.eq(GCSystem.PROPERTY_SEQNO, configurationSequence));
      int countGCSystemCriteria = gcSystemCriteria.count();
      createWarningMessage(info, countGCSystemCriteria);
    }
  }

  private void createWarningMessage(CalloutInfo info, int gcCount) {
    if (gcCount > 0) {
      String parsedMessage = Utility.messageBD(this, warningMessage,
          OBContext.getOBContext().getLanguage().getId());
      info.addResult("WARNING", parsedMessage);
    }
  }
}
