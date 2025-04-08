package org.openbravo.test.materialMgmt.linevalidation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.advpaymentmngt.utility.FIN_Utility;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.materialmgmt.InventoryCountProcess;
import org.openbravo.model.materialmgmt.transaction.InventoryCount;
import org.openbravo.model.materialmgmt.transaction.InventoryCountLine;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessContext;

/**
 * Tests for {@link InventoryCountProcess}.
 * Verifies:
 * - Error when inventory has no lines.
 * - Successful processing when inventory has lines.
 */
@RunWith(MockitoJUnitRunner.class)
public class InventoryCountProcessTest {

  private static final String PHYSICAL_INVENTORY_WITHOUT_LINES = "PhysicalInventoryWithoutLines";
  private static final String SUCCESS = "Success";
  private InventoryCountProcess inventoryCountProcess;

  @Mock
  private ProcessBundle bundle;

  @Mock
  private InventoryCount inventory;

  @Mock
  private ProcessContext processContext;

  /**
   * Sets up the test environment before each test.
   * Initializes the InventoryCountProcess instance and configures common behavior.
   */
  @Before
  public void setUp() {
    inventoryCountProcess = spy(new InventoryCountProcess());
  }

  /**
   * Verifies that an error is set when the inventory has no lines.
   *
   * @throws Exception
   *     if an error occurs during test execution
   */
  @Test
  public void shouldSetErrorResultWhenInventoryHasNoLines() throws Exception {
    String inventoryId = "TEST_INV_001";
    Map<String, Object> params = new HashMap<>();
    params.put("M_Inventory_ID", inventoryId);

    when(bundle.getParams()).thenReturn(params);
    when(bundle.getContext()).thenReturn(processContext);
    when(processContext.getLanguage()).thenReturn(null);

    when(inventory.getMaterialMgmtInventoryCountLineList()).thenReturn(Collections.emptyList());

    try (var obDalMock = mockStatic(OBDal.class); var messageUtilsMock = mockStatic(
        OBMessageUtils.class); var finUtilityMock = mockStatic(
        FIN_Utility.class); MockedStatic<OBContext> mockedOBContext = mockStatic(OBContext.class)) {

      OBDal obDalInstance = mock(OBDal.class);
      obDalMock.when(OBDal::getInstance).thenReturn(obDalInstance);
      when(obDalInstance.get(InventoryCount.class, inventoryId)).thenReturn(inventory);

      messageUtilsMock.when(() -> OBMessageUtils.messageBD(PHYSICAL_INVENTORY_WITHOUT_LINES)).thenReturn(
          PHYSICAL_INVENTORY_WITHOUT_LINES);

      finUtilityMock.when(() -> FIN_Utility.getExceptionMessage(any())).thenReturn(PHYSICAL_INVENTORY_WITHOUT_LINES);

      doNothing().when(obDalInstance).rollbackAndClose();
      doNothing().when(obDalInstance).save(any(InventoryCount.class));

      mockedOBContext.when(OBContext::setAdminMode).thenAnswer(invocation -> null);
      mockedOBContext.when(OBContext::restorePreviousMode).thenAnswer(invocation -> null);

      inventoryCountProcess.execute(bundle);

      ArgumentCaptor<OBError> captor = ArgumentCaptor.forClass(OBError.class);
      verify(bundle).setResult(captor.capture());

      OBError result = captor.getValue();
      assertEquals("Error", result.getType());
      assertEquals(PHYSICAL_INVENTORY_WITHOUT_LINES, result.getMessage());

      verify(inventory, atLeastOnce()).setProcessNow(false);
    }
  }

  /**
   * Verifies successful processing when the inventory has lines.
   */
  @Test
  public void shouldNotThrowExceptionWhenInventoryHasLines() {
    String inventoryId = "TEST_INV_002";
    Map<String, Object> params = new HashMap<>();
    params.put("M_Inventory_ID", inventoryId);

    when(bundle.getParams()).thenReturn(params);

    when(inventory.getMaterialMgmtInventoryCountLineList()).thenReturn(
        Collections.singletonList(mock(InventoryCountLine.class)));
    when(inventory.isProcessNow()).thenReturn(false);

    OBError successMessage = new OBError();
    successMessage.setType(SUCCESS);
    successMessage.setTitle(SUCCESS);
    doReturn(successMessage).when(inventoryCountProcess).processInventory(inventory);

    try (var obDalMock = mockStatic(OBDal.class); var messageUtilsMock = mockStatic(
        OBMessageUtils.class); var sessionHandlerMock = mockStatic(
        SessionHandler.class); MockedStatic<OBContext> mockedOBContext = mockStatic(OBContext.class)) {

      OBDal obDalInstance = mock(OBDal.class);
      obDalMock.when(OBDal::getInstance).thenReturn(obDalInstance);
      when(obDalInstance.get(InventoryCount.class, inventoryId)).thenReturn(inventory);

      messageUtilsMock.when(() -> OBMessageUtils.messageBD(SUCCESS)).thenReturn(SUCCESS);

      sessionHandlerMock.when(SessionHandler::isSessionHandlerPresent).thenReturn(true);
      SessionHandler sessionHandlerInstance = mock(SessionHandler.class);
      sessionHandlerMock.when(SessionHandler::getInstance).thenReturn(sessionHandlerInstance);
      doNothing().when(sessionHandlerInstance).commitAndStart();

      mockedOBContext.when(OBContext::setAdminMode).thenAnswer(invocation -> null);
      mockedOBContext.when(OBContext::restorePreviousMode).thenAnswer(invocation -> null);

      assertDoesNotThrow(() -> inventoryCountProcess.execute(bundle));

      ArgumentCaptor<OBError> captor = ArgumentCaptor.forClass(OBError.class);
      verify(bundle).setResult(captor.capture());

      OBError result = captor.getValue();
      assertEquals(SUCCESS, result.getType());
      assertEquals(SUCCESS, result.getTitle());

      verify(inventory, atLeastOnce()).setProcessNow(false);

      verify(sessionHandlerInstance, times(1)).commitAndStart();
    }
  }
}
