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
 * All portions are Copyright (C) 2010-2017 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.erpCommon.ad_callouts;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.filter.IsIDFilter;
import org.openbravo.base.filter.ValueListFilter;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.module.Module;

/** Prevents having more than one template in development. */
public class SL_ModuleCallout extends SimpleCallout {

  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    String strADModuleID = info.getStringParameter("inpadModuleId", IsIDFilter.instance);
    String moduleType = info.getStringParameter("inptype");
    String isInDev = info.getStringParameter("inpisindevelopment", new ValueListFilter("Y", "N"));

    if (StringUtils.equals(isInDev, "Y") && StringUtils.equals(moduleType, "T")) {
      // Check whether there are more templates in development
      OBCriteria<Module> obc = OBDal.getInstance().createCriteria(Module.class);
      obc.add(Restrictions.eq(Module.PROPERTY_TYPE, "T"));
      obc.add(Restrictions.eq(Module.PROPERTY_INDEVELOPMENT, true));
      if (StringUtils.isNotEmpty(strADModuleID)) {
        obc.add(Restrictions.ne(Module.PROPERTY_ID, strADModuleID));
      }

      if (!obc.list().isEmpty()) {
        String devTemplates = "";
        for (Module template : obc.list()) {
          devTemplates += template.getName() + " ";
        }
        info.showMessage(devTemplates + " "
            + Utility.messageBD(this, "MultipleDevelopmentTemplates", info.vars.getLanguage()));
        info.addResult("inpisindevelopment", "N");
      }
    }
  }
}
