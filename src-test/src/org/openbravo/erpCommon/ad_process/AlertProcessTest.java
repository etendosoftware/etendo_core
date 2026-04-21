package org.openbravo.erpCommon.ad_process;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessContext;
import org.openbravo.scheduling.ProcessLogger;

/**
 * Tests for {@link AlertProcess}.
 */
@SuppressWarnings({"java:S120"})
@RunWith(MockitoJUnitRunner.Silent.class)
public class AlertProcessTest {

  private static final String SYSTEM_CLIENT_ID = "0";
  private static final String NON_SYSTEM_CLIENT_ID = "1000000";

  private AlertProcess alertProcess;

  @Mock
  private ProcessBundle mockBundle;
  @Mock
  private ProcessLogger mockLogger;
  @Mock
  private ConnectionProvider mockConn;
  @Mock
  private ProcessContext mockContext;
  @Mock
  private OBDal mockOBDal;

  private MockedStatic<OBDal> obDalStatic;
  private MockedStatic<OBContext> obContextStatic;
  private MockedStatic<AlertProcessData> alertProcessDataStatic;
  private MockedStatic<SequenceIdData> sequenceIdDataStatic;
  private MockedStatic<Utility> utilityStatic;
  private MockedStatic<OBPropertiesProvider> propertiesStatic;
  /**
   * Sets up test fixtures.
   * @throws Exception if an error occurs
   */

  @Before
  public void setUp() throws Exception {
    ObjenesisStd objenesis = new ObjenesisStd();
    alertProcess = objenesis.newInstance(AlertProcess.class);

    obDalStatic = mockStatic(OBDal.class);
    obDalStatic.when(OBDal::getInstance).thenReturn(mockOBDal);

    obContextStatic = mockStatic(OBContext.class);
    alertProcessDataStatic = mockStatic(AlertProcessData.class);
    sequenceIdDataStatic = mockStatic(SequenceIdData.class);
    utilityStatic = mockStatic(Utility.class);

    lenient().when(mockBundle.getLogger()).thenReturn(mockLogger);
    lenient().when(mockBundle.getConnection()).thenReturn(mockConn);
    lenient().when(mockBundle.getContext()).thenReturn(mockContext);
    lenient().when(mockContext.getLanguage()).thenReturn("en_US");
  }
  /** Tears down test fixtures. */

  @After
  public void tearDown() {
    if (obDalStatic != null) obDalStatic.close();
    if (obContextStatic != null) obContextStatic.close();
    if (alertProcessDataStatic != null) alertProcessDataStatic.close();
    if (sequenceIdDataStatic != null) sequenceIdDataStatic.close();
    if (utilityStatic != null) utilityStatic.close();
    if (propertiesStatic != null) propertiesStatic.close();
  }
  /**
   * Execute with system client processes all clients.
   * @throws Exception if an error occurs
   */

  @Test
  public void testExecuteWithSystemClientProcessesAllClients() throws Exception {
    when(mockContext.getClient()).thenReturn(SYSTEM_CLIENT_ID);

    alertProcessDataStatic.when(() -> AlertProcessData.selectSQL(mockConn))
        .thenReturn(new AlertProcessData[0]);

    alertProcess.execute(mockBundle);

    alertProcessDataStatic.verify(() -> AlertProcessData.selectSQL(mockConn));
    verify(mockLogger).log("Starting Alert Backgrouond Process. Loop 0\n");
  }
  /**
   * Execute with non system client filters.
   * @throws Exception if an error occurs
   */

  @Test
  public void testExecuteWithNonSystemClientFilters() throws Exception {
    when(mockContext.getClient()).thenReturn(NON_SYSTEM_CLIENT_ID);

    alertProcessDataStatic.when(() -> AlertProcessData.selectSQL(mockConn, NON_SYSTEM_CLIENT_ID))
        .thenReturn(new AlertProcessData[0]);

    alertProcess.execute(mockBundle);

    alertProcessDataStatic.verify(() -> AlertProcessData.selectSQL(mockConn, NON_SYSTEM_CLIENT_ID));
  }
  /**
   * Execute with null alert rules.
   * @throws Exception if an error occurs
   */

  @Test
  public void testExecuteWithNullAlertRules() throws Exception {
    when(mockContext.getClient()).thenReturn(SYSTEM_CLIENT_ID);

    alertProcessDataStatic.when(() -> AlertProcessData.selectSQL(mockConn))
        .thenReturn(null);

    alertProcess.execute(mockBundle);

    verify(mockOBDal).commitAndClose();
  }
  /**
   * Execute with empty alert rules.
   * @throws Exception if an error occurs
   */

  @Test
  public void testExecuteWithEmptyAlertRules() throws Exception {
    when(mockContext.getClient()).thenReturn(SYSTEM_CLIENT_ID);

    alertProcessDataStatic.when(() -> AlertProcessData.selectSQL(mockConn))
        .thenReturn(new AlertProcessData[0]);

    alertProcess.execute(mockBundle);

    verify(mockOBDal).commitAndClose();
  }
  /**
   * Process alert with empty sql.
   * @throws Exception if an error occurs
   */

  @Test
  public void testProcessAlertWithEmptySql() throws Exception {
    when(mockContext.getClient()).thenReturn(SYSTEM_CLIENT_ID);

    AlertProcessData ruleData = createAlertProcessData("RULE1", "", "Test Rule");

    alertProcessDataStatic.when(() -> AlertProcessData.selectSQL(mockConn))
        .thenReturn(new AlertProcessData[] { ruleData });

    alertProcess.execute(mockBundle);

    verify(mockLogger).log("Processing rule Test Rule\n");
  }
  /**
   * Process alert with non select sql.
   * @throws Exception if an error occurs
   */

  @Test
  public void testProcessAlertWithNonSelectSql() throws Exception {
    when(mockContext.getClient()).thenReturn(SYSTEM_CLIENT_ID);

    AlertProcessData ruleData = createAlertProcessData("RULE1", "UPDATE something SET x=1", "Test Rule");

    alertProcessDataStatic.when(() -> AlertProcessData.selectSQL(mockConn))
        .thenReturn(new AlertProcessData[] { ruleData });
    utilityStatic.when(() -> Utility.messageBD(eq(mockConn), eq("AlertSelectConstraint"), anyString()))
        .thenReturn("SQL must start with SELECT");

    alertProcess.execute(mockBundle);

    verify(mockLogger).log("SQL must start with SELECT \n");
  }

  private AlertProcessData createAlertProcessData(String ruleId, String sql, String name) {
    ObjenesisStd objenesis = new ObjenesisStd();
    AlertProcessData data = objenesis.newInstance(AlertProcessData.class);
    data.adAlertruleId = ruleId;
    data.sql = sql;
    data.name = name;
    data.adClientId = SYSTEM_CLIENT_ID;
    data.adOrgId = "0";
    return data;
  }
}
