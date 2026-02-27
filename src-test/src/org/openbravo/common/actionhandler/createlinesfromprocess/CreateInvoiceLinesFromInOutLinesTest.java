/*
 * Copyright (C) 2026 Openbravo SLU
 * All Rights Reserved.
 */
package org.openbravo.common.actionhandler.createlinesfromprocess;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;

/**
 * Tests for {@link CreateInvoiceLinesFromInOutLines}.
 */
@RunWith(MockitoJUnitRunner.class)
public class CreateInvoiceLinesFromInOutLinesTest {
  /** Get from class returns shipment in out line. */

  @Test
  public void testGetFromClassReturnsShipmentInOutLine() {
    CreateInvoiceLinesFromInOutLines handler = new CreateInvoiceLinesFromInOutLines();
    Class<ShipmentInOutLine> result = handler.getFromClass();
    assertEquals(ShipmentInOutLine.class, result);
  }
}
