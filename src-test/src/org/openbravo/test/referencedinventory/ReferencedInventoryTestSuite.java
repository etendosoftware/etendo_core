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

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
    // Box without reservation
    ReferencedInventoryFullBoxTest.class, //
    ReferencedInventoryPartialBoxTest.class, //
    ReferencedInventoryExceptionTest.class, //
    ReferencedInventorySequenceTest.class, //
    ReferencedInventoryBoxSeveralStorageDetailsTest.class, //
    // Unbox without reservation
    ReferencedInventoryFullUnboxTest.class, //
    ReferencedInventoryPartialUnboxTest.class, //
    // Box with reservation
    ReferencedInventoryBoxFullReservationTest.class, //
    ReferencedInventoryBoxPartialReservation2MovementLinesTest.class, //
    ReferencedInventoryBoxPartialReservation1MovementLineTest.class, //
    ReferencedInventoryBoxOverReservation1MovementLineTest.class, //
    ReferencedInventoryBoxOverReservation2MovementLinesTest.class, //
    // Unbox with reservations
    ReferencedInventoryFullUnboxFullReservation.class, //
    ReferencedInventoryPartialUnboxFullReservation.class, //
    ReferencedInventoryPartialUnboxPartialReservation1MovementLineUnboxTest.class, //
    ReferencedInventoryPartialUnboxPartialReservation2MovementLinesUnboxTest.class, //
    ReferencedInventoryFullUnboxPartialReservation.class, //
    // Force bin or attribute set at reservation header
    ReferencedInventoryBoxForcedReservation.class //
})
public class ReferencedInventoryTestSuite {
}
