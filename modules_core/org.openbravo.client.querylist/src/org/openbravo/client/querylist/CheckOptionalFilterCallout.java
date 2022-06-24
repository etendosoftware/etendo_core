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

/**
 * Checks if the hql query enabled if can be filtered check box is selected in column tab.
 *
 * @author shankar balachandran
 */

package org.openbravo.client.querylist;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.erpCommon.utility.Utility;

public class CheckOptionalFilterCallout extends SimpleCallout {

  private static final String OPTIONAL_FILTERS = "@optional_filters@";
  private static final String warningMessage = "OBUIAPP_CheckOptionalFilters";

  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    String lastChanged = null, lastChangedValue = null, obcqlQueryColumnId = null, HQL = null,
        parsedMessage = null;

    // retrieving the last changed value
    lastChanged = info.getStringParameter("inpLastFieldChanged", null);
    lastChangedValue = info.getStringParameter(lastChanged, null);

    // check hql if 'can be filtered' value is changed
    if ("inpcanBeFiltered".equals(lastChanged)) {
      if ("Y".equals(lastChangedValue)) {
        obcqlQueryColumnId = info.getStringParameter("inpobcqlQueryColumnId", null);
        if (OBDal.getInstance().exists("OBCQL_QueryColumn", obcqlQueryColumnId)) {
          OBCQL_QueryColumn obcql_QueryColumn = OBDal.getInstance()
              .get(OBCQL_QueryColumn.class, obcqlQueryColumnId);
          HQL = obcql_QueryColumn.getWidgetQuery().getHQL();

          // check whether HQL contains @optional_filters@
          if (StringUtils.isNotEmpty(HQL)) {
            if (!HQL.contains(OPTIONAL_FILTERS)) {
              // translate message before display
              parsedMessage = Utility.messageBD(this, warningMessage,
                  OBContext.getOBContext().getLanguage().getId());
              info.addResult("WARNING", parsedMessage);
            }
          }
        }
      }
    }
  }
}
