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
 * All portions are Copyright (C) 2026 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.test.invoice;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.materialmgmt.InvoiceGeneratorFromGoodsShipment;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceLine;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.financialmgmt.accounting.Costcenter;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.openbravo.model.project.Project;
import org.openbravo.test.materialMgmt.invoiceFromShipment.TestUtils;

/**
 * Tests how the accounting dimensions are inherited when an Invoice is generated from a Goods
 * Shipment: only the Invoice lines take them, from the Goods Shipment line and, when the line has
 * none, from the Goods Shipment header. The Invoice header is always left empty.
 */
public class InvoiceDimensionsFromShipmentTest extends WeldBaseTest {

  private static final Logger log = LogManager.getLogger();

  private static final String CLIENT_ID = "4028E6C72959682B01295A070852010D"; // QA Testing
  private static final String ORG_ID = "357947E87C284935AD1D783CF6F099A1"; // Spain
  private static final String USER_ID = "100"; // Openbravo
  private static final String ROLE_ID = "4028E6C72959682B01295A071429011E"; // QA Testing Admin
  private static final String SALES_ORDER_ID = "5B29AF263D004CD3830D4F9B23C17DFD";
  private static final String GOODS_SHIPMENT_ID = "8BEAC8CAFFCE444FA15D0170F897B641";
  private static final String PRODUCT_ID = "0CF7C882B8BD4D249F3BCC8727A736D1"; // T-Shirts
  private static final String PROJECT_ID = "B4731348B2CB48DCB24CC369CBC9DD83"; // TestProject
  // Test Cost Center
  private static final String COST_CENTER_ID = "5DDF281D28CF47639E1E05568A262591";
  private static final String AFTER_DELIVERY = "D";
  private static final String LINE_DIMENSION_PREFIX = "ETP4762-LINE";
  private static final int UNIQUE_KEY_SUFFIX_LENGTH = 8;

  @Before
  public void initialize() {
    log.info("Initializing Invoice Dimensions From Shipment Tests ...");
    OBContext.setOBContext(USER_ID, ROLE_ID, CLIENT_ID, ORG_ID);
    final OBContext obContext = OBContext.getOBContext();
    final VariablesSecureApp vars = new VariablesSecureApp(obContext.getUser().getId(),
        obContext.getCurrentClient().getId(), obContext.getCurrentOrganization().getId(),
        obContext.getRole().getId(), obContext.getLanguage().getLanguage());
    RequestContext.get().setVariableSecureApp(vars);
  }

  /**
   * A Goods Shipment linked to a Sales Order, with the dimensions only in its header, generates an
   * Invoice whose header has no dimensions and whose line inherits the ones of the header
   */
  @Test
  public void invoiceFromShipmentLinkedToOrderLeavesHeaderEmpty() {
    assertInvoiceDimensions("InvoiceDimensions_001", true, PROJECT_ID, COST_CENTER_ID);
  }

  /**
   * Same as above for a Goods Shipment which is not linked to any Sales Order, so the dimensions
   * can only come from the Goods Shipment header itself
   */
  @Test
  public void invoiceFromShipmentWithoutOrderLeavesHeaderEmpty() {
    assertInvoiceDimensions("InvoiceDimensions_002", false, PROJECT_ID, COST_CENTER_ID);
  }

  /**
   * A dimension which is empty in the Goods Shipment stays empty in the Invoice line, and the
   * Invoice header keeps both dimensions empty
   */
  @Test
  public void emptyCostCenterIsNotInheritedByTheInvoiceLine() {
    assertInvoiceDimensions("InvoiceDimensions_003", false, PROJECT_ID, null);
  }

  /**
   * A Goods Shipment without dimensions neither in its header nor in its lines generates an Invoice
   * without dimensions at all
   */
  @Test
  public void shipmentWithoutDimensionsGeneratesInvoiceWithoutDimensions() {
    assertInvoiceDimensions("InvoiceDimensions_004", false, null, null);
  }

  /**
   * The dimensions of the Goods Shipment line take precedence over the ones of the Goods Shipment
   * header: a line with its own dimensions keeps them in the Invoice line, a line without
   * dimensions inherits the ones of the Goods Shipment header, and the Invoice header stays empty
   */
  @Test
  public void lineDimensionsTakePrecedenceOverTheHeaderOnes() {
    OBContext.setAdminMode();
    try {
      final String testName = "InvoiceDimensions_005";
      final Project headerProject = OBDal.getInstance().get(Project.class, PROJECT_ID);
      final Costcenter headerCostCenter = OBDal.getInstance().get(Costcenter.class, COST_CENTER_ID);
      final Project lineProject = cloneProject(headerProject);
      final Costcenter lineCostCenter = cloneCostCenter(headerCostCenter);

      final Product productWithLineDimensions = TestUtils.cloneProduct(PRODUCT_ID,
          testName + "-Line");
      final Product productWithoutLineDimensions = TestUtils.cloneProduct(PRODUCT_ID,
          testName + "-Header");

      final ShipmentInOut shipment = createShipmentWithTwoLines(testName);
      final ShipmentInOutLine lineWithDimensions = getShipmentLine(shipment, 10L);
      final ShipmentInOutLine lineWithoutDimensions = getShipmentLine(shipment, 20L);
      lineWithDimensions.setProduct(productWithLineDimensions);
      lineWithoutDimensions.setProduct(productWithoutLineDimensions);
      setDimensions(lineWithDimensions, lineProject, lineCostCenter);
      setDimensions(lineWithoutDimensions, null, null);
      shipment.setProject(headerProject);
      shipment.setCostcenter(headerCostCenter);
      OBDal.getInstance().flush();
      TestUtils.processShipmentReceipt(shipment);

      final Invoice invoice = generateInvoiceFrom(shipment);

      assertHeaderHasNoDimensions(invoice);
      assertLineDimensions(getInvoiceLineByProduct(invoice, productWithLineDimensions), lineProject,
          lineCostCenter);
      assertLineDimensions(getInvoiceLineByProduct(invoice, productWithoutLineDimensions),
          headerProject, headerCostCenter);

    } catch (Exception e) {
      log.error(e.getMessage(), e);
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Generates an Invoice from a Goods Shipment which carries the given dimensions in its header and
   * none in its line, and verifies that the Invoice header has no dimensions while the Invoice line
   * inherits the ones of the Goods Shipment header
   *
   * @param testName
   *          Name used to identify the documents created by the test
   * @param linkedToSalesOrder
   *          Whether the Goods Shipment must be linked to a Sales Order or not
   * @param projectId
   *          Project to set in the Goods Shipment header, null to leave it empty
   * @param costCenterId
   *          Cost Center to set in the Goods Shipment header, null to leave it empty
   */
  private void assertInvoiceDimensions(final String testName, final boolean linkedToSalesOrder,
      final String projectId, final String costCenterId) {
    OBContext.setAdminMode();
    try {
      final Project project = projectId == null ? null
          : OBDal.getInstance().get(Project.class, projectId);
      final Costcenter costCenter = costCenterId == null ? null
          : OBDal.getInstance().get(Costcenter.class, costCenterId);
      final Product product = TestUtils.cloneProduct(PRODUCT_ID, testName);

      final ShipmentInOut shipment = createShipment(testName, product, linkedToSalesOrder, project,
          costCenter);
      final Invoice invoice = generateInvoiceFrom(shipment);

      assertHeaderHasNoDimensions(invoice);
      assertLineDimensions(getInvoiceLineByProduct(invoice, product), project, costCenter);

    } catch (Exception e) {
      log.error(e.getMessage(), e);
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Creates a completed Goods Shipment with the given dimensions in its header and no dimensions in
   * its line, optionally linked to a Sales Order with invoice term "After Delivery"
   */
  private ShipmentInOut createShipment(final String testName, final Product product,
      final boolean linkedToSalesOrder, final Project project, final Costcenter costCenter) {
    OrderLine orderLine = null;
    if (linkedToSalesOrder) {
      final Order salesOrder = TestUtils.cloneOrder(SALES_ORDER_ID, testName);
      salesOrder.setInvoiceTerms(AFTER_DELIVERY);
      salesOrder.setProject(project);
      salesOrder.setCostcenter(costCenter);
      orderLine = getOrderLine(salesOrder, 10L);
      orderLine.setProduct(product);
      TestUtils.processOrder(salesOrder);
    }

    final ShipmentInOut shipment = TestUtils.cloneReceiptShipment(GOODS_SHIPMENT_ID, testName);
    final ShipmentInOutLine shipmentLine = getShipmentLine(shipment, 10L);
    shipmentLine.setProduct(product);
    shipmentLine.setSalesOrderLine(orderLine);
    setDimensions(shipmentLine, null, null);

    shipment.setProject(project);
    shipment.setCostcenter(costCenter);
    OBDal.getInstance().save(shipment);
    OBDal.getInstance().flush();

    TestUtils.processShipmentReceipt(shipment);
    return shipment;
  }

  private ShipmentInOut createShipmentWithTwoLines(final String testName) {
    final ShipmentInOut shipment = TestUtils.cloneReceiptShipment(GOODS_SHIPMENT_ID, testName);
    final ShipmentInOutLine firstLine = shipment.getMaterialMgmtShipmentInOutLineList().get(0);
    final ShipmentInOutLine secondLine = TestUtils.cloneReceiptShipmentLine(firstLine, shipment);
    secondLine.setLineNo(firstLine.getLineNo() + 10);
    OBDal.getInstance().flush();
    return shipment;
  }

  private Invoice generateInvoiceFrom(final ShipmentInOut shipment) {
    final Invoice invoice = new InvoiceGeneratorFromGoodsShipment(shipment.getId())
        .createInvoiceConsideringInvoiceTerms(true);
    assertNotNull("Invoice should not be null", invoice);
    return invoice;
  }

  private void setDimensions(final ShipmentInOutLine shipmentLine, final Project project,
      final Costcenter costCenter) {
    shipmentLine.setProject(project);
    shipmentLine.setCostcenter(costCenter);
  }

  private OrderLine getOrderLine(final Order salesOrder, final long lineNo) {
    return salesOrder.getOrderLineList()
        .stream()
        .filter(line -> line.getLineNo() == lineNo)
        .findFirst()
        .orElseThrow(() -> new OBException("Order line " + lineNo + " not found"));
  }

  private ShipmentInOutLine getShipmentLine(final ShipmentInOut shipment, final long lineNo) {
    return shipment.getMaterialMgmtShipmentInOutLineList()
        .stream()
        .filter(line -> line.getLineNo() == lineNo)
        .findFirst()
        .orElseThrow(() -> new OBException("Shipment line " + lineNo + " not found"));
  }

  private InvoiceLine getInvoiceLineByProduct(final Invoice invoice, final Product product) {
    final String hql = "as il where il.invoice.id = :invoiceId and il.product.id = :productId";
    final OBQuery<InvoiceLine> query = OBDal.getInstance().createQuery(InvoiceLine.class, hql);
    query.setNamedParameter("invoiceId", invoice.getId());
    query.setNamedParameter("productId", product.getId());
    query.setMaxResult(1);
    return query.uniqueResult();
  }

  private Project cloneProject(final Project project) {
    final Project newProject = (Project) DalUtil.copy(project, false);
    final String newId = SequenceIdData.getUUID();
    newProject.setId(newId);
    newProject.setNewOBObject(true);
    newProject.setSearchKey(buildUniqueKey(newId));
    newProject.setName(buildUniqueKey(newId));
    OBDal.getInstance().save(newProject);
    OBDal.getInstance().flush();
    return newProject;
  }

  private Costcenter cloneCostCenter(final Costcenter costCenter) {
    final Costcenter newCostCenter = (Costcenter) DalUtil.copy(costCenter, false);
    final String newId = SequenceIdData.getUUID();
    newCostCenter.setId(newId);
    newCostCenter.setNewOBObject(true);
    newCostCenter.setSearchKey(buildUniqueKey(newId));
    newCostCenter.setName(buildUniqueKey(newId));
    OBDal.getInstance().save(newCostCenter);
    OBDal.getInstance().flush();
    return newCostCenter;
  }

  /**
   * Builds a key which is unique even if the test is executed more than once in the same instance,
   * short enough for the Search Key of Project and Cost Center, which allows 40 characters
   */
  private String buildUniqueKey(final String id) {
    return LINE_DIMENSION_PREFIX + "-" + id.substring(0, UNIQUE_KEY_SUFFIX_LENGTH);
  }

  private void assertHeaderHasNoDimensions(final Invoice invoice) {
    assertDimension("Invoice header Project", invoice.getProject(), null);
    assertDimension("Invoice header Cost Center", invoice.getCostcenter(), null);
  }

  private void assertLineDimensions(final InvoiceLine invoiceLine, final Project project,
      final Costcenter costCenter) {
    assertNotNull("Invoice Line should not be null", invoiceLine);
    assertDimension("Invoice Line Project", invoiceLine.getProject(), project);
    assertDimension("Invoice Line Cost Center", invoiceLine.getCostcenter(), costCenter);
  }

  private void assertDimension(final String dimensionName, final BaseOBObject actual,
      final BaseOBObject expected) {
    final Object actualId = actual == null ? null : actual.getId();
    if (expected == null) {
      assertThat(dimensionName + " should be empty", actualId == null, equalTo(true));
    } else {
      assertThat(dimensionName + " should be " + expected.getIdentifier(), actualId,
          equalTo(expected.getId()));
    }
  }
}
