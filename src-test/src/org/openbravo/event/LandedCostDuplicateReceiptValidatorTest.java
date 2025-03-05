package org.openbravo.event;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.materialmgmt.cost.LCReceipt;
import org.openbravo.model.materialmgmt.cost.LandedCost;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;

/**
 * Unit tests for {@link LandedCostDuplicateReceiptValidator}.
 * This test class validates the behavior of the {@link LandedCostDuplicateReceiptValidator}
 * when handling new and update events for {@link LCReceipt} entities.
 * It ensures that duplicate receipt lines in the Landed Cost window are detected correctly.
 */
@ExtendWith(MockitoExtension.class)
public class LandedCostDuplicateReceiptValidatorTest {

  public static final String ID = "12345";
  @Mock
  private EntityNewEvent newEvent;
  @Mock
  private EntityUpdateEvent updateEvent;
  @Mock
  private Entity lcReceiptEntity;
  @Mock
  private Property landedCostProperty;
  @Mock
  private Property shipmentLineProperty;
  @Mock
  private LandedCost landedCost;
  @Mock
  private ShipmentInOutLine shipmentLine;
  @Mock
  private ShipmentInOutLine shipmentLine2;
  @Mock
  private LCReceipt lcReceipt;
  @Mock
  private LCReceipt lcReceipt2;
  @Mock
  private ModelProvider modelProvider;
  private LandedCostDuplicateReceiptValidator validator;
  private List<LCReceipt> receiptList;

  /**
   * Sets up the test environment before each test execution.
   * Initializes the validator and configures lenient stubbing for common mock interactions.
   */
  @BeforeEach
  public void setUp() {
    validator = new LandedCostDuplicateReceiptValidator() {
      @Override
      protected boolean isValidEvent(org.openbravo.client.kernel.event.EntityPersistenceEvent event) {
        return true;
      }
    };

    receiptList = new ArrayList<>();
    lenient().when(modelProvider.getEntity(LCReceipt.class)).thenReturn(lcReceiptEntity);
    lenient().when(lcReceiptEntity.getProperty(LCReceipt.PROPERTY_LANDEDCOST)).thenReturn(landedCostProperty);
    lenient().when(lcReceiptEntity.getProperty(LCReceipt.PROPERTY_GOODSSHIPMENTLINE)).thenReturn(shipmentLineProperty);
    lenient().when(landedCost.getLandedCostReceiptList()).thenReturn(receiptList);

  }

  /**
   * Tests that saving a new receipt without duplicates succeeds without exceptions.
   */
  @Test
  public void testOnSaveWhenNoDuplicateReceipts() {
    try (MockedStatic<ModelProvider> modelProviderMock = mockStatic(ModelProvider.class)) {
      modelProviderMock.when(ModelProvider::getInstance).thenReturn(modelProvider);

      when(newEvent.getCurrentState(landedCostProperty)).thenReturn(landedCost);
      when(newEvent.getCurrentState(shipmentLineProperty)).thenReturn(shipmentLine);
      when(landedCost.getLandedCostReceiptList()).thenReturn(receiptList);

      assertDoesNotThrow(() -> validator.onSave(newEvent));
    }
  }

  /**
   * Tests that saving a new receipt with a duplicate shipment line
   * throws an {@link OBException} with the appropriate error message.
   */
  @Test
  public void testOnSaveWhenDuplicateReceiptsExist() {
    receiptList.add(lcReceipt);

    String errorMessage = "LandedCostDuplicateReceipt";

    try (MockedStatic<ModelProvider> modelProviderMock = mockStatic(
        ModelProvider.class); MockedStatic<OBMessageUtils> messageUtilsMock = mockStatic(OBMessageUtils.class)) {

      modelProviderMock.when(ModelProvider::getInstance).thenReturn(modelProvider);
      messageUtilsMock.when(() -> OBMessageUtils.messageBD(anyString())).thenReturn(errorMessage);

      when(newEvent.getCurrentState(landedCostProperty)).thenReturn(landedCost);
      when(newEvent.getCurrentState(shipmentLineProperty)).thenReturn(shipmentLine);
      when(landedCost.getLandedCostReceiptList()).thenReturn(receiptList);
      when(lcReceipt.getGoodsShipmentLine()).thenReturn(shipmentLine);

      OBException exception = assertThrows(OBException.class, () -> validator.onSave(newEvent));

      assert (exception.getMessage().equals(errorMessage));
    }

  }

  /**
   * Tests that updating an existing receipt without duplicates
   * succeeds without exceptions.
   */
  @Test
  public void testOnUpdateWhenNoDuplicateReceipts() {
    receiptList.add(lcReceipt);

    try (MockedStatic<ModelProvider> modelProviderMock = mockStatic(
        ModelProvider.class); MockedStatic<OBDal> obDalMock = mockStatic(OBDal.class)) {

      modelProviderMock.when(ModelProvider::getInstance).thenReturn(modelProvider);
      obDalMock.when(OBDal::getInstance).thenReturn(mock(OBDal.class));

      when(modelProvider.getEntity(LCReceipt.class)).thenReturn(lcReceiptEntity);

      when(updateEvent.getId()).thenReturn(ID);
      when(updateEvent.getCurrentState(landedCostProperty)).thenReturn(landedCost);

      when(OBDal.getInstance().get(LCReceipt.class, ID)).thenReturn(lcReceipt);
      when(lcReceipt.getGoodsShipmentLine()).thenReturn(shipmentLine);
      when(landedCost.getLandedCostReceiptList()).thenReturn(receiptList);

      assertDoesNotThrow(() -> validator.onUpdate(updateEvent));
    }
  }

  /**
   * Tests that updating an existing receipt when no duplicate shipment lines exist
   * succeeds without exceptions.
   */
  @Test
  public void testOnUpdateWhenReceiptsHaveDifferentShipmentLines() {
    receiptList.add(lcReceipt2);
    try (MockedStatic<ModelProvider> modelProviderMock = mockStatic(
        ModelProvider.class); MockedStatic<OBDal> obDalMock = mockStatic(OBDal.class)) {

      modelProviderMock.when(ModelProvider::getInstance).thenReturn(modelProvider);
      obDalMock.when(OBDal::getInstance).thenReturn(mock(OBDal.class));

      when(modelProvider.getEntity(LCReceipt.class)).thenReturn(lcReceiptEntity);

      when(updateEvent.getId()).thenReturn(ID);
      when(updateEvent.getCurrentState(landedCostProperty)).thenReturn(landedCost);

      when(OBDal.getInstance().get(LCReceipt.class, ID)).thenReturn(lcReceipt);
      when(lcReceipt.getGoodsShipmentLine()).thenReturn(shipmentLine);
      when(landedCost.getLandedCostReceiptList()).thenReturn(receiptList);
      when(lcReceipt2.getGoodsShipmentLine()).thenReturn(shipmentLine2);

      assertDoesNotThrow(() -> validator.onUpdate(updateEvent));
    }
  }
}
