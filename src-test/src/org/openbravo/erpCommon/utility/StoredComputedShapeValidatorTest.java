/*
 *************************************************************************
 * The contents of this file are subject to the Etendo License
 * (the "License"), you may not use this file except in compliance with
 * the License.
 * You may obtain a copy of the License at
 * https://github.com/etendosoftware/etendo_core/blob/main/legal/Etendo_license.txt
 * Software distributed under the License is distributed on an
 * "AS IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing rights
 * and limitations under the License.
 * All portions are Copyright © 2026 FUTIT SERVICES, S.L
 * All Rights Reserved.
 * Contributor(s): Futit Services S.L.
 *************************************************************************
 */
package org.openbravo.erpCommon.utility;// NOSONAR - package name fixed by Etendo core convention

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Pure-logic unit tests for {@link StoredComputedShapeValidator}, covering the V18 refresh-mode
 * rule ({@link StoredComputedShapeValidator#checkRefreshMode(String, String)}, EPL-1807). No
 * database, no Mockito: the method under test only touches {@code String} arguments.
 */
public class StoredComputedShapeValidatorTest {

  private static final String STORED_COMPUTED = "S";

  // ================================================================================================
  // V18 — checkRefreshMode: non-stored-computed columns are always valid, whatever refreshMode is.
  // ================================================================================================

  @Test
  void nonStoredComputedColumnIsAlwaysValidRegardlessOfRefreshMode() {
    assertAll("Computation_Mode != 'S' short-circuits before the refresh mode is even inspected",
        () -> assertNull(StoredComputedShapeValidator.checkRefreshMode(null, "S")),
        () -> assertNull(StoredComputedShapeValidator.checkRefreshMode(null, "X")),
        () -> assertNull(StoredComputedShapeValidator.checkRefreshMode("N", "S")),
        () -> assertNull(StoredComputedShapeValidator.checkRefreshMode("N", "X")),
        () -> assertNull(StoredComputedShapeValidator.checkRefreshMode("V", "S")),
        () -> assertNull(StoredComputedShapeValidator.checkRefreshMode("V", "X")));
  }

  // ================================================================================================
  // V18 — checkRefreshMode: stored computed columns accept only S / Q / M.
  // ================================================================================================

  @Test
  void storedComputedColumnWithValidRefreshModeReturnsNull() {
    assertAll("S/Q/M are the only accepted Refresh_Mode values for a stored computed column",
        () -> assertNull(StoredComputedShapeValidator.checkRefreshMode(STORED_COMPUTED, "S")),
        () -> assertNull(StoredComputedShapeValidator.checkRefreshMode(STORED_COMPUTED, "Q")),
        () -> assertNull(StoredComputedShapeValidator.checkRefreshMode(STORED_COMPUTED, "M")));
  }

  @Test
  void storedComputedColumnWithInvalidRefreshModeIsRejected() {
    assertAll("anything other than exactly S/Q/M is a V18 violation",
        () -> assertEquals(StoredComputedShapeValidator.ETGO_STORED_COMPUTED_REFRESH_MODE,
            StoredComputedShapeValidator.checkRefreshMode(STORED_COMPUTED, null)),
        () -> assertEquals(StoredComputedShapeValidator.ETGO_STORED_COMPUTED_REFRESH_MODE,
            StoredComputedShapeValidator.checkRefreshMode(STORED_COMPUTED, "")),
        () -> assertEquals(StoredComputedShapeValidator.ETGO_STORED_COMPUTED_REFRESH_MODE,
            StoredComputedShapeValidator.checkRefreshMode(STORED_COMPUTED, "   ")),
        () -> assertEquals(StoredComputedShapeValidator.ETGO_STORED_COMPUTED_REFRESH_MODE,
            StoredComputedShapeValidator.checkRefreshMode(STORED_COMPUTED, "X")),
        () -> assertEquals(StoredComputedShapeValidator.ETGO_STORED_COMPUTED_REFRESH_MODE,
            StoredComputedShapeValidator.checkRefreshMode(STORED_COMPUTED, "s"),
            "Refresh_Mode is compared case-sensitively: lowercase 's' is not the same as 'S'"),
        () -> assertEquals(StoredComputedShapeValidator.ETGO_STORED_COMPUTED_REFRESH_MODE,
            StoredComputedShapeValidator.checkRefreshMode(STORED_COMPUTED, "SS")));
  }
}
