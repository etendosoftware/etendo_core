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
 * All portions are Copyright (C) 2010-2018 Openbravo SLU 
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
import org.openbravo.model.ad.datamodel.Table;

/** After setting audit trail for a table, shows message with next steps */
public class SL_TableAudit extends SimpleCallout {

  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    String strChanged = info.getLastFieldChanged();

    if (StringUtils.equalsIgnoreCase(strChanged, "inpisfullyaudited")) {
      String strIsFullyAudited = info.getStringParameter("inpisfullyaudited");
      boolean currentRecordFullyAudited = StringUtils.equals(strIsFullyAudited, "Y");
      if (currentRecordFullyAudited) {
        OBCriteria<Table> qTables = OBDal.getInstance().createCriteria(Table.class);
        qTables.add(Restrictions.eq(Table.PROPERTY_ISFULLYAUDITED, true));
        qTables.add(Restrictions.eq(Table.PROPERTY_ISAUDITINSERTS, true));
        if (qTables.count() == 0) {
          info.addResult("inpisauditinserts", "N");
        } else {
          info.addResult("inpisauditinserts", "Y");
        }
        info.showMessage(
            Utility.messageBD(this, "RegenerateAudit_ExcludeColumn", info.vars.getLanguage()));
      } else {
        info.showMessage(Utility.messageBD(this, "RegenerateAudit", info.vars.getLanguage()));
      }
    } else if (StringUtils.equalsIgnoreCase(strChanged, "inpisexcludeaudit")) {
      info.showMessage(Utility.messageBD(this, "RegenerateAudit", info.vars.getLanguage()));
    }
  }
}
