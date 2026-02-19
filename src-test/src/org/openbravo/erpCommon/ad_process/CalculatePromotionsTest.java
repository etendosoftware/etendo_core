package org.openbravo.erpCommon.ad_process;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.model.ad.access.User;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.CallStoredProcedure;

/**
 * Tests for {@link CalculatePromotions}.
 */
@SuppressWarnings({"java:S120"})
@RunWith(MockitoJUnitRunner.class)
public class CalculatePromotionsTest {

  private static final String SUCCESS = "Success";
  private static final String TAB_ID = "tabId";
  private static final String DO_EXECUTE = "doExecute";

  private static final String TEST_TAB_ID = "100";
  private static final String TEST_ORDER_ID = "ORDER001";
  private static final String TEST_INVOICE_ID = "INVOICE001";
  private static final String TEST_USER_ID = "USER001";

  private CalculatePromotions instance;

  @Mock
  private OBDal mockOBDal;
  @Mock
  private OBContext mockOBContext;
  @Mock
  private ProcessBundle mockBundle;
  @Mock
  private Tab mockTab;
  @Mock
  private Table mockTable;
  @Mock
  private CallStoredProcedure mockCallStoredProcedure;
  @Mock
  private User mockUser;

  private MockedStatic<OBDal> obDalStatic;
  private MockedStatic<OBContext> obContextStatic;
  private MockedStatic<OBMessageUtils> obMessageUtilsStatic;
  private MockedStatic<CallStoredProcedure> callStoredProcedureStatic;
  /** Sets up test fixtures. */

  @Before
  public void setUp() {
    ObjenesisStd objenesis = new ObjenesisStd();
    instance = objenesis.newInstance(CalculatePromotions.class);

    obDalStatic = mockStatic(OBDal.class);
    obDalStatic.when(OBDal::getInstance).thenReturn(mockOBDal);

    obContextStatic = mockStatic(OBContext.class);
    obContextStatic.when(OBContext::getOBContext).thenReturn(mockOBContext);
    obContextStatic.when(() -> OBContext.setAdminMode()).thenAnswer(inv -> null);
    obContextStatic.when(() -> OBContext.restorePreviousMode()).thenAnswer(inv -> null);

    obMessageUtilsStatic = mockStatic(OBMessageUtils.class);
    lenient().when(OBMessageUtils.messageBD(anyString())).thenReturn(SUCCESS);

    callStoredProcedureStatic = mockStatic(CallStoredProcedure.class);
    callStoredProcedureStatic.when(CallStoredProcedure::getInstance).thenReturn(mockCallStoredProcedure);

    lenient().when(mockOBContext.getUser()).thenReturn(mockUser);
    lenient().when(mockUser.getId()).thenReturn(TEST_USER_ID);
  }
  /** Tears down test fixtures. */

  @After
  public void tearDown() {
    if (obDalStatic != null) obDalStatic.close();
    if (obContextStatic != null) obContextStatic.close();
    if (obMessageUtilsStatic != null) obMessageUtilsStatic.close();
    if (callStoredProcedureStatic != null) callStoredProcedureStatic.close();
  }
  /**
   * Do execute with order table.
   * @throws Exception if an error occurs
   */

  @Test
  public void testDoExecuteWithOrderTable() throws Exception {
    setupBundleWithTableName("c_order_ID", TEST_ORDER_ID, "c_order");
    when(mockCallStoredProcedure.call(anyString(), anyList(), isNull(), eq(true), eq(false)))
        .thenReturn(null);

    invokeDoExecute();

    verify(mockCallStoredProcedure).call(eq("M_PROMOTION_CALCULATE"), anyList(), isNull(), eq(true), eq(false));
    assertBundleResultType(SUCCESS);
  }
  /**
   * Do execute with invoice table.
   * @throws Exception if an error occurs
   */

  @Test
  public void testDoExecuteWithInvoiceTable() throws Exception {
    setupBundleWithTableName("c_invoice_ID", TEST_INVOICE_ID, "c_invoice");
    when(mockCallStoredProcedure.call(anyString(), anyList(), isNull(), eq(true), eq(false)))
        .thenReturn(null);

    invokeDoExecute();

    verify(mockCallStoredProcedure).call(eq("M_PROMOTION_CALCULATE"), anyList(), isNull(), eq(true), eq(false));
    assertBundleResultType(SUCCESS);
  }
  /**
   * Do execute handles exception gracefully.
   * @throws Exception if an error occurs
   */

  @Test
  public void testDoExecuteHandlesExceptionGracefully() throws Exception {
    setupBundleWithTableName(null, null, "c_order");
    when(mockCallStoredProcedure.call(anyString(), anyList(), isNull(), eq(true), eq(false)))
        .thenThrow(new RuntimeException("DB error"));

    invokeDoExecute();

    ArgumentCaptor<OBError> captor = ArgumentCaptor.forClass(OBError.class);
    verify(mockBundle).setResult(captor.capture());
    assertEquals("Error", captor.getValue().getType());
    assertEquals("DB error", captor.getValue().getMessage());
  }

  private void setupBundleWithTableName(String paramKey, String paramValue, String tableName) {
    Map<String, Object> params = new HashMap<>();
    params.put(TAB_ID, TEST_TAB_ID);
    if (paramKey != null) {
      params.put(paramKey, paramValue);
    }
    when(mockBundle.getParams()).thenReturn(params);
    when(mockOBDal.get(Tab.class, TEST_TAB_ID)).thenReturn(mockTab);
    when(mockTab.getTable()).thenReturn(mockTable);
    when(mockTable.getDBTableName()).thenReturn(tableName);
  }

  private void invokeDoExecute() throws Exception {
    Method doExecute = CalculatePromotions.class.getDeclaredMethod(DO_EXECUTE, ProcessBundle.class);
    doExecute.setAccessible(true);
    doExecute.invoke(instance, mockBundle);
  }

  private void assertBundleResultType(String expectedType) {
    ArgumentCaptor<OBError> captor = ArgumentCaptor.forClass(OBError.class);
    verify(mockBundle).setResult(captor.capture());
    assertEquals(expectedType, captor.getValue().getType());
  }
}
