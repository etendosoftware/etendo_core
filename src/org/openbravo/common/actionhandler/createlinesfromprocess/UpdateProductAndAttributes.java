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

package org.openbravo.common.actionhandler.createlinesfromprocess;

import javax.enterprise.context.Dependent;

import org.openbravo.client.kernel.ComponentProvider.Qualifier;
import org.openbravo.model.common.plm.AttributeSetInstance;
import org.openbravo.model.common.plm.Product;

@Dependent
@Qualifier(CreateLinesFromProcessHook.CREATE_LINES_FROM_PROCESS_HOOK_QUALIFIER)
class UpdateProductAndAttributes extends CreateLinesFromProcessHook {

  @Override
  public int getOrder() {
    return -50;
  }

  /**
   * Update the product and attribute set to the new invoice line
   */
  @Override
  public void exec() {
    getInvoiceLine().setProduct((Product) getCopiedFromLine().get("product"));
    getInvoiceLine()
        .setAttributeSetValue((AttributeSetInstance) getCopiedFromLine().get("attributeSetValue"));
  }
}
