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
 * Box of 10 unit of the 10 units available in the storage detail without reservations
 *
 */
public class ReferencedInventoryFullBoxTest extends ReferencedInventoryBoxTest {
  private static final String QTYTOBOX = "10";

  @Rule
  public ParameterCdiTestRule<ParamsBoxTest> parameterValuesRule = new ParameterCdiTestRule<ParamsBoxTest>(
      Arrays.asList(new ParamsBoxTest[] { new ParamsBoxTest(
          "Box of " + QTYTOBOX
              + " unit of the 10 units available in the storage detail without reservations",
          QTYTOBOX) }));

  protected @ParameterCdiTest ParamsBoxTest params;

  @Test
  public void allTests() throws Exception {
    for (String[] product : PRODUCTS) {
      for (String toBinId : BINS) {
        testBox(toBinId, product[0], product[1], params.qtyToBox, null, false);
        OBDal.getInstance().getSession().clear();
      }
    }
  }
}
