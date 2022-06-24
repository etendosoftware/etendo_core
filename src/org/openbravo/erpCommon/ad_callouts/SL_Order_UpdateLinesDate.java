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

import org.hibernate.criterion.Restrictions;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.ui.Field;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.model.ad.ui.Window;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;

public class SL_Order_UpdateLinesDate extends SimpleCallout {

  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    try {
      OBContext.setAdminMode();
      String lastChanged = info.getStringParameter("inpLastFieldChanged", null);
      String id = info.getStringParameter("C_Order_ID", null);

      /*
       * Fetching fields and check whether its visible
       */
      String fieldsToCompare = null;
      if (info.getWindowId() != null) {
        Tab orderLineTab = null;
        OBCriteria<Tab> tabCriteria = OBDal.getInstance().createCriteria(Tab.class);
        tabCriteria.add(Restrictions.eq(Tab.PROPERTY_WINDOW,
            OBDal.getInstance().get(Window.class, info.getWindowId())));
        for (Tab tab : tabCriteria.list()) {
          if (tab.getTable().getDBTableName().toUpperCase().equals("C_ORDERLINE")) {
            orderLineTab = tab;
            break;
          }
        }

        OBCriteria<Field> fieldCriteria = OBDal.getInstance().createCriteria(Field.class, null);
        fieldCriteria.add(Restrictions.eq(Field.PROPERTY_TAB, orderLineTab));
        for (Field field : fieldCriteria.list()) {
          if ((field.isDisplayed()
              && field.getColumn().getDBColumnName().toUpperCase().equals("DATEORDERED")
              && lastChanged.equals("inpdateordered"))
              || (field.isDisplayed()
                  && field.getColumn().getDBColumnName().toUpperCase().equals("DATEPROMISED")
                  && lastChanged.equals("inpdatepromised"))) {
            if (fieldsToCompare == null) {
              fieldsToCompare = field.getName();
            } else {
              fieldsToCompare = fieldsToCompare.concat(field.getName());
            }
          }
        }
      }

      /*
       * Set dateordered and datepromised in orderlines with dateordered and datepromised in order
       */
      OBCriteria<OrderLine> orderLineCriteria = OBDal.getInstance().createCriteria(OrderLine.class);
      orderLineCriteria.add(
          Restrictions.eq(OrderLine.PROPERTY_SALESORDER, OBDal.getInstance().get(Order.class, id)));
      if (fieldsToCompare != null && orderLineCriteria.count() > 0) {
        String message = String.format(
            Utility.messageBD(this, "OrderLineUpdate", info.vars.getLanguage()), fieldsToCompare,
            fieldsToCompare);
        info.addResult("WARNING", message);
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
