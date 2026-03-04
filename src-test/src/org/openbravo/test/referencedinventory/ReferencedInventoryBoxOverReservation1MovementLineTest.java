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
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openbravo.dal.service.OBDal;

/**
 * Box storage details that are over reserved (allocated and non-allocated reservation). The box
 * movement will only have 1 line.
 */
public class ReferencedInventoryBoxOverReservation1MovementLineTest
    extends ReferencedInventoryBoxTest {

  private static final List<ParamsBoxReservationTest> PARAMS = Arrays.asList(
      new ParamsBoxReservationTest("Box 3 units where 5 where reserved (over reservation)", "3",
          "5"),
      new ParamsBoxReservationTest("Box 4 units where 10 where reserved (over reservation)", "4",
          "10"));

  @Test
  public void allTests() throws Exception {
    for (boolean isAllocated : ISALLOCATED) {
      for (String[] product : PRODUCTS) {
        for (ParamsBoxReservationTest params : PARAMS) {
          for (String toBinId : BINS) {
            testBox(toBinId, product[0], product[1], params.qtyToBox, params.reservationQty,
                isAllocated);
            OBDal.getInstance().getSession().clear();
          }
        }
      }
    }
  }
}
