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
package org.openbravo.event;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEvent;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;

/**
 * Unit tests for {@link MInOutLineEventHandler}.
 */
@RunWith(MockitoJUnitRunner.class)
public class MInOutLineEventHandlerTest {

  private static final String PRODUCT_NULL_AND_MOVEMENT_QTY_ZERO = "ProductNullAndMovementQtyZero";
  private static final String PRODUCT_NULL_AND_MOVEMENT_QTY_GREATER_ZERO =
      "ProductNullAndMovementQtyGreaterZero";

  @Mock private EntityNewEvent newEvent;
  @Mock private EntityUpdateEvent updateEvent;
  @Mock private ShipmentInOutLine shipmentInOutLine;
  @Mock private Product product;

  private MockedStatic<OBMessageUtils> staticOBMessageUtils;

  /**
   * Initializes common mocks.
   */
  @Before
  public void setUp() {
    staticOBMessageUtils = mockStatic(OBMessageUtils.class);
    staticOBMessageUtils.when(() -> OBMessageUtils.messageBD(PRODUCT_NULL_AND_MOVEMENT_QTY_ZERO))
        .thenReturn(PRODUCT_NULL_AND_MOVEMENT_QTY_ZERO);
    staticOBMessageUtils.when(() -> OBMessageUtils.messageBD(PRODUCT_NULL_AND_MOVEMENT_QTY_GREATER_ZERO))
        .thenReturn(PRODUCT_NULL_AND_MOVEMENT_QTY_GREATER_ZERO);

    when(newEvent.getTargetInstance()).thenReturn(shipmentInOutLine);
    when(updateEvent.getTargetInstance()).thenReturn(shipmentInOutLine);
  }

  /**
   * Releases static mocks.
   */
  @After
  public void tearDown() {
    staticOBMessageUtils.close();
  }

  /**
   * Lines without product, zero movement quantity and not marked as description-only must be blocked
   * when they are saved.
   */
  @Test
  public void testOnSaveBlocksLineWithoutProductAndZeroQtyWhenNotDescriptionOnly() {
    when(shipmentInOutLine.getProduct()).thenReturn(null);
    when(shipmentInOutLine.getMovementQuantity()).thenReturn(BigDecimal.ZERO);
    when(shipmentInOutLine.isDescriptionOnly()).thenReturn(false);

    assertThrows(OBException.class, () -> createHandlerWithValidEvent().onSave(newEvent));
  }

  /**
   * Lines without product, zero movement quantity and not marked as description-only must be blocked
   * when they are updated.
   */
  @Test
  public void testOnUpdateBlocksLineWithoutProductAndZeroQtyWhenNotDescriptionOnly() {
    when(shipmentInOutLine.getProduct()).thenReturn(null);
    when(shipmentInOutLine.getMovementQuantity()).thenReturn(BigDecimal.ZERO);
    when(shipmentInOutLine.isDescriptionOnly()).thenReturn(false);

    assertThrows(OBException.class, () -> createHandlerWithValidEvent().onUpdate(updateEvent));
  }

  /**
   * Description-only lines keep being allowed even if they do not have product and movement quantity is
   * zero.
   */
  @Test
  public void testOnSaveAllowsDescriptionOnlyLineWithoutProductAndZeroQty() {
    when(shipmentInOutLine.getProduct()).thenReturn(null);
    when(shipmentInOutLine.getMovementQuantity()).thenReturn(BigDecimal.ZERO);
    when(shipmentInOutLine.isDescriptionOnly()).thenReturn(true);

    assertDoesNotThrow(() -> createHandlerWithValidEvent().onSave(newEvent));
  }

  /**
   * Existing validation for lines without product and non-zero quantity must keep being enforced.
   */
  @Test
  public void testOnSaveKeepsBlockingLineWithoutProductAndNonZeroQty() {
    when(shipmentInOutLine.getProduct()).thenReturn(null);
    when(shipmentInOutLine.getMovementQuantity()).thenReturn(BigDecimal.ONE);

    assertThrows(OBException.class, () -> createHandlerWithValidEvent().onSave(newEvent));
  }

  /**
   * Lines with product are not part of this event handler validation.
   */
  @Test
  public void testOnSaveAllowsLineWithProductAndZeroQty() {
    when(shipmentInOutLine.getProduct()).thenReturn(product);
    when(shipmentInOutLine.getMovementQuantity()).thenReturn(BigDecimal.ZERO);

    assertDoesNotThrow(() -> createHandlerWithValidEvent().onSave(newEvent));
  }

  private MInOutLineEventHandler createHandlerWithValidEvent() {
    return new MInOutLineEventHandler() {
      @Override
      protected boolean isValidEvent(EntityPersistenceEvent event) {
        return true;
      }
    };
  }
}
