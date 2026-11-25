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
 * All portions are Copyright (C) 2021-2026 FUTIT SERVICES, S.L
 * All Rights Reserved.
 * Contributor(s): Futit Services S.L.
 *************************************************************************
 */
package org.openbravo.erpCommon.utility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link SequenceIdData#getUUID()}.
 */
@DisplayName("SequenceIdData")
public class SequenceIdDataTest {

  @Test
  @DisplayName("returns 32-character string")
  void testLength() {
    assertEquals(32, SequenceIdData.getUUID().length());
  }

  @Test
  @DisplayName("returns uppercase hex string")
  void testUppercase() {
    String uuid = SequenceIdData.getUUID();
    assertEquals(uuid, uuid.toUpperCase());
  }

  @Test
  @DisplayName("contains no hyphens")
  void testNoHyphens() {
    assertNotNull(SequenceIdData.getUUID());
    assertTrue(!SequenceIdData.getUUID().contains("-"));
  }

  @Test
  @DisplayName("contains only hex characters")
  void testHexOnly() {
    String uuid = SequenceIdData.getUUID();
    assertTrue(uuid.matches("[0-9A-F]{32}"));
  }

  @Test
  @DisplayName("multiple calls return different UUIDs")
  void testUniqueness() {
    String uuid1 = SequenceIdData.getUUID();
    String uuid2 = SequenceIdData.getUUID();
    assertNotEquals(uuid1, uuid2);
  }

  @Test
  @DisplayName("is a valid Openbravo UUID")
  void testIsValidOBUUID() {
    String uuid = SequenceIdData.getUUID();
    assertTrue(Utility.isUUIDString(uuid));
  }
}
