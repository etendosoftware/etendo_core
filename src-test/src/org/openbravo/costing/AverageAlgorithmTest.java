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

  @Test
  public void testModifiesAverageReceiptReturnsTrue() {
    assertTrue(AverageAlgorithm.modifiesAverage(TrxType.Receipt));
  }

  @Test
  public void testModifiesAverageReceiptVoidReturnsTrue() {
    assertTrue(AverageAlgorithm.modifiesAverage(TrxType.ReceiptVoid));
  }

  @Test
  public void testModifiesAverageShipmentVoidReturnsTrue() {
    assertTrue(AverageAlgorithm.modifiesAverage(TrxType.ShipmentVoid));
  }

  @Test
  public void testModifiesAverageShipmentReturnReturnsTrue() {
    assertTrue(AverageAlgorithm.modifiesAverage(TrxType.ShipmentReturn));
  }

  @Test
  public void testModifiesAverageShipmentNegativeReturnsTrue() {
    assertTrue(AverageAlgorithm.modifiesAverage(TrxType.ShipmentNegative));
  }

  @Test
  public void testModifiesAverageInventoryIncreaseReturnsTrue() {
    assertTrue(AverageAlgorithm.modifiesAverage(TrxType.InventoryIncrease));
  }

  @Test
  public void testModifiesAverageInventoryOpeningReturnsTrue() {
    assertTrue(AverageAlgorithm.modifiesAverage(TrxType.InventoryOpening));
  }

  @Test
  public void testModifiesAverageIntMovementToReturnsTrue() {
    assertTrue(AverageAlgorithm.modifiesAverage(TrxType.IntMovementTo));
  }

  @Test
  public void testModifiesAverageInternalConsNegativeReturnsTrue() {
    assertTrue(AverageAlgorithm.modifiesAverage(TrxType.InternalConsNegative));
  }

  @Test
  public void testModifiesAverageInternalConsVoidReturnsTrue() {
    assertTrue(AverageAlgorithm.modifiesAverage(TrxType.InternalConsVoid));
  }

  @Test
  public void testModifiesAverageBOMProductReturnsTrue() {
    assertTrue(AverageAlgorithm.modifiesAverage(TrxType.BOMProduct));
  }

  @Test
  public void testModifiesAverageManufacturingProducedReturnsTrue() {
    assertTrue(AverageAlgorithm.modifiesAverage(TrxType.ManufacturingProduced));
  }

  @Test
  public void testModifiesAverageShipmentReturnsFalse() {
    assertFalse(AverageAlgorithm.modifiesAverage(TrxType.Shipment));
  }

  @Test
  public void testModifiesAverageReceiptReturnReturnsFalse() {
    assertFalse(AverageAlgorithm.modifiesAverage(TrxType.ReceiptReturn));
  }

  @Test
  public void testModifiesAverageReceiptNegativeReturnsFalse() {
    assertFalse(AverageAlgorithm.modifiesAverage(TrxType.ReceiptNegative));
  }

  @Test
  public void testModifiesAverageInventoryDecreaseReturnsFalse() {
    assertFalse(AverageAlgorithm.modifiesAverage(TrxType.InventoryDecrease));
  }

  @Test
  public void testModifiesAverageInventoryClosingReturnsFalse() {
    assertFalse(AverageAlgorithm.modifiesAverage(TrxType.InventoryClosing));
  }

  @Test
  public void testModifiesAverageIntMovementFromReturnsFalse() {
    assertFalse(AverageAlgorithm.modifiesAverage(TrxType.IntMovementFrom));
  }

  @Test
  public void testModifiesAverageInternalConsReturnsFalse() {
    assertFalse(AverageAlgorithm.modifiesAverage(TrxType.InternalCons));
  }

  @Test
  public void testModifiesAverageBOMPartReturnsFalse() {
    assertFalse(AverageAlgorithm.modifiesAverage(TrxType.BOMPart));
  }

  @Test
  public void testModifiesAverageManufacturingConsumedReturnsFalse() {
    assertFalse(AverageAlgorithm.modifiesAverage(TrxType.ManufacturingConsumed));
  }

  @Test
  public void testModifiesAverageUnknownReturnsFalse() {
    assertFalse(AverageAlgorithm.modifiesAverage(TrxType.Unknown));
  }
}
