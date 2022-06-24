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

import java.util.UUID;

import org.junit.Test;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.utility.Sequence;
import org.openbravo.model.materialmgmt.onhandquantity.ReferencedInventory;
import org.openbravo.model.materialmgmt.onhandquantity.ReferencedInventoryType;

/**
 * Test referenced inventory type sequence is properly used
 */
public class ReferencedInventorySequenceTest extends ReferencedInventoryTest {
  private static final String ANY_EXISTING_SEQUENCE_ID = "FF8080812C2ABFC6012C2B3BE4970094";

  @Test
  public void testReferencedInventorySequenceIsUsed() {
    final Sequence sequence = (Sequence) DalUtil
        .copy(OBDal.getInstance().getProxy(Sequence.class, ANY_EXISTING_SEQUENCE_ID));
    sequence.setName(UUID.randomUUID().toString());
    OBDal.getInstance().save(sequence);
    OBDal.getInstance().flush(); // Required to lock sequence at db level later on

    final ReferencedInventoryType refInvType = ReferencedInventoryTestUtils
        .createReferencedInventoryType();
    refInvType.setSequence(sequence);
    OBDal.getInstance().save(refInvType);
    Long currentSequenceNumber = sequence.getNextAssignedNumber();

    final ReferencedInventory refInv = ReferencedInventoryTestUtils
        .createReferencedInventory(ReferencedInventoryTestUtils.QA_SPAIN_ORG_ID, refInvType);
    assertThat("Referenced Inventory Search Key is taken from sequence", refInv.getSearchKey(),
        equalTo(Long.toString(currentSequenceNumber)));

    final ReferencedInventory refInv2 = ReferencedInventoryTestUtils
        .createReferencedInventory(ReferencedInventoryTestUtils.QA_SPAIN_ORG_ID, refInvType);
    assertThat("Referenced Inventory Search Key is updated from sequence", refInv2.getSearchKey(),
        equalTo(Long.toString(currentSequenceNumber + 1)));
  }
}
