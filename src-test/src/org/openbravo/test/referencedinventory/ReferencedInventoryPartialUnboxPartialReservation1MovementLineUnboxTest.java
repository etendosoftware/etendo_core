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

import org.apache.commons.lang.StringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.openbravo.base.weld.test.ParameterCdiTest;
import org.openbravo.base.weld.test.ParameterCdiTestRule;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.materialmgmt.transaction.InternalMovement;

/**
 * Unbox less quantity than reserved one. Reservation is fully in box. Box movement should have 2
 * lines, while unbox movement just one
 */
public class ReferencedInventoryPartialUnboxPartialReservation1MovementLineUnboxTest
    extends ReferencedInventoryUnboxReservationTest {
  @Rule
  public ParameterCdiTestRule<ParamsUnboxReservationTest> parameterValuesRule = new ParameterCdiTestRule<ParamsUnboxReservationTest>(
      Arrays.asList(new ParamsUnboxReservationTest[] {
          new ParamsUnboxReservationTest(
              "Unbox less quantity than reserved one. Reservation is fully in box", "10", "1", "9"),
          new ParamsUnboxReservationTest(
              "Unbox less quantity than reserved one. Reservation is fully in box", "10", "3",
              "1") }));

  protected @ParameterCdiTest ParamsUnboxReservationTest params;

  @Test
  public void allTests() throws Exception {
    for (boolean isAllocated : ISALLOCATED) {
      for (String[] product : PRODUCTS) {
        for (String toBinId : BINS) {
          final TestUnboxOutputParams outParams = testUnboxReservation(toBinId, product[0],
              product[1], params.qtyToBox, params.qtyToUnbox, params.reservationQty, isAllocated);
          assertsReferenceInventoryIsNotEmpty(outParams.refInv,
              params.qtyToBox.subtract(params.qtyToUnbox));
          OBDal.getInstance().getSession().clear();
        }
      }
    }
  }

  @Override
  void assertsGoodsMovementNumberOfLines(final InternalMovement boxMovement,
      final int expectedNumberOfLines) {
    if (StringUtils.contains(boxMovement.getName(), "UNBOX")) {
      assertThat("Unbox Movement has 1 line",
          boxMovement.getMaterialMgmtInternalMovementLineList().size(), equalTo(1));
    } else {
      assertThat("Box Movement has two lines",
          boxMovement.getMaterialMgmtInternalMovementLineList().size(), equalTo(2));
    }
  }
}
