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
 * All portions are Copyright (C) 2013 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  
 *************************************************************************
 */
package org.openbravo.advpaymentmngt;

import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.model.common.invoice.Invoice;

/**
 * @author <a href="mailto:daniel.martins@xpand-it.com">Daniel Martins</a>
 * 
 */
public interface ProcessInvoiceHook {

  /*
   * Returns an OBError when an error occurred and null if it succeed
   */
  public OBError preProcess(Invoice invoice, String strDocAction);

  /*
   * Returns an OBError when an error occurred and null if it succeed
   */
  public OBError postProcess(Invoice invoice, String strDocAction);
}
