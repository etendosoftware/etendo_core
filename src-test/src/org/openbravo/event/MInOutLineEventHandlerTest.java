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

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.SimpleExpression;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEvent;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
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
  @Mock private EntityDeleteEvent deleteEvent;
  @Mock private ShipmentInOutLine shipmentInOutLine;
  @Mock private ShipmentInOut shipmentInOut;
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

    OBException exception = assertThrows(OBException.class,
        () -> createHandlerWithValidEvent().onSave(newEvent));

    assertEquals(PRODUCT_NULL_AND_MOVEMENT_QTY_ZERO, exception.getMessage());
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

    OBException exception = assertThrows(OBException.class,
        () -> createHandlerWithValidEvent().onUpdate(updateEvent));

    assertEquals(PRODUCT_NULL_AND_MOVEMENT_QTY_ZERO, exception.getMessage());
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
   * Description-only lines keep being allowed during updates even if they do not have product and
   * movement quantity is zero.
   */
  @Test
  public void testOnUpdateAllowsDescriptionOnlyLineWithoutProductAndZeroQty() {
    when(shipmentInOutLine.getProduct()).thenReturn(null);
    when(shipmentInOutLine.getMovementQuantity()).thenReturn(BigDecimal.ZERO);
    when(shipmentInOutLine.isDescriptionOnly()).thenReturn(true);

    assertDoesNotThrow(() -> createHandlerWithValidEvent().onUpdate(updateEvent));
  }

  /**
   * Existing validation for lines without product and non-zero quantity must keep being enforced.
   */
  @Test
  public void testOnSaveKeepsBlockingLineWithoutProductAndNonZeroQty() {
    when(shipmentInOutLine.getProduct()).thenReturn(null);
    when(shipmentInOutLine.getMovementQuantity()).thenReturn(BigDecimal.ONE);

    OBException exception = assertThrows(OBException.class,
        () -> createHandlerWithValidEvent().onSave(newEvent));

    assertEquals(PRODUCT_NULL_AND_MOVEMENT_QTY_GREATER_ZERO, exception.getMessage());
  }

  /**
   * Existing validation for lines without product and non-zero quantity must also be enforced during
   * updates.
   */
  @Test
  public void testOnUpdateKeepsBlockingLineWithoutProductAndNonZeroQty() {
    when(shipmentInOutLine.getProduct()).thenReturn(null);
    when(shipmentInOutLine.getMovementQuantity()).thenReturn(BigDecimal.ONE);

    OBException exception = assertThrows(OBException.class,
        () -> createHandlerWithValidEvent().onUpdate(updateEvent));

    assertEquals(PRODUCT_NULL_AND_MOVEMENT_QTY_GREATER_ZERO, exception.getMessage());
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

  /**
   * Null movement quantity is ignored by this validation to avoid blocking incomplete transient lines.
   */
  @Test
  public void testOnSaveAllowsLineWithoutProductWhenMovementQtyIsNull() {
    assertDoesNotThrow(() -> createHandlerWithValidEvent().onSave(newEvent));
  }

  /**
   * Invalid save events are skipped before reading the target instance.
   */
  @Test
  public void testOnSaveSkipsWhenEventIsInvalid() {
    assertDoesNotThrow(() -> createHandlerWithInvalidEvent().onSave(newEvent));
  }

  /**
   * Invalid update events are skipped before reading the target instance.
   */
  @Test
  public void testOnUpdateSkipsWhenEventIsInvalid() {
    assertDoesNotThrow(() -> createHandlerWithInvalidEvent().onUpdate(updateEvent));
  }

  /**
   * Invalid delete events are skipped before reading the target instance.
   */
  @Test
  public void testOnDeleteSkipsWhenEventIsInvalid() {
    assertDoesNotThrow(() -> createHandlerWithInvalidEvent().onDelete(deleteEvent));
  }

  /**
   * Deleting the last shipment line must remove the order relation from the parent shipment.
   */
  @Test
  @SuppressWarnings("unchecked")
  public void testOnDeleteClearsSalesOrderWhenLineIsLastInShipment() {
    OBDal obDal = mock(OBDal.class);
    OBCriteria<ShipmentInOutLine> criteria = mock(OBCriteria.class);
    SimpleExpression criterion = mock(SimpleExpression.class);

    when(deleteEvent.getTargetInstance()).thenReturn(shipmentInOutLine);
    when(shipmentInOutLine.getShipmentReceipt()).thenReturn(shipmentInOut);
    when(shipmentInOut.getId()).thenReturn("shipment-id");
    when(obDal.createCriteria(ShipmentInOutLine.class)).thenReturn(criteria);
    when(criteria.add(any(Criterion.class))).thenReturn(criteria);
    when(criteria.count()).thenReturn(1);
    when(obDal.get(ShipmentInOut.class, "shipment-id")).thenReturn(shipmentInOut);

    try (MockedStatic<OBDal> staticOBDal = mockStatic(OBDal.class);
         MockedStatic<org.hibernate.criterion.Restrictions> staticRestrictions =
             mockStatic(org.hibernate.criterion.Restrictions.class)) {
      staticOBDal.when(OBDal::getInstance).thenReturn(obDal);
      staticRestrictions.when(() -> org.hibernate.criterion.Restrictions.eq(
          ShipmentInOutLine.PROPERTY_SHIPMENTRECEIPT, shipmentInOut)).thenReturn(criterion);

      assertDoesNotThrow(() -> createHandlerWithValidEvent().onDelete(deleteEvent));
    }

    verify(shipmentInOut).setSalesOrder(null);
    verify(obDal).save(shipmentInOut);
  }

  /**
   * Deleting a shipment line must keep the order relation when more lines remain in the shipment.
   */
  @Test
  @SuppressWarnings("unchecked")
  public void testOnDeleteKeepsSalesOrderWhenShipmentHasMoreLines() {
    OBDal obDal = mock(OBDal.class);
    OBCriteria<ShipmentInOutLine> criteria = mock(OBCriteria.class);
    SimpleExpression criterion = mock(SimpleExpression.class);

    when(deleteEvent.getTargetInstance()).thenReturn(shipmentInOutLine);
    when(shipmentInOutLine.getShipmentReceipt()).thenReturn(shipmentInOut);
    when(obDal.createCriteria(ShipmentInOutLine.class)).thenReturn(criteria);
    when(criteria.add(any(Criterion.class))).thenReturn(criteria);
    when(criteria.count()).thenReturn(2);

    try (MockedStatic<OBDal> staticOBDal = mockStatic(OBDal.class);
         MockedStatic<org.hibernate.criterion.Restrictions> staticRestrictions =
             mockStatic(org.hibernate.criterion.Restrictions.class)) {
      staticOBDal.when(OBDal::getInstance).thenReturn(obDal);
      staticRestrictions.when(() -> org.hibernate.criterion.Restrictions.eq(
          ShipmentInOutLine.PROPERTY_SHIPMENTRECEIPT, shipmentInOut)).thenReturn(criterion);

      assertDoesNotThrow(() -> createHandlerWithValidEvent().onDelete(deleteEvent));
    }

    verify(shipmentInOut, never()).setSalesOrder(null);
    verify(obDal, never()).save(shipmentInOut);
  }

  /**
   * {@code getObservedEntities()} must return a non-{@code null} array.
   */
  @Test
  public void testGetObservedEntitiesReturnsNonNull() {
    assertNotNull(new MInOutLineEventHandler().getObservedEntities());
  }

  private MInOutLineEventHandler createHandlerWithValidEvent() {
    return new MInOutLineEventHandler() {
      @Override
      protected boolean isValidEvent(EntityPersistenceEvent event) {
        return true;
      }
    };
  }

  private MInOutLineEventHandler createHandlerWithInvalidEvent() {
    return new MInOutLineEventHandler() {
      @Override
      protected boolean isValidEvent(EntityPersistenceEvent event) {
        return false;
      }
    };
  }
}
