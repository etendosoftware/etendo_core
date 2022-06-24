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

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.openbravo.dal.service.OBDal;
import org.openbravo.materialmgmt.refinventory.ReferencedInventoryUtil;
import org.openbravo.materialmgmt.refinventory.UnboxProcessor;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.materialmgmt.onhandquantity.ReferencedInventory;
import org.openbravo.model.materialmgmt.onhandquantity.StorageDetail;
import org.openbravo.model.materialmgmt.transaction.InternalMovement;

public abstract class ReferencedInventoryUnboxTest extends ReferencedInventoryBoxTest {
  private static final BigDecimal QTYINBOX_10 = new BigDecimal("10");

  protected TestUnboxOutputParams testUnbox(final String toBinId, final String productId,
      final String attributeSetInstanceId, final BigDecimal qtyToUnbox,
      final BigDecimal reservationQty, final boolean isAllocated) throws Exception {

    ReferencedInventory refInv = testBox(null, productId, attributeSetInstanceId, QTYINBOX_10,
        reservationQty, isAllocated);
    final StorageDetail storageDetail = refInv.getMaterialMgmtStorageDetailList().get(0);
    final Product originalProduct = storageDetail.getProduct();
    final String originalAttributeSet = ReferencedInventoryUtil
        .getParentAttributeSetInstance(storageDetail)
        .getId();

    final InternalMovement unBoxMovement = new UnboxProcessor(refInv, ReferencedInventoryTestUtils
        .getUnboxStorageDetailsJSArray(storageDetail, qtyToUnbox, toBinId))
            .createAndProcessGoodsMovement();

    OBDal.getInstance().refresh(unBoxMovement);
    OBDal.getInstance().getSession().evict(refInv); // Hack to avoid problems in Hibernate when the
                                                    // unbox process is executed
    refInv = OBDal.getInstance().get(ReferencedInventory.class, refInv.getId());

    assertsGoodsMovementIsProcessed(unBoxMovement);
    assertsGoodsMovementNumberOfLines(unBoxMovement, 1);
    assertsUnboxedStorageDetailIsInRightBinAndHasRestoredOriginalAttribute(originalProduct,
        qtyToUnbox, refInv, toBinId, originalAttributeSet);
    assertsStillBoxedStorageDetail(originalProduct, qtyToUnbox, refInv, originalAttributeSet);

    return new TestUnboxOutputParams(refInv, originalProduct, originalAttributeSet, toBinId);
  }

  protected class TestUnboxOutputParams {
    protected ReferencedInventory refInv;
    protected Product originalProduct;
    protected String originalAttributeSetId;
    protected String toBinId;

    TestUnboxOutputParams(ReferencedInventory refInv, Product originalProduct,
        String originalAttributeSetId, String toBinId) {
      this.refInv = refInv;
      this.originalProduct = originalProduct;
      this.originalAttributeSetId = originalAttributeSetId;
      this.toBinId = toBinId;
    }
  }

  private void assertsUnboxedStorageDetailIsInRightBinAndHasRestoredOriginalAttribute(
      final Product product, final BigDecimal qtyToUnbox, final ReferencedInventory refInv,
      final String toBinId, final String originalAttributeSet) {
    final List<StorageDetail> afterUnboxStorageDetails = ReferencedInventoryTestUtils
        .getAvailableStorageDetailsOrderByQtyOnHand(product);
    for (StorageDetail sd : afterUnboxStorageDetails) {
      OBDal.getInstance().refresh(sd); // Need to refresh to get new qty on hand
      if (sd.getQuantityOnHand().compareTo(qtyToUnbox) == 0) {
        assertThat("Unboxed storage detail is in the expected bin", sd.getStorageBin().getId(),
            equalTo(toBinId));
        assertThat("Current storage detail has restored its original attribute set",
            sd.getAttributeSetValue().getId(), equalTo(originalAttributeSet));
        assertThat("Attribute Set description doesn't contain info about referenced inventory",
            sd.getAttributeSetValue().getDescription(),
            not(endsWith(ReferencedInventoryUtil.REFERENCEDINVENTORYPREFIX + refInv.getSearchKey()
                + ReferencedInventoryUtil.REFERENCEDINVENTORYSUFFIX)));
      }
    }
  }

  private void assertsStillBoxedStorageDetail(final Product product, final BigDecimal qtyToUnbox,
      final ReferencedInventory refInv, final String originalAttributeSet) {
    final List<StorageDetail> afterUnboxStorageDetails = ReferencedInventoryTestUtils
        .getAvailableStorageDetailsOrderByQtyOnHand(product);
    for (StorageDetail sd : afterUnboxStorageDetails) {
      OBDal.getInstance().refresh(sd); // Need to refresh to get new qty on hand
      if (sd.getQuantityOnHand().compareTo(qtyToUnbox) != 0) {
        assertThat("Boxed qty is the expected one", QTYINBOX_10.subtract(qtyToUnbox),
            equalTo(sd.getQuantityOnHand()));
        assertsAttributeSetIsValid(refInv, originalAttributeSet, sd);
      }
    }
  }

  protected void assertsReferenceInventoryIsNotEmpty(final ReferencedInventory refInv,
      final BigDecimal expectedQtyInRefInv) throws Exception {
    final List<StorageDetail> storageDetails = refInv.getMaterialMgmtStorageDetailList();
    assertThat("Referenced inventory must still be linked to a storage detail",
        storageDetails.size(), equalTo(1));
    assertThat("Storage detail must have expected qty on hand",
        storageDetails.get(0).getQuantityOnHand(), equalTo(expectedQtyInRefInv));
    assertThat("Attribute Set description does contain info about referenced inventory",
        storageDetails.get(0).getAttributeSetValue().getDescription(),
        endsWith(ReferencedInventoryUtil.REFERENCEDINVENTORYPREFIX + refInv.getSearchKey()
            + ReferencedInventoryUtil.REFERENCEDINVENTORYSUFFIX));
  }

  protected void assertsReferenceInventoryIsEmpty(final ReferencedInventory refInv)
      throws Exception {
    for (final StorageDetail storageDetail : refInv.getMaterialMgmtStorageDetailList()) {
      assertThat("Storage detail found in referenced inventory must not have qty on hand",
          storageDetail.getQuantityOnHand(), equalTo(BigDecimal.ZERO));
    }
  }

  protected class ParamsUnboxTest extends ParamsBoxTest {
    BigDecimal qtyToUnbox;

    ParamsUnboxTest(String testDesc, String qtyToBox, String qtyToUnbox) {
      super(testDesc, qtyToBox);
      this.qtyToUnbox = new BigDecimal(qtyToUnbox);
    }

    @Override
    public String toString() {
      return "ParamsUnboxTest [testDesc=" + testDesc + ", qtyToBox=" + qtyToBox + ", qtyToUnbox="
          + qtyToUnbox + "]";
    }
  }

}
