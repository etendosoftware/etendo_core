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

package org.openbravo.test.createlinesfrom;

import static org.junit.Assert.fail;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.base.weld.test.ParameterCdiTest;
import org.openbravo.base.weld.test.ParameterCdiTestRule;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.common.actionhandler.createlinesfromprocess.CreateInvoiceLinesFromProcess;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.openbravo.service.db.CallStoredProcedure;
import org.openbravo.test.createlinesfrom.data.CLFTestDataPO_02;
import org.openbravo.test.createlinesfrom.data.CLFTestDataSO_01;
import org.openbravo.test.createlinesfrom.data.CreateLinesFromTestData;
import org.openbravo.test.createlinesfrom.data.InvoiceLineData;

/**
 * Tests cases to check Create Lines From Orders/Shipments/Receipts process
 * 
 * @author Andy Armaignac
 *
 */
public class CreateLinesFromTest extends WeldBaseTest {
  final static private Logger log = LogManager.getLogger();

  // User Openbravo
  private final String USER_ID = "100";
  // Client QA Testing
  private final String CLIENT_ID = "4028E6C72959682B01295A070852010D";
  // Organization Spain
  private final String ORGANIZATION_ID = "357947E87C284935AD1D783CF6F099A1";
  // Role QA Testing Admin
  private final String ROLE_ID = "4028E6C72959682B01295A071429011E";
  // Order Complete Procedure
  private static final String ORDER_COMPLETE_PROCEDURE_NAME = "c_order_post1";
  // Invoice Complete Procedure
  private static final String INVOICE_COMPLETE_PROCEDURE_NAME = "c_invoice_post";
  // Shipment Complete Procedure
  private static final String SHIPMENT_INOUT_COMPLETE_PROCEDURE_NAME = "m_inout_post";

  // Test information
  private String testNumber;
  private String testDescription;
  private SimpleDateFormat dateFormat;

  @Rule
  public ParameterCdiTestRule<CreateLinesFromTestData> parameterValuesRule = new ParameterCdiTestRule<>(
      PARAMS);

  private @ParameterCdiTest CreateLinesFromTestData data;

  public CreateLinesFromTest() {
    this.dateFormat = new SimpleDateFormat("dd-MM-yyyy");
  }

  public static final List<CreateLinesFromTestData> PARAMS = Arrays.asList(new CLFTestDataSO_01(),
      new CLFTestDataPO_02());

  @Before
  public void before() {
    this.testNumber = data.getTestNumber();
    this.testDescription = data.getTestDescription();
    log.info("Test Started {}: {} ", this.testNumber, this.testDescription);
    // Set QA context
    OBContext.setOBContext(USER_ID, ROLE_ID, CLIENT_ID, ORGANIZATION_ID);
  }

  @After
  public void after() {
    // Reset invoice line data identifiers to avoid wrong data check
    for (InvoiceLineData lineData : data.getInvoiceLineData()) {
      lineData.setOrderLineIdentifier(null);
      lineData.setShipmentInOutLineIdentifier(null);
    }
  }

  /**
   * Execute the test with the current data
   * <ul>
   * <li>Create the Order</li>
   * <li>Check order header and lines</li>
   * <li>Complete the order</li>
   * <li>Check order header and lines</li>
   * <li>Create the Invoice</li>
   * <li>Execute Create Lines From Order process</li>
   * <li>Check invoice header and lines</li>
   * <li>Complete the Invoice</li>
   * <li>Check invoice header and lines</li>
   * </ul>
   */
  @Test
  public void testCreateLinesFromOrders() {
    Order order = data.createOrder();
    data.assertDraftOrder(order);
    order.setDocumentAction("CO");
    order = processOrder(order);
    data.assertCompletedOrder(order);

    Invoice invoice = data.createInvoiceHeader();

    JSONArray selectedLines = createSelectedLinesFromOrder(order);

    // CreateLinesFromProcess is instantiated using Weld so it can use Dependency Injection
    CreateInvoiceLinesFromProcess createLinesFromProcess = WeldUtils
        .getInstanceFromStaticBeanManager(CreateInvoiceLinesFromProcess.class);
    createLinesFromProcess.createInvoiceLinesFromDocumentLines(selectedLines, invoice,
        OrderLine.class);

    OBDal.getInstance().refresh(invoice);
    data.assertDraftInvoice(invoice);
    invoice = processInvoice(invoice);
    data.assertCompletedInvoice(invoice);
  }

  /**
   * Execute the test with the current data
   * <ul>
   * <li>Create the Shipment</li>
   * <li>Check shipment header and lines</li>
   * <li>Complete the shipment</li>
   * <li>Check shipment header and lines</li>
   * <li>Create the Invoice</li>
   * <li>Execute Create Lines From Shipment process</li>
   * <li>Check invoice header and lines</li>
   * <li>Complete the Invoice</li>
   * <li>Check invoice header and lines</li>
   * </ul>
   */
  @Test
  public void testCreateLinesFromShipment() {
    ShipmentInOut shipmentInOut = data.createShipmentInOut();
    data.assertDraftShipmentInOut(shipmentInOut);
    shipmentInOut.setDocumentAction("CO");
    shipmentInOut = processShipmentInOut(shipmentInOut);
    data.assertCompletedShipmentInOut(shipmentInOut);

    Invoice invoice = data.createInvoiceHeader();

    JSONArray selectedLines = createSelectedLinesFromShipmentInOut(shipmentInOut);

    // CreateLinesFromProcess is instantiated using Weld so it can use Dependency Injection
    CreateInvoiceLinesFromProcess createLinesFromProcess = WeldUtils
        .getInstanceFromStaticBeanManager(CreateInvoiceLinesFromProcess.class);
    createLinesFromProcess.createInvoiceLinesFromDocumentLines(selectedLines, invoice,
        ShipmentInOutLine.class);

    OBDal.getInstance().refresh(invoice);
    data.assertDraftInvoice(invoice);
    invoice = processInvoice(invoice);
    data.assertCompletedInvoice(invoice);
  }

  private Order processOrder(Order testOrder) {
    final List<Object> params = new ArrayList<Object>();
    params.add(null);
    params.add(testOrder.getId());
    CallStoredProcedure.getInstance()
        .call(ORDER_COMPLETE_PROCEDURE_NAME, params, null, true, false);
    OBDal.getInstance().refresh(testOrder);
    return testOrder;
  }

  private Invoice processInvoice(Invoice invoice) {
    final List<Object> params = new ArrayList<Object>();
    params.add(null);
    params.add(invoice.getId());
    CallStoredProcedure.getInstance()
        .call(INVOICE_COMPLETE_PROCEDURE_NAME, params, null, true, false);
    OBDal.getInstance().refresh(invoice);
    return invoice;
  }

  private JSONArray createSelectedLinesFromOrder(Order order) {
    JSONArray selectedLines = new JSONArray();
    for (int i = 0; i < order.getOrderLineList().size(); i++) {
      try {
        OrderLine orderLine = order.getOrderLineList().get(i);
        String salesOrderLineIdentifier = String.format("%s - %s - %s - %s - %s",
            order.getDocumentNo(), dateFormat.format(order.getOrderDate()),
            order.getGrandTotalAmount(), orderLine.getLineNo(), order.getSummedLineAmount());

        this.data.getInvoiceLineData().get(i).setOrderLineIdentifier(salesOrderLineIdentifier);

        JSONObject line = new JSONObject();
        line.put("uOM", orderLine.getUOM().getId());
        line.put("uOM$_identifier", orderLine.getUOM().getIdentifier());
        line.put("product", orderLine.getProduct().getId());
        line.put("product$_identifier", orderLine.getProduct().getIdentifier());
        line.put("lineNo", orderLine.getLineNo());
        line.put("orderedQuantity", orderLine.getOrderedQuantity().toString());
        line.put("operativeQuantity",
            orderLine.getOperativeQuantity() == null ? orderLine.getOrderedQuantity().toString()
                : orderLine.getOperativeQuantity().toString());
        line.put("id", orderLine.getId());
        line.put("salesOrder", order.getId());
        line.put("operativeUOM", orderLine.getOperativeUOM() == null ? orderLine.getUOM().getId()
            : orderLine.getOperativeUOM().getId());
        line.put("operativeUOM$_identifier",
            orderLine.getOperativeUOM() == null ? orderLine.getUOM().getIdentifier()
                : orderLine.getOperativeUOM().getIdentifier());
        line.put("orderQuantity", "");
        selectedLines.put(line);
      } catch (JSONException e) {
        fail(e.getMessage());
      }
    }
    return selectedLines;
  }

  private JSONArray createSelectedLinesFromShipmentInOut(ShipmentInOut shipmentInOut) {

    JSONArray selectedLines = new JSONArray();

    for (int i = 0; i < shipmentInOut.getMaterialMgmtShipmentInOutLineList().size(); i++) {
      try {
        ShipmentInOutLine shipmentInOutLine = shipmentInOut.getMaterialMgmtShipmentInOutLineList()
            .get(i);

        String shipmentInOutLineIdentifier = String.format("%s - %s - %s - %s - %s - %s",
            shipmentInOut.getDocumentNo(), dateFormat.format(shipmentInOut.getMovementDate()),
            shipmentInOut.getBusinessPartner().getName(), shipmentInOutLine.getLineNo(),
            shipmentInOutLine.getProduct().getName(), shipmentInOutLine.getMovementQuantity());

        this.data.getInvoiceLineData()
            .get(i)
            .setShipmentInOutLineIdentifier(shipmentInOutLineIdentifier);

        JSONObject line = new JSONObject();
        line.put("uOM", shipmentInOutLine.getUOM().getId());
        line.put("uOM$_identifier", shipmentInOutLine.getUOM().getIdentifier());
        line.put("product", shipmentInOutLine.getProduct().getId());
        line.put("product$_identifier", shipmentInOutLine.getProduct().getIdentifier());
        line.put("lineNo", shipmentInOutLine.getLineNo());
        line.put("movementQuantity", shipmentInOutLine.getMovementQuantity().toString());
        line.put("operativeQuantity",
            shipmentInOutLine.getOperativeQuantity() == null
                ? shipmentInOutLine.getMovementQuantity().toString()
                : shipmentInOutLine.getOperativeQuantity().toString());
        line.put("id", shipmentInOutLine.getId());
        line.put("operativeUOM",
            shipmentInOutLine.getOperativeUOM() == null ? shipmentInOutLine.getUOM().getId()
                : shipmentInOutLine.getOperativeUOM().getId());
        line.put("operativeUOM$_identifier",
            shipmentInOutLine.getOperativeUOM() == null ? shipmentInOutLine.getUOM().getIdentifier()
                : shipmentInOutLine.getOperativeUOM().getIdentifier());
        line.put("orderQuantity", "");
        selectedLines.put(line);
      } catch (JSONException e) {
        fail(e.getMessage());
      }
    }
    return selectedLines;

  }

  private ShipmentInOut processShipmentInOut(ShipmentInOut shipmentInOut) {
    final List<Object> params = new ArrayList<Object>();
    params.add(null);
    params.add(shipmentInOut.getId());
    CallStoredProcedure.getInstance()
        .call(SHIPMENT_INOUT_COMPLETE_PROCEDURE_NAME, params, null, true, false);
    OBDal.getInstance().refresh(shipmentInOut);
    return shipmentInOut;
  }
}
