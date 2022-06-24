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
 * All portions are Copyright (C) 2016 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  
 *************************************************************************
 */
package org.openbravo.advpaymentmngt;

import java.util.List;

import org.openbravo.model.financialmgmt.payment.FIN_FinaccTransaction;

/**
 * Interface to be used to extend the Funds Transfer functionality.
 * 
 * @author Daniel Martins
 * 
 */
public interface FundsTransferPostProcessHook {

  /**
   * Method to implement in extension classes. This method will be called to extend functionality.
   * 
   * @param transactions
   *          A list of all transactions created by Funds Transfer
   */
  public void exec(List<FIN_FinaccTransaction> transactions) throws Exception;
}
