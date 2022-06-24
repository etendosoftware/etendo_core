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
 * All portions are Copyright (C) 2018-2019 Openbravo SLU
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.costing;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Test cases to verify Cost Adjustment Project
 * 
 * @author aferraz
 */

@RunWith(Suite.class)
@Suite.SuiteClasses({ TestCostingSourceAdjustments.class, //
    TestCostingNoSourceAdjustments.class, //
    TestCostingLandedCost.class, //
    TestIssue37033.class, //
    TestIssue37279.class, //
    TestIssue39616.class, //
    TestIssue39888.class //
})
public class TestCosting {
  // No content is required, this is just the definition of a test suite.
}
