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

@RunWith(MockitoJUnitRunner.class)
public class CheckOptionalFilterCalloutTest {

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

  @After
  public void tearDown() {
    if (obDalStatic != null) obDalStatic.close();
    if (obContextStatic != null) obContextStatic.close();
    if (utilityStatic != null) utilityStatic.close();
  }

  private void invokeExecute() throws Exception {
    Method method = CheckOptionalFilterCallout.class.getDeclaredMethod("execute",
        SimpleCallout.CalloutInfo.class);
    method.setAccessible(true);
    method.invoke(instance, mockInfo);
  }

  @Test
  public void testExecuteWithNonCanBeFilteredFieldDoesNothing() throws Exception {
    when(mockInfo.getStringParameter("inpLastFieldChanged", null)).thenReturn("inpname");
    lenient().when(mockInfo.getStringParameter("inpname", null)).thenReturn("someValue");

    invokeExecute();

    verify(mockInfo, never()).addResult(anyString(), anyString());
  }

  @Test
  public void testExecuteWithCanBeFilteredNotYDoesNothing() throws Exception {
    when(mockInfo.getStringParameter("inpLastFieldChanged", null)).thenReturn("inpcanBeFiltered");
    when(mockInfo.getStringParameter("inpcanBeFiltered", null)).thenReturn("N");

    invokeExecute();

    verify(mockInfo, never()).addResult(anyString(), anyString());
  }

  @Test
  public void testExecuteWithCanBeFilteredYAndHqlContainsOptionalFilters() throws Exception {
    when(mockInfo.getStringParameter("inpLastFieldChanged", null)).thenReturn("inpcanBeFiltered");
    when(mockInfo.getStringParameter("inpcanBeFiltered", null)).thenReturn("Y");
    when(mockInfo.getStringParameter("inpobcqlQueryColumnId", null)).thenReturn(QUERY_COLUMN_ID);

    when(mockOBDal.exists("OBCQL_QueryColumn", QUERY_COLUMN_ID)).thenReturn(true);
    when(mockOBDal.get(OBCQL_QueryColumn.class, QUERY_COLUMN_ID)).thenReturn(mockQueryColumn);
    when(mockQueryColumn.getWidgetQuery()).thenReturn(mockWidgetQuery);
    when(mockWidgetQuery.getHQL()).thenReturn("SELECT e FROM Entity e WHERE @optional_filters@");

    invokeExecute();

    verify(mockInfo, never()).addResult(eq("WARNING"), anyString());
  }

  @Test
  public void testExecuteWithCanBeFilteredYAndHqlMissingOptionalFilters() throws Exception {
    when(mockInfo.getStringParameter("inpLastFieldChanged", null)).thenReturn("inpcanBeFiltered");
    when(mockInfo.getStringParameter("inpcanBeFiltered", null)).thenReturn("Y");
    when(mockInfo.getStringParameter("inpobcqlQueryColumnId", null)).thenReturn(QUERY_COLUMN_ID);

    when(mockOBDal.exists("OBCQL_QueryColumn", QUERY_COLUMN_ID)).thenReturn(true);
    when(mockOBDal.get(OBCQL_QueryColumn.class, QUERY_COLUMN_ID)).thenReturn(mockQueryColumn);
    when(mockQueryColumn.getWidgetQuery()).thenReturn(mockWidgetQuery);
    when(mockWidgetQuery.getHQL()).thenReturn("SELECT e FROM Entity e");

    invokeExecute();

    verify(mockInfo).addResult("WARNING", WARNING_MSG);
  }

  @Test
  public void testExecuteWithCanBeFilteredYAndEmptyHql() throws Exception {
    when(mockInfo.getStringParameter("inpLastFieldChanged", null)).thenReturn("inpcanBeFiltered");
    when(mockInfo.getStringParameter("inpcanBeFiltered", null)).thenReturn("Y");
    when(mockInfo.getStringParameter("inpobcqlQueryColumnId", null)).thenReturn(QUERY_COLUMN_ID);

    when(mockOBDal.exists("OBCQL_QueryColumn", QUERY_COLUMN_ID)).thenReturn(true);
    when(mockOBDal.get(OBCQL_QueryColumn.class, QUERY_COLUMN_ID)).thenReturn(mockQueryColumn);
    when(mockQueryColumn.getWidgetQuery()).thenReturn(mockWidgetQuery);
    when(mockWidgetQuery.getHQL()).thenReturn("");

    invokeExecute();

    verify(mockInfo, never()).addResult(eq("WARNING"), anyString());
  }

  @Test
  public void testExecuteWithCanBeFilteredYAndColumnNotFound() throws Exception {
    when(mockInfo.getStringParameter("inpLastFieldChanged", null)).thenReturn("inpcanBeFiltered");
    when(mockInfo.getStringParameter("inpcanBeFiltered", null)).thenReturn("Y");
    when(mockInfo.getStringParameter("inpobcqlQueryColumnId", null)).thenReturn(QUERY_COLUMN_ID);

    when(mockOBDal.exists("OBCQL_QueryColumn", QUERY_COLUMN_ID)).thenReturn(false);

    invokeExecute();

    verify(mockInfo, never()).addResult(anyString(), anyString());
  }

  @Test
  public void testExecuteWithCanBeFilteredYAndNullHql() throws Exception {
    when(mockInfo.getStringParameter("inpLastFieldChanged", null)).thenReturn("inpcanBeFiltered");
    when(mockInfo.getStringParameter("inpcanBeFiltered", null)).thenReturn("Y");
    when(mockInfo.getStringParameter("inpobcqlQueryColumnId", null)).thenReturn(QUERY_COLUMN_ID);

    when(mockOBDal.exists("OBCQL_QueryColumn", QUERY_COLUMN_ID)).thenReturn(true);
    when(mockOBDal.get(OBCQL_QueryColumn.class, QUERY_COLUMN_ID)).thenReturn(mockQueryColumn);
    when(mockQueryColumn.getWidgetQuery()).thenReturn(mockWidgetQuery);
    when(mockWidgetQuery.getHQL()).thenReturn(null);

    invokeExecute();

    verify(mockInfo, never()).addResult(eq("WARNING"), anyString());
  }
}
