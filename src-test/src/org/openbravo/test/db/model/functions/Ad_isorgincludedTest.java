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
 * All portions are Copyright (C) 2015-2017 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.db.model.functions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.openbravo.service.db.CallStoredProcedure;
import org.openbravo.test.base.OBBaseTest;

public class Ad_isorgincludedTest extends OBBaseTest {

  /**
   * All Organization *
   */
  protected static final String ORG_0 = "0";

  /**
   * QA Testing Client
   */
  protected static final String CLIENT_QA = "4028E6C72959682B01295A070852010D";

  /**
   * Main Organization
   */
  protected static final String ORG_QA_MAIN = "43D590B4814049C6B85C6545E8264E37";

  /**
   * Spain Organization
   */
  protected static final String ORG_QA_SPAIN = "357947E87C284935AD1D783CF6F099A1";

  /**
   * F&amp;B International Group Client
   */
  protected static final String CLIENT_FB = "23C59575B9CF467C9620760EB255B389";

  /**
   * F&amp;B International Group Organization
   */
  protected static final String ORG_FB_FBGROUP = "19404EAD144C49A0AF37D54377CF452D";

  /**
   * F&amp;B US, Inc.
   */
  protected static final String ORG_FB_US = "2E60544D37534C0B89E765FE29BC0B43";

  /**
   * F&amp;B US East Coast
   */
  protected static final String ORG_FB_EAST = "7BABA5FF80494CAFA54DEBD22EC46F01";

  /**
   * F&amp;B US West Coast
   */
  protected static final String ORG_FB_WEST = "BAE22373FEBE4CCCA24517E23F0C8A48";

  /**
   * F&amp;B España, S.A.
   */
  protected static final String ORG_FB_SPAIN = "B843C30461EA4501935CB1D125C9C25A";

  /**
   * F&amp;B España - Región Norte
   */
  protected static final String ORG_FB_NORTE = "E443A31992CB4635AFCAEABE7183CE85";

  /**
   * F&amp;B España - Región Sur
   */
  protected static final String ORG_FB_SUR = "DC206C91AA6A4897B44DA897936E0EC3";

  /**
   * Case I: Distinct Organization in the same branch with different levels.
   */

  /**
   * Case II: Distinct Organization in the different branch with different levels.
   */

  /**
   * Case III: Swap parent/child order
   */

  /**
   * Case IV: Organization with different clients
   */

  /**
   * Case V: Same Organization
   */

  /**
   * Case VI: Organization that does not exists.
   */

  @Test
  public void testIsOrgIncluded() {

    // Case I
    assertEquals("Level 1 Organization", 1, isOrgIncluded(ORG_0, ORG_0, CLIENT_FB));
    assertEquals("Level 2 Organization", 2, isOrgIncluded(ORG_FB_FBGROUP, ORG_0, CLIENT_FB));
    assertEquals("Level 3 Organization", 3, isOrgIncluded(ORG_FB_US, ORG_0, CLIENT_FB));
    assertEquals("Level 4 Organization", 4, isOrgIncluded(ORG_FB_WEST, ORG_0, CLIENT_FB));

    // Case II
    assertTrue(isOrgIncluded(ORG_FB_WEST, ORG_FB_SPAIN, CLIENT_QA) == -1);
    assertTrue(isOrgIncluded(ORG_FB_EAST, ORG_FB_SPAIN, CLIENT_QA) == -1);
    assertTrue(isOrgIncluded(ORG_FB_NORTE, ORG_FB_US, CLIENT_QA) == -1);
    assertTrue(isOrgIncluded(ORG_FB_SUR, ORG_FB_US, CLIENT_QA) == isOrgIncluded(ORG_FB_EAST,
        ORG_FB_SPAIN, CLIENT_QA));

    // Case III
    assertTrue(isOrgIncluded(ORG_QA_MAIN, ORG_QA_SPAIN, CLIENT_QA) == -1);
    assertTrue(isOrgIncluded(ORG_FB_US, ORG_FB_WEST, CLIENT_QA) == -1);

    // Case IV
    assertTrue(isOrgIncluded(ORG_QA_SPAIN, ORG_QA_MAIN, CLIENT_FB) == -1);
    assertTrue(isOrgIncluded(ORG_FB_US, ORG_FB_FBGROUP, CLIENT_QA) == -1);

    // Case V
    assertTrue(isOrgIncluded(ORG_QA_MAIN, ORG_QA_MAIN, CLIENT_QA) == 1);

    // Case VI
    assertTrue(isOrgIncluded("ABC", ORG_FB_FBGROUP, CLIENT_QA) == -1);
  }

  protected int isOrgIncluded(String orgId, String parentOrgId, String clientId) {
    return callOrgIncludedFunction(orgId, parentOrgId, clientId, "AD_ISORGINCLUDED");
  }

  protected int callOrgIncludedFunction(String orgId, String parentOrgId, String clientId,
      String functionName) {
    int value = 0;
    try {
      final List<Object> parameters = new ArrayList<Object>();
      parameters.add(orgId);
      parameters.add(parentOrgId);
      parameters.add(clientId);
      value = ((BigDecimal) CallStoredProcedure.getInstance().call(functionName, parameters, null))
          .intValue();
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
    return value;
  }
}
