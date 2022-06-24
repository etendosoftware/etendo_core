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
 * All portions are Copyright (C) 2018-2020 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.common.actionhandler.createlinesfromprocess;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.client.kernel.ComponentProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceLine;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;

public class CreateInvoiceLinesFromProcess {
  private static final Logger log = LogManager.getLogger();

  @Inject
  @Any
  private Instance<CreateLinesFromProcessHook> createLinesFromProcessHooks;

  // The class of the objects from which the invoice lines will be created
  private Class<? extends BaseOBObject> linesFromClass;
  // Last Line number of the Processing Line
  private Long lastLineNo = 0L;

  /**
   * This process copies the selected Lines into the Invoice that is being processed by this same
   * Process
   * <ul>
   * <li>1. Update Invoice and Invoice Line related information</li>
   * <li>2. Copy product and attributes</li>
   * <li>3. Calculate amounts and UOM's</li>
   * <li>4. Calculate Prices based on price list</li>
   * <li>5. Calculate Acc and Def Plan from Product</li>
   * <li>6. Recalculate Taxes</li>
   * </ul>
   * 
   * @param selectedLinesParam
   *          Order/InOut Lines selected by the user from which the invoice lines will be created
   * @param currentInvoice
   *          The invoice currently being created
   * @param selectedLinesFromClass
   *          The class of the lines being copied (Order/InOut)
   * 
   * @return The number of invoice lines created by the process
   */
  public int createInvoiceLinesFromDocumentLines(final JSONArray selectedLinesParam,
      final Invoice currentInvoice, final Class<? extends BaseOBObject> selectedLinesFromClass) {
    OBContext.setAdminMode(true);
    try {
      validateAndSetLinesFromClassOrThrowException(selectedLinesFromClass);
      // Initialize the line number with the last one in the processing invoice.
      lastLineNo = getLastLineNoOfCurrentInvoice(currentInvoice);
      return createInvoiceLines(currentInvoice, getLinesToProcess(selectedLinesParam));
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private void validateAndSetLinesFromClassOrThrowException(
      final Class<? extends BaseOBObject> clazz) {
    if (clazz.isAssignableFrom(OrderLine.class)
        || clazz.isAssignableFrom(ShipmentInOutLine.class)) {
      this.linesFromClass = clazz;
    } else {
      throw new OBException("CreateLinesFromProccessInvalidDocumentType");
    }
  }

  private Long getLastLineNoOfCurrentInvoice(final Invoice currentInvoice) {
    OBCriteria<InvoiceLine> obc = OBDal.getInstance().createCriteria(InvoiceLine.class);
    obc.add(Restrictions.eq(InvoiceLine.PROPERTY_INVOICE, currentInvoice));
    obc.setProjection(Projections.max(InvoiceLine.PROPERTY_LINENO));
    Long lineNumber = 0L;
    obc.setMaxResults(1);
    Object o = obc.uniqueResult();
    if (o != null) {
      lineNumber = (Long) o;
    }
    return lineNumber;
  }

  /**
   * Loop over the lines selected by the user and returns a JSONArray with the lines from which an
   * invoice line will be created.
   * 
   * If either the line is an order not related to an InOut or if the line is an InOut line, it
   * directly adds it to the lines to be processed.
   * 
   * When the line belongs to an order linked to an InOut, it adds any related InOut line (instead
   * of the order line itself) to the lines to be processed.
   * 
   * 
   */
  private JSONArray getLinesToProcess(final JSONArray selectedLinesParam) throws JSONException {
    final JSONArray linesToProcess = new JSONArray();
    for (int index = 0; index < selectedLinesParam.length(); index++) {
      JSONObject selectedLine = selectedLinesParam.getJSONObject(index);
      BaseOBObject copiedLine = OBDal.getInstance()
          .get(linesFromClass, selectedLine.getString("id"));
      List<JSONObject> relatedInOutLinesNotAlreadyInvoiced = getRelatedInOutLinesNotAlreadyInvoiced(
          selectedLine, copiedLine);
      if (relatedInOutLinesNotAlreadyInvoiced != null
          && !relatedInOutLinesNotAlreadyInvoiced.isEmpty()) {
        for (JSONObject inOutLineRelatedToOrderLine : relatedInOutLinesNotAlreadyInvoiced) {
          linesToProcess.put(inOutLineRelatedToOrderLine);
        }
      } else {
        // Two possibilities: Order line not linked to InOut line or InOut line
        linesToProcess.put(selectedLine);
      }
    }
    return linesToProcess;
  }

  private List<JSONObject> getRelatedInOutLinesNotAlreadyInvoiced(JSONObject selectedLine,
      BaseOBObject copiedLine) {
    boolean isOrderLineWithRelatedShipmentReceiptLines = CreateLinesFromUtil
        .isOrderLineWithRelatedShipmentReceiptLines(copiedLine, selectedLine);
    List<JSONObject> relatedInOutLinesNotAlreadyInvoiced = null;
    if (isOrderLineWithRelatedShipmentReceiptLines) {
      relatedInOutLinesNotAlreadyInvoiced = getRelatedNotInvoicedInOutLinesAsJSONObjects(
          (OrderLine) copiedLine);
    }
    return relatedInOutLinesNotAlreadyInvoiced;
  }

  private List<JSONObject> getRelatedNotInvoicedInOutLinesAsJSONObjects(final OrderLine orderLine) {
    final List<JSONObject> relatedShipmentLinesToOrderLine = new ArrayList<>();
    for (InOutLineData shipmentInOutLineData : CreateLinesFromUtil
        .getRelatedNotInvoicedInOutLines(orderLine)) {
      relatedShipmentLinesToOrderLine
          .add(getInOutLineJson((OrderLine) orderLine, shipmentInOutLineData));
      OBDal.getInstance()
          .getSession()
          .evict(OBDal.getInstance()
              .getProxy(ShipmentInOutLine.class, shipmentInOutLineData.getShipmentInOutLineId()));
    }
    return relatedShipmentLinesToOrderLine;
  }

  private JSONObject getInOutLineJson(OrderLine orderLine, InOutLineData shipmentInOutLineData) {
    JSONObject line = new JSONObject();
    try {
      line.put("uOM", shipmentInOutLineData.getUOMId());
      line.put("product", orderLine.getProduct().getId());
      line.put("lineNo", orderLine.getLineNo());
      line.put("orderedQuantity", shipmentInOutLineData.getMovementQuantity());
      line.put("operativeQuantity", shipmentInOutLineData.getOperativeQuantity());
      line.put("id", orderLine.getId());
      line.put("salesOrder", orderLine.getSalesOrder().getId());
      line.put("operativeUOM", shipmentInOutLineData.getOperativeUOMId());
      line.put("orderQuantity", shipmentInOutLineData.getOrderQuantity());
      line.put("shipmentInOutLine", shipmentInOutLineData.getShipmentInOutLineId());
      return line;
    } catch (JSONException e) {
      throw new OBException(e);
    }
  }

  private int createInvoiceLines(final Invoice currentInvoice, final JSONArray linesToProcess)
      throws JSONException {
    int createdInvoiceLinesCount = 0;
    for (int index = 0; index < linesToProcess.length(); index++) {
      JSONObject selectedLineJS = linesToProcess.getJSONObject(index);
      BaseOBObject createdFromLine = OBDal.getInstance()
          .get(linesFromClass, selectedLineJS.getString("id"));
      InvoiceLine newInvoiceLine = createLineFromSelectedLineAndRunHooks(currentInvoice,
          createdFromLine, selectedLineJS);
      currentInvoice.getInvoiceLineList().add(newInvoiceLine);
      OBDal.getInstance().save(newInvoiceLine);
      OBDal.getInstance().save(currentInvoice);
      new InsertAcctDimensions(newInvoiceLine, createdFromLine).createAndSaveAcctDimensionLines();
      // Flush is needed to persist this created invoice line in the database to be taken into
      // account when the invoice's order reference is updated at document level
      OBDal.getInstance().flush();
      createdInvoiceLinesCount++;
    }
    return createdInvoiceLinesCount;
  }

  /**
   * Creates a new invoice line from an existing line
   * 
   * @param currentInvoice
   *          The invoice currently being created
   * @param createdFromLine
   *          The BaseOBObject representing the selected object in the PE
   * @param selectedLine
   *          The JSONOBject representing the selected object in the PE
   * @return The created invoice line
   */
  private InvoiceLine createLineFromSelectedLineAndRunHooks(final Invoice currentInvoice,
      final BaseOBObject createdFromLine, final JSONObject pickExecuteLineValues) {
    InvoiceLine newInvoiceLine = OBProvider.getInstance().get(InvoiceLine.class);

    // Always increment the lineNo when adding a new invoice line
    newInvoiceLine.setLineNo(nextLineNo());
    newInvoiceLine.setInvoice(currentInvoice);

    // Execute Hooks to perform operations
    executeHooks(pickExecuteLineValues, createdFromLine, newInvoiceLine);

    return newInvoiceLine;
  }

  private Long nextLineNo() {
    lastLineNo = lastLineNo + 10L;
    return lastLineNo;
  }

  private void executeHooks(JSONObject pickExecuteLineValues, final BaseOBObject createdFromLine,
      InvoiceLine newInvoiceLine) {
    try {
      if (createLinesFromProcessHooks != null) {
        final List<CreateLinesFromProcessHook> hooks = new ArrayList<>();
        for (CreateLinesFromProcessHook hook : createLinesFromProcessHooks
            .select(new ComponentProvider.Selector(
                CreateLinesFromProcessHook.CREATE_LINES_FROM_PROCESS_HOOK_QUALIFIER))) {
          if (hook != null) {
            hooks.add(hook);
          }
        }

        Collections.sort(hooks, new CreateLinesFromHookComparator());
        for (CreateLinesFromProcessHook hook : hooks) {
          hook.initAndExecute(newInvoiceLine, pickExecuteLineValues, createdFromLine);
        }
      }
    } catch (Exception e) {
      log.error("Error in CreateLinesFromProcess executing hooks.", e);
      throw new OBException(e);
    }
  }

  private static class CreateLinesFromHookComparator
      implements Comparator<CreateLinesFromProcessHook> {
    @Override
    public int compare(CreateLinesFromProcessHook a, CreateLinesFromProcessHook b) {
      return a.getOrder() < b.getOrder() ? -1 : a.getOrder() == b.getOrder() ? 0 : 1;
    }
  }
}
