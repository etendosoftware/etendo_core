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
package org.openbravo.advpaymentmngt.actionHandler;

import java.util.Iterator;
import java.util.List;

import org.openbravo.advpaymentmngt.ModifyPaymentPlanHook;
import org.openbravo.base.exception.OBException;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentSchedule;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@Dependent
public class ModifyPaymentPlanHookCaller {

  @Inject
  @Any
  private Instance<ModifyPaymentPlanHook> hooks;

  public void validatePaymentSchedule(List<FIN_PaymentSchedule> modifiedPaymentSchedule)
      throws OBException {
    for (Iterator<ModifyPaymentPlanHook> procIter = hooks.iterator(); procIter.hasNext();) {
      ModifyPaymentPlanHook proc = procIter.next();
      proc.validatePaymentSchedule(modifiedPaymentSchedule);
    }
  }
}
