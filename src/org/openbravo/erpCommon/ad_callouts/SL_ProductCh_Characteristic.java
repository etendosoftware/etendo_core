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
 * All portions are Copyright (C) 2013-2016 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.ad_callouts;

import javax.servlet.ServletException;

import org.apache.commons.lang3.StringUtils;
import org.openbravo.base.filter.IsIDFilter;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.plm.Characteristic;

public class SL_ProductCh_Characteristic extends SimpleCallout {

  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    final String strCharacteristicId = info.getStringParameter("inpmCharacteristicId",
        IsIDFilter.instance);
    if (StringUtils.isNotBlank(strCharacteristicId)) {
      Characteristic ch = OBDal.getInstance().get(Characteristic.class, strCharacteristicId);
      info.addResult("inpisvariant", ch.isVariant() ? "Y" : "N");
      info.addResult("inpisexplodeconf", ch.isExplodeConfigurationTab() ? "Y" : "N");
    }
  }

}
