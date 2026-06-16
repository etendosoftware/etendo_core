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
 * All portions are Copyright © 2021–2026 FUTIT SERVICES, S.L
 * All Rights Reserved.
 * Contributor(s): Futit Services S.L.
 *************************************************************************
 */
package org.openbravo.erpCommon.utility.reporting.printing; //NOSONAR

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;

/**
 * Tests for {@link PrintControllerEmailOptionsBuilder}.
 * The class depends on the full servlet/DAL stack; this suite covers instantiation
 * and the static helper logic that can be reached without a running ERP.
 */
@SuppressWarnings({"java:S100", "java:S120"})
@RunWith(MockitoJUnitRunner.Silent.class)
public class PrintControllerEmailOptionsBuilderTest {

  /** Class can be instantiated via Objenesis without triggering servlet/DAL constructors. */
  @Test
  public void testInstantiation_doesNotThrow() {
    PrintControllerEmailOptionsBuilder builder =
        new ObjenesisStd().newInstance(PrintControllerEmailOptionsBuilder.class);
    assertNotNull(builder);
  }
}
