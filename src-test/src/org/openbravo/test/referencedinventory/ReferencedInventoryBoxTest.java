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

package org.openbravo.test.referencedinventory;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.openbravo.dal.service.OBDal;
import org.openbravo.materialmgmt.ReservationUtils;
import org.openbravo.materialmgmt.refinventory.BoxProcessor;
import org.openbravo.materialmgmt.refinventory.ReferencedInventoryUtil;
import org.openbravo.model.common.plm.AttributeSetInstance;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.materialmgmt.onhandquantity.ReferencedInventory;
import org.openbravo.model.materialmgmt.onhandquantity.ReferencedInventoryType;
import org.openbravo.model.materialmgmt.onhandquantity.ReservationStock;
import org.openbravo.model.materialmgmt.onhandquantity.StorageDetail;
import org.openbravo.model.materialmgmt.transaction.InternalMovement;

public abstract class ReferencedInventoryBoxTest extends ReferencedInventoryTest {

  /**
   * Runs a box process and verifies it. If reservationQty is not null, it first create a
   * reservation for reservationQty and then boxes the qtyInBox units
   * 
   * @param toBinId
   *          new bin where the referenced inventory is left. If null it is the same as the current
   *          storage detail
   * @param productId
   *          Mandatory. A new product will be created as a clone of this one
   * @param attributeSetInstanceId
   *          Mandatory only when the product does require it.
   * @param qtyInBox
   *          quantity of the storage detail to be included in the reference inventory. Note that
   *          there is maximum availability of 10 units of the cloned product, so this quantity
   *          should be lower or equal than 10
   * @param reservationQty
   *          quantity to create a reservation before running the boxing. If null, no reservation
   *          will be created
   * @param isAllocated
   *          creates allocated or non-allocated reservation. Only useful if reservationQty is not
   *          null
   * @return the created referenced inventory
   */
  protected ReferencedInventory testBox(final String toBinId, final String productId,
      final String attributeSetInstanceId, final BigDecimal qtyInBox,
      final BigDecimal reservationQty, final boolean isAllocated) throws Exception {
    return testBox(toBinId, productId, attributeSetInstanceId, qtyInBox, reservationQty,
        isAllocated, false, false);
  }

  protected ReferencedInventory testBox(final String _toBinId, final String productId,
      final String attributeSetInstanceId, final BigDecimal _qtyInBox,
      final BigDecimal reservationQty, final boolean isAllocated, final boolean isForceBin,
      final boolean isForceAttribute) throws Exception {
    final ReferencedInventoryType refInvType = ReferencedInventoryTestUtils
        .createReferencedInventoryType();
    final ReferencedInventory refInv = ReferencedInventoryTestUtils
        .createReferencedInventory(ReferencedInventoryTestUtils.QA_SPAIN_ORG_ID, refInvType);

    final Product product = ReferencedInventoryTestUtils.cloneProduct(productId);
    ReferencedInventoryTestUtils.receiveProduct(product, RECEIVEDQTY_10, attributeSetInstanceId);
    final StorageDetail storageDetail = ReferencedInventoryTestUtils
        .getUniqueStorageDetail(product);
    final String originalStorageBinId = storageDetail.getStorageBin().getId();
    final String originalAttributeId = storageDetail.getAttributeSetValue().getId();

    ReferencedInventoryTestUtils.createProcessAndAssertReservation(storageDetail, reservationQty,
        isAllocated, isForceBin, isForceAttribute);

    final String toBinId = StringUtils.isBlank(_toBinId) ? storageDetail.getStorageBin().getId()
        : _toBinId;
    final BigDecimal qtyInBox = _qtyInBox == null ? storageDetail.getQuantityOnHand() : _qtyInBox;
    final InternalMovement boxMovement = new BoxProcessor(refInv,
        ReferencedInventoryTestUtils.getStorageDetailsToBoxJSArray(storageDetail, qtyInBox),
        toBinId).createAndProcessGoodsMovement();

    OBDal.getInstance().refresh(boxMovement);
    OBDal.getInstance().refresh(refInv);
    assertsGoodsMovementIsProcessed(boxMovement);
    assertsGoodsMovementNumberOfLines(boxMovement, 1);
    assertsReferencedInventoryIsNotEmptyAndHasRightQtyAndProduct(toBinId, qtyInBox, refInv, product,
        storageDetail);

    if (isPartialBoxing(qtyInBox)) {
      final List<StorageDetail> storageDetails = ReferencedInventoryTestUtils
          .getAvailableStorageDetailsOrderByQtyOnHand(product);
      assertThat("Two storage details were found", storageDetails.size(), equalTo(2));
    }

    assertsStorageDetails(toBinId, qtyInBox, product, originalStorageBinId, originalAttributeId);

    if (reservationQty != null) {
      if (hasBoxedSomethingPreviouslyReserved(qtyInBox, reservationQty, storageDetail)) {
        assertsNewReservationAllocatedFlag(reservationQty, isAllocated, refInv);
      }
      assertsReservationQtyInStorageDetails(qtyInBox, reservationQty, product);
    }

    return refInv;
  }

  private void assertsReferencedInventoryIsNotEmptyAndHasRightQtyAndProduct(final String toBinId,
      final BigDecimal qtyInBox, final ReferencedInventory refInv, final Product product,
      final StorageDetail originalStorageDetail) {
    assertThat("Referenced Inventory is not empty", refInv.getMaterialMgmtStorageDetailList(),
        not(empty()));
    assertThat("Referenced Inventory has one record",
        refInv.getMaterialMgmtStorageDetailList().size(), equalTo(1));
    assertThat("Referenced Inventory has right product",
        refInv.getMaterialMgmtStorageDetailList().get(0).getProduct().getId(),
        equalTo(product.getId()));
    assertThat("Referenced Inventory has right quantity",
        refInv.getMaterialMgmtStorageDetailList().get(0).getQuantityOnHand(), equalTo(qtyInBox));
    assertThat("Referenced Inventory is in the right bin",
        refInv.getMaterialMgmtStorageDetailList().get(0).getStorageBin().getId(), equalTo(toBinId));
  }

  void assertsAttributeSetIsValid(final ReferencedInventory refInv,
      final String originalAttributeId, final StorageDetail boxedStorageDetail) {
    final AttributeSetInstance attributeSetValue = boxedStorageDetail.getAttributeSetValue();
    assertThat("Storage Detail attribute set is not null", attributeSetValue, notNullValue());
    assertThat("Storage Detail attribute set is not zero", attributeSetValue.getId(),
        not(equalTo("0")));

    assertThat("New attribute set is related to the referenced inventory",
        attributeSetValue.getReferencedInventory().getId(), equalTo(refInv.getId()));
    assertThat("New attribute set is related to cloned one",
        ReferencedInventoryUtil.getParentAttributeSetInstance(boxedStorageDetail).getId(),
        equalTo(originalAttributeId));
    assertThat("New attribute set description contains referenced inventory string",
        attributeSetValue.getDescription(),
        endsWith(ReferencedInventoryUtil.REFERENCEDINVENTORYPREFIX + refInv.getSearchKey()
            + ReferencedInventoryUtil.REFERENCEDINVENTORYSUFFIX));
  }

  private boolean isPartialBoxing(final BigDecimal qtyInBox) {
    return qtyInBox.compareTo(RECEIVEDQTY_10) < 0;
  }

  private void assertsStorageDetails(final String toBinId, final BigDecimal qtyInBox,
      final Product product, final String originalStorageBinId, final String originalAttributeId) {
    for (StorageDetail sd : ReferencedInventoryTestUtils
        .getAvailableStorageDetailsOrderByQtyOnHand(product)) {
      OBDal.getInstance().refresh(sd);
      if (sd.getQuantityOnHand().compareTo(qtyInBox) == 0) {
        // In box
        assertThat("Storage detail is linked to referenced inventory", sd.getReferencedInventory(),
            notNullValue());
        assertThat("Storage detail is in new bin", toBinId, equalTo(sd.getStorageBin().getId()));
        assertsAttributeSetIsValid(sd.getReferencedInventory(), originalAttributeId, sd);
      } else {
        // Not in box
        assertThat("Storage detail is not linked to referenced inventory",
            sd.getReferencedInventory(), nullValue());
        assertThat("Storage detail qty is original - boxed", RECEIVEDQTY_10.subtract(qtyInBox),
            equalTo(sd.getQuantityOnHand()));
        assertThat("Second storage detail is in old bin", originalStorageBinId,
            equalTo(sd.getStorageBin().getId()));
      }
    }
  }

  private boolean hasBoxedSomethingPreviouslyReserved(final BigDecimal qtyInBox,
      final BigDecimal reservationQty, final StorageDetail storageDetail) {
    // I must box something already reserved, so I need to create a new reservation after boxing
    return (RECEIVEDQTY_10.subtract(reservationQty)).compareTo(qtyInBox) < 0;
  }

  private void assertsNewReservationAllocatedFlag(final BigDecimal reservationQty,
      final boolean isAllocated, ReferencedInventory refInv) {
    for (StorageDetail sd : ReferencedInventoryTestUtils.getAvailableStorageDetailsOrderByQtyOnHand(
        refInv.getMaterialMgmtStorageDetailList().get(0).getProduct())) {
      for (ReservationStock rs : ReservationUtils.getReservationStockFromStorageDetail(sd)) {
        assertThat("Allocated flag is properly set", isAllocated, equalTo(rs.isAllocated()));
      }
    }
  }

  private void assertsReservationQtyInStorageDetails(final BigDecimal qtyInBox,
      final BigDecimal reservationQty, final Product product) {
    final BigDecimal qtyNotReserved = RECEIVEDQTY_10.subtract(reservationQty);
    final List<StorageDetail> newStorageDetails = ReferencedInventoryTestUtils
        .getAvailableStorageDetailsOrderByQtyOnHand(product);
    for (StorageDetail sd : newStorageDetails) {
      if (sd.getReferencedInventory() != null) {
        // Boxed storage detail.
        if (qtyNotReserved.compareTo(qtyInBox) >= 0) {
          assertThat("No reservation linked to boxed storage detail", sd.getReservedQty(),
              equalTo(BigDecimal.ZERO));
        } else {
          assertThat("Expected reservation qty linked to boxed storage detail", sd.getReservedQty(),
              equalTo(reservationQty.subtract(RECEIVEDQTY_10.subtract(qtyInBox))));
        }
      } else {
        // Not boxed storage detail
        if (qtyNotReserved.compareTo(qtyInBox) >= 0) {
          assertThat("Fully original reservation qty must be linked to not boxed storage detail",
              sd.getReservedQty(), equalTo(reservationQty));
        } else {
          assertThat("Expected reservation qty linked to not boxed storage detail",
              sd.getReservedQty(), equalTo(reservationQty
                  .subtract(reservationQty.subtract(RECEIVEDQTY_10.subtract(qtyInBox)))));
        }
      }
    }
  }

  protected class ParamsBoxTest {
    String testDesc;
    BigDecimal qtyToBox;

    ParamsBoxTest(String testDesc, String qtyToBox) {
      this.testDesc = testDesc;
      this.qtyToBox = new BigDecimal(qtyToBox);
    }

    @Override
    public String toString() {
      return "ParamsBoxTest [testDesc=" + testDesc + ", qtyToBox=" + qtyToBox + "]";
    }
  }

  protected class ParamsBoxReservationTest extends ParamsBoxTest {
    BigDecimal reservationQty;

    ParamsBoxReservationTest(String testDesc, String qtyToBox, String reservationQty) {
      super(testDesc, qtyToBox);
      this.reservationQty = new BigDecimal(reservationQty);
    }

    @Override
    public String toString() {
      return "ParamsBoxReservationTest [testDesc=" + testDesc + ", qtyToBox=" + qtyToBox
          + ", reservationQty=" + reservationQty + "]";
    }
  }

}
