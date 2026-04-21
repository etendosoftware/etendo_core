package org.openbravo.advpaymentmngt.filterexpression;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.enterprise.inject.Instance;

import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;
import org.openbravo.client.application.OBBindingsConstants;
import org.openbravo.client.kernel.ComponentProvider;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;

/**
 * Unit tests for AddOrderOrInvoiceFilterExpressionHandler.
 * Tests the concrete methods of the abstract class using a test subclass.
 */
@SuppressWarnings("java:S112")
@RunWith(MockitoJUnitRunner.Silent.class)
public class AddOrderOrInvoiceFilterExpressionHandlerTest {

  private static final String TEST_WINDOW_ID = "testWindowId123";

  @Mock
  private Instance<AddPaymentDefaultValuesHandler> addPaymentDefaultValuesHandlers;

  @Mock
  private Instance<AddPaymentDefaultValuesHandler> selectedHandlers;

  @Mock
  private AddPaymentDefaultValuesHandler mockHandler;

  private MockedStatic<OBDal> obDalStatic;

  @Mock
  private OBDal mockOBDal;

  private TestHandler handler;
  /**
   * Sets up test fixtures.
   * @throws Exception if an error occurs
   */

  @Before
  public void setUp() throws Exception {
    obDalStatic = mockStatic(OBDal.class);
    obDalStatic.when(OBDal::getInstance).thenReturn(mockOBDal);

    handler = new TestHandler();
    setField(handler, AddOrderOrInvoiceFilterExpressionHandler.class,
        "addPaymentDefaultValuesHandlers", addPaymentDefaultValuesHandlers);
  }
  /** Tears down test fixtures. */

  @After
  public void tearDown() {
    if (obDalStatic != null) {
      obDalStatic.close();
    }
  }
  /** Get defaults handler returns single handler. */

  @Test
  public void testGetDefaultsHandlerReturnsSingleHandler() {
    List<AddPaymentDefaultValuesHandler> handlerList = new ArrayList<>();
    handlerList.add(mockHandler);

    when(addPaymentDefaultValuesHandlers.select(any(ComponentProvider.Selector.class)))
        .thenReturn(selectedHandlers);
    when(selectedHandlers.iterator()).thenReturn(handlerList.iterator());
    lenient().when(mockHandler.getSeq()).thenReturn(100L);

    AddPaymentDefaultValuesHandler result = handler.getDefaultsHandler(TEST_WINDOW_ID);

    assertEquals(mockHandler, result);
  }
  /** Get defaults handler returns null when no handlers. */

  @Test
  public void testGetDefaultsHandlerReturnsNullWhenNoHandlers() {
    List<AddPaymentDefaultValuesHandler> handlerList = new ArrayList<>();

    when(addPaymentDefaultValuesHandlers.select(any(ComponentProvider.Selector.class)))
        .thenReturn(selectedHandlers);
    when(selectedHandlers.iterator()).thenReturn(handlerList.iterator());

    AddPaymentDefaultValuesHandler result = handler.getDefaultsHandler(TEST_WINDOW_ID);

    assertNull(result);
  }
  /** Get defaults handler returns lowest seq handler. */

  @Test
  public void testGetDefaultsHandlerReturnsLowestSeqHandler() {
    AddPaymentDefaultValuesHandler handler1 = mock(AddPaymentDefaultValuesHandler.class);
    AddPaymentDefaultValuesHandler handler2 = mock(AddPaymentDefaultValuesHandler.class);
    when(handler1.getSeq()).thenReturn(200L);
    when(handler2.getSeq()).thenReturn(100L);

    List<AddPaymentDefaultValuesHandler> handlerList = new ArrayList<>();
    handlerList.add(handler1);
    handlerList.add(handler2);

    when(addPaymentDefaultValuesHandlers.select(any(ComponentProvider.Selector.class)))
        .thenReturn(selectedHandlers);
    when(selectedHandlers.iterator()).thenReturn(handlerList.iterator());

    AddPaymentDefaultValuesHandler result = handler.getDefaultsHandler(TEST_WINDOW_ID);

    assertEquals(handler2, result);
  }

  // --- Concrete test subclass ---

  private static class TestHandler extends AddOrderOrInvoiceFilterExpressionHandler {
    @Override
    protected long getSeq() {
      return 100L;
    }
  }

  // --- Helper ---

  private void setField(Object target, Class<?> clazz, String fieldName, Object value)
      throws IllegalAccessException, NoSuchFieldException {
    Field field = clazz.getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }
}
