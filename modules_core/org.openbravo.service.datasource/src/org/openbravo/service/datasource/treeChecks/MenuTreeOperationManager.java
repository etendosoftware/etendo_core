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
 * All portions are Copyright (C) 2014-2015 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.service.datasource.treeChecks;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import org.hibernate.criterion.Restrictions;
import org.openbravo.client.kernel.ComponentProvider.Qualifier;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.module.Module;
import org.openbravo.model.ad.ui.Menu;
import org.openbravo.service.datasource.CheckTreeOperationManager;

@ApplicationScoped
@Qualifier("ADMenu")
public class MenuTreeOperationManager extends CheckTreeOperationManager {
  private final String TEMPLATE_TYPE = "T";

  /**
   * Only allows to move a node if it belong to a module that is in development
   */
  @Override
  public ActionResponse checkNodeMovement(Map<String, String> parameters, String nodeId,
      String newParentId, String prevNodeId, String nextNodeId) {
    Menu menuEntry = OBDal.getInstance().get(Menu.class, nodeId);
    Module module = menuEntry.getModule();

    if (module != null && !module.isInDevelopment() && !areThereTemplatesInDevelopment()) {
      return new ActionResponse(false, "error",
          OBMessageUtils.messageBD("OBSERDS_ReparentNotInDevTreeNode"));
    } else {
      return new ActionResponse(true);
    }
  }

  private boolean areThereTemplatesInDevelopment() {
    OBCriteria<Module> templatesInDevelopmentCriteria = OBDal.getInstance()
        .createCriteria(Module.class);
    templatesInDevelopmentCriteria.add(Restrictions.eq(Module.PROPERTY_TYPE, TEMPLATE_TYPE));
    templatesInDevelopmentCriteria.add(Restrictions.eq(Module.PROPERTY_INDEVELOPMENT, true));
    return (templatesInDevelopmentCriteria.count() > 0);
  }
}
