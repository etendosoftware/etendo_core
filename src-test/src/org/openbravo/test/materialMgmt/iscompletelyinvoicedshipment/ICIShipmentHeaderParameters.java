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

package org.openbravo.test.materialMgmt.iscompletelyinvoicedshipment;

import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.enterprise.Locator;
import org.openbravo.model.common.order.Order;

public class ICIShipmentHeaderParameters {

  private Order order;
  private boolean issotrx;
  private Locator locator;
  int day;

  public ICIShipmentHeaderParameters(Order order) {
    this.order = order;
    this.issotrx = true;
    this.locator = OBDal.getInstance().get(Locator.class, ICIConstants.LOCATOR_RN_000_ID);
    this.day = 0;
  }

  public Order getOrder() {
    return order;
  }

  public void setOrder(Order order) {
    this.order = order;
  }

  public boolean isIssotrx() {
    return issotrx;
  }

  public void setIssotrx(boolean issotrx) {
    this.issotrx = issotrx;
  }

  public Locator getLocator() {
    return locator;
  }

  public void setLocator(Locator locator) {
    this.locator = locator;
  }

  public int getDay() {
    return day;
  }

  public void setDay(int day) {
    this.day = day;
  }

}
