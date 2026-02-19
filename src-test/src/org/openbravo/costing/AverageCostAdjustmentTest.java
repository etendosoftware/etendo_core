package org.openbravo.costing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.math.BigDecimal;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;
import org.openbravo.costing.CostingServer.TrxType;
import org.openbravo.model.materialmgmt.transaction.InternalConsumption;
import org.openbravo.model.materialmgmt.transaction.InternalConsumptionLine;
import org.openbravo.model.materialmgmt.transaction.MaterialTransaction;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;

/**
 * Tests for {@link AverageCostAdjustment}.
 */
@RunWith(MockitoJUnitRunner.class)
public class AverageCostAdjustmentTest {

  private AverageCostAdjustment instance;
  private ObjenesisStd objenesis;

  @Before
  public void setUp() {
    objenesis = new ObjenesisStd();
    instance = objenesis.newInstance(AverageCostAdjustment.class);
  }

  @Test
  public void testGetTransactionPriceWithZeroMovementQuantity() throws Exception {
    BigDecimal trxCost = new BigDecimal("100.00");
    BigDecimal negativeStockAdj = BigDecimal.ZERO;
    BigDecimal movementQty = BigDecimal.ZERO;

    BigDecimal result = invokeGetTransactionPrice(trxCost, negativeStockAdj, movementQty);

    assertEquals(BigDecimal.ZERO, result);
  }

  @Test
  public void testGetTransactionPriceWithPositiveMovementQuantity() throws Exception {
    setPrivateField(instance, "costCurPrecission", 2);
    BigDecimal trxCost = new BigDecimal("100.00");
    BigDecimal negativeStockAdj = BigDecimal.ZERO;
    BigDecimal movementQty = new BigDecimal("10");

    BigDecimal result = invokeGetTransactionPrice(trxCost, negativeStockAdj, movementQty);

    assertEquals(new BigDecimal("10.00"), result);
  }

  @Test
  public void testGetTransactionPriceWithNegativeMovementQuantity() throws Exception {
    setPrivateField(instance, "costCurPrecission", 2);
    BigDecimal trxCost = new BigDecimal("50.00");
    BigDecimal negativeStockAdj = BigDecimal.ZERO;
    BigDecimal movementQty = new BigDecimal("-5");

    BigDecimal result = invokeGetTransactionPrice(trxCost, negativeStockAdj, movementQty);

    assertEquals(new BigDecimal("10.00"), result);
  }

  @Test
  public void testGetTransactionPriceSubtractsNegativeStockAdj() throws Exception {
    setPrivateField(instance, "costCurPrecission", 2);
    BigDecimal trxCost = new BigDecimal("120.00");
    BigDecimal negativeStockAdj = new BigDecimal("20.00");
    BigDecimal movementQty = new BigDecimal("10");

    BigDecimal result = invokeGetTransactionPrice(trxCost, negativeStockAdj, movementQty);

    assertEquals(new BigDecimal("10.00"), result);
  }

  @Test
  public void testIsVoidedTrxReceiptVoidReturnsTrue() throws Exception {
    MaterialTransaction trx = mock(MaterialTransaction.class);

    boolean result = invokeIsVoidedTrx(trx, TrxType.ReceiptVoid);

    assertTrue(result);
  }

  @Test
  public void testIsVoidedTrxShipmentVoidReturnsTrue() throws Exception {
    MaterialTransaction trx = mock(MaterialTransaction.class);

    boolean result = invokeIsVoidedTrx(trx, TrxType.ShipmentVoid);

    assertTrue(result);
  }

  @Test
  public void testIsVoidedTrxInternalConsVoidReturnsTrue() throws Exception {
    MaterialTransaction trx = mock(MaterialTransaction.class);

    boolean result = invokeIsVoidedTrx(trx, TrxType.InternalConsVoid);

    assertTrue(result);
  }

  @Test
  public void testIsVoidedTrxReceiptWithVOStatusReturnsTrue() throws Exception {
    MaterialTransaction trx = mock(MaterialTransaction.class);
    ShipmentInOutLine shipmentLine = mock(ShipmentInOutLine.class);
    ShipmentInOut shipment = mock(ShipmentInOut.class);
    when(trx.getGoodsShipmentLine()).thenReturn(shipmentLine);
    when(shipmentLine.getShipmentReceipt()).thenReturn(shipment);
    when(shipment.getDocumentStatus()).thenReturn("VO");

    boolean result = invokeIsVoidedTrx(trx, TrxType.Receipt);

    assertTrue(result);
  }

  @Test
  public void testIsVoidedTrxReceiptWithNonVOStatusReturnsFalse() throws Exception {
    MaterialTransaction trx = mock(MaterialTransaction.class);
    ShipmentInOutLine shipmentLine = mock(ShipmentInOutLine.class);
    ShipmentInOut shipment = mock(ShipmentInOut.class);
    when(trx.getGoodsShipmentLine()).thenReturn(shipmentLine);
    when(shipmentLine.getShipmentReceipt()).thenReturn(shipment);
    when(shipment.getDocumentStatus()).thenReturn("CO");

    boolean result = invokeIsVoidedTrx(trx, TrxType.Receipt);

    assertFalse(result);
  }

  @Test
  public void testIsVoidedTrxInternalConsWithVOStatusReturnsTrue() throws Exception {
    MaterialTransaction trx = mock(MaterialTransaction.class);
    InternalConsumptionLine consLine = mock(InternalConsumptionLine.class);
    InternalConsumption consumption = mock(InternalConsumption.class);
    when(trx.getInternalConsumptionLine()).thenReturn(consLine);
    when(consLine.getInternalConsumption()).thenReturn(consumption);
    when(consumption.getStatus()).thenReturn("VO");

    boolean result = invokeIsVoidedTrx(trx, TrxType.InternalCons);

    assertTrue(result);
  }

  @Test
  public void testIsVoidedTrxInternalConsWithNonVOStatusReturnsFalse() throws Exception {
    MaterialTransaction trx = mock(MaterialTransaction.class);
    InternalConsumptionLine consLine = mock(InternalConsumptionLine.class);
    InternalConsumption consumption = mock(InternalConsumption.class);
    when(trx.getInternalConsumptionLine()).thenReturn(consLine);
    when(consLine.getInternalConsumption()).thenReturn(consumption);
    when(consumption.getStatus()).thenReturn("CO");

    boolean result = invokeIsVoidedTrx(trx, TrxType.InternalCons);

    assertFalse(result);
  }

  @Test
  public void testIsVoidedTrxInventoryIncreaseReturnsFalse() throws Exception {
    MaterialTransaction trx = mock(MaterialTransaction.class);

    boolean result = invokeIsVoidedTrx(trx, TrxType.InventoryIncrease);

    assertFalse(result);
  }

  @Test
  public void testIsVoidedTrxBOMProductReturnsFalse() throws Exception {
    MaterialTransaction trx = mock(MaterialTransaction.class);

    boolean result = invokeIsVoidedTrx(trx, TrxType.BOMProduct);

    assertFalse(result);
  }

  @Test
  public void testIsVoidedTrxShipmentWithVOStatusReturnsTrue() throws Exception {
    MaterialTransaction trx = mock(MaterialTransaction.class);
    ShipmentInOutLine shipmentLine = mock(ShipmentInOutLine.class);
    ShipmentInOut shipment = mock(ShipmentInOut.class);
    when(trx.getGoodsShipmentLine()).thenReturn(shipmentLine);
    when(shipmentLine.getShipmentReceipt()).thenReturn(shipment);
    when(shipment.getDocumentStatus()).thenReturn("VO");

    boolean result = invokeIsVoidedTrx(trx, TrxType.Shipment);

    assertTrue(result);
  }

  @Test
  public void testIsVoidedTrxManufacturingProducedReturnsFalse() throws Exception {
    MaterialTransaction trx = mock(MaterialTransaction.class);

    boolean result = invokeIsVoidedTrx(trx, TrxType.ManufacturingProduced);

    assertFalse(result);
  }

  private BigDecimal invokeGetTransactionPrice(BigDecimal trxCost,
      BigDecimal trxNegativeStockAdjAmt, BigDecimal movementQuantity) throws Exception {
    Method method = AverageCostAdjustment.class.getDeclaredMethod("getTransactionPrice",
        BigDecimal.class, BigDecimal.class, BigDecimal.class);
    method.setAccessible(true);
    return (BigDecimal) method.invoke(instance, trxCost, trxNegativeStockAdjAmt, movementQuantity);
  }

  private boolean invokeIsVoidedTrx(MaterialTransaction trx, TrxType trxType) throws Exception {
    Method method = AverageCostAdjustment.class.getDeclaredMethod("isVoidedTrx",
        MaterialTransaction.class, TrxType.class);
    method.setAccessible(true);
    return (boolean) method.invoke(instance, trx, trxType);
  }

  private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
    java.lang.reflect.Field field = findField(target.getClass(), fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }

  private java.lang.reflect.Field findField(Class<?> clazz, String fieldName) {
    while (clazz != null) {
      try {
        return clazz.getDeclaredField(fieldName);
      } catch (NoSuchFieldException e) {
        clazz = clazz.getSuperclass();
      }
    }
    throw new RuntimeException("Field not found: " + fieldName);
  }
}
