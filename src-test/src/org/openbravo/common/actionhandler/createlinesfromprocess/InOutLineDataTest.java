package org.openbravo.common.actionhandler.createlinesfromprocess;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for the {@link InOutLineData} class.
 * Verifies the behavior of the constructor, getters, and setters.
 */
class InOutLineDataTest {

  /**
   * Tests the constructor and getter methods of the {@link InOutLineData} class.
   * Verifies that the object is correctly initialized with the provided data.
   */
  @Test
  void testConstructorAndGetters() {
    Object[] data = { "lineId123", new BigDecimal("10.5"), new BigDecimal("20.0"), new BigDecimal(
        "15.5"), "operativeUOM123", "uom123" };

    InOutLineData inOutLineData = new InOutLineData(data);

    assertEquals("lineId123", inOutLineData.getShipmentInOutLineId());
    assertEquals(new BigDecimal("10.5"), inOutLineData.getMovementQuantity());
    assertEquals(new BigDecimal("20.0"), inOutLineData.getOrderQuantity());
    assertEquals(new BigDecimal("15.5"), inOutLineData.getOperativeQuantity());
    assertEquals("operativeUOM123", inOutLineData.getOperativeUOMId());
    assertEquals("uom123", inOutLineData.getUOMId());
  }

  /**
   * Tests the setter methods of the {@link InOutLineData} class.
   * Verifies that the properties are correctly updated.
   */
  @Test
  void testSetters() {
    InOutLineData inOutLineData = new InOutLineData(new Object[6]);

    inOutLineData.setShipmentInOutLineId("newLineId123");
    inOutLineData.setMovementQuantity(new BigDecimal("30.5"));
    inOutLineData.setOrderQuantity(new BigDecimal("40.0"));
    inOutLineData.setOperativeQuantity(new BigDecimal("25.5"));
    inOutLineData.setOperativeUOMId("newOperativeUOM123");
    inOutLineData.setUOMId("newUOM123");

    assertEquals("newLineId123", inOutLineData.getShipmentInOutLineId());
    assertEquals(new BigDecimal("30.5"), inOutLineData.getMovementQuantity());
    assertEquals(new BigDecimal("40.0"), inOutLineData.getOrderQuantity());
    assertEquals(new BigDecimal("25.5"), inOutLineData.getOperativeQuantity());
    assertEquals("newOperativeUOM123", inOutLineData.getOperativeUOMId());
    assertEquals("newUOM123", inOutLineData.getUOMId());
  }
}
