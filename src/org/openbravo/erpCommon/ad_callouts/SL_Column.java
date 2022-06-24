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
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.datamodel.Column;

/** Checks there is only one key column per table */
public class SL_Column extends SimpleCallout {
  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    String strIsKey = info.getStringParameter("inpiskey");
    String strTableId = info.getStringParameter("inpadTableId");

    if (StringUtils.equals(strIsKey, "Y")) {
      OBCriteria<Column> keyCriteria = OBDal.getInstance().createCriteria(Column.class);
      keyCriteria.add(Restrictions.eq(Column.PROPERTY_TABLE + ".id", strTableId));
      keyCriteria.add(Restrictions.eq(Column.PROPERTY_KEYCOLUMN, true));
      if (keyCriteria.count() != 0) {
        info.addResult("inpiskey", "N");
        info.showWarning(Utility.messageBD(this, "MultipleKeyColumns", info.vars.getLanguage())
            + keyCriteria.list().get(0).getDBColumnName());
      }
    }
  }
}
