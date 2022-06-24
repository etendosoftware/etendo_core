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

import java.util.Arrays;

import org.junit.Rule;
import org.junit.Test;
import org.openbravo.base.weld.test.ParameterCdiTest;
import org.openbravo.base.weld.test.ParameterCdiTestRule;
import org.openbravo.dal.service.OBDal;

/**
 * Box a storage detail which had some units (not 100%) previously reserved. The box movement will
 * only have 1 line.
 */
public class ReferencedInventoryBoxPartialReservation1MovementLineTest
    extends ReferencedInventoryBoxTest {

  @Rule
  public ParameterCdiTestRule<ParamsBoxReservationTest> parameterValuesRule = new ParameterCdiTestRule<ParamsBoxReservationTest>(
      Arrays.asList(new ParamsBoxReservationTest[] { new ParamsBoxReservationTest(
          "Box 4 units where 3 were previously reserved", "4", "3") }));

  private @ParameterCdiTest ParamsBoxReservationTest params;

  @Test
  public void allTests() throws Exception {
    for (boolean isAllocated : ISALLOCATED) {
      for (String[] product : PRODUCTS) {
        for (String toBinId : BINS) {
          testBox(toBinId, product[0], product[1], params.qtyToBox, params.reservationQty,
              isAllocated);
          OBDal.getInstance().getSession().clear();
        }
      }
    }
  }
}
