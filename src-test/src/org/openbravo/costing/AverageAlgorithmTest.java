package org.openbravo.costing;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.costing.CostingServer.TrxType;

/**
 * Tests for {@link AverageAlgorithm}.
 */
@RunWith(MockitoJUnitRunner.class)
public class AverageAlgorithmTest {
  /** Modifies average receipt returns true. */

  @Test
  public void testModifiesAverageReceiptReturnsTrue() {
    assertTrue(AverageAlgorithm.modifiesAverage(TrxType.Receipt));
  }
  /** Modifies average receipt void returns true. */

  @Test
  public void testModifiesAverageReceiptVoidReturnsTrue() {
    assertTrue(AverageAlgorithm.modifiesAverage(TrxType.ReceiptVoid));
  }
  /** Modifies average shipment void returns true. */

  @Test
  public void testModifiesAverageShipmentVoidReturnsTrue() {
    assertTrue(AverageAlgorithm.modifiesAverage(TrxType.ShipmentVoid));
  }
  /** Modifies average shipment return returns true. */

  @Test
  public void testModifiesAverageShipmentReturnReturnsTrue() {
    assertTrue(AverageAlgorithm.modifiesAverage(TrxType.ShipmentReturn));
  }
  /** Modifies average shipment negative returns true. */

  @Test
  public void testModifiesAverageShipmentNegativeReturnsTrue() {
    assertTrue(AverageAlgorithm.modifiesAverage(TrxType.ShipmentNegative));
  }
  /** Modifies average inventory increase returns true. */

  @Test
  public void testModifiesAverageInventoryIncreaseReturnsTrue() {
    assertTrue(AverageAlgorithm.modifiesAverage(TrxType.InventoryIncrease));
  }
  /** Modifies average inventory opening returns true. */

  @Test
  public void testModifiesAverageInventoryOpeningReturnsTrue() {
    assertTrue(AverageAlgorithm.modifiesAverage(TrxType.InventoryOpening));
  }
  /** Modifies average int movement to returns true. */

  @Test
  public void testModifiesAverageIntMovementToReturnsTrue() {
    assertTrue(AverageAlgorithm.modifiesAverage(TrxType.IntMovementTo));
  }
  /** Modifies average internal cons negative returns true. */

  @Test
  public void testModifiesAverageInternalConsNegativeReturnsTrue() {
    assertTrue(AverageAlgorithm.modifiesAverage(TrxType.InternalConsNegative));
  }
  /** Modifies average internal cons void returns true. */

  @Test
  public void testModifiesAverageInternalConsVoidReturnsTrue() {
    assertTrue(AverageAlgorithm.modifiesAverage(TrxType.InternalConsVoid));
  }
  /** Modifies average bom product returns true. */

  @Test
  public void testModifiesAverageBOMProductReturnsTrue() {
    assertTrue(AverageAlgorithm.modifiesAverage(TrxType.BOMProduct));
  }
  /** Modifies average manufacturing produced returns true. */

  @Test
  public void testModifiesAverageManufacturingProducedReturnsTrue() {
    assertTrue(AverageAlgorithm.modifiesAverage(TrxType.ManufacturingProduced));
  }
  /** Modifies average shipment returns false. */

  @Test
  public void testModifiesAverageShipmentReturnsFalse() {
    assertFalse(AverageAlgorithm.modifiesAverage(TrxType.Shipment));
  }
  /** Modifies average receipt return returns false. */

  @Test
  public void testModifiesAverageReceiptReturnReturnsFalse() {
    assertFalse(AverageAlgorithm.modifiesAverage(TrxType.ReceiptReturn));
  }
  /** Modifies average receipt negative returns false. */

  @Test
  public void testModifiesAverageReceiptNegativeReturnsFalse() {
    assertFalse(AverageAlgorithm.modifiesAverage(TrxType.ReceiptNegative));
  }
  /** Modifies average inventory decrease returns false. */

  @Test
  public void testModifiesAverageInventoryDecreaseReturnsFalse() {
    assertFalse(AverageAlgorithm.modifiesAverage(TrxType.InventoryDecrease));
  }
  /** Modifies average inventory closing returns false. */

  @Test
  public void testModifiesAverageInventoryClosingReturnsFalse() {
    assertFalse(AverageAlgorithm.modifiesAverage(TrxType.InventoryClosing));
  }
  /** Modifies average int movement from returns false. */

  @Test
  public void testModifiesAverageIntMovementFromReturnsFalse() {
    assertFalse(AverageAlgorithm.modifiesAverage(TrxType.IntMovementFrom));
  }
  /** Modifies average internal cons returns false. */

  @Test
  public void testModifiesAverageInternalConsReturnsFalse() {
    assertFalse(AverageAlgorithm.modifiesAverage(TrxType.InternalCons));
  }
  /** Modifies average bom part returns false. */

  @Test
  public void testModifiesAverageBOMPartReturnsFalse() {
    assertFalse(AverageAlgorithm.modifiesAverage(TrxType.BOMPart));
  }
  /** Modifies average manufacturing consumed returns false. */

  @Test
  public void testModifiesAverageManufacturingConsumedReturnsFalse() {
    assertFalse(AverageAlgorithm.modifiesAverage(TrxType.ManufacturingConsumed));
  }
  /** Modifies average unknown returns false. */

  @Test
  public void testModifiesAverageUnknownReturnsFalse() {
    assertFalse(AverageAlgorithm.modifiesAverage(TrxType.Unknown));
  }
}
