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

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceLine;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;

/**
 * Interface to be implemented by the hooks to be executed on the Create Lines From Order process.
 * 
 * Example of a hook:
 * 
 * <pre>
 * &#064;Dependent
 * &#064;Qualifier(CreateLinesFromProcessHook.CREATE_LINES_FROM_PROCESS_HOOK_QUALIFIER)
 * public class TestHook implements CreateLinesFromProcessHook {
 * 
 *   &#064;Override
 *   public int getOrder() {
 *     return 10;
 *   }
 * 
 *   &#064;Override
 *   public void exec() {
 *     getInvoiceLine().setDescription(&quot;Test&quot;);
 *   }
 * }
 * </pre>
 * 
 * This class has useful public method to access to the invoice and invoice line currently being
 * created, the BaseOBObject line (Order/InOut) from which the invoice line is being created or even
 * the JSONObject from which the invoice line is being created.
 *
 */
public abstract class CreateLinesFromProcessHook {
  public static final String CREATE_LINES_FROM_PROCESS_HOOK_QUALIFIER = "CreatelinesFromProcessHookQualifier";

  private Invoice invoice;
  private InvoiceLine invoiceLine; // Not saved yet!
  private String copiedFromLineId;
  private boolean isCopiedFromOrderLine;
  private JSONObject pickedJSONObject;

  /**
   * Returns the order when the concrete hook will be implemented. A positive value will execute the
   * hook after the core's logic
   */
  public abstract int getOrder();

  /**
   * Executes the hook logic on the Create Lines From process
   * 
   */
  public abstract void exec();

  void initAndExecute(final InvoiceLine newInvoiceLine, final JSONObject pickExecuteLineValues,
      final BaseOBObject copiedFromLine) {
    init(newInvoiceLine, pickExecuteLineValues, copiedFromLine);
    exec();
  }

  private void init(final InvoiceLine newInvoiceLine, final JSONObject pickExecuteLineValues,
      final BaseOBObject copiedFromLine) {
    this.invoice = newInvoiceLine.getInvoice();
    this.invoiceLine = newInvoiceLine;
    this.copiedFromLineId = (String) copiedFromLine.getId();
    this.isCopiedFromOrderLine = CreateLinesFromUtil.isOrderLine(copiedFromLine);
    this.pickedJSONObject = pickExecuteLineValues;
  }

  /**
   * Returns the Invoice currently being created
   */
  public Invoice getInvoice() {
    return invoice;
  }

  /**
   * Returns the Invoice Line currently being created
   */
  public InvoiceLine getInvoiceLine() {
    return invoiceLine;
  }

  /**
   * Returns the line from which the invoice line will be created. It can be either a shipment or an
   * order line
   */
  public BaseOBObject getCopiedFromLine() {
    return OBDal.getInstance()
        .getProxy(isCopiedFromOrderLine() ? OrderLine.ENTITY_NAME : ShipmentInOutLine.ENTITY_NAME,
            copiedFromLineId);
  }

  /**
   * Returns true if the line from which the invoice line will be created is an order line.
   */
  public boolean isCopiedFromOrderLine() {
    return isCopiedFromOrderLine;
  }

  public JSONObject getPickExecJSONObject() {
    return pickedJSONObject;
  }

}
