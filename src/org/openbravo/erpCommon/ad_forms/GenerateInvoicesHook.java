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

package org.openbravo.erpCommon.ad_forms;

import java.sql.Connection;
import java.util.List;

import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.model.common.order.Order;

/**
 * @author <a href="mailto:daniel.martins@xpand-it.com">Daniel Martins</a>
 * 
 */
public interface GenerateInvoicesHook {

  /*
   * Returns an OBError when an error occurred and null if it succeed
   */
  public OBError executeHook(Connection conn, List<Order> orders, OBError myMessage);
}
