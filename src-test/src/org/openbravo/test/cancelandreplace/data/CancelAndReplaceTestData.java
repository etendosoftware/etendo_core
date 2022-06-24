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
 * All portions are Copyright (C) 2016-2019 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.cancelandreplace.data;

import java.util.List;

public abstract class CancelAndReplaceTestData {

  /*
   * CONSTANTS:
   */
  protected static final String BP_CUSTOMER_A = "4028E6C72959682B01295F40C3CB02EC";
  // Sales order: 50017
  protected static final String SALESORDER_50017_ID = "F1AAB8C608AA434C9FC7FC1D685BA016";
  // Sales order: 50011
  protected static final String SALESORDER_50011_ID = "80AAF8AC57EC4A7EB2F85B3B2675F88C";
  // Sales order: 50008
  protected static final String SALESORDER_50008_ID = "A88E5504A1D5443CA4B73EDFB143B848";

  private String testNumber;
  private String testDescription;
  private String bpartnerId;
  private String cloneOrderId;
  private boolean activateNettingGoodsShipmentPref;
  private boolean activateAssociateNettingGoodsShipmentPref;
  private boolean orderPaid;
  private CancelAndReplaceOrderTestData oldOrder;
  private CancelAndReplaceOrderTestData inverseOrder;
  private List<CancelAndReplaceOrderTestData> newOrders;

  private String errorMessage;

  public String getBpartnerId() {
    return bpartnerId;
  }

  public void setBpartnerId(String bpartnerId) {
    this.bpartnerId = bpartnerId;
  }

  public String getTestDescription() {
    return testDescription;
  }

  public void setTestDescription(String testDescription) {
    this.testDescription = testDescription;
  }

  public String getTestNumber() {
    return testNumber;
  }

  public void setTestNumber(String testNumber) {
    this.testNumber = testNumber;
  }

  public CancelAndReplaceOrderTestData getOldOrder() {
    return oldOrder;
  }

  public void setOldOrder(CancelAndReplaceOrderTestData oldOrder) {
    this.oldOrder = oldOrder;
  }

  public CancelAndReplaceOrderTestData getInverseOrder() {
    return inverseOrder;
  }

  public void setInverseOrder(CancelAndReplaceOrderTestData inverseOrder) {
    this.inverseOrder = inverseOrder;
  }

  public void setNewOrders(List<CancelAndReplaceOrderTestData> newOrders) {
    this.newOrders = newOrders;
  }

  public List<CancelAndReplaceOrderTestData> getNewOrders() {
    return newOrders;
  }

  public boolean isActivateNettingGoodsShipmentPref() {
    return activateNettingGoodsShipmentPref;
  }

  public void setActivateNettingGoodsShipmentPref(boolean activateNettingGoodsShipmentPref) {
    this.activateNettingGoodsShipmentPref = activateNettingGoodsShipmentPref;
  }

  public boolean isActivateAssociateNettingGoodsShipmentPref() {
    return activateAssociateNettingGoodsShipmentPref;
  }

  public void setActivateAssociateNettingGoodsShipmentPref(
      boolean activateAssociateNettingGoodsShipmentPref) {
    this.activateAssociateNettingGoodsShipmentPref = activateAssociateNettingGoodsShipmentPref;
  }

  public CancelAndReplaceTestData() {
    initialize();
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public abstract void initialize();

  public boolean isOrderPaid() {
    return orderPaid;
  }

  public void setOrderPaid(boolean orderPaid) {
    this.orderPaid = orderPaid;
  }

  public String getCloneOrderId() {
    return cloneOrderId;
  }

  public void setCloneOrderId(String cloneOrderId) {
    this.cloneOrderId = cloneOrderId;
  }

}
