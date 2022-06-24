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

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import java.math.BigDecimal;

import org.openbravo.dal.service.OBDal;
import org.openbravo.model.materialmgmt.onhandquantity.StorageDetail;

/**
 * Abstract class to test unboxing related to reservations
 */
public abstract class ReferencedInventoryUnboxReservationTest extends ReferencedInventoryUnboxTest {

  protected TestUnboxOutputParams testUnboxReservation(final String toBinId, final String productId,
      final String attributeSetInstanceId, final BigDecimal qtyToBox, final BigDecimal qtyToUnbox,
      final BigDecimal reservationQty, final boolean isAllocated) throws Exception {
    final TestUnboxOutputParams outParams = super.testUnbox(toBinId, productId,
        attributeSetInstanceId, qtyToUnbox, reservationQty, isAllocated);
    assertsStorageDetailsQtyAndReservationQty(qtyToBox, qtyToUnbox, outParams, reservationQty);
    return outParams;
  }

  protected void assertsStorageDetailsQtyAndReservationQty(final BigDecimal qtyToBox,
      final BigDecimal qtyToUnbox, final TestUnboxOutputParams outParams,
      final BigDecimal reservationQty) {
    for (StorageDetail sd : ReferencedInventoryTestUtils
        .getAvailableStorageDetailsOrderByQtyOnHand(outParams.originalProduct)) {
      OBDal.getInstance().refresh(sd);
      if (sd.getReferencedInventory() != null) {
        // In box Storage details
        assertThat("Qty in box is the expected one", qtyToBox.subtract(qtyToUnbox),
            equalTo(sd.getQuantityOnHand()));
        if (hasUnboxedReservedQty(qtyToUnbox, reservationQty)) {
          assertThat("Storage Detail reserved qty is expected one", sd.getReservedQty(),
              equalTo(reservationQty
                  .subtract(reservationQty.subtract(RECEIVEDQTY_10.subtract(qtyToUnbox)))));
        } else {
          assertThat("Qty in box reserved is expected one", sd.getReservedQty(),
              equalTo((qtyToBox.subtract(qtyToUnbox)).compareTo(reservationQty) < 0
                  ? reservationQty.subtract(qtyToBox.subtract(qtyToUnbox))
                  : reservationQty));
        }
      } else {
        // Out box
        assertThat("Qty out box is the expected one", qtyToUnbox, equalTo(sd.getQuantityOnHand()));
        if (hasUnboxedReservedQty(qtyToUnbox, reservationQty)) {
          assertThat("Storage Detail reserved qty is expected one", sd.getReservedQty(),
              equalTo(reservationQty.subtract(RECEIVEDQTY_10.subtract(qtyToUnbox))));
        } else {
          assertThat("Qty out box reserved is Zero", sd.getReservedQty(), equalTo(BigDecimal.ZERO));
        }
      }
    }
  }

  protected boolean hasUnboxedReservedQty(final BigDecimal qtyToUnbox,
      final BigDecimal reservationQty) {
    return reservationQty != null
        // If available qty not reserved is lower than qty to unbox then I need to unbox reserved
        // qty
        && (RECEIVEDQTY_10.subtract(reservationQty)).compareTo(qtyToUnbox) < 0;
  }

  protected class ParamsUnboxReservationTest extends ParamsBoxReservationTest {
    BigDecimal qtyToUnbox;

    ParamsUnboxReservationTest(String testDesc, String qtyToBox, String qtyToUnbox,
        String reservationQty) {
      super(testDesc, qtyToBox, reservationQty);
      this.qtyToUnbox = new BigDecimal(qtyToUnbox);
    }
  }

}
