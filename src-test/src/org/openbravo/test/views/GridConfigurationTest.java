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

package org.openbravo.test.views;

import static org.hibernate.criterion.Restrictions.in;
import static org.hibernate.criterion.Restrictions.not;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.openbravo.client.application.GCSystem;
import org.openbravo.client.application.GCTab;
import org.openbravo.client.application.window.StandardWindowComponent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.test.base.OBBaseTest;

public class GridConfigurationTest extends OBBaseTest {

  /**
   * Standard Grid Configuration IDs
   */
  private static final List<String> CORE_DEFAULT_GRID_CONFIGS = Arrays.asList(
      "4701BC23719C41FAA422305FCDBBAF85", "FDA9AFD8D7504E18A220EFC01F5D28D3",
      "1AD989605ACA4F5FB6C11B2E7AC88867");

  /**
   * @return the current number of grid configurations defined in the system.
   */
  protected static int getNumberOfGridConfigurations() {
    OBContext.setAdminMode(false);
    try {
      OBCriteria<GCSystem> systemGridConfig = OBDal.getInstance().createCriteria(GCSystem.class);
      OBCriteria<GCTab> tabGridConfig = OBDal.getInstance().createCriteria(GCTab.class);
      tabGridConfig.add(not(in(GCTab.PROPERTY_ID, CORE_DEFAULT_GRID_CONFIGS)));
      return systemGridConfig.count() + tabGridConfig.count();
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  protected static Optional<GCSystem> getSystemGridConfig() {
    return StandardWindowComponent.getSystemGridConfig();
  }

  protected static Optional<GCTab> getTabGridConfig(Tab tab) {
    return StandardWindowComponent.getTabsGridConfig(tab.getWindow()).get(tab.getId());
  }
}
