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

import java.math.BigDecimal;
import java.util.Arrays;

import org.junit.Rule;
import org.junit.Test;
import org.openbravo.base.weld.test.ParameterCdiTest;
import org.openbravo.base.weld.test.ParameterCdiTestRule;
import org.openbravo.dal.service.OBDal;

/**
 * Full unbox (10 units of 10 units) of a storage detail without reservations
 */
public class ReferencedInventoryFullUnboxTest extends ReferencedInventoryUnboxTest {
  @Rule
  public ParameterCdiTestRule<ParamsUnboxTest> parameterValuesRule = new ParameterCdiTestRule<ParamsUnboxTest>(
      Arrays.asList(new ParamsUnboxTest[] { new ParamsUnboxTest(
          "Full unbox (10 units of 10 units) of a storage detail without reservations", "10",
          "10") }));

  private @ParameterCdiTest ParamsUnboxTest params;

  @Test
  public void allTests() throws Exception {
    for (String[] product : PRODUCTS) {
      for (String toBinId : BINS) {
        testFullUnbox(toBinId, product[0], product[1]);
        OBDal.getInstance().getSession().clear();
      }
    }
  }

  private void testFullUnbox(final String _toBinId, final String productId,
      final String attributeSetInstanceId) throws Exception {
    final TestUnboxOutputParams outParams = testUnbox(_toBinId, productId, attributeSetInstanceId,
        new BigDecimal("10"), null, false);
    assertsReferenceInventoryIsEmpty(outParams.refInv);
  }

}
