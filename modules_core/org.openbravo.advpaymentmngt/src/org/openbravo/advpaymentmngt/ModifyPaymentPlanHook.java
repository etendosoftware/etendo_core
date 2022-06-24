/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.0  (the  "License"),  being   the  Mozilla   Public  License
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
 * Contributor(s):  
 *************************************************************************
 */
package org.openbravo.advpaymentmngt;

import java.util.List;

import org.openbravo.base.exception.OBException;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentSchedule;

/***
 * Abstract class created to implement hooks inside Modify Payment Plan process. All hooks in that
 * process must extend this class so they implement the methods
 * 
 * @author Andy Armaignac Collazo
 *
 */
public abstract class ModifyPaymentPlanHook {

  /***
   * Method to validate the modified payment schedule
   * 
   * @param modifiedPaymentSchedule
   * 
   * @throws OBException
   */
  public abstract void validatePaymentSchedule(List<FIN_PaymentSchedule> modifiedPaymentSchedule)
      throws OBException;
}
