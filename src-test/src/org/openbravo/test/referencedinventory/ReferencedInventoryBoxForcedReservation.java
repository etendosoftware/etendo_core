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

import java.util.Arrays;
import java.util.List;

import org.hibernate.criterion.Restrictions;
import org.junit.Rule;
import org.junit.Test;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.weld.test.ParameterCdiTest;
import org.openbravo.base.weld.test.ParameterCdiTestRule;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBDao;
import org.openbravo.model.materialmgmt.onhandquantity.ReferencedInventory;
import org.openbravo.model.materialmgmt.onhandquantity.Reservation;
import org.openbravo.model.materialmgmt.onhandquantity.StorageDetail;

/**
 * Test created reservation has the expected bin and attribute set at header level based on the
 * original reservation found
 *
 */
public class ReferencedInventoryBoxForcedReservation extends ReferencedInventoryBoxTest {

  private boolean[] ISFORCEBIN = { true, false };
  private boolean[] ISFORCEATTRIBUTE = { true, false };
  private static final String EXPECTED_ERROR = "The process implies to reallocate reservation(s), however the system was unable "
      + "to reallocate 10 reserved quantities. This error is usually thrown when you try to move stock to other bin and you "
      + "have don't have enough stock available in the current bin to fulfill the reservation.";

  @Rule
  public ParameterCdiTestRule<ParamsBoxReservationTest> parameterValuesRule = new ParameterCdiTestRule<ParamsBoxReservationTest>(
      Arrays.asList(new ParamsBoxReservationTest[] { new ParamsBoxReservationTest(
          " Full Box (10 units of 10 units) of a storage details with a previous reservation of these 10 units.",
          "10", "10") }));

  private @ParameterCdiTest ParamsBoxReservationTest params;

  @Test
  public void testBoxSameBinIsPossible() throws Exception {
    for (boolean isAllocated : ISALLOCATED) {
      for (String[] product : PRODUCTS) {
        for (boolean isForceBin : ISFORCEBIN) {
          for (boolean isForceAttribute : ISFORCEATTRIBUTE) {
            if (productDoesNotHaveAttributeAndIsForceAttribute(product[1], isForceAttribute)
                || isNeitherForceBinNorForceAttribute(isForceBin, isForceAttribute)) {
              continue;
            }
            final ReferencedInventory refInv = testBox(BINS[0], product[0], product[1],
                params.qtyToBox, params.reservationQty, isAllocated, isForceBin, isForceAttribute);
            assertsReservation(refInv, isForceBin, isForceAttribute);
            OBDal.getInstance().getSession().clear();
          }
        }
      }
    }
  }

  private boolean productDoesNotHaveAttributeAndIsForceAttribute(String attribute,
      boolean isForceAttribute) {
    return isForceAttribute && attribute == null;
  }

  private boolean isNeitherForceBinNorForceAttribute(boolean isForceBin, boolean isForceAttribute) {
    return !isForceBin && !isForceAttribute;
  }

  @Test
  public void testBoxInDifferentBinIsNotPossible() throws Exception {
    thrown.expect(OBException.class);
    thrown.expectMessage(equalTo(EXPECTED_ERROR));
    testBox(BINS[1], PRODUCTS[0][0], PRODUCTS[0][1], params.qtyToBox, params.reservationQty, false,
        true, false);
  }

  @Test
  public void testBoxInDifferentBinIsNotPossible2() throws Exception {
    thrown.expect(OBException.class);
    thrown.expectMessage(equalTo(EXPECTED_ERROR));
    testBox(BINS[1], PRODUCTS[1][0], PRODUCTS[1][1], params.qtyToBox, params.reservationQty, true,
        true, true);
  }

  @Test
  public void testBoxInDifferentBinIsNotPossible3() throws Exception {
    thrown.expect(OBException.class);
    thrown.expectMessage(equalTo(EXPECTED_ERROR));
    testBox(BINS[1], PRODUCTS[2][0], PRODUCTS[2][1], params.qtyToBox, params.reservationQty, false,
        true, true);
  }

  private void assertsReservation(final ReferencedInventory refInv, boolean isForceBin,
      boolean isForceAttribute) {
    final OBCriteria<Reservation> crit = OBDao.getFilteredCriteria(Reservation.class,
        Restrictions.eq(StorageDetail.PROPERTY_PRODUCT,
            refInv.getMaterialMgmtStorageDetailList().get(0).getProduct()));
    if (isForceBin) {
      crit.add(Restrictions.eq(Reservation.PROPERTY_STORAGEBIN + ".id", BINS[0]));
    }
    if (isForceAttribute) {
      crit.add(Restrictions.isNotNull(Reservation.PROPERTY_ATTRIBUTESETVALUE));
    }
    crit.addOrderBy(Reservation.PROPERTY_RESSTATUS, true);
    final List<Reservation> reservations = crit.list();
    assertThat("One reservation must be found", reservations.size(), equalTo(1));

    final Reservation reservation = reservations.get(0);
    assertThat("Reservation must be completed", reservation.getRESStatus(), equalTo("CO"));
    ReferencedInventoryTestUtils.assertsReservationHeader(isForceBin, isForceAttribute,
        reservation);
    assertThat("Reservation must have 1 line",
        reservation.getMaterialMgmtReservationStockList().size(), equalTo(1));
  }
}
