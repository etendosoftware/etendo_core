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
 * All portions are Copyright (C) 2009-2017 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.ad_callouts;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.module.Module;

/**
 * A callout used in the module dependency form. When the dependent module changes then the minor
 * version is set to the version of the dependent module.
 * 
 * @author mtaal
 */
public class SL_Module_Minor_Version extends SimpleCallout {

  private static final String DEPENDENT_MODULE_FIELD = "inpadDependentModuleId";
  private static final String MINOR_VERSION_FIELD = "inpstartversion";

  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    String strChanged = info.getLastFieldChanged();
    String strModule = info.getStringParameter(DEPENDENT_MODULE_FIELD);

    // Set Minor Version
    if (StringUtils.equals(strChanged, DEPENDENT_MODULE_FIELD)) {
      final Module dependsOnModule = OBDal.getInstance().get(Module.class, strModule);
      info.addResult(MINOR_VERSION_FIELD, dependsOnModule.getVersion());
    }
  }
}
