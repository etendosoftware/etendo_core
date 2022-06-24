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
 * All portions are Copyright (C) 2015-2018 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.db.model.triggers;

import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.query.Query;
import org.junit.Test;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.utility.Sequence;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.businesspartner.Location;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Locator;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.plm.AttributeSetInstance;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.plm.ProductUOM;
import org.openbravo.model.common.uom.UOM;
import org.openbravo.model.materialmgmt.onhandquantity.StorageDetail;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.openbravo.test.base.OBBaseTest;

public class M_inoutlinetrgTest extends OBBaseTest {

  private static Logger log = LogManager.getLogger();
  // User Openbravo
  private static String USER_ID = "100";

  // Role QA Testing Admin
  private static String ROLE_ID = "4028E6C72959682B01295A071429011E";

  // QA Testing Client
  private static final String ClientId = "4028E6C72959682B01295A070852010D";

  // Spain Organization
  private static final String OrganizationId = "357947E87C284935AD1D783CF6F099A1";

  // Movement Quantity: 10
  private static BigDecimal MovementQty = BigDecimal.TEN;

  private BigDecimal afterValue = BigDecimal.ZERO;
  private BigDecimal beforeValue = BigDecimal.ZERO;

  private StorageDetail storageDetail;
  private ShipmentInOut inOut;
  private ShipmentInOutLine inOutLine;
  private Product product;
  private Locator locator;
  private AttributeSetInstance attributeSetInstance;

  private void salesSetup() {
    // Product: Soccer Ball
    final String ProductId = "EBCD272DC37B4ABBB12B96139E5837BF";
    // Locator: spain111
    final String LocatorId = "4028E6C72959682B01295ECFEF6502A3";
    // Orange
    final String attributeSetInstanceId = "E2F81DE34D404177BB13E2B4198B83AB";

    product = OBDal.getInstance().get(Product.class, ProductId);
    locator = OBDal.getInstance().get(Locator.class, LocatorId);
    attributeSetInstance = OBDal.getInstance()
        .get(AttributeSetInstance.class, attributeSetInstanceId);
  }

  private void purchaseSetup() {
    // Product: T-Shirts
    final String ProductId = "0CF7C882B8BD4D249F3BCC8727A736D1";
    // Locator: M01
    final String LocatorId = "96DEDCC179504711A81497DE68900F49";
    //
    final String attributeSetInstanceId = "0";

    product = OBDal.getInstance().get(Product.class, ProductId);
    locator = OBDal.getInstance().get(Locator.class, LocatorId);
    attributeSetInstance = OBDal.getInstance()
        .get(AttributeSetInstance.class, attributeSetInstanceId);
  }

  @Test
  public void testM_InOutLineTrg_Sales1() throws SQLException {
    salesSetup();
    batchOfTests(true, 1);
  }

  @Test
  public void testM_InOutLineTrg_Sales2() throws SQLException {
    salesSetup();
    batchOfTests(true, 2);
  }

  @Test
  public void testM_InOutLineTrg_Sales4() throws SQLException {
    salesSetup();
    batchOfTests(true, 4);
  }

  @Test
  public void testM_InOutLineTrg_Purchase1() throws SQLException {
    purchaseSetup();
    batchOfTests(false, 1);
  }

  @Test
  public void testM_InOutLineTrg_Purchase2() throws SQLException {
    purchaseSetup();
    batchOfTests(false, 2);
  }

  @Test
  public void testM_InOutLineTrg_Purchase4() throws SQLException {
    purchaseSetup();
    batchOfTests(false, 4);
  }

  private void batchOfTests(boolean isSales, int numberOfLines) throws SQLException {
    log.info("START TEST for Sales = " + isSales + ". Number of lines: " + numberOfLines);

    test_CreateLineWith10Products(isSales, numberOfLines);
    test_SetProductAsNull();
    test_SetAProductAgain();
    test_UpdateDescription();
    test_UpdateMovementQuantity();
    test_UpdateMovementNegativeQuantity();
    test_UpdateMovementQuantity();
    test_UpdateMovementQuantityToSameQuantity();
    // Return bin
    test_UpdateLocator(OBDal.getInstance().get(Locator.class, "67C3E9C2ADF74AC7A48C0F94CE571AB9"));
    test_UpdateLocator(locator);
    if (isSales) {
      // blue
      test_UpdateAttribute(
          OBDal.getInstance().get(AttributeSetInstance.class, "1B78D7E95FBC47788B4962B11E80002B"));
      test_UpdateAttribute(attributeSetInstance);
    }
    test_DeleteLine();

    OBDal.getInstance().remove(inOut);

    log.info("END TEST for Sales = " + isSales + ". Number of lines: " + numberOfLines);
  }

  // Create a new shipment line with a Product (10 units)
  private void test_CreateLineWith10Products(boolean isSales, int numberOfLines)
      throws SQLException {
    inOut = insertMInOut(isSales);
    assertTrue("M_Inout header not inserted successfully ", inOut != null);

    storageDetail = getStorageDetail(product, product.getUOM(), locator, null,
        attributeSetInstance);
    if (storageDetail != null) {
      OBDal.getInstance().refresh(storageDetail);
      beforeValue = storageDetail.getQuantityInDraftTransactions();
    } else {
      beforeValue = BigDecimal.ZERO;
    }

    log.info(
        "*************** Create a new shipment line with a Product (10 units) *****************");
    log.info("Qty in Draft transaction before insertion: " + beforeValue);

    for (int i = 0; i < numberOfLines; i++) {
      inOutLine = insertMInOutLine(inOut);
      assertTrue(inOutLine != null);
    }

    storageDetail = getStorageDetail(product, inOutLine.getUOM(), inOutLine.getStorageBin(),
        inOutLine.getOrderUOM(), attributeSetInstance);
    OBDal.getInstance().refresh(storageDetail);

    afterValue = storageDetail.getQuantityInDraftTransactions();
    log.info("Qty in Draft transaction after insertion " + afterValue);

    assertTrue("Quantities should not be equal because a new line with a product has been saved",
        afterValue.compareTo(beforeValue) != 0);
  }

  // Set Product null
  private void test_SetProductAsNull() throws SQLException {
    OBDal.getInstance().refresh(storageDetail);
    beforeValue = storageDetail.getQuantityInDraftTransactions();

    log.info("*************** Set Product null *****************");
    log.info(
        "Qty in Draft transaction before setting product as null in m_inoutline " + beforeValue);

    updateMInOutLine(ShipmentInOutLine.PROPERTY_PRODUCT, null);
    assertTrue(inOutLine.getProduct() == null);

    storageDetail = getStorageDetail(product, inOutLine.getUOM(), inOutLine.getStorageBin(),
        inOutLine.getOrderUOM(), attributeSetInstance);
    if (storageDetail != null) {
      OBDal.getInstance().refresh(storageDetail);
      afterValue = storageDetail.getQuantityInDraftTransactions();
    } else {
      afterValue = BigDecimal.ZERO;
    }

    log.info("Qty in Draft transaction after setting product as null in m_inoutline " + afterValue);

    assertTrue("Quantities should not be equal because product has been removed",
        afterValue.compareTo(beforeValue) != 0);
  }

  // Set blank product in InOutLine with Product: Distribution good A
  private void test_SetAProductAgain() throws SQLException {
    if (storageDetail != null) {
      OBDal.getInstance().refresh(storageDetail);
      beforeValue = storageDetail.getQuantityInDraftTransactions();
    } else {
      beforeValue = BigDecimal.ZERO;
    }

    log.info("*************** Set Product not null *****************");
    log.info("Qty in Draft transaction before setting null product with value in m_inoutline "
        + beforeValue);

    updateMInOutLine(ShipmentInOutLine.PROPERTY_PRODUCT, product);

    storageDetail = getStorageDetail(product, inOutLine.getUOM(), inOutLine.getStorageBin(),
        inOutLine.getOrderUOM(), attributeSetInstance);
    OBDal.getInstance().refresh(storageDetail);

    afterValue = storageDetail.getQuantityInDraftTransactions();

    log.info("Qty in Draft transaction after setting null product with value in m_inoutline "
        + afterValue);

    assertTrue("Quantities should not be equal because a product has been set again",
        afterValue.compareTo(beforeValue) != 0);

  }

  // Update description
  private void test_UpdateDescription() throws SQLException {
    OBDal.getInstance().getConnection().commit();
    OBDal.getInstance().refresh(storageDetail);
    beforeValue = storageDetail.getQuantityInDraftTransactions();
    final Date previousDate = storageDetail.getUpdated();

    log.info("*************** Update description *****************");
    updateMInOutLine(ShipmentInOutLine.PROPERTY_DESCRIPTION, "description updated for this line");

    storageDetail = getStorageDetail(product, inOutLine.getUOM(), inOutLine.getStorageBin(),
        inOutLine.getOrderUOM(), attributeSetInstance);
    OBDal.getInstance().refresh(storageDetail);

    afterValue = storageDetail.getQuantityInDraftTransactions();
    final Date newDate = storageDetail.getUpdated();

    log.info("Qty in Draft transaction before updating description in m_inoutline " + beforeValue);
    log.info("Qty in Draft transaction after updating description in m_inoutline " + afterValue);

    assertTrue("Quantities should be equal because we have only updated the description",
        afterValue.compareTo(beforeValue) == 0);

    log.info("Previous modification in Storage detail: " + previousDate);
    log.info("Las modification in Storage detail: " + newDate);

    // FIXME This assert usually is not able to detect a problem because the m_update_inventory set
    // the updated column with UPDATED=to_date(now()), which seems to set always the same date
    // because to_date() is declared as IMMUTABLE. To properly test it, it should be UPDATED=now().
    // However the assert is kept as it doesn't create false positives
    assertTrue("Storage detail is not updated because only description has been updated. Previous: "
        + previousDate + ". newDate: " + newDate, previousDate.compareTo(newDate) == 0);
  }

  // Update product quantity (positive qty)
  private void test_UpdateMovementQuantity() throws SQLException {
    OBDal.getInstance().refresh(storageDetail);
    beforeValue = storageDetail.getQuantityInDraftTransactions();
    log.info("*************** Update Product quantity (1 Unit) *****************");

    updateMInOutLine(ShipmentInOutLine.PROPERTY_MOVEMENTQUANTITY, BigDecimal.ONE);

    storageDetail = getStorageDetail(product, inOutLine.getUOM(), inOutLine.getStorageBin(),
        inOutLine.getOrderUOM(), attributeSetInstance);
    OBDal.getInstance().refresh(storageDetail);

    afterValue = storageDetail.getQuantityInDraftTransactions();

    log.info("Qty in Draft transaction before updating quantity " + beforeValue);
    log.info("Qty in Draft transaction after updating quantity " + afterValue);

    assertTrue("Quantities should not be equal because quantity has been updated",
        afterValue.compareTo(beforeValue) != 0);
  }

  // Update product quantity to the same quantity
  private void test_UpdateMovementQuantityToSameQuantity() throws SQLException {
    OBDal.getInstance().refresh(storageDetail);
    OBDal.getInstance().refresh(inOutLine);
    beforeValue = storageDetail.getQuantityInDraftTransactions();
    log.info(
        "*************** Update Product quantity to the same quantity (no change) *****************");

    updateMInOutLine(ShipmentInOutLine.PROPERTY_MOVEMENTQUANTITY, inOutLine.getMovementQuantity());

    storageDetail = getStorageDetail(product, inOutLine.getUOM(), inOutLine.getStorageBin(),
        inOutLine.getOrderUOM(), attributeSetInstance);
    OBDal.getInstance().refresh(storageDetail);

    afterValue = storageDetail.getQuantityInDraftTransactions();

    log.info("Qty in Draft transaction before updating quantity " + beforeValue);
    log.info("Qty in Draft transaction after updating quantity " + afterValue);

    assertTrue("Quantities should be equal because quantity has not been actually updated",
        afterValue.compareTo(beforeValue) == 0);
  }

  // Update product quantity (negative qty)
  private void test_UpdateMovementNegativeQuantity() throws SQLException {
    OBDal.getInstance().refresh(storageDetail);
    beforeValue = storageDetail.getQuantityInDraftTransactions();
    log.info("*************** Update Product negative quantity (-2 Unit) *****************");

    updateMInOutLine(ShipmentInOutLine.PROPERTY_MOVEMENTQUANTITY, new BigDecimal("-2"));

    storageDetail = getStorageDetail(product, inOutLine.getUOM(), inOutLine.getStorageBin(),
        inOutLine.getOrderUOM(), attributeSetInstance);
    OBDal.getInstance().refresh(storageDetail);

    afterValue = storageDetail.getQuantityInDraftTransactions();

    log.info("Qty in Draft transaction before updating quantity " + beforeValue);
    log.info("Qty in Draft transaction after updating quantity " + afterValue);

    assertTrue("Quantities should not be equal because quantity has been updated",
        afterValue.compareTo(beforeValue) != 0);
  }

  // Update attribute
  private void test_UpdateAttribute(AttributeSetInstance newAttributeSetInstance)
      throws SQLException {
    final AttributeSetInstance previousAttributeSetInstance = inOutLine.getAttributeSetValue();
    storageDetail = getStorageDetail(product, inOutLine.getUOM(), inOutLine.getStorageBin(),
        inOutLine.getOrderUOM(), previousAttributeSetInstance);
    if (storageDetail != null) {
      OBDal.getInstance().refresh(storageDetail);
      beforeValue = storageDetail.getQuantityInDraftTransactions();
    } else {
      beforeValue = BigDecimal.ZERO;
    }

    log.info("*************** Update attribute *****************");

    BigDecimal beforeValueNewAttribute;
    try {
      storageDetail = getStorageDetail(product, inOutLine.getUOM(), inOutLine.getStorageBin(),
          inOutLine.getOrderUOM(), newAttributeSetInstance);
      OBDal.getInstance().refresh(storageDetail);
      beforeValueNewAttribute = storageDetail.getQuantityInDraftTransactions();
    } catch (Exception notfound) {
      beforeValueNewAttribute = BigDecimal.ZERO;
    }

    updateMInOutLine(ShipmentInOutLine.PROPERTY_ATTRIBUTESETVALUE, newAttributeSetInstance);

    storageDetail = getStorageDetail(product, inOutLine.getUOM(), inOutLine.getStorageBin(),
        inOutLine.getOrderUOM(), previousAttributeSetInstance);
    if (storageDetail != null) {
      OBDal.getInstance().refresh(storageDetail);
      afterValue = storageDetail.getQuantityInDraftTransactions();
    } else {
      afterValue = BigDecimal.ZERO;
    }

    log.info(
        "Qty in Draft transaction for old attributesetinstance before updating attributesetinstance "
            + beforeValue);
    log.info(
        "Qty in Draft transaction for old attributesetinstance after updating attributesetinstance "
            + afterValue);

    assertTrue(
        "Quantities should not be equal for old attributesetinstance because we have updated the attributesetinstance",
        afterValue.compareTo(beforeValue) != 0);

    storageDetail = getStorageDetail(product, inOutLine.getUOM(), inOutLine.getStorageBin(),
        inOutLine.getOrderUOM(), newAttributeSetInstance);
    BigDecimal afterValueNewAttribute = BigDecimal.ZERO;
    if (storageDetail != null) {
      OBDal.getInstance().refresh(storageDetail);
      afterValueNewAttribute = storageDetail.getQuantityInDraftTransactions();
    }

    log.info(
        "Qty in Draft transaction for new attributesetinstance before updating attributesetinstance "
            + beforeValueNewAttribute);
    log.info(
        "Qty in Draft transaction for new attributesetinstance after updating attributesetinstance "
            + afterValueNewAttribute);

    assertTrue(
        "Quantities should not be equal for new attributesetinstance because we have updated the attributesetinstance",
        afterValueNewAttribute.compareTo(beforeValueNewAttribute) != 0);
  }

  // Update locator
  private void test_UpdateLocator(Locator newLocator) throws SQLException {
    final Locator previousLocator = inOutLine.getStorageBin();
    storageDetail = getStorageDetail(product, inOutLine.getUOM(), previousLocator,
        inOutLine.getOrderUOM(), inOutLine.getAttributeSetValue());
    if (storageDetail != null) {
      OBDal.getInstance().refresh(storageDetail);
      beforeValue = storageDetail.getQuantityInDraftTransactions();
    } else {
      beforeValue = BigDecimal.ZERO;
    }

    log.info("*************** Update locator *****************");

    BigDecimal beforeValueNewLocator;
    try {
      storageDetail = getStorageDetail(product, inOutLine.getUOM(), newLocator,
          inOutLine.getOrderUOM(), inOutLine.getAttributeSetValue());
      OBDal.getInstance().refresh(storageDetail);
      beforeValueNewLocator = storageDetail.getQuantityInDraftTransactions();
    } catch (Exception notfound) {
      beforeValueNewLocator = BigDecimal.ZERO;
    }

    updateMInOutLine(ShipmentInOutLine.PROPERTY_STORAGEBIN, newLocator);

    storageDetail = getStorageDetail(product, inOutLine.getUOM(), previousLocator,
        inOutLine.getOrderUOM(), inOutLine.getAttributeSetValue());
    if (storageDetail != null) {
      OBDal.getInstance().refresh(storageDetail);
      afterValue = storageDetail.getQuantityInDraftTransactions();
    } else {
      afterValue = BigDecimal.ZERO;
    }

    log.info("Qty in Draft transaction for old locator before updating locator " + beforeValue);
    log.info("Qty in Draft transaction for old locator after updating locator " + afterValue);

    assertTrue("Quantities should not be equal for old locator because we have updated the locator",
        afterValue.compareTo(beforeValue) != 0);

    storageDetail = getStorageDetail(product, inOutLine.getUOM(), newLocator,
        inOutLine.getOrderUOM(), inOutLine.getAttributeSetValue());
    BigDecimal afterValueNewLocator = BigDecimal.ZERO;
    if (storageDetail != null) {
      OBDal.getInstance().refresh(storageDetail);
      afterValueNewLocator = storageDetail.getQuantityInDraftTransactions();
    }

    log.info("Qty in Draft transaction for new locator before updating locator "
        + beforeValueNewLocator);
    log.info(
        "Qty in Draft transaction for new locator after updating locator " + afterValueNewLocator);

    assertTrue("Quantities should not be equal for new locator because we have updated the locator",
        afterValueNewLocator.compareTo(beforeValueNewLocator) != 0);

  }

  // Delete a M_InoutLine
  private void test_DeleteLine() throws SQLException {
    OBDal.getInstance().refresh(storageDetail);
    beforeValue = storageDetail.getQuantityInDraftTransactions();

    log.info("*************** Delete line *****************");
    log.info("Qty in Draft transaction before deletion: " + beforeValue);

    deleteMInOutLines();
    assertTrue(inOut.getMaterialMgmtShipmentInOutLineList().isEmpty());

    storageDetail = getStorageDetail(product, product.getUOM(), locator, null,
        attributeSetInstance);
    if (storageDetail != null) {
      OBDal.getInstance().refresh(storageDetail);
      afterValue = storageDetail.getQuantityInDraftTransactions();
    } else {
      afterValue = BigDecimal.ZERO;
    }

    log.info("Qty in Draft transaction after deletion: " + afterValue);

    assertTrue("Quantities should not be equal because line has been deleted",
        afterValue.compareTo(beforeValue) != 0);
  }

  private ShipmentInOut insertMInOut(boolean isSales) throws SQLException {
    final String shipment_DocumentTypeId;
    final String shipment_DocSequenceId = "FF8080812C2ABFC6012C2B3BDF4A004D";
    final String customerId;
    final String customerAddressId;
    // Warehouse: Spain Warehouse
    final String WarehouseId = "4D7B97565A024DB7B4C61650FA2B9560";
    if (isSales) {
      // MM Shipment Document Type, Document Sequence
      shipment_DocumentTypeId = "FF8080812C2ABFC6012C2B3BDF4A004E";

      // Business Partner: Customer A
      customerId = "4028E6C72959682B01295F40C3CB02EC";
      customerAddressId = "4028E6C72959682B01295F40C43802EE";
    } else {
      // MM Shipment Document Type
      shipment_DocumentTypeId = "FF8080812C2ABFC6012C2B3BDF530078";

      // Business Partner: Creditor
      customerId = "4028E6C72959682B01295F40CA140307";
      customerAddressId = "4028E6C72959682B01295F40CA620309";
    }

    try {
      OBContext.setAdminMode(true);
      // Set QA context
      OBContext.setOBContext(USER_ID, ROLE_ID, ClientId, OrganizationId);
      ShipmentInOut shipmentInOut = OBProvider.getInstance().get(ShipmentInOut.class);
      BusinessPartner bpartner = OBDal.getInstance().get(BusinessPartner.class, customerId);
      Location bpLocation = OBDal.getInstance().get(Location.class, customerAddressId);
      DocumentType doctype = OBDal.getInstance().get(DocumentType.class, shipment_DocumentTypeId);
      Warehouse warehouse = OBDal.getInstance().get(Warehouse.class, WarehouseId);
      shipmentInOut.setBusinessPartner(bpartner);
      shipmentInOut.setDocumentType(doctype);
      shipmentInOut.setPartnerAddress(bpLocation);
      shipmentInOut.setDocumentNo(getDocumentNo(shipment_DocSequenceId));
      shipmentInOut.setMovementDate(new Date());
      shipmentInOut.setAccountingDate(new Date());
      shipmentInOut.setWarehouse(warehouse);
      shipmentInOut.setSalesTransaction(isSales);

      OBDal.getInstance().save(shipmentInOut);
      OBDal.getInstance().getConnection().commit();
      OBDal.getInstance().refresh(shipmentInOut);

      return shipmentInOut;
    } finally {
      OBContext.restorePreviousMode();
    }

  }

  private ShipmentInOutLine insertMInOutLine(ShipmentInOut shipmentInOut) throws SQLException {
    try {
      OBContext.setAdminMode(true);
      // Set QA context
      OBContext.setOBContext(USER_ID, ROLE_ID, ClientId, OrganizationId);
      ShipmentInOutLine shipmentInOutLine = OBProvider.getInstance().get(ShipmentInOutLine.class);
      shipmentInOutLine.setShipmentReceipt(shipmentInOut);
      shipmentInOutLine.setLineNo(10L);
      shipmentInOutLine.setProduct(product);
      shipmentInOutLine.setMovementQuantity(MovementQty);
      shipmentInOutLine.setStorageBin(locator);
      shipmentInOutLine.setUOM(product.getUOM());
      shipmentInOutLine.setAttributeSetValue(attributeSetInstance);
      OBDal.getInstance().save(shipmentInOutLine);
      OBDal.getInstance().getConnection().commit();
      OBDal.getInstance().refresh(shipmentInOutLine);

      return shipmentInOutLine;

    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private void updateMInOutLine(String propertyName, Object value) throws SQLException {
    inOutLine.set(propertyName, value);
    OBDal.getInstance().save(inOutLine);
    OBDal.getInstance().getConnection().commit();
    OBDal.getInstance().refresh(inOutLine);

  }

  private void deleteMInOutLines() throws SQLException {
    final StringBuffer hqlString = new StringBuffer();
    hqlString
        .append(" delete from MaterialMgmtShipmentInOutLine where shipmentReceipt.id = :mInOutId ");
    @SuppressWarnings("rawtypes")
    Query deleteQry = OBDal.getInstance().getSession().createQuery(hqlString.toString());
    deleteQry.setParameter("mInOutId", inOut.getId());
    deleteQry.executeUpdate();
    OBDal.getInstance().getConnection().commit();
    OBDal.getInstance().refresh(inOut);
  }

  // Calculates the next document number for this sequence
  private String getDocumentNo(String sequenceId) {
    try {
      Sequence sequence = OBDal.getInstance().get(Sequence.class, sequenceId);
      String prefix = sequence.getPrefix() == null ? "" : sequence.getPrefix();
      String suffix = sequence.getSuffix() == null ? "" : sequence.getSuffix();
      String documentNo = prefix + sequence.getNextAssignedNumber().toString() + suffix;
      sequence.setNextAssignedNumber(sequence.getNextAssignedNumber() + sequence.getIncrementBy());
      return documentNo;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  private StorageDetail getStorageDetail(Product _product, UOM uom, Locator _locator,
      ProductUOM productUom, AttributeSetInstance _attributeInstance) {
    final StringBuffer hqlString = new StringBuffer();
    hqlString.append(" select sd from  MaterialMgmtStorageDetail sd ");
    hqlString.append(" where sd.product.id = :productId ");
    hqlString.append(" and sd.storageBin.id = :locatorId ");
    hqlString.append(" and sd.uOM.id = :uomId ");
    hqlString.append(" and sd.attributeSetValue.id = :attributeSetInstanceId ");
    if (productUom != null) {
      hqlString.append(" and sd.orderUOM.id = :productUOMId ");
    } else {
      hqlString.append(" and sd.orderUOM.id is null ");
    }

    Query<StorageDetail> query = OBDal.getInstance()
        .getSession()
        .createQuery(hqlString.toString(), StorageDetail.class);
    query.setParameter("productId", _product.getId());
    query.setParameter("locatorId", _locator.getId());
    query.setParameter("uomId", uom.getId());
    query.setParameter("attributeSetInstanceId", _attributeInstance.getId());
    if (productUom != null) {
      query.setParameter("productUOMId", productUom.getId());
    }
    query.setMaxResults(1);

    List<StorageDetail> queryList = query.list();
    if (!queryList.isEmpty()) {
      return queryList.get(0);
    }
    return null;
  }
}
