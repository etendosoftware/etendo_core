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

package org.openbravo.test.createlinesfrom.data;

import static org.hamcrest.Matchers.comparesEqualTo;
import static org.junit.Assert.assertThat;

import java.math.BigDecimal;
import java.util.Arrays;

import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.invoice.InvoiceLine;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.financialmgmt.accounting.Costcenter;
import org.openbravo.model.financialmgmt.accounting.UserDimension1;
import org.openbravo.model.financialmgmt.accounting.UserDimension2;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.openbravo.model.project.Project;

/**
 * Check Create Lines From Sales Order with dimensions
 * 
 * @author Andy Armaignac
 *
 */
public class CLFTestDataSO_01 extends CreateLinesFromTestData {

  public CLFTestDataSO_01() {
    orderLineData = Arrays
        .asList(new OrderLineData.Builder().productId(CLFTestDataConstants.PRODUCT_RMA)
            .orderedQuantity(BigDecimal.TEN)
            .build());

    invoiceLineData = Arrays
        .asList(new InvoiceLineData.Builder().invoicedQuantity(BigDecimal.TEN).build());

    shipmentInOutLineData = Arrays
        .asList(new ShipmentInOutLineData.Builder().productId(CLFTestDataConstants.PRODUCT_RMA)
            .movementQuantity(BigDecimal.TEN)
            .build());
  }

  @Override
  public String getTestNumber() {
    return "01";
  }

  @Override
  public String getTestDescription() {
    return "Check Create Lines From Sales Order with dimensions";
  }

  @Override
  public Boolean isSales() {
    return true;
  }

  @Override
  protected void updateOrderLine(OrderLine orderLine, OrderLineData lineData) {
    super.updateOrderLine(orderLine, lineData);
    orderLine.setProject(OBDal.getInstance().get(Project.class, CLFTestDataConstants.PROJECT_ID));
    orderLine.setCostcenter(
        OBDal.getInstance().get(Costcenter.class, CLFTestDataConstants.COSTCENTER_ID));
    orderLine.setStDimension(
        OBDal.getInstance().get(UserDimension1.class, CLFTestDataConstants.ST_DIMENSION_ID));
    orderLine.setNdDimension(
        OBDal.getInstance().get(UserDimension2.class, CLFTestDataConstants.ND_DIMENSION_ID));
  }

  @Override
  protected void assertDraftOrderLines(OrderLine orderLine, OrderLineData lineData) {
    super.assertDraftOrderLines(orderLine, lineData);

    assertOrderLineDimensions(orderLine);
  }

  @Override
  protected void assertCompletedOrderLines(OrderLine orderLine, OrderLineData lineData) {
    super.assertCompletedOrderLines(orderLine, lineData);

    assertOrderLineDimensions(orderLine);
  }

  private void assertOrderLineDimensions(OrderLine orderLine) {
    String lineProjectName = orderLine.getProject() != null ? orderLine.getProject().getName() : "";
    String expectedProjectName = OBDal.getInstance()
        .get(Project.class, CLFTestDataConstants.PROJECT_ID)
        .getName();
    assertThat(getTestNumber() + ". Wrong Order Line Project Name = " + lineProjectName
        + ". Was expected " + expectedProjectName, lineProjectName,
        comparesEqualTo(expectedProjectName));

    String lineCostCenterName = orderLine.getCostcenter() != null
        ? orderLine.getCostcenter().getName()
        : "";
    String expectedCostCenterName = OBDal.getInstance()
        .get(Costcenter.class, CLFTestDataConstants.COSTCENTER_ID)
        .getName();
    assertThat(
        getTestNumber() + ". Wrong Order Line Costcenter Name = " + lineCostCenterName
            + ". Was expected " + expectedCostCenterName,
        lineCostCenterName, comparesEqualTo(expectedCostCenterName));

    String lineStDimensionName = orderLine.getStDimension() != null
        ? orderLine.getStDimension().getName()
        : "";
    String expectedStDimensionName = OBDal.getInstance()
        .get(UserDimension1.class, CLFTestDataConstants.ST_DIMENSION_ID)
        .getName();
    assertThat(
        getTestNumber() + ". Wrong Order Line StDimension Name = " + lineStDimensionName
            + ". Was expected " + expectedStDimensionName,
        lineStDimensionName, comparesEqualTo(expectedStDimensionName));

    String lineNdDimensionName = orderLine.getNdDimension() != null
        ? orderLine.getNdDimension().getName()
        : "";
    String expectedNdDimensionName = OBDal.getInstance()
        .get(UserDimension2.class, CLFTestDataConstants.ND_DIMENSION_ID)
        .getName();
    assertThat(
        getTestNumber() + ". Wrong Order Line NdDimension Name = " + lineNdDimensionName
            + ". Was expected " + expectedNdDimensionName,
        lineNdDimensionName, comparesEqualTo(expectedNdDimensionName));
  }

  @Override
  protected void assertDraftInvoiceLines(InvoiceLine invoiceLine, InvoiceLineData lineData) {
    super.assertDraftInvoiceLines(invoiceLine, lineData);
    assertInvoiceLineDimensions(invoiceLine);
  }

  @Override
  protected void assertCompletedInvoiceLines(InvoiceLine invoiceLine, InvoiceLineData lineData) {
    super.assertCompletedInvoiceLines(invoiceLine, lineData);
    assertInvoiceLineDimensions(invoiceLine);
  }

  private void assertInvoiceLineDimensions(InvoiceLine invoiceLine) {
    String lineProjectName = invoiceLine.getProject() != null ? invoiceLine.getProject().getName()
        : "";
    String expectedProjectName = OBDal.getInstance()
        .get(Project.class, CLFTestDataConstants.PROJECT_ID)
        .getName();
    assertThat(
        getTestNumber() + ". Wrong Invoice Line Project Name = " + lineProjectName
            + ". Was expected " + expectedProjectName,
        lineProjectName, comparesEqualTo(expectedProjectName));

    String lineCostCenterName = invoiceLine.getCostcenter() != null
        ? invoiceLine.getCostcenter().getName()
        : "";
    String expectedCostCenterName = OBDal.getInstance()
        .get(Costcenter.class, CLFTestDataConstants.COSTCENTER_ID)
        .getName();
    assertThat(
        getTestNumber() + ". Wrong Invoice Line Costcenter Name = " + lineCostCenterName
            + ". Was expected " + expectedCostCenterName,
        lineCostCenterName, comparesEqualTo(expectedCostCenterName));

    String lineStDimensionName = invoiceLine.getStDimension() != null
        ? invoiceLine.getStDimension().getName()
        : "";
    String expectedStDimensionName = OBDal.getInstance()
        .get(UserDimension1.class, CLFTestDataConstants.ST_DIMENSION_ID)
        .getName();
    assertThat(
        getTestNumber() + ". Wrong Invoice Line StDimension Name = " + lineStDimensionName
            + ". Was expected " + expectedStDimensionName,
        lineStDimensionName, comparesEqualTo(expectedStDimensionName));

    String lineNdDimensionName = invoiceLine.getNdDimension() != null
        ? invoiceLine.getNdDimension().getName()
        : "";
    String expectedNdDimensionName = OBDal.getInstance()
        .get(UserDimension2.class, CLFTestDataConstants.ND_DIMENSION_ID)
        .getName();
    assertThat(
        getTestNumber() + ". Wrong Invoice Line NdDimension Name = " + lineNdDimensionName
            + ". Was expected " + expectedNdDimensionName,
        lineNdDimensionName, comparesEqualTo(expectedNdDimensionName));
  }

  @Override
  protected void updateShipmentLine(ShipmentInOutLine shipmentInOutLine,
      ShipmentInOutLineData lineData) {
    super.updateShipmentLine(shipmentInOutLine, lineData);

    shipmentInOutLine
        .setProject(OBDal.getInstance().get(Project.class, CLFTestDataConstants.PROJECT_ID));
    shipmentInOutLine.setCostcenter(
        OBDal.getInstance().get(Costcenter.class, CLFTestDataConstants.COSTCENTER_ID));
    shipmentInOutLine.setStDimension(
        OBDal.getInstance().get(UserDimension1.class, CLFTestDataConstants.ST_DIMENSION_ID));
    shipmentInOutLine.setNdDimension(
        OBDal.getInstance().get(UserDimension2.class, CLFTestDataConstants.ND_DIMENSION_ID));
  }

  @Override
  protected void assertDraftShipmentLines(ShipmentInOutLine shipmentInOutLine,
      ShipmentInOutLineData lineData) {
    super.assertDraftShipmentLines(shipmentInOutLine, lineData);

    assertShipmentInOutLineDimensions(shipmentInOutLine);
  }

  @Override
  protected void assertCompletedShipmentInOutLines(ShipmentInOutLine shipmentInOutLine,
      ShipmentInOutLineData lineData) {
    super.assertCompletedShipmentInOutLines(shipmentInOutLine, lineData);

    assertShipmentInOutLineDimensions(shipmentInOutLine);
  }

  private void assertShipmentInOutLineDimensions(ShipmentInOutLine shipmentInOutLine) {
    String lineProjectName = shipmentInOutLine.getProject() != null
        ? shipmentInOutLine.getProject().getName()
        : "";
    String expectedProjectName = OBDal.getInstance()
        .get(Project.class, CLFTestDataConstants.PROJECT_ID)
        .getName();
    assertThat(getTestNumber() + ". Wrong Order Line Project Name = " + lineProjectName
        + ". Was expected " + expectedProjectName, lineProjectName,
        comparesEqualTo(expectedProjectName));

    String lineCostCenterName = shipmentInOutLine.getCostcenter() != null
        ? shipmentInOutLine.getCostcenter().getName()
        : "";
    String expectedCostCenterName = OBDal.getInstance()
        .get(Costcenter.class, CLFTestDataConstants.COSTCENTER_ID)
        .getName();
    assertThat(
        getTestNumber() + ". Wrong Order Line Costcenter Name = " + lineCostCenterName
            + ". Was expected " + expectedCostCenterName,
        lineCostCenterName, comparesEqualTo(expectedCostCenterName));

    String lineStDimensionName = shipmentInOutLine.getStDimension() != null
        ? shipmentInOutLine.getStDimension().getName()
        : "";
    String expectedStDimensionName = OBDal.getInstance()
        .get(UserDimension1.class, CLFTestDataConstants.ST_DIMENSION_ID)
        .getName();
    assertThat(
        getTestNumber() + ". Wrong Order Line StDimension Name = " + lineStDimensionName
            + ". Was expected " + expectedStDimensionName,
        lineStDimensionName, comparesEqualTo(expectedStDimensionName));

    String lineNdDimensionName = shipmentInOutLine.getNdDimension() != null
        ? shipmentInOutLine.getNdDimension().getName()
        : "";
    String expectedNdDimensionName = OBDal.getInstance()
        .get(UserDimension2.class, CLFTestDataConstants.ND_DIMENSION_ID)
        .getName();
    assertThat(
        getTestNumber() + ". Wrong Order Line NdDimension Name = " + lineNdDimensionName
            + ". Was expected " + expectedNdDimensionName,
        lineNdDimensionName, comparesEqualTo(expectedNdDimensionName));
  }
}
