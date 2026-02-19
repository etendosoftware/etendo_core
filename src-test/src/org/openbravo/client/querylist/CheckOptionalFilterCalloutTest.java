package org.openbravo.client.querylist;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.lang.reflect.InvocationTargetException;

import java.lang.reflect.Method;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.system.Language;
/** Tests for {@link CheckOptionalFilterCallout}. */
@SuppressWarnings("java:S112")

@RunWith(MockitoJUnitRunner.class)
public class CheckOptionalFilterCalloutTest {

  private static final String INP_LAST_FIELD_CHANGED = "inpLastFieldChanged";
  private static final String INPCAN_BE_FILTERED = "inpcanBeFiltered";
  private static final String INPOBCQL_QUERY_COLUMN_ID = "inpobcqlQueryColumnId";
  private static final String OBCQL_QUERY_COLUMN = "OBCQL_QueryColumn";
  private static final String WARNING = "WARNING";

  private static final String QUERY_COLUMN_ID = "TEST_QC_001";
  private static final String WARNING_MSG = "Warning: optional filters missing";

  private CheckOptionalFilterCallout instance;

  @Mock
  private SimpleCallout.CalloutInfo mockInfo;

  @Mock
  private OBDal mockOBDal;

  @Mock
  private OBContext mockOBContext;

  @Mock
  private Language mockLanguage;

  @Mock
  private OBCQL_QueryColumn mockQueryColumn;

  @Mock
  private OBCQL_WidgetQuery mockWidgetQuery;

  private MockedStatic<OBDal> obDalStatic;
  private MockedStatic<OBContext> obContextStatic;
  private MockedStatic<Utility> utilityStatic;
  /** Sets up test fixtures. */

  @Before
  public void setUp() {
    ObjenesisStd objenesis = new ObjenesisStd();
    instance = objenesis.newInstance(CheckOptionalFilterCallout.class);

    obDalStatic = mockStatic(OBDal.class);
    obDalStatic.when(OBDal::getInstance).thenReturn(mockOBDal);

    obContextStatic = mockStatic(OBContext.class);
    obContextStatic.when(OBContext::getOBContext).thenReturn(mockOBContext);

    lenient().when(mockOBContext.getLanguage()).thenReturn(mockLanguage);
    lenient().when(mockLanguage.getId()).thenReturn("en_US");

    utilityStatic = mockStatic(Utility.class);
    lenient().when(Utility.messageBD(any(), anyString(), anyString())).thenReturn(WARNING_MSG);
  }
  /** Tears down test fixtures. */

  @After
  public void tearDown() {
    if (obDalStatic != null) obDalStatic.close();
    if (obContextStatic != null) obContextStatic.close();
    if (utilityStatic != null) utilityStatic.close();
  }

  private void invokeExecute() throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    Method method = CheckOptionalFilterCallout.class.getDeclaredMethod("execute",
        SimpleCallout.CalloutInfo.class);
    method.setAccessible(true);
    method.invoke(instance, mockInfo);
  }
  /**
   * Execute with non can be filtered field does nothing.
   * @throws Exception if an error occurs
   */

  @Test
  public void testExecuteWithNonCanBeFilteredFieldDoesNothing() throws Exception {
    when(mockInfo.getStringParameter(INP_LAST_FIELD_CHANGED, null)).thenReturn("inpname");
    lenient().when(mockInfo.getStringParameter("inpname", null)).thenReturn("someValue");

    invokeExecute();

    verify(mockInfo, never()).addResult(anyString(), anyString());
  }
  /**
   * Execute with can be filtered not y does nothing.
   * @throws Exception if an error occurs
   */

  @Test
  public void testExecuteWithCanBeFilteredNotYDoesNothing() throws Exception {
    when(mockInfo.getStringParameter(INP_LAST_FIELD_CHANGED, null)).thenReturn(INPCAN_BE_FILTERED);
    when(mockInfo.getStringParameter(INPCAN_BE_FILTERED, null)).thenReturn("N");

    invokeExecute();

    verify(mockInfo, never()).addResult(anyString(), anyString());
  }
  /**
   * Execute with can be filtered y and hql contains optional filters.
   * @throws Exception if an error occurs
   */

  @Test
  public void testExecuteWithCanBeFilteredYAndHqlContainsOptionalFilters() throws Exception {
    setupCanBeFilteredYWithHql("SELECT e FROM Entity e WHERE @optional_filters@");

    invokeExecute();

    verify(mockInfo, never()).addResult(eq(WARNING), anyString());
  }
  /**
   * Execute with can be filtered y and hql missing optional filters.
   * @throws Exception if an error occurs
   */

  @Test
  public void testExecuteWithCanBeFilteredYAndHqlMissingOptionalFilters() throws Exception {
    setupCanBeFilteredYWithHql("SELECT e FROM Entity e");

    invokeExecute();

    verify(mockInfo).addResult(WARNING, WARNING_MSG);
  }
  /**
   * Execute with can be filtered y and empty hql.
   * @throws Exception if an error occurs
   */

  @Test
  public void testExecuteWithCanBeFilteredYAndEmptyHql() throws Exception {
    setupCanBeFilteredYWithHql("");

    invokeExecute();

    verify(mockInfo, never()).addResult(eq(WARNING), anyString());
  }
  /**
   * Execute with can be filtered y and column not found.
   * @throws Exception if an error occurs
   */

  @Test
  public void testExecuteWithCanBeFilteredYAndColumnNotFound() throws Exception {
    setupCanBeFilteredYParams();
    when(mockOBDal.exists(OBCQL_QUERY_COLUMN, QUERY_COLUMN_ID)).thenReturn(false);

    invokeExecute();

    verify(mockInfo, never()).addResult(anyString(), anyString());
  }
  /**
   * Execute with can be filtered y and null hql.
   * @throws Exception if an error occurs
   */

  @Test
  public void testExecuteWithCanBeFilteredYAndNullHql() throws Exception {
    setupCanBeFilteredYWithHql(null);

    invokeExecute();

    verify(mockInfo, never()).addResult(eq(WARNING), anyString());
  }

  private void setupCanBeFilteredYParams() {
    when(mockInfo.getStringParameter(INP_LAST_FIELD_CHANGED, null)).thenReturn(INPCAN_BE_FILTERED);
    when(mockInfo.getStringParameter(INPCAN_BE_FILTERED, null)).thenReturn("Y");
    when(mockInfo.getStringParameter(INPOBCQL_QUERY_COLUMN_ID, null)).thenReturn(QUERY_COLUMN_ID);
  }

  private void setupCanBeFilteredYWithHql(String hql) {
    setupCanBeFilteredYParams();
    when(mockOBDal.exists(OBCQL_QUERY_COLUMN, QUERY_COLUMN_ID)).thenReturn(true);
    when(mockOBDal.get(OBCQL_QueryColumn.class, QUERY_COLUMN_ID)).thenReturn(mockQueryColumn);
    when(mockQueryColumn.getWidgetQuery()).thenReturn(mockWidgetQuery);
    when(mockWidgetQuery.getHQL()).thenReturn(hql);
  }
}
